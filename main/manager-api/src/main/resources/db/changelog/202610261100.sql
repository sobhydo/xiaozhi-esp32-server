-- ElevenLabs Scribe v2 Realtime: streaming ASR, transcribes while the user is speaking
delete from `ai_model_provider` where id = 'SYSTEM_ASR_ElevenLabsStream';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_ASR_ElevenLabsStream', 'ASR', 'elevenlabs_stream', 'ElevenLabs Scribe Realtime (streaming)', '[{"key":"api_key","label":"API Key","type":"password"},{"key":"model_id","label":"Model ID","type":"string"},{"key":"language_code","label":"Language (empty = auto, e.g. en, ar)","type":"string"},{"key":"output_dir","label":"Output Directory","type":"string"}]', 11, 1, NOW(), 1, NOW());

-- Insert-only so a key entered later survives re-runs
INSERT IGNORE INTO `ai_model_config` (`id`, `model_type`, `model_code`, `model_name`, `is_default`, `is_enabled`, `config_json`, `doc_link`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('ASR_ElevenLabsStreamASR', 'ASR', 'ElevenLabsStreamASR', 'ElevenLabs Scribe Realtime (streaming, Arabic + English)', 0, 1,
 '{"type": "elevenlabs_stream", "api_key": "", "model_id": "scribe_v2_realtime", "language_code": "", "output_dir": "tmp/"}',
 'https://elevenlabs.io/app/settings/api-keys',
 'ElevenLabs Scribe Realtime notes:\n1. Streams audio while the user speaks, so the transcript is ready as soon as they stop\n2. The API key needs the Speech to Text permission\n3. Leave language empty to auto-detect English, Arabic and other languages',
 11, NULL, NOW(), NULL, NOW());
