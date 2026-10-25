-- Order system models by language support: English + Arabic first (1-9), Arabic-focused next (10-19),
-- then everything else (20+) keeping the previous relative order

-- ASR
update `ai_model_config` set `sort` = 1 where id = 'ASR_ElevenLabsStreamASR';
update `ai_model_config` set `sort` = 2 where id = 'ASR_ElevenLabsASR';
update `ai_model_config` set `sort` = 3 where id = 'ASR_OpenaiASR';
update `ai_model_config` set `sort` = 4 where id = 'ASR_GroqASR';
update `ai_model_config` set `sort` = 10 where id = 'ASR_IFMJaisASR';
update `ai_model_config` set `sort` = 20 where id = 'ASR_FunASR';
update `ai_model_config` set `sort` = 21 where id = 'ASR_FunASRServer';
update `ai_model_config` set `sort` = 22 where id = 'ASR_SherpaASR';
update `ai_model_config` set `sort` = 23 where id = 'ASR_VoskASR';
update `ai_model_config` set `sort` = 24 where id = 'ASR_Qwen3Flash';
update `ai_model_config` set `sort` = 25 where id = 'ASR_XunfeiStream';
update `ai_model_config` set `sort` = 26 where id = 'ASR_AliyunStreamASR';
update `ai_model_config` set `sort` = 27 where id = 'ASR_AliyunBLStream';
update `ai_model_config` set `sort` = 28 where id = 'ASR_DoubaoStreamASR';
update `ai_model_config` set `sort` = 29 where id = 'ASR_DoubaoStreamASRV2';
update `ai_model_config` set `sort` = 30 where id = 'ASR_TencentASR';
update `ai_model_config` set `sort` = 31 where id = 'ASR_BaiduASR';
update `ai_model_config` set `sort` = 32 where id = 'ASR_DoubaoASR';
update `ai_model_config` set `sort` = 33 where id = 'ASR_AliyunASR';

-- LLM (Jais is bilingual Arabic/English)
update `ai_model_config` set `sort` = 1 where id = 'LLM_OpenAILLM';
update `ai_model_config` set `sort` = 2 where id = 'LLM_ClaudeLLM';
update `ai_model_config` set `sort` = 3 where id = 'LLM_GeminiLLM';
update `ai_model_config` set `sort` = 4 where id = 'LLM_IFMK2Horizon';
update `ai_model_config` set `sort` = 5 where id = 'LLM_IFMJais3';
update `ai_model_config` set `sort` = 20 where id = 'LLM_DeepSeekLLM';
update `ai_model_config` set `sort` = 21 where id = 'LLM_OllamaLLM';
update `ai_model_config` set `sort` = 22 where id = 'LLM_LMStudioLLM';
update `ai_model_config` set `sort` = 23 where id = 'LLM_DifyLLM';
update `ai_model_config` set `sort` = 24 where id = 'LLM_CozeLLM';
update `ai_model_config` set `sort` = 25 where id = 'LLM_FastgptLLM';
update `ai_model_config` set `sort` = 26 where id = 'LLM_XinferenceLLM';
update `ai_model_config` set `sort` = 27 where id = 'LLM_XinferenceSmallLLM';
update `ai_model_config` set `sort` = 28 where id = 'LLM_ChatGLMLLM';
update `ai_model_config` set `sort` = 29 where id = 'LLM_AliLLM';
update `ai_model_config` set `sort` = 30 where id = 'LLM_AliAppLLM';
update `ai_model_config` set `sort` = 31 where id = 'LLM_DoubaoLLM';
update `ai_model_config` set `sort` = 32 where id = 'LLM_VolcesAiGatewayLLM';
update `ai_model_config` set `sort` = 33 where id = 'LLM_XunfeiSparkLLM';

-- TTS (Edge first: free default with English and Arabic voices)
update `ai_model_config` set `sort` = 1 where id = 'TTS_EdgeTTS';
update `ai_model_config` set `sort` = 2 where id = 'TTS_ElevenLabs';
update `ai_model_config` set `sort` = 3 where id = 'TTS_OpenAITTS';
update `ai_model_config` set `sort` = 20 where id = 'TTS_HuoshanDoubleStreamTTS';
update `ai_model_config` set `sort` = 21 where id = 'TTS_HSDSTTS_V2';
update `ai_model_config` set `sort` = 22 where id = 'TTS_AliBLStreamTTS';
update `ai_model_config` set `sort` = 23 where id = 'TTS_XunFeiStreamTTS';
update `ai_model_config` set `sort` = 24 where id = 'TTS_MinimaxStreamTTS';
update `ai_model_config` set `sort` = 25 where id = 'TTS_AliyunStreamTTS';
update `ai_model_config` set `sort` = 26 where id = 'TTS_IndexStreamTTS';
update `ai_model_config` set `sort` = 27 where id = 'TTS_PaddleSpeechTTS';
update `ai_model_config` set `sort` = 28 where id = 'TTS_DoubaoTTS';
update `ai_model_config` set `sort` = 29 where id = 'TTS_TencentTTS';
update `ai_model_config` set `sort` = 30 where id = 'TTS_AliyunTTS';
update `ai_model_config` set `sort` = 31 where id = 'TTS_CosyVoiceSiliconflow';
update `ai_model_config` set `sort` = 32 where id = 'TTS_CozeCnTTS';
update `ai_model_config` set `sort` = 33 where id = 'TTS_FishSpeech';
update `ai_model_config` set `sort` = 34 where id = 'TTS_GPT_SOVITS_V3';
update `ai_model_config` set `sort` = 35 where id = 'TTS_GPT_SOVITS_V2';
update `ai_model_config` set `sort` = 36 where id = 'TTS_TTS302AI';
update `ai_model_config` set `sort` = 37 where id = 'TTS_VolcesAiGatewayTTS';
update `ai_model_config` set `sort` = 38 where id = 'TTS_CustomTTS';
