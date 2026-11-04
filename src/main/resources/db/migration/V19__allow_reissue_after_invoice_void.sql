-- A voided invoice stays as a record, and the repair order may be invoiced again.
-- Uniqueness now applies only to invoices that are not VOID.

-- invoices: one non-void invoice per repair order.
-- The foreign key on repair_order_id needs its own index before the unique one is dropped.
ALTER TABLE invoices ADD INDEX ix_invoices_order (repair_order_id);
ALTER TABLE invoices DROP INDEX uq_invoices_order;
ALTER TABLE invoices
    ADD COLUMN active_repair_order_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status <> 'VOID' THEN repair_order_id END) STORED,
    ADD CONSTRAINT uq_invoices_active_order UNIQUE (active_repair_order_id);

-- invoice_items: a task or order part may appear again on the replacement invoice.
-- Only one non-void invoice exists per order (above), and issuing locks the order,
-- so a line can never be billed twice on live invoices.
ALTER TABLE invoice_items ADD INDEX ix_invoice_items_task (repair_task_id);
ALTER TABLE invoice_items DROP INDEX uq_invoice_items_task;
ALTER TABLE invoice_items ADD INDEX ix_invoice_items_order_part (repair_order_part_id);
ALTER TABLE invoice_items DROP INDEX uq_invoice_items_order_part;
