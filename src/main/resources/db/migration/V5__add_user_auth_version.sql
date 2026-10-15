-- Revoke issued JWTs after account security changes without adding a business table.
ALTER TABLE users
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_users_auth_version CHECK (auth_version >= 0),
    ADD INDEX ix_users_role_active (role,is_active);
