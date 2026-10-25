import requests
from core.utils.util import check_model_key
from core.providers.tts.base import TTSProviderBase
from config.logger import setup_logging

TAG = __name__
logger = setup_logging()

API_BASE = "https://api.elevenlabs.io/v1"


class TTSProvider(TTSProviderBase):
    """ElevenLabs text-to-speech. Multilingual models speak English and Arabic with the same voice."""

    TTS_PARAM_CONFIG = [
        ("ttsRate", "speed", 0.7, 1.2, 1.0, lambda v: round(float(v), 2)),
    ]

    def __init__(self, config, delete_audio_file):
        super().__init__(config, delete_audio_file)
        self.api_key = config.get("api_key")
        self.api_base = (config.get("api_base") or API_BASE).rstrip("/")
        self.model_id = config.get("model_id") or "eleven_flash_v2_5"
        # A cloned or per-agent voice arrives as private_voice
        self.voice = config.get("private_voice") or config.get("voice") or "21m00Tcm4TlvDq8ikWAM"
        self.output_format = config.get("output_format") or "mp3_44100_128"
        self.audio_file_type = "mp3" if self.output_format.startswith("mp3") else "wav"

        speed = config.get("speed", "1.0")
        self.speed = float(speed) if speed else 1.0
        self.stability = float(config.get("stability") or 0.5)
        self.similarity_boost = float(config.get("similarity_boost") or 0.75)
        self._apply_percentage_params(config)

        self.output_file = config.get("output_dir", "tmp/")
        model_key_msg = check_model_key("TTS", self.api_key)
        if model_key_msg:
            logger.bind(tag=TAG).error(model_key_msg)

    async def text_to_speak(self, text, output_file):
        url = f"{self.api_base}/text-to-speech/{self.voice}"
        headers = {
            "xi-api-key": self.api_key,
            "Content-Type": "application/json",
            "Accept": "audio/mpeg" if self.audio_file_type == "mp3" else "audio/wav",
        }
        data = {
            "text": text,
            "model_id": self.model_id,
            "voice_settings": {
                "stability": self.stability,
                "similarity_boost": self.similarity_boost,
                "speed": self.speed,
            },
        }
        response = requests.post(
            url,
            params={"output_format": self.output_format},
            json=data,
            headers=headers,
            timeout=self.tts_timeout,
        )
        if response.status_code == 200:
            if output_file:
                with open(output_file, "wb") as audio_file:
                    audio_file.write(response.content)
            else:
                return response.content
        else:
            raise Exception(
                f"ElevenLabs TTS request failed: {response.status_code} - {response.text}"
            )
