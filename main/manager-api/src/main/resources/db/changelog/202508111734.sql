-- Update the HuoshanDoubleStreamTTS provider to add speech rate, pitch, and other configuration options
UPDATE `ai_model_provider`
SET fields = '[{"key": "ws_url", "type": "string", "label": "WebSocket URL"}, {"key": "appid", "type": "string", "label": "App ID"}, {"key": "access_token", "type": "string", "label": "Access Token"}, {"key": "resource_id", "type": "string", "label": "Resource ID"}, {"key": "speaker", "type": "string", "label": "Default Voice"}, {"key": "speech_rate", "type": "number", "label": "Speech Rate (-50~100)"}, {"key": "loudness_rate", "type": "number", "label": "Volume (-50~100)"}, {"key": "pitch", "type": "number", "label": "Pitch (-12~12)"}]'
WHERE id = 'SYSTEM_TTS_HSDSTTS';

UPDATE `ai_model_config` SET 
`doc_link` = 'https://console.volcengine.com/speech/service/10007',
`remark` = 'Volcengine text-to-speech service configuration guide:
1. Visit https://www.volcengine.com/ to register and activate a Volcengine account
2. Visit https://console.volcengine.com/speech/service/10007 to enable the TTS large model and purchase voices
3. Get the appid and access_token at the bottom of the page
5. The resource ID is fixed as: volc.service_type.10029 (large model TTS and mixing)
6. Speech rate: -50~100, optional, default is 0, can be set to -50~100
7. Volume: -50~100, optional, default is 0, can be set to -50~100
8. Pitch: -12~12, optional, default is 0, can be set to -12~12
9. Fill in the configuration file' WHERE `id` = 'TTS_HuoshanDoubleStreamTTS';