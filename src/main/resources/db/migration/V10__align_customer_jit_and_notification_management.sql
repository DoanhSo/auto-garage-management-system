-- Allow customer profile provisioning from OIDC / Google Login before phone is supplied,
-- and index customer email for automatic linking on first verified login.
ALTER TABLE customers
    MODIFY COLUMN phone VARCHAR(30) NULL,
    ADD INDEX ix_customers_email (email);
