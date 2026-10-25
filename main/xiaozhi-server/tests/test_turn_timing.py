"""Turn timing: one log line per turn with the gap between each stage."""

from types import SimpleNamespace

from core.utils import turn_timing


def test_format_line_reports_each_stage():
    line = turn_timing.format_line({
        "speech_end": 10.0, "vad_stop": 10.7, "text": 11.2, "llm_first": 11.9, "audio_first": 12.6,
    })
    assert line == (
        "Turn timing: silence wait 700 ms | ASR 500 ms | LLM first token 700 ms"
        " | TTS first audio 700 ms | total 2600 ms"
    )


def test_missing_stage_shows_dash():
    line = turn_timing.format_line({"speech_end": 1.0, "vad_stop": 1.5, "text": 2.0, "audio_first": 3.0})
    assert "LLM first token -" in line and "total 2000 ms" in line


def test_turn_lifecycle(monkeypatch):
    monkeypatch.setattr(turn_timing, "ENABLED", True)
    conn = SimpleNamespace()
    turn_timing.mark(conn, "text")  # no turn started yet: ignored
    assert getattr(conn, "turn_timing", None) is None

    turn_timing.start(conn, speech_end=100.0)
    first = dict(conn.turn_timing)
    turn_timing.start(conn, speech_end=100.0)  # same pause reported again: kept
    assert conn.turn_timing == first

    turn_timing.mark(conn, "text")
    turn_timing.mark(conn, "llm_first")
    stamp = conn.turn_timing["llm_first"]
    turn_timing.mark(conn, "llm_first")  # recorded once per turn
    assert conn.turn_timing["llm_first"] == stamp

    turn_timing.mark(conn, "audio_first")  # logs and ends the turn
    assert conn.turn_timing is None


def test_disabled_does_nothing(monkeypatch):
    monkeypatch.setattr(turn_timing, "ENABLED", False)
    conn = SimpleNamespace()
    turn_timing.start(conn, speech_end=1.0)
    assert getattr(conn, "turn_timing", None) is None
