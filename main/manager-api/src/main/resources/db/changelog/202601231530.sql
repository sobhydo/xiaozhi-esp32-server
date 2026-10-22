-- Update the HuoshanDoubleStreamTTS provider config, changing scattered parameters into JSON dictionary configs
-- Consolidate parameters such as speech_rate, loudness_rate, pitch, emotion, emotion_scale into three JSON dictionaries: audio_params, additions, mix_speaker

UPDATE `ai_model_provider`
SET `fields` = '[
  {"key": "ws_url", "type": "string", "label": "WebSocket Address"},
  {"key": "appid", "type": "string", "label": "App ID"},
  {"key": "access_token", "type": "string", "label": "Access Token"},
  {"key": "resource_id", "type": "string", "label": "Resource ID"},
  {"key": "speaker", "type": "string", "label": "Default Voice"},
  {"key": "enable_ws_reuse", "type": "boolean", "label": "Enable Connection Reuse", "default": true},
  {"key": "audio_params", "type": "dict", "label": "Audio Output Config"},
  {"key": "additions", "type": "dict", "label": "Advanced Text Processing Config"},
  {"key": "mix_speaker", "type": "dict", "label": "Voice Mixing Config"}
]'
WHERE `id` = 'SYSTEM_TTS_HSDSTTS';

-- Update existing config, migrating the old scattered parameters into the new JSON dictionary structure
UPDATE `ai_model_config`
SET `config_json` = JSON_SET(
    `config_json`,
    '$.audio_params', JSON_OBJECT(
        'speech_rate', CAST(COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`config_json`, '$.speech_rate')), ''), '0') AS SIGNED),
        'loudness_rate', CAST(COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`config_json`, '$.loudness_rate')), ''), '0') AS SIGNED)
    ),
    '$.additions', JSON_OBJECT(
        'aigc_metadata', JSON_OBJECT(),
        'cache_config', JSON_OBJECT(),
        'post_process', JSON_OBJECT(
            'pitch', CAST(COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(`config_json`, '$.pitch')), ''), '0') AS SIGNED)
        )
    ),
    '$.mix_speaker', JSON_OBJECT()
)
WHERE `id` = 'TTS_HuoshanDoubleStreamTTS';

-- Remove the old scattered parameter fields
UPDATE `ai_model_config`
SET `config_json` = JSON_REMOVE(
    `config_json`,
    '$.speech_rate',
    '$.loudness_rate',
    '$.pitch',
    '$.emotion',
    '$.emotion_scale'
)
WHERE `id` = 'TTS_HuoshanDoubleStreamTTS';

-- Update the documentation link and remark notes
UPDATE `ai_model_config` SET
`doc_link` = 'https://www.volcengine.com/docs/6561/1329505',
`remark` = 'Volcengine Bidirectional Streaming TTS configuration guide:
1. Visit https://www.volcengine.com/ to register and activate a Volcengine account
2. Visit https://console.volcengine.com/speech/service/10007 to enable the large-model speech synthesis service and purchase voices
3. Get the appid and access_token at the bottom of the page
4. The Resource ID is fixed as: volc.service_type.10029 (large-model speech synthesis and voice mixing)
5. Connection reuse: enables WebSocket connection reuse, defaults to true to reduce connection overhead (note: after reuse, idle connections will count toward the concurrency limit while the device is listening)

Detailed parameter docs: https://www.volcengine.com/docs/6561/1329505
[audio_params] Audio Output Config - users may freely add any audio parameter supported by Volcengine
  - speech_rate: Speech rate (-50~100), default 0
  - loudness_rate: Volume (-50~100), default 0
  - emotion: Emotion type (supported by some voices only), options: neutral, happy, sad, angry, fearful, disgusted, surprised
  - emotion_scale: Emotion intensity (1~5), default 4
  Example: {"speech_rate": 10, "loudness_rate": 5, "emotion": "happy", "emotion_scale": 4}

[additions] Advanced Text Processing Config - users may freely add any advanced parameter supported by Volcengine
  - post_process.pitch: Pitch (-12~12), default 0
  - aigc_metadata: AIGC metadata config
  - cache_config: Cache config
  Example: {"post_process": {"pitch": 2}, "aigc_metadata": {}, "cache_config": {}}

[mix_speaker] Voice Mixing Config - multi-voice blending (TTS 1.0 only)
  Example:
    {"speakers": [
      {"source_speaker": "zh_male_bvlazysheep","mix_factor": 0.3},
      {"source_speaker": "BV120_streaming","mix_factor": 0.3},
      {"source_speaker": "zh_male_ahu_conversation_wvae_bigtts","mix_factor": 0.4}
    ]}

Notes:
- Multi-emotion voice parameters (emotion, emotion_scale) are supported by some voices only
- Related voice list: https://www.volcengine.com/docs/6561/1257544
- Users can add more parameters per the Volcengine API documentation
- The mixing feature applies mainly to Doubao Speech Synthesis Model 1.0 voices; to use it, set req_params.speaker to custom_mix_bigtts
'
WHERE `id` = 'TTS_HuoshanDoubleStreamTTS';
