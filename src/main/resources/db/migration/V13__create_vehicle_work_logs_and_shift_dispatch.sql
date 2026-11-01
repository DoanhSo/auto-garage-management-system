-- Reception dispatch columns and the vehicle-technician-shift audit trail.
-- employee_shifts.shift_type/service_bay_id and repair_orders.lead_technician_id already exist since V12.

ALTER TABLE repair_orders
    ADD COLUMN diagnostic_technician_id BIGINT NULL AFTER lead_technician_id,
    ADD COLUMN service_bay_id BIGINT NULL AFTER diagnostic_technician_id,
    ADD COLUMN qc_technician_id BIGINT NULL AFTER service_bay_id,
    ADD CONSTRAINT fk_orders_diagnostic_technician FOREIGN KEY (diagnostic_technician_id) REFERENCES employees(id),
    ADD CONSTRAINT fk_orders_service_bay FOREIGN KEY (service_bay_id) REFERENCES service_bays(id),
    ADD CONSTRAINT fk_orders_qc_technician FOREIGN KEY (qc_technician_id) REFERENCES employees(id);

CREATE TABLE repair_order_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    repair_order_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    shift_id BIGINT NULL,
    assignment_stage VARCHAR(30) NOT NULL,
    task_id BIGINT NULL,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    actual_seconds BIGINT NOT NULL DEFAULT 0,
    hourly_rate_snapshot DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    earned_commission DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    result_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_roa_order FOREIGN KEY (repair_order_id) REFERENCES repair_orders(id),
    CONSTRAINT fk_roa_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_roa_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_roa_shift FOREIGN KEY (shift_id) REFERENCES employee_shifts(id),
    CONSTRAINT fk_roa_task FOREIGN KEY (task_id) REFERENCES repair_tasks(id),
    CONSTRAINT ck_roa_stage CHECK (assignment_stage IN ('DIAGNOSIS','REPAIR_MAIN','REPAIR_ASSISTANT','QC_INSPECTION')),
    CONSTRAINT ck_roa_result CHECK (result_status IN ('IN_PROGRESS','PASSED','REWORK_FAILED','COMPLETED')),
    CONSTRAINT ck_roa_values CHECK (actual_seconds >= 0 AND hourly_rate_snapshot >= 0 AND earned_commission >= 0),
    CONSTRAINT ck_roa_interval CHECK (completed_at IS NULL OR completed_at >= started_at),
    INDEX ix_roa_vehicle (vehicle_id, started_at),
    INDEX ix_roa_employee (employee_id, started_at),
    INDEX ix_roa_order (repair_order_id),
    INDEX ix_roa_shift (shift_id),
    INDEX ix_roa_stage (assignment_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
