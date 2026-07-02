ALTER TABLE hsn_codes
    ADD COLUMN IF NOT EXISTS igst_rate DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS cgst_rate DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS sgst_rate DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS cess_rate DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS rate_source VARCHAR(50),
    ADD COLUMN IF NOT EXISTS effective_from DATE,
    ADD COLUMN IF NOT EXISTS notification_ref VARCHAR(100);

UPDATE hsn_codes
SET igst_rate = COALESCE(igst_rate, gst_rate),
    cgst_rate = COALESCE(cgst_rate, gst_rate / 2),
    sgst_rate = COALESCE(sgst_rate, gst_rate / 2)
WHERE gst_rate IS NOT NULL;
