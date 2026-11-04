-- Admins can void a warranty book or adjust its limits; keep who did it, when and why on the book itself.
ALTER TABLE warranties
    ADD COLUMN void_reason VARCHAR(500) NULL,
    ADD COLUMN voided_by BIGINT NULL,
    ADD COLUMN voided_at DATETIME(6) NULL,
    ADD COLUMN adjustment_note VARCHAR(500) NULL,
    ADD COLUMN adjusted_by BIGINT NULL,
    ADD COLUMN adjusted_at DATETIME(6) NULL;
