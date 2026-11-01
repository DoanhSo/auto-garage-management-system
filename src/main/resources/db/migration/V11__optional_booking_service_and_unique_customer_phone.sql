-- 1. Normalize blank customer phones to NULL and deduplicate existing duplicate phones
--    (keep records linked to a user first, then lowest id; clear phone on later duplicates so FKs remain intact).
UPDATE customers
SET phone = NULL
WHERE phone IS NOT NULL AND TRIM(phone) = '';

UPDATE customers c
JOIN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY phone
                   ORDER BY (user_id IS NULL) ASC, id ASC
               ) AS rn
        FROM customers
        WHERE phone IS NOT NULL
    ) ranked
    WHERE ranked.rn > 1
) dup ON c.id = dup.id
SET c.phone = NULL;

-- 2. Enforce uniqueness on customer phone (NULL remains allowed for OIDC JIT profiles)
ALTER TABLE customers
    DROP INDEX ix_customers_phone,
    ADD CONSTRAINT uq_customers_phone UNIQUE (phone);

-- 3. Allow bookings without a pre-selected catalog service ("Khám & Chẩn đoán tổng quát")
ALTER TABLE bookings
    MODIFY COLUMN service_id BIGINT NULL;
