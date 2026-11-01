-- Payroll, shift typing, lead technician and per-order commissions.

ALTER TABLE employees
    ADD COLUMN base_salary DECIMAL(18,2) NOT NULL DEFAULT 0.00 AFTER hourly_cost,
    ADD COLUMN overtime_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.50 AFTER base_salary,
    ADD COLUMN default_commission_rate DECIMAL(5,2) NOT NULL DEFAULT 10.00 AFTER overtime_multiplier,
    ADD CONSTRAINT ck_employees_payroll CHECK (base_salary >= 0 AND overtime_multiplier >= 1 AND default_commission_rate BETWEEN 0 AND 100);

ALTER TABLE employee_shifts
    ADD COLUMN shift_type VARCHAR(20) NOT NULL DEFAULT 'FULL_DAY' AFTER ends_at,
    ADD COLUMN service_bay_id BIGINT NULL AFTER shift_type,
    ADD CONSTRAINT fk_shifts_bay FOREIGN KEY (service_bay_id) REFERENCES service_bays(id),
    ADD CONSTRAINT ck_shifts_type CHECK (shift_type IN ('MORNING','AFTERNOON','OVERTIME','FULL_DAY','NIGHT'));

ALTER TABLE repair_orders
    ADD COLUMN lead_technician_id BIGINT NULL AFTER received_by,
    ADD CONSTRAINT fk_orders_lead_technician FOREIGN KEY (lead_technician_id) REFERENCES employees(id);

CREATE TABLE payroll_periods (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    period_code VARCHAR(50) NOT NULL,
    period_month VARCHAR(7) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_employees INT NOT NULL DEFAULT 0,
    total_regular_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_overtime_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_base_pay DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_commissions DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_allowances DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_deductions DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_net_pay DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    approved_by BIGINT NULL,
    approved_at DATETIME(6) NULL,
    payment_id BIGINT NULL,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payroll_period_code UNIQUE (period_code),
    CONSTRAINT uq_payroll_period_month UNIQUE (period_month),
    CONSTRAINT fk_payroll_period_approver FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT fk_payroll_period_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT ck_payroll_period_range CHECK (to_date >= from_date),
    CONSTRAINT ck_payroll_period_status CHECK (status IN ('DRAFT','CALCULATED','APPROVED','PAID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payroll_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payroll_period_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    base_salary_snapshot DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    hourly_rate_snapshot DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    regular_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    overtime_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    shift_count INT NOT NULL DEFAULT 0,
    completed_order_count INT NOT NULL DEFAULT 0,
    hourly_pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    commission_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    allowance_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    deduction_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    gross_pay DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    net_pay DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'CALCULATED',
    paid_at DATETIME(6) NULL,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payroll_item_period_employee UNIQUE (payroll_period_id, employee_id),
    CONSTRAINT fk_payroll_item_period FOREIGN KEY (payroll_period_id) REFERENCES payroll_periods(id),
    CONSTRAINT fk_payroll_item_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT ck_payroll_item_status CHECK (status IN ('CALCULATED','CONFIRMED','PAID')),
    CONSTRAINT ck_payroll_item_amounts CHECK (allowance_amount >= 0 AND deduction_amount >= 0),
    INDEX ix_payroll_items_employee (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_commissions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    repair_order_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    employee_id BIGINT NOT NULL,
    commission_type VARCHAR(30) NOT NULL,
    base_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    rate_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    commission_amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    earned_at DATETIME(6) NOT NULL,
    payroll_item_id BIGINT NULL,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_commissions_order FOREIGN KEY (repair_order_id) REFERENCES repair_orders(id),
    CONSTRAINT fk_commissions_task FOREIGN KEY (task_id) REFERENCES repair_tasks(id),
    CONSTRAINT fk_commissions_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_commissions_item FOREIGN KEY (payroll_item_id) REFERENCES payroll_items(id),
    CONSTRAINT ck_commissions_type CHECK (commission_type IN ('ORDER_COMPLETION','LABOR_SHARE','FAST_TURNAROUND','QC_EXCELLENCE')),
    CONSTRAINT ck_commissions_status CHECK (status IN ('PENDING','APPROVED','PAID_OUT','CANCELLED')),
    CONSTRAINT ck_commissions_amount CHECK (commission_amount >= 0 AND base_amount >= 0),
    INDEX ix_order_commissions_order (repair_order_id),
    INDEX ix_order_commissions_employee (employee_id, status, earned_at),
    INDEX ix_order_commissions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Payroll disbursements are cash-out payments that belong to neither an invoice nor a supplier document.
ALTER TABLE payments
    ADD COLUMN payroll_period_id BIGINT NULL AFTER inventory_document_id,
    ADD CONSTRAINT fk_payments_payroll_period FOREIGN KEY (payroll_period_id) REFERENCES payroll_periods(id),
    DROP CHECK ck_payments_target,
    ADD CONSTRAINT ck_payments_target CHECK (
        (direction = 'IN' AND invoice_id IS NOT NULL AND inventory_document_id IS NULL AND payroll_period_id IS NULL)
        OR (direction = 'OUT' AND invoice_id IS NULL AND (
            (inventory_document_id IS NOT NULL AND payroll_period_id IS NULL)
            OR (inventory_document_id IS NULL AND payroll_period_id IS NOT NULL)))
    );
