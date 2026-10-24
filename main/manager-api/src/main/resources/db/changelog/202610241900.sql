-- Add English Edge TTS voices (US and UK)
delete from `ai_tts_voice` where id in ('TTS_EdgeTTS0012', 'TTS_EdgeTTS0013', 'TTS_EdgeTTS0014', 'TTS_EdgeTTS0015', 'TTS_EdgeTTS0016', 'TTS_EdgeTTS0017', 'TTS_EdgeTTS0018', 'TTS_EdgeTTS0019', 'TTS_EdgeTTS0020', 'TTS_EdgeTTS0021');
INSERT INTO `ai_tts_voice` (`id`, `tts_model_id`, `name`, `tts_voice`, `languages`, `voice_demo`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('TTS_EdgeTTS0012', 'TTS_EdgeTTS', 'EdgeTTS Female - Aria (US)', 'en-US-AriaNeural', 'American English', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0013', 'TTS_EdgeTTS', 'EdgeTTS Female - Jenny (US)', 'en-US-JennyNeural', 'American English', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0014', 'TTS_EdgeTTS', 'EdgeTTS Female - Emma (US)', 'en-US-EmmaNeural', 'American English', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0015', 'TTS_EdgeTTS', 'EdgeTTS Male - Guy (US)', 'en-US-GuyNeural', 'American English', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0016', 'TTS_EdgeTTS', 'EdgeTTS Male - Andrew (US)', 'en-US-AndrewNeural', 'American English', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0017', 'TTS_EdgeTTS', 'EdgeTTS Male - Brian (US)', 'en-US-BrianNeural', 'American English', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0018', 'TTS_EdgeTTS', 'EdgeTTS Child - Ana (US)', 'en-US-AnaNeural', 'American English', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0019', 'TTS_EdgeTTS', 'EdgeTTS Female - Sonia (UK)', 'en-GB-SoniaNeural', 'British English', NULL, NULL, 3, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0020', 'TTS_EdgeTTS', 'EdgeTTS Female - Libby (UK)', 'en-GB-LibbyNeural', 'British English', NULL, NULL, 3, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0021', 'TTS_EdgeTTS', 'EdgeTTS Male - Ryan (UK)', 'en-GB-RyanNeural', 'British English', NULL, NULL, 3, NULL, NULL, NULL, NULL);
