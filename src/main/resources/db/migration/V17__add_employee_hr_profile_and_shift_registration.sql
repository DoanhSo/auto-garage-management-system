ALTER TABLE employees
    ADD COLUMN joined_at DATE NULL AFTER employee_code,
    ADD COLUMN bank_name VARCHAR(50) NULL AFTER joined_at,
    ADD COLUMN bank_account_number VARCHAR(30) NULL AFTER bank_name,
    ADD COLUMN bank_account_holder VARCHAR(100) NULL AFTER bank_account_number;

ALTER TABLE employee_shifts
    ADD COLUMN registered_at DATETIME(6) NULL AFTER notes,
    ADD COLUMN approved_by BIGINT NULL AFTER registered_at,
    ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER approved_by,
    DROP CHECK ck_shifts_status,
    ADD CONSTRAINT ck_shifts_status CHECK (status IN ('ACTIVE','CANCELLED','PENDING','REJECTED')),
    ADD CONSTRAINT fk_shifts_approver FOREIGN KEY (approved_by) REFERENCES users(id);
