-- Resolve the cyclic relationship only after vehicle_checks exists.
-- The composite key also prevents selecting another order's QC record.
ALTER TABLE repair_orders
    ADD CONSTRAINT fk_orders_latest_qc FOREIGN KEY (latest_qc_check_id,id)
    REFERENCES vehicle_checks(id,repair_order_id);

-- Claims may contain evidence before a new warranty repair order is approved.
ALTER TABLE attachments
    ADD CONSTRAINT fk_attachments_claim FOREIGN KEY (warranty_claim_id)
    REFERENCES warranty_claims(id);

-- Aggregate payments BEFORE joining to avoid multiplying invoice amounts.
CREATE VIEW v_invoice_balances AS
SELECT i.id AS invoice_id, i.invoice_code, i.customer_id, i.repair_order_id,
       i.issued_at, i.due_date, i.status, i.total_amount,
       COALESCE(p.paid_amount,0.00) AS paid_amount,
       i.total_amount - COALESCE(p.paid_amount,0.00) AS balance_amount
FROM invoices i
LEFT JOIN (
    SELECT invoice_id, SUM(amount) AS paid_amount
    FROM payments WHERE direction = 'IN' GROUP BY invoice_id
) p ON p.invoice_id = i.id
WHERE i.status <> 'VOID';

-- Grain: one row per supplier, including suppliers without purchase documents.
-- OPENING, DRAFT, CANCELLED documents never contribute supplier debt.
CREATE VIEW v_supplier_balances AS
SELECT s.id AS supplier_id, s.supplier_code, s.supplier_name,
       COALESCE(d.purchase_amount,0.00) AS purchase_amount,
       COALESCE(d.paid_amount,0.00) AS paid_amount,
       COALESCE(d.purchase_amount,0.00) - COALESCE(d.paid_amount,0.00) AS balance_amount
FROM suppliers s
LEFT JOIN (
    SELECT d.supplier_id, SUM(d.total_amount) AS purchase_amount,
           SUM(COALESCE(p.paid_amount,0.00)) AS paid_amount
    FROM inventory_documents d
    LEFT JOIN (
        SELECT inventory_document_id, SUM(amount) AS paid_amount
        FROM payments WHERE direction = 'OUT' GROUP BY inventory_document_id
    ) p ON p.inventory_document_id = d.id
    WHERE d.document_type = 'IN' AND d.status = 'POSTED' AND d.supplier_id IS NOT NULL
    GROUP BY d.supplier_id
) d ON d.supplier_id = s.id;
