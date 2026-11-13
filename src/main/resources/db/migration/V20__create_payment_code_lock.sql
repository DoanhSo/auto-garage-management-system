-- Single-row lock taken (SELECT ... FOR UPDATE) while a payment code is generated or checked, so concurrent
-- receipts/payouts cannot pick the same code. The lock is released automatically at commit or rollback.
CREATE TABLE payment_code_lock (
    id TINYINT NOT NULL PRIMARY KEY
);
INSERT INTO payment_code_lock (id) VALUES (1);
