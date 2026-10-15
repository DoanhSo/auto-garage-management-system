ALTER TABLE notifications
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD INDEX idx_notifications_inbox (user_id,channel,created_at);
