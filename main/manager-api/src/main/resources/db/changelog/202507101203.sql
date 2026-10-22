-- OpenAI ASR model provider
delete from `ai_model_provider` where id = 'SYSTEM_ASR_OpenaiASR';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_ASR_OpenaiASR', 'ASR', 'openai', 'OpenAI Speech Recognition', '[{"key": "base_url", "type": "string", "label": "Base URL"}, {"key": "model_name", "type": "string", "label": "Model Name"}, {"key": "api_key", "type": "string", "label": "API Key"}, {"key": "output_dir", "type": "string", "label": "Output Directory"}]', 9, 1, NOW(), 1, NOW());


-- OpenAI ASR model config
delete from `ai_model_config` where id = 'ASR_OpenaiASR';
INSERT INTO `ai_model_config` VALUES ('ASR_OpenaiASR', 'ASR', 'OpenaiASR', 'OpenAI Speech Recognition', 0, 1, '{\"type\": \"openai\", \"api_key\": \"\", \"base_url\": \"https://api.openai.com/v1/audio/transcriptions\", \"model_name\": \"gpt-4o-mini-transcribe\", \"output_dir\": \"tmp/\"}', NULL, NULL, 9, NULL, NULL, NULL, NULL);

-- Groq ASR model config
delete from `ai_model_config` where id = 'ASR_GroqASR';
INSERT INTO `ai_model_config` VALUES ('ASR_GroqASR', 'ASR', 'GroqASR', 'Groq Speech Recognition', 0, 1, '{\"type\": \"openai\", \"api_key\": \"\", \"base_url\": \"https://api.groq.com/openai/v1/audio/transcriptions\", \"model_name\": \"whisper-large-v3-turbo\", \"output_dir\": \"tmp/\"}', NULL, NULL, 10, NULL, NULL, NULL, NULL);


-- Update OpenAI ASR config notes
UPDATE `ai_model_config` SET
`doc_link` = 'https://platform.openai.com/docs/api-reference/audio/createTranscription',
`remark` = 'OpenAI ASR configuration notes:
1. You need to create an organization on the OpenAI platform and obtain an api_key
2. Supports recognition of multiple languages including Chinese, English, Japanese, Korean and more. See the documentation for details: https://platform.openai.com/docs/guides/speech-to-text
3. Network connection required
4. Output files are saved in the tmp/ directory
Sign-up steps:
**OpenAI ASR sign-up steps:**
1. Log in to the OpenAI Platform. https://auth.openai.com/log-in
2. Create an api-key  https://platform.openai.com/settings/organization/api-keys
3. You can choose the gpt-4o-transcribe or GPT-4o mini Transcribe model
' WHERE `id` = 'ASR_OpenaiASR';

-- Update Groq ASR config notes
UPDATE `ai_model_config` SET
`doc_link` = 'https://console.groq.com/docs/speech-to-text',
`remark` = 'Groq ASR configuration notes:
1. Log in to the Groq Console. https://console.groq.com/home
2. Create an api-key  https://console.groq.com/keys
3. You can choose the whisper-large-v3-turbo or whisper-large-v3 model (distil-whisper-large-v3-en supports English transcription only)
' WHERE `id` = 'ASR_GroqASR';