delete from `ai_model_config` where id = 'LLM_XunfeiSparkLLM';
INSERT INTO `ai_model_config` VALUES ('LLM_XunfeiSparkLLM', 'LLM', 'iFlytek Spark Cognitive LLM', 'iFlytek Spark Cognitive LLM', 0, 1, '{"type": "openai", "model_name": "generalv3.5", "base_url": "https://spark-api-open.xf-yun.com/v1", "api_password": "your_api_password", "temperature": 0.5, "max_tokens": 2048, "top_p": 1.0, "frequency_penalty": 0.0}', 'https://www.xfyun.cn/doc/spark/HTTP%E8%B0%83%E7%94%A8%E6%96%87%E6%A1%A3.html', 'iFlytek Spark Cognitive LLM, supports multi-turn conversation, text generation and more', 14, NULL, NULL, NULL, NULL);

-- Update the documentation for the iFlytek Spark Cognitive LLM configuration
UPDATE `ai_model_config` SET
`doc_link` = 'https://www.xfyun.cn/doc/spark/HTTP%E8%B0%83%E7%94%A8%E6%96%87%E6%A1%A3.html',
`remark` = 'iFlytek Spark Cognitive LLM configuration guide:
1. Log in to the iFlytek Open Platform https://www.xfyun.cn/. Each model has its own api_password; when switching models, check the api_password of the corresponding model.
2. Create a Spark Cognitive LLM application to obtain the API Password
3. Parameter descriptions:
   - api_password: API Password, obtained after creating an application on the iFlytek Open Platform
   - model_name: Model name, supports versions such as generalv3.5 and generalv3
   - base_url: API address, defaults to https://spark-api-open.xf-yun.com/v1
   - temperature: Temperature parameter, controls generation randomness, range 0-1, defaults to 0.5
   - max_tokens: Maximum output tokens, defaults to 2048
   - top_p: Nucleus sampling parameter, controls vocabulary diversity, defaults to 1.0
   - frequency_penalty: Frequency penalty, reduces repetitive content, defaults to 0.0
4. Each model has its own api_password; when switching models, check the api_password of the corresponding model.
' WHERE `id` = 'LLM_XunfeiSparkLLM';