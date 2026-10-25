-- English as the platform default: role templates (which new agents copy) and the Edge TTS fallback voice
UPDATE `ai_agent_template` SET `lang_code` = 'en', `language` = 'English';
UPDATE `ai_agent_template`
   SET `tts_voice_id` = 'TTS_EdgeTTS0013', `tts_language` = 'American English'
 WHERE `tts_model_id` = 'TTS_EdgeTTS'
   AND (`tts_voice_id` IS NULL OR `tts_voice_id` IN ('TTS_EdgeTTS0001', 'TTS_EdgeTTS0002', 'TTS_EdgeTTS0003', 'TTS_EdgeTTS0004', 'TTS_EdgeTTS0005', 'TTS_EdgeTTS0006', 'TTS_EdgeTTS0007', 'TTS_EdgeTTS0008', 'TTS_EdgeTTS0009', 'TTS_EdgeTTS0010', 'TTS_EdgeTTS0011'));
UPDATE `ai_model_config` SET `config_json` = JSON_SET(`config_json`, '$.voice', 'en-US-JennyNeural') WHERE `id` = 'TTS_EdgeTTS';
