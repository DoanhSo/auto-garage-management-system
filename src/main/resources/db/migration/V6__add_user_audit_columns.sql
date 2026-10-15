-- Align the existing user table with the shared BaseEntity; preserve V1-V5 checksums.
ALTER TABLE users
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL,
    ADD INDEX idx_users_created_by (created_by),
    ADD INDEX idx_users_updated_by (updated_by);
