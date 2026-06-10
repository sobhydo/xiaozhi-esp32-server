-- Add Alibaba Cloud streaming TTS provider
delete from `ai_model_provider` where id = 'SYSTEM_TTS_AliyunStreamTTS';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_TTS_AliyunStreamTTS', 'TTS', 'aliyun_stream', 'Alibaba Cloud Speech Synthesis (Streaming)', '[{"key":"appkey","label":"App AppKey","type":"string"},{"key":"token","label":"Temporary Token","type":"string"},{"key":"access_key_id","label":"AccessKey ID","type":"string"},{"key":"access_key_secret","label":"AccessKey Secret","type":"string"},{"key":"host","label":"Service Address","type":"string"},{"key":"voice","label":"Default Voice","type":"string"},{"key":"format","label":"Audio Format","type":"string"},{"key":"sample_rate","label":"Sample Rate","type":"number"},{"key":"volume","label":"Volume","type":"number"},{"key":"speech_rate","label":"Speech Rate","type":"number"},{"key":"pitch_rate","label":"Pitch","type":"number"},{"key":"output_dir","label":"Output Directory","type":"string"}]', 15, 1, NOW(), 1, NOW());

-- Add Alibaba Cloud streaming TTS model configuration
delete from `ai_model_config` where id = 'TTS_AliyunStreamTTS';
INSERT INTO `ai_model_config` VALUES ('TTS_AliyunStreamTTS', 'TTS', 'AliyunStreamTTS', 'Alibaba Cloud Speech Synthesis (Streaming)', 0, 1, '{\"type\": \"aliyun_stream\", \"appkey\": \"\", \"token\": \"\", \"access_key_id\": \"\", \"access_key_secret\": \"\", \"host\": \"nls-gateway-cn-beijing.aliyuncs.com\", \"voice\": \"longxiaochun\", \"format\": \"pcm\", \"sample_rate\": 16000, \"volume\": 50, \"speech_rate\": 0, \"pitch_rate\": 0, \"output_dir\": \"tmp/\"}', NULL, NULL, 18, NULL, NULL, NULL, NULL);

-- Update Alibaba Cloud streaming TTS configuration notes
UPDATE `ai_model_config` SET
`doc_link` = 'https://nls-portal.console.aliyun.com/',
`remark` = 'Alibaba Cloud streaming TTS configuration notes:
1. The difference between Alibaba Cloud TTS and Alibaba Cloud (Streaming) TTS: Alibaba Cloud TTS performs one-time synthesis, while Alibaba Cloud (Streaming) TTS performs real-time streaming synthesis
2. Streaming TTS offers lower latency and better real-time performance, suitable for voice interaction scenarios
3. You need to create an application and obtain authentication credentials in the Alibaba Cloud Intelligent Speech Interaction console
4. Supports CosyVoice large-model voices for more natural sound quality
5. Supports real-time adjustment of volume, speech rate, pitch and other parameters
Application steps:
1. Visit https://nls-portal.console.aliyun.com/ to activate the Intelligent Speech Interaction service
2. Visit https://nls-portal.console.aliyun.com/applist to create a project and obtain the appkey
3. Visit https://nls-portal.console.aliyun.com/overview to obtain a temporary token (or configure access_key_id and access_key_secret for automatic retrieval)
4. For dynamic token management, configuring access_key_id and access_key_secret is recommended
5. You can choose servers in different regions such as Beijing or Shanghai to optimize latency
6. The voice parameter supports CosyVoice large-model voices such as longxiaochun, longyueyue, etc.
For more parameter configuration details, please refer to: https://help.aliyun.com/zh/isi/developer-reference/real-time-speech-synthesis
' WHERE `id` = 'TTS_AliyunStreamTTS';

-- Add Alibaba Cloud streaming TTS voices
delete from `ai_tts_voice` where tts_model_id = 'TTS_AliyunStreamTTS';
-- Gentle female voice series
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0001', 'TTS_AliyunStreamTTS', 'Longxiaochun - Gentle Sister', 'longxiaochun', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0002', 'TTS_AliyunStreamTTS', 'Longxiaoxia - Gentle Female Voice', 'longxiaoxia', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 2, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0003', 'TTS_AliyunStreamTTS', 'Longmei - Gentle Female Voice', 'longmei', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 3, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0004', 'TTS_AliyunStreamTTS', 'Longgui - Gentle Female Voice', 'longgui', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 4, NULL, NULL, NULL, NULL);
-- Mature female voice series
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0005', 'TTS_AliyunStreamTTS', 'Longyu - Mature Female Voice', 'longyu', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 5, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0006', 'TTS_AliyunStreamTTS', 'Longjiao - Mature Female Voice', 'longjiao', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 6, NULL, NULL, NULL, NULL);
-- Male voice series
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0007', 'TTS_AliyunStreamTTS', 'Longchen - Dubbing Male Voice', 'longchen', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 7, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0008', 'TTS_AliyunStreamTTS', 'Longxiu - Young Male Voice', 'longxiu', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 8, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0009', 'TTS_AliyunStreamTTS', 'Longcheng - Sunny Male Voice', 'longcheng', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 9, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0010', 'TTS_AliyunStreamTTS', 'Longzhe - Mature Male Voice', 'longzhe', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 10, NULL, NULL, NULL, NULL);
-- Professional broadcast series
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0011', 'TTS_AliyunStreamTTS', 'Bella2.0 - News Female Voice', 'loongbella', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 11, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0012', 'TTS_AliyunStreamTTS', 'Stella2.0 - Crisp Female Voice', 'loongstella', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 12, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0013', 'TTS_AliyunStreamTTS', 'Longshu - News Male Voice', 'longshu', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 13, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0014', 'TTS_AliyunStreamTTS', 'Longjing - Serious Female Voice', 'longjing', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 14, NULL, NULL, NULL, NULL);
-- Specialty voice series
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0015', 'TTS_AliyunStreamTTS', 'Longqi - Lively Child Voice', 'longqi', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 15, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0016', 'TTS_AliyunStreamTTS', 'Longhua - Lively Girl Voice', 'longhua', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 16, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0017', 'TTS_AliyunStreamTTS', 'Longwu - Silly Male Voice', 'longwu', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 17, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0018', 'TTS_AliyunStreamTTS', 'Longdachui - Humorous Male Voice', 'longdachui', 'Chinese and Chinese-English mixed', NULL, NULL, NULL, NULL, 18, NULL, NULL, NULL, NULL);
-- Cantonese series
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0019', 'TTS_AliyunStreamTTS', 'Longjiayi - Cantonese Female Voice', 'longjiayi', 'Cantonese and Cantonese-English mixed', NULL, NULL, NULL, NULL, 19, NULL, NULL, NULL, NULL);
INSERT INTO `ai_tts_voice` VALUES ('TTS_AliyunStreamTTS_0020', 'TTS_AliyunStreamTTS', 'Longtao - Cantonese Female Voice', 'longtao', 'Cantonese and Cantonese-English mixed', NULL, NULL, NULL, NULL, 20, NULL, NULL, NULL, NULL);
