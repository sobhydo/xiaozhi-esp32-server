"""ElevenLabs Scribe v2 Realtime streaming speech recognition.

Audio is streamed to ElevenLabs while the user is talking, so the transcript is
ready (almost) the moment they stop. One websocket per utterance:

  voice detected -> connect (xi-api-key header) -> wait for ``session_started``
  -> stream PCM 16 kHz chunks (``input_audio_chunk``, base64)
  -> local VAD says speech ended (or device sends listen/stop in manual mode)
  -> send an empty chunk with ``commit: true``
  -> ``committed_transcript`` -> handle_voice_stop -> startToChat -> close socket.

``commit_strategy=manual`` is used so we commit as soon as our own VAD reports
end of speech instead of waiting for ElevenLabs' server-side silence detection.

Docs:
  https://elevenlabs.io/docs/api-reference/speech-to-text/v-1-speech-to-text-realtime
  https://elevenlabs.io/docs/eleven-api/guides/how-to/speech-to-text/realtime/server-side-streaming
  https://elevenlabs.io/docs/eleven-api/guides/how-to/speech-to-text/realtime/transcripts-and-commit-strategies
"""

import json
import time
import base64
import asyncio
from typing import List, Optional, Tuple, TYPE_CHECKING
from urllib.parse import urlencode, urlparse

import websockets

from config.logger import setup_logging
from core.providers.asr.base import ASRProviderBase
from core.providers.asr.dto.dto import InterfaceType

if TYPE_CHECKING:
    from core.connection import ConnectionHandler

TAG = __name__
logger = setup_logging()

DEFAULT_BASE_URL = "wss://api.elevenlabs.io/v1/speech-to-text/realtime"
REALTIME_PATH = "/v1/speech-to-text/realtime"
DEFAULT_MODEL_ID = "scribe_v2_realtime"
SAMPLE_RATE = 16000
AUDIO_FORMAT = "pcm_16000"

# Device frames are 60 ms (1920 bytes of 16-bit PCM). ElevenLabs recommends
# 0.1-1 s chunks, so frames are batched to >= ~120 ms before sending.
MIN_CHUNK_BYTES = 3840
# How long to wait for the committed transcript after we commit.
COMMIT_TIMEOUT_SECS = 5.0
# How long to wait for session_started after connecting.
SESSION_START_TIMEOUT_SECS = 5.0
# Back-off before reconnecting after a failed connect.
CONNECT_RETRY_SECS = 2.0

ERROR_TYPES = {
    "error",
    "auth_error",
    "quota_exceeded",
    "commit_throttled",
    "unaccepted_terms",
    "rate_limited",
    "queue_overflow",
    "resource_exhausted",
    "session_time_limit_exceeded",
    "input_error",
    "invalid_request",
    "chunk_size_exceeded",
    "insufficient_audio_activity",
    "transcriber_error",
}


# ---------------------------------------------------------------------------
# Pure helpers (unit-tested, no network)
# ---------------------------------------------------------------------------
def build_ws_url(base_url: Optional[str], model_id: str, language_code: str = "") -> str:
    """Build the realtime websocket URL with query params.

    ``base_url`` may be empty (official endpoint), a full realtime URL, or just a
    host such as ``https://api.eu.residency.elevenlabs.io`` (path is appended and
    http(s) is mapped to ws(s)).
    """
    url = (base_url or "").strip() or DEFAULT_BASE_URL
    if url.startswith("https://"):
        url = "wss://" + url[len("https://"):]
    elif url.startswith("http://"):
        url = "ws://" + url[len("http://"):]
    elif "://" not in url:
        url = "wss://" + url
    parsed = urlparse(url)
    if "speech-to-text/realtime" not in parsed.path:
        url = url.split("?", 1)[0].rstrip("/") + REALTIME_PATH

    params = {
        "model_id": model_id or DEFAULT_MODEL_ID,
        "audio_format": AUDIO_FORMAT,
        "commit_strategy": "manual",
    }
    if language_code:
        params["language_code"] = language_code
    sep = "&" if "?" in url else "?"
    return url + sep + urlencode(params)


def build_headers(api_key: str) -> dict:
    return {"xi-api-key": api_key}


def encode_audio_chunk(pcm: bytes, commit: bool = False) -> str:
    """Encode one ``input_audio_chunk`` client message."""
    return json.dumps(
        {
            "message_type": "input_audio_chunk",
            "audio_base_64": base64.b64encode(pcm).decode("ascii") if pcm else "",
            "commit": bool(commit),
            "sample_rate": SAMPLE_RATE,
        }
    )


def parse_event(raw) -> Tuple[str, str]:
    """Parse a server message into ``(kind, text)``.

    kind is one of: ``session_started``, ``partial``, ``committed``, ``error``,
    ``ignore``. For ``error`` the text is ``"<type>: <message>"``.
    """
    try:
        if isinstance(raw, (bytes, bytearray)):
            raw = raw.decode("utf-8")
        msg = json.loads(raw)
    except Exception:
        return "ignore", ""
    if not isinstance(msg, dict):
        return "ignore", ""
    mtype = msg.get("message_type", "")
    if mtype == "session_started":
        return "session_started", msg.get("session_id", "") or ""
    if mtype == "partial_transcript":
        return "partial", msg.get("text", "") or ""
    if mtype in ("committed_transcript", "committed_transcript_with_timestamps"):
        return "committed", msg.get("text", "") or ""
    if mtype in ERROR_TYPES or ("error" in msg and mtype not in ("warning",)):
        return "error", f"{mtype or 'error'}: {msg.get('error', '')}"
    return "ignore", ""


def join_segments(segments: List[str]) -> str:
    return " ".join(s.strip() for s in segments if s and s.strip()).strip()


# ---------------------------------------------------------------------------
# Provider
# ---------------------------------------------------------------------------
class ASRProvider(ASRProviderBase):
    def __init__(self, config, delete_audio_file):
        super().__init__()
        self.interface_type = InterfaceType.STREAM
        self.config = config
        self.api_key = config.get("api_key")
        if not self.api_key:
            raise ValueError("ElevenLabs streaming ASR requires api_key")
        self.model_id = config.get("model_id") or DEFAULT_MODEL_ID
        # "" = auto-detect; "en", "ar"... to force one language
        self.language_code = (config.get("language_code") or "").strip()
        self.base_url = config.get("base_url") or ""
        self.ws_url = build_ws_url(self.base_url, self.model_id, self.language_code)
        self.output_dir = config.get("output_dir", "tmp/")
        self.delete_audio_file = delete_audio_file

        self.text = ""
        self.asr_ws = None
        self.forward_task = None
        self._conn = None
        self._retry_after = 0.0
        self._reset_session_state()

    def _reset_session_state(self):
        self.is_processing = False  # a session (websocket) is active for this utterance
        self.server_ready = False  # session_started received
        self.commit_sent = False
        self._sent_frames = 0  # how many frames of conn.asr_audio were already queued
        self._pending = bytearray()  # batched PCM not yet sent
        self._segments: List[str] = []
        self._partial = ""
        self._commit_time = 0.0
        self._finishing = False
        self.stop_requested = False

    async def open_audio_channels(self, conn: "ConnectionHandler"):
        await super().open_audio_channels(conn)

    # -- audio in -----------------------------------------------------------
    async def receive_audio(self, conn: "ConnectionHandler", pcm_frame, audio_have_voice):
        await super().receive_audio(conn, pcm_frame, audio_have_voice)

        # VAD ended an utterance but no session is running (e.g. connect failed):
        # reset so the next utterance starts cleanly.
        if (
            conn.client_listen_mode != "manual"
            and conn.client_voice_stop
            and not self.is_processing
        ):
            conn.reset_audio_states()
            return

        if (
            audio_have_voice
            and self.asr_ws is None
            and not self.is_processing
            and time.monotonic() >= self._retry_after
        ):
            try:
                await self._start_recognition(conn)
            except Exception as e:
                logger.bind(tag=TAG).error(f"ElevenLabs realtime connect failed: {e}")
                # Don't retry on every 60 ms frame (e.g. bad key -> HTTP 401).
                self._retry_after = time.monotonic() + CONNECT_RETRY_SECS
                await self._cleanup()
                return

        if (
            not (self.asr_ws and self.is_processing and self.server_ready)
            or self.commit_sent
            or self._finishing
        ):
            return

        try:
            await self._flush_frames(conn, force=False)
            # Auto mode: our VAD says the user stopped -> commit right away.
            if conn.client_listen_mode != "manual" and conn.client_voice_stop:
                await self._commit(conn)
        except Exception as e:
            logger.bind(tag=TAG).warning(f"ElevenLabs realtime send failed: {e}")
            await self._finish(conn, error=True)

    async def _flush_frames(self, conn: "ConnectionHandler", force: bool):
        """Queue frames of conn.asr_audio not sent yet and send in >=MIN_CHUNK_BYTES chunks.

        Runs only from the serialized receive_audio path (or commit), so ordering is kept.
        """
        frames = conn.asr_audio
        if self._sent_frames > len(frames):  # buffer was reset under us
            self._sent_frames = len(frames)
        for frame in frames[self._sent_frames:]:
            self._pending.extend(frame)
        self._sent_frames = len(frames)
        if self._pending and (force or len(self._pending) >= MIN_CHUNK_BYTES):
            data = bytes(self._pending)
            self._pending.clear()
            await self.asr_ws.send(encode_audio_chunk(data))

    async def _commit(self, conn: "ConnectionHandler"):
        if self.commit_sent or not self.asr_ws:
            return
        # Mark first so a concurrent caller (receive_audio vs. forward task)
        # can't send a second commit while we await the flush.
        self.commit_sent = True
        self._commit_time = time.monotonic()
        await self._flush_frames(conn, force=True)
        await self.asr_ws.send(encode_audio_chunk(b"", commit=True))
        logger.bind(tag=TAG).debug("ElevenLabs realtime: commit sent")

    async def _start_recognition(self, conn: "ConnectionHandler"):
        self._reset_session_state()
        self._conn = conn
        self.is_processing = True
        logger.bind(tag=TAG).debug(
            f"Connecting to ElevenLabs realtime STT: {self.ws_url}"
        )
        self.asr_ws = await websockets.connect(
            self.ws_url,
            additional_headers=build_headers(self.api_key),
            max_size=10_000_000,
            ping_interval=None,
            ping_timeout=None,
            close_timeout=3,
            open_timeout=5,
        )
        # Frames before connect (pre-roll) are flushed after session_started.
        self._sent_frames = 0
        self.forward_task = asyncio.create_task(self._forward_results(conn))

    # -- results out --------------------------------------------------------
    async def _forward_results(self, conn: "ConnectionHandler"):
        ws = self.asr_ws
        connect_time = time.monotonic()
        try:
            while not conn.stop_event.is_set() and ws is self.asr_ws and ws is not None:
                if not self.server_ready and time.monotonic() - connect_time > SESSION_START_TIMEOUT_SECS:
                    logger.bind(tag=TAG).error("ElevenLabs realtime: session_started timeout")
                    await self._finish(conn, error=True)
                    return
                if self.commit_sent and time.monotonic() - self._commit_time > COMMIT_TIMEOUT_SECS:
                    logger.bind(tag=TAG).warning("ElevenLabs realtime: committed transcript timeout")
                    await self._finish(conn, error=True)
                    return
                try:
                    raw = await asyncio.wait_for(ws.recv(), timeout=0.5)
                except asyncio.TimeoutError:
                    continue

                kind, text = parse_event(raw)
                if kind == "session_started":
                    self.server_ready = True
                    logger.bind(tag=TAG).debug(f"ElevenLabs realtime session started: {text}")
                    voice_already_stopped = (
                        conn.client_listen_mode != "manual" and conn.client_voice_stop
                    )
                    if (self.stop_requested or voice_already_stopped) and not self.commit_sent:
                        # Speech ended before the session was ready: send it all and commit.
                        await self._commit(conn)
                elif kind == "partial":
                    self._partial = text
                elif kind == "committed":
                    if text.strip():
                        self._segments.append(text)
                    self._partial = ""
                    # Before our commit this is an auto-commit (~36 s of audio):
                    # keep accumulating. After our commit, the utterance is done.
                    if self.commit_sent:
                        await self._finish(conn)
                        return
                elif kind == "error":
                    if text.startswith("insufficient_audio_activity"):
                        logger.bind(tag=TAG).info(f"ElevenLabs realtime: {text}")
                    else:
                        logger.bind(tag=TAG).error(f"ElevenLabs realtime error: {text}")
                    await self._finish(conn, error=True)
                    return
        except websockets.ConnectionClosed as e:
            logger.bind(tag=TAG).info(f"ElevenLabs realtime connection closed: {e}")
            if self.commit_sent or self._segments:
                await self._finish(conn, error=True)
        except Exception as e:
            logger.bind(tag=TAG).error(f"ElevenLabs realtime result handling failed: {e}")
            if self.commit_sent or self._segments:
                await self._finish(conn, error=True)
        finally:
            if ws is self.asr_ws:
                await self._cleanup()
                conn.reset_audio_states()

    def final_text(self, error: bool = False) -> str:
        """Committed segments; on error/timeout also fall back to the last partial."""
        segments = list(self._segments)
        if error and self._partial.strip():
            segments.append(self._partial)
        return join_segments(segments)

    async def _finish(self, conn: "ConnectionHandler", error: bool = False):
        """Deliver the utterance result (possibly empty) and end the session."""
        if not self.is_processing or self._finishing:
            return
        # is_processing stays True until _cleanup so receive_audio can't open a
        # new session while the result is being handed to the chat pipeline.
        self._finishing = True
        self.text = self.final_text(error=error)
        audio = list(conn.asr_audio)
        if self.text:
            await self.handle_voice_stop(conn, audio)
        else:
            if self.commit_sent or error:
                logger.bind(tag=TAG).info("识别文本: (empty)")
            self.text = ""
        await self._cleanup()
        conn.reset_audio_states()

    async def _send_stop_request(self):
        """Manual mode (device listen/stop): commit what was streamed so far."""
        self.stop_requested = True
        conn = self._conn
        if not self.asr_ws or self.commit_sent or conn is None:
            return
        if not self.server_ready:
            # Commit happens as soon as session_started arrives.
            return
        try:
            await self._commit(conn)
            logger.bind(tag=TAG).debug("ElevenLabs realtime: commit sent (manual stop)")
        except Exception as e:
            logger.bind(tag=TAG).error(f"ElevenLabs realtime stop request failed: {e}")

    def stop_ws_connection(self):
        # Called by base.handle_voice_stop; _cleanup closes the socket afterwards.
        pass

    async def _cleanup(self):
        ws = self.asr_ws
        self.asr_ws = None
        self.forward_task = None
        self.is_processing = False
        self.server_ready = False
        self.commit_sent = False
        self._finishing = False
        self._pending.clear()
        self._conn = None
        if ws is not None:
            try:
                await asyncio.wait_for(ws.close(), timeout=2.0)
            except Exception as e:
                logger.bind(tag=TAG).debug(f"ElevenLabs realtime close failed: {e}")

    async def speech_to_text(self, opus_data, session_id, artifacts=None):
        result = self.text
        self.text = ""
        return result, None

    async def close(self):
        task = self.forward_task
        await self._cleanup()
        if task and not task.done():
            task.cancel()
            try:
                await task
            except (asyncio.CancelledError, Exception):
                pass
