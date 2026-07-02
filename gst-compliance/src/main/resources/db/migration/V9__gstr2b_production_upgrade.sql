-- ============================================================
-- V9 — GSTR-2B production upgrade
-- Adds: tax_period, import_status, match_status to invoices
--       cess, itc_eligible, reverse_charge to line items
--       gstr2b_import_sessions table
--       Fixes unique constraint to include tax_period
-- ============================================================

-- 1. Drop the old unique constraint (user + supplier_gstin + invoice_number)
ALTER TABLE gstr2b_invoices
    DROP CONSTRAINT IF EXISTS gstr2b_invoices_user_id_supplier_gstin_invoice_number_key;

-- 2. Add new columns to gstr2b_invoices
ALTER TABLE gstr2b_invoices
    ADD COLUMN IF NOT EXISTS tax_period   VARCHAR(7),
    ADD COLUMN IF NOT EXISTS import_status VARCHAR(20) NOT NULL DEFAULT 'IMPORTED',
    ADD COLUMN IF NOT EXISTS match_status  VARCHAR(20) NOT NULL DEFAULT 'NOT_CHECKED',
    ADD COLUMN IF NOT EXISTS cess          NUMERIC(15,2);

-- 3. Back-fill tax_period for existing rows from invoice_date
UPDATE gstr2b_invoices
SET tax_period = TO_CHAR(invoice_date, 'MM-YYYY')
WHERE tax_period IS NULL;

-- 4. Make tax_period NOT NULL now that it is back-filled
ALTER TABLE gstr2b_invoices
    ALTER COLUMN tax_period SET NOT NULL;

-- 5. Add new unique constraint that includes tax_period
ALTER TABLE gstr2b_invoices
    ADD CONSTRAINT gstr2b_invoices_unique_per_period
    UNIQUE (user_id, supplier_gstin, invoice_number, tax_period);

-- 6. Index on tax_period for fast period queries
CREATE INDEX IF NOT EXISTS idx_gstr2b_invoices_tax_period
    ON gstr2b_invoices (user_id, tax_period);

-- 7. Add extra columns to gstr2b_line_items
ALTER TABLE gstr2b_line_items
    ADD COLUMN IF NOT EXISTS cess           NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS itc_eligible   BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS reverse_charge BOOLEAN NOT NULL DEFAULT FALSE;

-- 8. Create gstr2b_import_sessions table
CREATE TABLE IF NOT EXISTS gstr2b_import_sessions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id),
    tax_period      VARCHAR(7)   NOT NULL,
    total_invoices  INTEGER      NOT NULL DEFAULT 0,
    successful      INTEGER      NOT NULL DEFAULT 0,
    failed          INTEGER      NOT NULL DEFAULT 0,
    total_taxable   NUMERIC(18,2),
    total_cgst      NUMERIC(18,2),
    total_sgst      NUMERIC(18,2),
    total_igst      NUMERIC(18,2),
    total_itc       NUMERIC(18,2),
    status          VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    imported_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gstr2b_sessions_user_period
    ON gstr2b_import_sessions (user_id, tax_period);
