"""Per-turn latency log: speech end → silence detected → text → first LLM token → first audio.

One INFO line per turn. Disable with the environment variable XIAOZHI_TURN_TIMING=0.
"""

import os
import time

from config.logger import setup_logging

TAG = __name__
logger = setup_logging()

ENABLED = os.environ.get("XIAOZHI_TURN_TIMING", "1") != "0"

STAGES = ("speech_end", "vad_stop", "text", "llm_first", "audio_first")


def start(conn, speech_end=None):
    """New turn: the user stopped talking at speech_end (seconds), the VAD noticed now."""
    if not ENABLED:
        return
    now = time.time()
    speech_end = speech_end or now
    timing = getattr(conn, "turn_timing", None)
    # The VAD can report the same pause on several frames; keep the first report
    if timing and "text" not in timing and abs(timing["speech_end"] - speech_end) < 0.001:
        return
    conn.turn_timing = {"speech_end": speech_end, "vad_stop": now}


def mark(conn, stage):
    """Record a stage once per turn; the first-audio stage writes the log line."""
    if not ENABLED:
        return
    timing = getattr(conn, "turn_timing", None)
    if not timing or stage in timing:
        return
    timing[stage] = time.time()
    if stage == "audio_first":
        logger.bind(tag=TAG).info(format_line(timing))
        conn.turn_timing = None


def format_line(timing):
    def gap(a, b):
        if a in timing and b in timing:
            return f"{(timing[b] - timing[a]) * 1000:.0f} ms"
        return "-"

    return (
        f"Turn timing: silence wait {gap('speech_end', 'vad_stop')} | ASR {gap('vad_stop', 'text')}"
        f" | LLM first token {gap('text', 'llm_first')} | TTS first audio {gap('llm_first', 'audio_first')}"
        f" | total {gap('speech_end', 'audio_first')}"
    )
