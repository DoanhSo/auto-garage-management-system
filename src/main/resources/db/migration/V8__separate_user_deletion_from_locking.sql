-- Locked accounts remain editable; deleted accounts retain references but leave management lists.
ALTER TABLE users
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD INDEX idx_users_deleted_active (deleted_at, is_active),
    ADD CONSTRAINT chk_users_deleted_inactive CHECK (deleted_at IS NULL OR is_active = FALSE);
