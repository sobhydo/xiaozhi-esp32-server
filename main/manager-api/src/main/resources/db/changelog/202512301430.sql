-- Add Alibaba Bailian Paraformer real-time speech recognition service configuration
delete from `ai_model_provider` where id = 'SYSTEM_ASR_AliyunBLStream';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_ASR_AliyunBLStream', 'ASR', 'aliyunbl_stream', 'Alibaba Bailian Paraformer Real-time Speech Recognition', '[{"key":"api_key","label":"API Key","type":"password"},{"key":"model","label":"Model Name","type":"string"},{"key":"format","label":"Audio Format","type":"string"},{"key":"sample_rate","label":"Sample Rate","type":"number"},{"key":"output_dir","label":"Output Directory","type":"string"}]', 18, 1, NOW(), 1, NOW());

delete from `ai_model_config` where id = 'ASR_AliyunBLStream';
INSERT INTO `ai_model_config` VALUES ('ASR_AliyunBLStream', 'ASR', 'AliyunBLStream', 'Alibaba Bailian Paraformer Real-time Speech Recognition', 0, 1, '{"type": "aliyunbl_stream", "api_key": "", "model": "paraformer-realtime-v2", "format": "pcm", "sample_rate": 16000, "disfluency_removal_enabled": false, "semantic_punctuation_enabled": false, "max_sentence_silence": 200, "multi_threshold_mode_enabled": false, "punctuation_prediction_enabled": true, "inverse_text_normalization_enabled": true, "output_dir": "tmp/"}', 'https://help.aliyun.com/zh/model-studio/websocket-for-paraformer-real-time-service', 'Supports advanced features such as multiple languages, custom hot words, and semantic segmentation', 21, NULL, NULL, NULL, NULL);

-- Update the documentation for the Alibaba Bailian Paraformer model configuration
UPDATE `ai_model_config` SET
`doc_link` = 'https://help.aliyun.com/zh/model-studio/websocket-for-paraformer-real-time-service',
`remark` = 'Alibaba Bailian Paraformer real-time speech recognition configuration guide:
1. Log in to the Alibaba Cloud Bailian platform at https://bailian.console.aliyun.com/
2. Create an API-KEY at https://bailian.console.aliyun.com/#/api-key
3. Supported models: paraformer-realtime-v2 (recommended), paraformer-realtime-8k-v2, paraformer-realtime-v1, paraformer-realtime-8k-v1
4. Features:
   - Multi-language support (Chinese including dialects, English, Japanese, Korean, German, French, Russian)
   - Custom hot words (vocabulary_id parameter). For details, see: https://help.aliyun.com/zh/model-studio/custom-hot-words?
   - Semantic segmentation / VAD segmentation (semantic_punctuation_enabled parameter)
   - Automatic punctuation, ITN, filler-word filtering, and more
5. Parameter descriptions:
   - model: Model name, paraformer-realtime-v2 recommended
   - sample_rate: Sample rate (Hz). v2 supports any sample rate, v1 supports only 16000, the 8k versions support only 8000
   - semantic_punctuation_enabled: false for VAD segmentation (low latency), true for semantic segmentation (high accuracy)
   - max_sentence_silence: Silence duration threshold for VAD segmentation (200-6000ms)
' WHERE `id` = 'ASR_AliyunBLStream';


-- Update the Doubao streaming ASR provider and add configuration
delete from `ai_model_provider` where id = 'SYSTEM_ASR_DoubaoStreamASR';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_ASR_DoubaoStreamASR', 'ASR', 'doubao_stream', 'Volcano Engine Speech Recognition (Streaming)', '[{"key":"appid","label":"App ID","type":"string"},{"key":"access_token","label":"Access Token","type":"string"},{"key":"cluster","label":"Cluster","type":"string"},{"key":"boosting_table_name","label":"Hot Words File Name","type":"string"},{"key":"correct_table_name","label":"Replacement Words File Name","type":"string"},{"key":"output_dir","label":"Output Directory","type":"string"},{"key":"end_window_size","label":"Silence Detection Duration (ms)","type":"number"},{"key":"enable_multilingual","label":"Enable Multilingual Recognition Mode","type":"boolean"},{"key":"language","label":"Specified Language Code","type":"string"}]', 3, 1, NOW(), 1, NOW());
UPDATE `ai_model_config` SET
`remark` = 'Doubao ASR configuration guide:
1. The difference between Doubao ASR and Doubao (Streaming) ASR is: Doubao ASR is billed per request, while Doubao (Streaming) ASR is billed by duration
2. Generally, per-request billing is cheaper, but Doubao (Streaming) ASR uses large-model technology and delivers better results
3. You need to create an application in the Volcano Engine console and obtain the appid and access_token
4. Supports Chinese speech recognition
5. Requires a network connection
6. Output files are saved in the tmp/ directory
Application steps:
1. Visit https://console.volcengine.com/speech/app
2. Create a new application
3. Obtain the appid and access_token
4. Fill them into the configuration file
To set hot words, see: https://www.volcengine.com/docs/6561/155738
To enable multilingual recognition mode, set the language parameter. When this key is empty, the model supports recognition of Chinese, English, Shanghainese, Hokkien, Sichuanese, Shaanxi dialect, and Cantonese. For other languages, see: https://www.volcengine.com/docs/6561/1354869
' WHERE `id` = 'ASR_DoubaoStreamASR';

-- Update the Doubao streaming ASR model configuration and add the enable_multilingual default value
UPDATE `ai_model_config` SET
`config_json` = JSON_SET(
    `config_json`, 
    '$.enable_multilingual', false,
    '$.language', 'zh-CN'
)
WHERE `id` = 'ASR_DoubaoStreamASR' 
AND JSON_EXTRACT(`config_json`, '$.enable_multilingual') IS NULL 
AND JSON_EXTRACT(`config_json`, '$.language') IS NULL;


-- Update the HuoshanDoubleStreamTTS provider configuration and add multi-emotion voice parameters
UPDATE `ai_model_provider`
SET `fields` = '[{"key": "ws_url", "type": "string", "label": "WebSocket Address"}, {"key": "appid", "type": "string", "label": "App ID"}, {"key": "access_token", "type": "string", "label": "Access Token"}, {"key": "resource_id", "type": "string", "label": "Resource ID"}, {"key": "speaker", "type": "string", "label": "Default Voice"}, {"key": "enable_ws_reuse", "type": "boolean", "label": "Enable Connection Reuse", "default": true}, {"key": "speech_rate", "type": "number", "label": "Speech Rate (-50~100)"}, {"key": "loudness_rate", "type": "number", "label": "Volume (-50~100)"}, {"key": "pitch", "type": "number", "label": "Pitch (-12~12)"}, {"key": "emotion_scale", "type": "number", "label": "Emotion Intensity (1-5)"}, {"key": "emotion", "type": "string", "label": "Emotion Type"}]'
WHERE `id` = 'SYSTEM_TTS_HSDSTTS';

-- Update default values
UPDATE `ai_model_config` SET
`config_json` = JSON_SET(
    `config_json`,
    '$.emotion', 'neutral',
    '$.emotion_scale', 4
)
WHERE `id` = 'TTS_HuoshanDoubleStreamTTS'
AND JSON_EXTRACT(`config_json`, '$.emotion') IS NULL 
AND JSON_EXTRACT(`config_json`, '$.emotion_scale') IS NULL;

-- Add documentation link and remark
UPDATE `ai_model_config` SET
`doc_link` = 'https://console.volcengine.com/speech/service/10007',
`remark` = 'Volcano Engine speech synthesis service configuration guide:
1. Visit https://www.volcengine.com/ to register and activate a Volcano Engine account
2. Visit https://console.volcengine.com/speech/service/10007 to activate the speech synthesis large model and purchase voices
3. Obtain the appid and access_token at the bottom of the page
5. The resource ID is fixed as: volc.service_type.10029 (large-model speech synthesis and mixing)
6. Connection reuse: enables WebSocket connection reuse, defaults to true to reduce connection overhead (note: after reuse, idle connections will occupy concurrency slots while the device is listening)
7. Speech rate: -50~100, optional, normal default 0, can be set to -50~100
8. Volume: -50~100, optional, normal default 0, can be set to -50~100
9. Pitch: -12~12, optional, normal default 0, can be set to -12~12
10. Multi-emotion parameters (currently only some voices support setting emotion):
   Related voice list: https://www.volcengine.com/docs/6561/1257544
    - emotion_scale: Emotion intensity, possible values: 1~5, default 4
    - emotion: Emotion type, possible values: neutral, happy, sad, angry, fearful, disgusted, surprised
' WHERE `id` = 'TTS_HuoshanDoubleStreamTTS';
