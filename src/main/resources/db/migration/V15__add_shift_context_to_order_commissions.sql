-- Per-order bonuses remember the shift, bay and vehicle they were earned on, so a payslip can list every repaired car.

ALTER TABLE order_commissions
    ADD COLUMN shift_id BIGINT NULL AFTER task_id,
    ADD COLUMN shift_type VARCHAR(20) NULL AFTER shift_id,
    ADD COLUMN service_bay_id BIGINT NULL AFTER shift_type,
    ADD COLUMN bay_code VARCHAR(30) NULL AFTER service_bay_id,
    ADD COLUMN license_plate VARCHAR(20) NULL AFTER bay_code,
    ADD CONSTRAINT fk_commissions_shift FOREIGN KEY (shift_id) REFERENCES employee_shifts(id),
    ADD CONSTRAINT fk_commissions_bay FOREIGN KEY (service_bay_id) REFERENCES service_bays(id),
    ADD INDEX ix_order_commissions_shift (employee_id, shift_id, commission_type);

ALTER TABLE payroll_items
    ADD COLUMN order_bonus_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 AFTER commission_amount;
