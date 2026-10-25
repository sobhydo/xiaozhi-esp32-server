-- English/Arabic-first defaults for remaining Chinese seed data
UPDATE `ai_agent_template` SET `agent_code` = 'Xiaozhi' WHERE `agent_code` = '小智';

-- Spoken exit phrases: English and Arabic first, Chinese kept
UPDATE `sys_params` SET `param_value` = 'exit;quit;goodbye;bye bye;stop talking;خروج;مع السلامة;إلى اللقاء;退出;关闭'
 WHERE `param_code` = 'exit_commands' AND `param_value` NOT LIKE '%goodbye%';

-- Wake phrases: English first, Chinese kept (the device wake word model is still "你好小智")
UPDATE `sys_params` SET `param_value` = CONCAT('hello xiaozhi;hey xiaozhi;hi xiaozhi;', `param_value`)
 WHERE `param_code` = 'wakeup_words' AND `param_value` NOT LIKE '%hello xiaozhi%';

-- Weather plugin default city
UPDATE `ai_model_provider` SET `fields` = REPLACE(`fields`, '"default": "广州"', '"default": "Dubai"') WHERE `id` = 'SYSTEM_PLUGIN_WEATHER';
UPDATE `ai_agent_plugin_mapping` SET `param_info` = REPLACE(`param_info`, '"default_location": "广州"', '"default_location": "Dubai"') WHERE `plugin_id` = 'SYSTEM_PLUGIN_WEATHER';
