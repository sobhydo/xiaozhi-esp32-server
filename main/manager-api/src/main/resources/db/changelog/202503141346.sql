-- Model provider table
DROP TABLE IF EXISTS `ai_model_provider`;
CREATE TABLE `ai_model_provider` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Primary key',
    `model_type` VARCHAR(20) COMMENT 'Model type (Memory/ASR/VAD/LLM/TTS)',
    `provider_code` VARCHAR(50) COMMENT 'Provider type',
    `name` VARCHAR(50) COMMENT 'Provider name',
    `fields` JSON COMMENT 'Provider field list (JSON format)',
    `sort` INT UNSIGNED DEFAULT 0 COMMENT 'Sort order',
    `creator` BIGINT COMMENT 'Creator',
    `create_date` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater',
    `update_date` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_ai_model_provider_model_type` (`model_type`) COMMENT 'Index on model type, used for fast lookup of all providers of a specific type'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Model configuration table';

-- Model configuration table
DROP TABLE IF EXISTS `ai_model_config`;
CREATE TABLE `ai_model_config` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Primary key',
    `model_type` VARCHAR(20) COMMENT 'Model type (Memory/ASR/VAD/LLM/TTS)',
    `model_code` VARCHAR(50) COMMENT 'Model code (e.g. AliLLM, DoubaoTTS)',
    `model_name` VARCHAR(50) COMMENT 'Model name',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT 'Whether default configuration (0 no, 1 yes)',
    `is_enabled` TINYINT(1) DEFAULT 0 COMMENT 'Whether enabled',
    `config_json` JSON COMMENT 'Model configuration (JSON format)',
    `doc_link` VARCHAR(200) COMMENT 'Official documentation link',
    `remark` VARCHAR(255) COMMENT 'Remark',
    `sort` INT UNSIGNED DEFAULT 0 COMMENT 'Sort order',
    `creator` BIGINT COMMENT 'Creator',
    `create_date` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater',
    `update_date` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_ai_model_config_model_type` (`model_type`) COMMENT 'Index on model type, used for fast lookup of all configurations of a specific type'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Model configuration table';

-- TTS voice table
DROP TABLE IF EXISTS `ai_tts_voice`;
CREATE TABLE `ai_tts_voice` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Primary key',
    `tts_model_id` VARCHAR(32) COMMENT 'Associated TTS model primary key',
    `name` VARCHAR(20) COMMENT 'Voice name',
    `tts_voice` VARCHAR(50) COMMENT 'Voice code',
    `languages` VARCHAR(50) COMMENT 'Language',
    `voice_demo` VARCHAR(500) DEFAULT NULL COMMENT 'Voice demo',
    `remark` VARCHAR(255) COMMENT 'Remark',
    `sort` INT UNSIGNED DEFAULT 0 COMMENT 'Sort order',
    `creator` BIGINT COMMENT 'Creator',
    `create_date` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater',
    `update_date` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_ai_tts_voice_tts_model_id` (`tts_model_id`) COMMENT 'Index on TTS model primary key, used for fast lookup of voices for the corresponding model'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TTS voice table';

-- Agent configuration template table
DROP TABLE IF EXISTS `ai_agent_template`;
CREATE TABLE `ai_agent_template` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Agent unique identifier',
    `agent_code` VARCHAR(36) COMMENT 'Agent code',
    `agent_name` VARCHAR(64) COMMENT 'Agent name',
    `asr_model_id` VARCHAR(32) COMMENT 'Speech recognition model identifier',
    `vad_model_id` VARCHAR(64) COMMENT 'Voice activity detection identifier',
    `llm_model_id` VARCHAR(32) COMMENT 'Large language model identifier',
    `tts_model_id` VARCHAR(32) COMMENT 'Speech synthesis model identifier',
    `tts_voice_id` VARCHAR(32) COMMENT 'Voice identifier',
    `mem_model_id` VARCHAR(32) COMMENT 'Memory model identifier',
    `intent_model_id` VARCHAR(32) COMMENT 'Intent model identifier',
    `system_prompt` TEXT COMMENT 'Role definition parameters',
    `lang_code` VARCHAR(10) COMMENT 'Language code',
    `language` VARCHAR(10) COMMENT 'Interaction language',
    `sort` INT UNSIGNED DEFAULT 0 COMMENT 'Sort weight',
    `creator` BIGINT COMMENT 'Creator ID',
    `created_at` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater ID',
    `updated_at` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent configuration template table';

-- Agent configuration table
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Agent unique identifier',
    `user_id` BIGINT COMMENT 'Owner user ID',
    `agent_code` VARCHAR(36) COMMENT 'Agent code',
    `agent_name` VARCHAR(64) COMMENT 'Agent name',
    `asr_model_id` VARCHAR(32) COMMENT 'Speech recognition model identifier',
    `vad_model_id` VARCHAR(64) COMMENT 'Voice activity detection identifier',
    `llm_model_id` VARCHAR(32) COMMENT 'Large language model identifier',
    `tts_model_id` VARCHAR(32) COMMENT 'Speech synthesis model identifier',
    `tts_voice_id` VARCHAR(32) COMMENT 'Voice identifier',
    `mem_model_id` VARCHAR(32) COMMENT 'Memory model identifier',
    `intent_model_id` VARCHAR(32) COMMENT 'Intent model identifier',
    `system_prompt` TEXT COMMENT 'Role definition parameters',
    `lang_code` VARCHAR(10) COMMENT 'Language code',
    `language` VARCHAR(10) COMMENT 'Interaction language',
    `sort` INT UNSIGNED DEFAULT 0 COMMENT 'Sort weight',
    `creator` BIGINT COMMENT 'Creator ID',
    `created_at` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater ID',
    `updated_at` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_ai_agent_user_id` (`user_id`) COMMENT 'Index on user, used for fast lookup of agents owned by a user'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent configuration table';

-- Device information table
DROP TABLE IF EXISTS `ai_device`;
CREATE TABLE `ai_device` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Device unique identifier',
    `user_id` BIGINT COMMENT 'Associated user ID',
    `mac_address` VARCHAR(50) COMMENT 'MAC address',
    `last_connected_at` DATETIME COMMENT 'Last connected time',
    `auto_update` TINYINT UNSIGNED DEFAULT 0 COMMENT 'Auto update switch (0 off / 1 on)',
    `board` VARCHAR(50) COMMENT 'Device hardware model',
    `alias` VARCHAR(64) DEFAULT NULL COMMENT 'Device alias',
    `agent_id` VARCHAR(32) COMMENT 'Agent ID',
    `app_version` VARCHAR(20) COMMENT 'Firmware version number',
    `sort` INT UNSIGNED DEFAULT 0 COMMENT 'Sort order',
    `creator` BIGINT COMMENT 'Creator',
    `create_date` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater',
    `update_date` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_ai_device_created_at` (`mac_address`) COMMENT 'Index on MAC, used for fast lookup of device information'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Device information table';

-- Voiceprint recognition table
DROP TABLE IF EXISTS `ai_voiceprint`;
CREATE TABLE `ai_voiceprint` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Voiceprint unique identifier',
    `name` VARCHAR(64) COMMENT 'Voiceprint name',
    `user_id` BIGINT COMMENT 'User ID (linked to user table)',
    `agent_id` VARCHAR(32) COMMENT 'Associated agent ID',
    `agent_code` VARCHAR(36) COMMENT 'Associated agent code',
    `agent_name` VARCHAR(36) COMMENT 'Associated agent name',
    `description` VARCHAR(255) COMMENT 'Voiceprint description',
    `embedding` LONGTEXT COMMENT 'Voiceprint feature vector (JSON array format)',
    `memory` TEXT COMMENT 'Associated memory data',
    `sort` INT UNSIGNED DEFAULT 0 COMMENT 'Sort weight',
    `creator` BIGINT COMMENT 'Creator ID',
    `created_at` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater ID',
    `updated_at` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Voiceprint recognition table';

-- Chat history table
DROP TABLE IF EXISTS `ai_chat_history`;
CREATE TABLE `ai_chat_history` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Conversation number',
    `user_id` BIGINT COMMENT 'User number',
    `agent_id` VARCHAR(32) DEFAULT NULL COMMENT 'Chat role',
    `device_id` VARCHAR(32) DEFAULT NULL COMMENT 'Device number',
    `message_count` INT COMMENT 'Message summary',
    `creator` BIGINT COMMENT 'Creator',
    `create_date` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater',
    `update_date` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Chat history table';

-- Chat message table
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Chat record unique identifier',
    `user_id` BIGINT COMMENT 'User unique identifier',
    `chat_id` VARCHAR(64) COMMENT 'Chat history ID',
    `role` ENUM('user', 'assistant') COMMENT 'Role (user or assistant)',
    `content` TEXT COMMENT 'Message content',
    `prompt_tokens` INT UNSIGNED DEFAULT 0 COMMENT 'Prompt token count',
    `total_tokens` INT UNSIGNED DEFAULT 0 COMMENT 'Total token count',
    `completion_tokens` INT UNSIGNED DEFAULT 0 COMMENT 'Completion token count',
    `prompt_ms` INT UNSIGNED DEFAULT 0 COMMENT 'Prompt time (milliseconds)',
    `total_ms` INT UNSIGNED DEFAULT 0 COMMENT 'Total time (milliseconds)',
    `completion_ms` INT UNSIGNED DEFAULT 0 COMMENT 'Completion time (milliseconds)',
    `creator` BIGINT COMMENT 'Creator',
    `create_date` DATETIME COMMENT 'Creation time',
    `updater` BIGINT COMMENT 'Updater',
    `update_date` DATETIME COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_ai_chat_message_user_id_chat_id_role` (`user_id`, `chat_id`) COMMENT 'Composite index on user ID, chat session ID and role, used for fast retrieval of chat records',
    INDEX `idx_ai_chat_message_created_at` (`create_date`) COMMENT 'Index on creation time, used to sort or retrieve chat records by time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Chat message table';
