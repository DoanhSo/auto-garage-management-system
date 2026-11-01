-- Shift attendance tracking and the two new commission types (time-saved bonus, warranty deduction).

ALTER TABLE employee_shifts
    ADD COLUMN actual_starts_at DATETIME(6) NULL AFTER ends_at,
    ADD COLUMN actual_ends_at DATETIME(6) NULL AFTER actual_starts_at,
    ADD COLUMN attendance_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' AFTER actual_ends_at,
    ADD CONSTRAINT ck_shifts_attendance CHECK (attendance_status IN ('SCHEDULED','CHECKED_IN','LATE','CHECKED_OUT','ABSENT')),
    ADD CONSTRAINT ck_shifts_actual_interval CHECK (actual_ends_at IS NULL OR actual_starts_at IS NULL OR actual_ends_at >= actual_starts_at);

ALTER TABLE order_commissions
    DROP CHECK ck_commissions_type,
    ADD CONSTRAINT ck_commissions_type CHECK (commission_type IN (
        'ORDER_COMPLETION','LABOR_SHARE','FAST_TURNAROUND','QC_EXCELLENCE','TIME_SAVED_EFFICIENCY','WARRANTY_DEDUCTION'));
