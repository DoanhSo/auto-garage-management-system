-- Migrate user authentication to Keycloak OIDC: link accounts by identity_id and make password_hash optional.
ALTER TABLE users
    ADD COLUMN identity_id VARCHAR(36) NULL,
    MODIFY COLUMN password_hash VARCHAR(255) NULL,
    ADD CONSTRAINT uq_users_identity_id UNIQUE (identity_id);
