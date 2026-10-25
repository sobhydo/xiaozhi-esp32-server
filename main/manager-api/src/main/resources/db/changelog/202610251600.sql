-- Anthropic Claude (native SDK provider), refreshed Gemini model, and ElevenLabs Scribe speech recognition

-- Claude
delete from `ai_model_provider` where id = 'SYSTEM_LLM_claude';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_LLM_claude', 'LLM', 'claude', 'Anthropic Claude', '[{"key":"api_key","label":"API Key","type":"password"},{"key":"model_name","label":"Model Name","type":"string"},{"key":"effort","label":"Effort (low/medium/high)","type":"string"},{"key":"max_tokens","label":"Max Tokens","type":"number"},{"key":"fallbacks","label":"Refusal fallback (default/off)","type":"string"},{"key":"base_url","label":"Base URL","type":"string"}]', 2, 1, NOW(), 1, NOW());

delete from `ai_model_config` where id = 'LLM_ClaudeLLM';
INSERT INTO `ai_model_config` (`id`, `model_type`, `model_code`, `model_name`, `is_default`, `is_enabled`, `config_json`, `doc_link`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('LLM_ClaudeLLM', 'LLM', 'ClaudeLLM', 'Anthropic Claude', 0, 1,
 '{"type": "claude", "api_key": "", "model_name": "claude-opus-5-5", "effort": "low", "max_tokens": 16000, "fallbacks": "default", "base_url": ""}',
 'https://platform.claude.com/',
 'Claude configuration notes:\n1. Create an API key in the Claude Console (platform.claude.com)\n2. Model: claude-opus-5-5 (most capable), claude-sonnet-5-5 (balanced) or claude-haiku-5-5 (fastest and lowest cost)\n3. Effort controls how long Claude thinks; low keeps voice replies quick\n4. Refusal fallback "default" lets the API retry a declined request on another Claude model',
 3, NULL, NOW(), NULL, NOW());

-- Gemini: gemini-2.0-flash is retired; move to a current Flash model (edit if your account lists a newer one)
UPDATE `ai_model_config` SET `config_json` = JSON_SET(`config_json`, '$.model_name', 'gemini-3.5-flash')
 WHERE `id` = 'LLM_GeminiLLM' AND JSON_UNQUOTE(JSON_EXTRACT(`config_json`, '$.model_name')) = 'gemini-2.0-flash';

-- ElevenLabs Scribe ASR
delete from `ai_model_provider` where id = 'SYSTEM_ASR_ElevenLabs';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_ASR_ElevenLabs', 'ASR', 'elevenlabs', 'ElevenLabs Scribe', '[{"key":"api_key","label":"API Key","type":"password"},{"key":"model_id","label":"Model ID","type":"string"},{"key":"language_code","label":"Language (empty = auto, e.g. ara, eng)","type":"string"},{"key":"output_dir","label":"Output Directory","type":"string"}]', 10, 1, NOW(), 1, NOW());

delete from `ai_model_config` where id = 'ASR_ElevenLabsASR';
INSERT INTO `ai_model_config` (`id`, `model_type`, `model_code`, `model_name`, `is_default`, `is_enabled`, `config_json`, `doc_link`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('ASR_ElevenLabsASR', 'ASR', 'ElevenLabsASR', 'ElevenLabs Scribe (Arabic + English)', 0, 1,
 '{"type": "elevenlabs", "api_key": "", "model_id": "scribe_v2", "language_code": "", "output_dir": "tmp/"}',
 'https://elevenlabs.io/app/settings/api-keys',
 'ElevenLabs Scribe notes:\n1. Uses the same API key as ElevenLabs Text-to-Speech\n2. Leave language empty to auto-detect English, Arabic and other languages',
 10, NULL, NOW(), NULL, NOW());
