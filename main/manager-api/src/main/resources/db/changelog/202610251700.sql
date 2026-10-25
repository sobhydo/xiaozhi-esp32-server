-- MBZUAI IFM models (OpenAI-compatible API at https://api.ifm.ai/v1): K2 Horizon and Jais 3 chat, Jais ASR
delete from `ai_model_config` where id in ('LLM_IFMK2Horizon', 'LLM_IFMJais3', 'ASR_IFMJaisASR');
INSERT INTO `ai_model_config` (`id`, `model_type`, `model_code`, `model_name`, `is_default`, `is_enabled`, `config_json`, `doc_link`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('LLM_IFMK2Horizon', 'LLM', 'IFMK2Horizon', 'MBZUAI K2 Horizon', 0, 1,
 '{"type": "openai", "base_url": "https://api.ifm.ai/v1", "model_name": "IFM/K2-Horizon-375B-A23B", "api_key": "", "max_tokens": 2000}',
 'https://docs.ifm.ai/docs/quickstart.md',
 'MBZUAI Institute of Foundation Models (IFM):\n1. Create an API key on the IFM platform (docs.ifm.ai)\n2. K2 Horizon: 512K context, English, Arabic and 30+ languages, tool calling\n3. A daily token cap applies per key',
 4, NULL, NOW(), NULL, NOW()),
('LLM_IFMJais3', 'LLM', 'IFMJais3', 'MBZUAI Jais 3 (Arabic)', 0, 1,
 '{"type": "openai", "base_url": "https://api.ifm.ai/v1", "model_name": "IFM/Jais-3-MoVA-36B-A4B", "api_key": "", "max_tokens": 2000}',
 'https://docs.ifm.ai/docs/model-catalog.md',
 'MBZUAI Jais 3: Arabic-first model covering Arabic dialects and English. Uses the same IFM API key as K2 Horizon.',
 5, NULL, NOW(), NULL, NOW()),
('ASR_IFMJaisASR', 'ASR', 'IFMJaisASR', 'MBZUAI Jais ASR (Arabic dialects)', 0, 1,
 '{"type": "openai", "base_url": "https://api.ifm.ai/v1/audio/transcriptions", "model_name": "IFM/Jais-ASR", "api_key": "", "output_dir": "tmp/"}',
 'https://docs.ifm.ai/docs/transcription.md',
 'MBZUAI Jais ASR: Arabic speech recognition with dialect detection (Gulf, Egyptian, Levantine and more) and English code-switching. Uses the IFM API key.',
 11, NULL, NOW(), NULL, NOW());
