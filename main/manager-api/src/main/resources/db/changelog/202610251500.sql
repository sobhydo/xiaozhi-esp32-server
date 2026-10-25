-- ElevenLabs text-to-speech (English + Arabic with multilingual models) and voice cloning
delete from `ai_model_provider` where id = 'SYSTEM_TTS_ElevenLabs';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_TTS_ElevenLabs', 'TTS', 'elevenlabs', 'ElevenLabs', '[{"key":"api_key","label":"API Key","type":"password"},{"key":"model_id","label":"Model ID","type":"string"},{"key":"voice","label":"Default Voice ID","type":"string"},{"key":"speed","label":"Speed","type":"number"},{"key":"output_dir","label":"Output Directory","type":"string"}]', 12, 1, NOW(), 1, NOW());

delete from `ai_model_config` where id = 'TTS_ElevenLabs';
INSERT INTO `ai_model_config` (`id`, `model_type`, `model_code`, `model_name`, `is_default`, `is_enabled`, `config_json`, `doc_link`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('TTS_ElevenLabs', 'TTS', 'ElevenLabs', 'ElevenLabs Text-to-Speech', 0, 1,
 '{"type": "elevenlabs", "api_key": "", "model_id": "eleven_flash_v2_5", "voice": "21m00Tcm4TlvDq8ikWAM", "speed": 1, "output_dir": "tmp/"}',
 'https://elevenlabs.io/app/settings/api-keys',
 'ElevenLabs configuration notes:\n1. Create an API key at https://elevenlabs.io/app/settings/api-keys\n2. eleven_flash_v2_5 is fast and multilingual (English, Arabic and 30+ languages); eleven_multilingual_v2 sounds richer but is slower\n3. Voice cloning (Voice Clone page) needs an ElevenLabs plan that includes Instant Voice Cloning',
 14, NULL, NOW(), NULL, NOW());

-- ElevenLabs premade voices; multilingual models speak English and Arabic with each of them
delete from `ai_tts_voice` where tts_model_id = 'TTS_ElevenLabs';
INSERT INTO `ai_tts_voice` (`id`, `tts_model_id`, `name`, `tts_voice`, `languages`, `voice_demo`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('TTS_ElevenLabs0001', 'TTS_ElevenLabs', 'ElevenLabs Female - Rachel', '21m00Tcm4TlvDq8ikWAM', 'English, Arabic', NULL, NULL, 1, NULL, NULL, NULL, NULL),
('TTS_ElevenLabs0002', 'TTS_ElevenLabs', 'ElevenLabs Female - Sarah', 'EXAVITQu4vr4xnSDxMaL', 'English, Arabic', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_ElevenLabs0003', 'TTS_ElevenLabs', 'ElevenLabs Female - Charlotte', 'XB0fDUnXU5powFXDhCwa', 'English, Arabic', NULL, NULL, 3, NULL, NULL, NULL, NULL),
('TTS_ElevenLabs0004', 'TTS_ElevenLabs', 'ElevenLabs Male - Adam', 'pNInz6obpgDQGcFmaJgB', 'English, Arabic', NULL, NULL, 4, NULL, NULL, NULL, NULL),
('TTS_ElevenLabs0005', 'TTS_ElevenLabs', 'ElevenLabs Male - Daniel', 'onwK4e9ZLuTAKqWW03F9', 'English, Arabic', NULL, NULL, 5, NULL, NULL, NULL, NULL),
('TTS_ElevenLabs0006', 'TTS_ElevenLabs', 'ElevenLabs Male - Antoni', 'ErXwobaYiN019PkySvjV', 'English, Arabic', NULL, NULL, 6, NULL, NULL, NULL, NULL);

-- OpenAI TTS voices are multilingual, not Chinese-only
UPDATE `ai_tts_voice` SET `languages` = 'English, Arabic' WHERE `tts_model_id` = 'TTS_OpenAITTS';
