import os
import time
from typing import List, Optional, Tuple

import requests

from config.logger import setup_logging
from core.providers.asr.base import ASRProviderBase
from core.providers.asr.dto.dto import InterfaceType

TAG = __name__
logger = setup_logging()

API_URL = "https://api.elevenlabs.io/v1/speech-to-text"


class ASRProvider(ASRProviderBase):
    """ElevenLabs Scribe speech recognition (auto-detects English, Arabic and 90+ languages)."""

    def __init__(self, config: dict, delete_audio_file: bool):
        self.interface_type = InterfaceType.NON_STREAM
        self.api_key = config.get("api_key")
        self.api_url = config.get("base_url") or API_URL
        self.model = config.get("model_id") or config.get("model_name") or "scribe_v2"
        # Empty = auto-detect; set e.g. "ara" or "eng" to force one language
        self.language_code = config.get("language_code") or ""
        self.output_dir = config.get("output_dir", "tmp/")
        self.delete_audio_file = delete_audio_file
        os.makedirs(self.output_dir, exist_ok=True)

    def requires_file(self) -> bool:
        return True

    async def speech_to_text(self, opus_data: List[bytes], session_id: str, artifacts=None) -> Tuple[Optional[str], Optional[str]]:
        file_path = None
        try:
            if artifacts is None:
                return "", None
            file_path = artifacts.file_path
            data = {"model_id": self.model, "tag_audio_events": "false"}
            if self.language_code:
                data["language_code"] = self.language_code

            with open(file_path, "rb") as audio_file:
                start_time = time.time()
                response = requests.post(
                    self.api_url,
                    headers={"xi-api-key": self.api_key},
                    data=data,
                    files={"file": audio_file},
                    timeout=30,
                )
                logger.bind(tag=TAG).debug(
                    f"ElevenLabs ASR took {time.time() - start_time:.3f}s | {response.text[:200]}"
                )

            if response.status_code == 200:
                return response.json().get("text", "").strip(), file_path
            raise Exception(f"ElevenLabs ASR request failed: {response.status_code} - {response.text}")
        except Exception as e:
            logger.bind(tag=TAG).error(f"Speech recognition failed: {e}")
            return "", None
