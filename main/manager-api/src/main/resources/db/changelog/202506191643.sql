-- LLM intent recognition configuration notes
UPDATE `ai_model_config` SET
`doc_link` = NULL,
`remark` = 'LLM intent recognition configuration notes:
1. Use a dedicated LLM for intent recognition
2. Defaults to the model in selected_module.LLM
3. You can configure a dedicated LLM (such as the free ChatGLMLLM)
4. Highly versatile, but increases processing time
Configuration notes:
1. Specify the LLM model to use in the llm field
2. If not specified, the model in selected_module.LLM is used' WHERE `id` = 'Intent_intent_llm';

-- Function-call intent recognition configuration notes
UPDATE `ai_model_config` SET
`doc_link` = NULL,
`remark` = 'Function-call intent recognition configuration notes:
1. Use the LLM function_call feature for intent recognition
2. The selected LLM must support function_call
3. Calls tools on demand for fast processing' WHERE `id` = 'Intent_function_call';