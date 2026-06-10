-- Update HuoshanDoubleStreamTTS provider configuration, add connection reuse option
UPDATE `ai_model_provider`
SET fields = '[{"key": "ws_url", "type": "string", "label": "WebSocket URL"}, {"key": "appid", "type": "string", "label": "App ID"}, {"key": "access_token", "type": "string", "label": "Access Token"}, {"key": "resource_id", "type": "string", "label": "Resource ID"}, {"key": "speaker", "type": "string", "label": "Default Voice"}, {"key": "enable_ws_reuse", "type": "boolean", "label": "Enable Connection Reuse", "default": true}, {"key": "speech_rate", "type": "number", "label": "Speech Rate (-50~100)"}, {"key": "loudness_rate", "type": "number", "label": "Volume (-50~100)"}, {"key": "pitch", "type": "number", "label": "Pitch (-12~12)"}]'
WHERE id = 'SYSTEM_TTS_HSDSTTS';

UPDATE `ai_model_config` SET 
`doc_link` = 'https://console.volcengine.com/speech/service/10007',
`remark` = 'Volcengine Text-to-Speech service configuration guide:
1. Visit https://www.volcengine.com/ to register and activate a Volcengine account
2. Visit https://console.volcengine.com/speech/service/10007 to activate the TTS large model and purchase voices
3. Get the appid and access_token at the bottom of the page
5. Resource ID is fixed as: volc.service_type.10029 (large model speech synthesis and mixing)
6. Connection reuse: enable WebSocket connection reuse, default true to reduce connection overhead (note: after reuse, idle connections occupy concurrency slots while the device is in listening state)
7. Speech rate: -50~100, optional, normal default value is 0, can be set to -50~100
8. Volume: -50~100, optional, normal default value is 0, can be set to -50~100
9. Pitch: -12~12, optional, normal default value is 0, can be set to -12~12
10. Fill into the configuration file' WHERE `id` = 'TTS_HuoshanDoubleStreamTTS';