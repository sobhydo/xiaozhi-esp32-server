-- Bring-your-own-keys: user-owned model entries. NULL user_id = system (admin-managed) model
ALTER TABLE `ai_model_config`
    ADD COLUMN `user_id` BIGINT NULL COMMENT 'Owner user id; NULL = system model' AFTER `is_enabled`,
    ADD INDEX `idx_model_config_user` (`user_id`);
