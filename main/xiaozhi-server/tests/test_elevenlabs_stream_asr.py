"""ElevenLabs Scribe v2 Realtime streaming ASR: URL/header building, message
encoding, event parsing and the per-utterance commit flow (fake websocket, no network)."""

import asyncio
import base64
import json
import threading
from urllib.parse import parse_qs, urlparse

import pytest

from core.providers.asr import elevenlabs_stream as mod
from core.providers.asr.dto.dto import InterfaceType
from core.providers.asr.elevenlabs_stream import (
    ASRProvider,
    build_headers,
    build_ws_url,
    encode_audio_chunk,
    join_segments,
    parse_event,
)


def make_provider(**overrides):
    cfg = {"api_key": "sk-test-key"}
    cfg.update(overrides)
    return ASRProvider(cfg, delete_audio_file=True)


def query(url):
    return {k: v[0] for k, v in parse_qs(urlparse(url).query).items()}


# -- URL / headers ----------------------------------------------------------
def test_default_url_and_query_params():
    url = build_ws_url("", "scribe_v2_realtime", "")
    assert url.startswith("wss://api.elevenlabs.io/v1/speech-to-text/realtime?")
    q = query(url)
    assert q == {
        "model_id": "scribe_v2_realtime",
        "audio_format": "pcm_16000",
        "commit_strategy": "manual",
    }
    assert "language_code" not in q  # auto-detect


def test_language_code_added_when_set():
    assert query(build_ws_url(None, "scribe_v2_realtime", "ar"))["language_code"] == "ar"


def test_base_url_host_only_gets_path_and_ws_scheme():
    url = build_ws_url("https://api.eu.residency.elevenlabs.io/", "scribe_v2_realtime")
    assert url.startswith("wss://api.eu.residency.elevenlabs.io/v1/speech-to-text/realtime?")


def test_full_base_url_kept():
    url = build_ws_url("wss://api.us.elevenlabs.io/v1/speech-to-text/realtime", "m")
    assert url.startswith("wss://api.us.elevenlabs.io/v1/speech-to-text/realtime?model_id=m")


def test_headers_use_xi_api_key_and_key_not_in_url():
    p = make_provider()
    assert build_headers("abc") == {"xi-api-key": "abc"}
    assert "sk-test-key" not in p.ws_url


def test_provider_defaults():
    p = make_provider()
    assert p.interface_type == InterfaceType.STREAM
    assert p.model_id == "scribe_v2_realtime"
    assert p.language_code == ""
    assert p.output_dir == "tmp/"


def test_missing_api_key_raises():
    with pytest.raises(ValueError):
        ASRProvider({}, delete_audio_file=True)


# -- message encoding -------------------------------------------------------
def test_encode_audio_chunk():
    msg = json.loads(encode_audio_chunk(b"\x01\x02\x03\x04"))
    assert msg == {
        "message_type": "input_audio_chunk",
        "audio_base_64": base64.b64encode(b"\x01\x02\x03\x04").decode(),
        "commit": False,
        "sample_rate": 16000,
    }


def test_encode_commit_chunk_is_empty_audio():
    msg = json.loads(encode_audio_chunk(b"", commit=True))
    assert msg["audio_base_64"] == "" and msg["commit"] is True


# -- event parsing ----------------------------------------------------------
def test_parse_events():
    assert parse_event(json.dumps({"message_type": "session_started", "session_id": "s1", "config": {}})) == (
        "session_started", "s1")
    assert parse_event(json.dumps({"message_type": "partial_transcript", "text": "hel"})) == ("partial", "hel")
    assert parse_event(json.dumps({"message_type": "committed_transcript", "text": "hello"})) == (
        "committed", "hello")
    assert parse_event(json.dumps({"message_type": "committed_transcript_with_timestamps",
                                   "text": "مرحبا", "language_code": "ar", "words": []})) == ("committed", "مرحبا")
    assert parse_event(json.dumps({"message_type": "auth_error", "error": "bad key"})) == (
        "error", "auth_error: bad key")
    assert parse_event(json.dumps({"message_type": "warning", "warning": "x"})) == ("ignore", "")
    assert parse_event(b"not json") == ("ignore", "")


def test_final_text_joins_segments_and_falls_back_to_partial_on_error():
    p = make_provider()
    p._segments = ["Hello there.", " How are you? "]
    p._partial = "I am"
    assert p.final_text() == "Hello there. How are you?"
    assert p.final_text(error=True) == "Hello there. How are you? I am"
    assert join_segments(["", "  "]) == ""


# -- utterance flow with a fake websocket -----------------------------------
class FakeWS:
    def __init__(self, reply_text="hello world", error=None):
        self.sent = []
        self.queue = asyncio.Queue()
        self.closed = False
        self.reply_text = reply_text
        self.queue.put_nowait(json.dumps({"message_type": "session_started", "session_id": "s", "config": {}}))
        self.error = error

    async def send(self, data):
        msg = json.loads(data)
        self.sent.append(msg)
        if msg["commit"]:
            if self.error:
                await self.queue.put(json.dumps({"message_type": self.error, "error": "nope"}))
            else:
                await self.queue.put(json.dumps({"message_type": "partial_transcript", "text": "hello"}))
                await self.queue.put(json.dumps({"message_type": "committed_transcript", "text": self.reply_text}))

    async def recv(self):
        return await self.queue.get()

    async def close(self):
        self.closed = True


class FakeConn:
    def __init__(self):
        self.asr_audio = []
        self.client_listen_mode = "auto"
        self.client_voice_stop = False
        self.client_have_voice = False
        self.stop_event = threading.Event()
        self.resets = 0

    def reset_audio_states(self):
        self.resets += 1
        self.client_voice_stop = False
        self.client_have_voice = False
        self.asr_audio.clear()


async def tick(delay=0.01):
    """Real yield to the loop (conftest replaces asyncio.sleep with a no-op)."""
    loop = asyncio.get_running_loop()
    fut = loop.create_future()
    loop.call_later(delay, fut.set_result, None)
    await fut


def run_utterance(monkeypatch, ws, frames=8):
    provider = make_provider(language_code="en")
    conn = FakeConn()
    conn.asr = provider
    captured = {}

    async def fake_connect(url, additional_headers=None, **kw):
        captured["url"] = url
        captured["headers"] = additional_headers
        return ws

    async def fake_voice_stop(c, audio):
        captured["text"], _ = await provider.speech_to_text(audio, "sid")
        captured["frames"] = len(audio)

    monkeypatch.setattr(mod.websockets, "connect", fake_connect)
    monkeypatch.setattr(provider, "handle_voice_stop", fake_voice_stop)

    async def scenario():
        frame = b"\x00\x01" * 960  # 60 ms of 16 kHz PCM
        for _ in range(frames):
            conn.client_have_voice = True
            await provider.receive_audio(conn, frame, True)
            await tick()  # let session_started be processed
        conn.client_voice_stop = True  # local VAD: user stopped talking
        await provider.receive_audio(conn, frame, False)
        await asyncio.wait_for(provider_done(provider), timeout=3)

    asyncio.run(scenario())
    return provider, conn, captured


async def provider_done(provider):
    while provider.is_processing:
        await tick()


def test_flow_streams_audio_commits_on_vad_stop_and_delivers_text(monkeypatch):
    ws = FakeWS("hello world")
    provider, conn, captured = run_utterance(monkeypatch, ws)
    assert captured["headers"] == {"xi-api-key": "sk-test-key"}
    assert query(captured["url"])["language_code"] == "en"
    assert captured["text"] == "hello world"
    audio_msgs = [m for m in ws.sent if not m["commit"]]
    commits = [m for m in ws.sent if m["commit"]]
    assert len(commits) == 1 and commits[0]["audio_base_64"] == ""
    sent_bytes = sum(len(base64.b64decode(m["audio_base_64"])) for m in audio_msgs)
    assert sent_bytes == 9 * 1920  # every frame, including the stop frame, was streamed
    assert all(len(base64.b64decode(m["audio_base_64"])) >= 1920 for m in audio_msgs)
    assert ws.closed and provider.asr_ws is None and not provider.is_processing
    assert conn.resets >= 1


def test_flow_error_yields_no_text_and_recovers(monkeypatch):
    ws = FakeWS(error="insufficient_audio_activity")
    provider, conn, captured = run_utterance(monkeypatch, ws, frames=3)
    # partial isn't sent in the error case, so no text -> handle_voice_stop not called
    assert "text" not in captured
    assert ws.closed and provider.asr_ws is None and not provider.is_processing
    assert conn.resets >= 1
