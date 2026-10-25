-- Add Arabic Edge TTS voices (UAE, Saudi Arabia, Egypt)
delete from `ai_tts_voice` where id in ('TTS_EdgeTTS0022', 'TTS_EdgeTTS0023', 'TTS_EdgeTTS0024', 'TTS_EdgeTTS0025', 'TTS_EdgeTTS0026', 'TTS_EdgeTTS0027');
INSERT INTO `ai_tts_voice` (`id`, `tts_model_id`, `name`, `tts_voice`, `languages`, `voice_demo`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('TTS_EdgeTTS0022', 'TTS_EdgeTTS', 'EdgeTTS Female - Fatima (UAE)', 'ar-AE-FatimaNeural', 'Arabic (UAE)', NULL, NULL, 4, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0023', 'TTS_EdgeTTS', 'EdgeTTS Male - Hamdan (UAE)', 'ar-AE-HamdanNeural', 'Arabic (UAE)', NULL, NULL, 4, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0024', 'TTS_EdgeTTS', 'EdgeTTS Female - Zariyah (Saudi)', 'ar-SA-ZariyahNeural', 'Arabic (Saudi Arabia)', NULL, NULL, 4, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0025', 'TTS_EdgeTTS', 'EdgeTTS Male - Hamed (Saudi)', 'ar-SA-HamedNeural', 'Arabic (Saudi Arabia)', NULL, NULL, 4, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0026', 'TTS_EdgeTTS', 'EdgeTTS Female - Salma (Egypt)', 'ar-EG-SalmaNeural', 'Arabic (Egypt)', NULL, NULL, 4, NULL, NULL, NULL, NULL),
('TTS_EdgeTTS0027', 'TTS_EdgeTTS', 'EdgeTTS Male - Shakir (Egypt)', 'ar-EG-ShakirNeural', 'Arabic (Egypt)', NULL, NULL, 4, NULL, NULL, NULL, NULL);
