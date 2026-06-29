-- ==========================================================
-- GSTR-2B Invoice Header
-- Government copy of purchase invoices
-- ==========================================================

CREATE TABLE IF NOT EXISTS gstr2b_invoices (

                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    supplier_name VARCHAR(255) NOT NULL,

    supplier_gstin VARCHAR(15) NOT NULL,

    buyer_gstin VARCHAR(15) NOT NULL,

    invoice_number VARCHAR(100) NOT NULL,

    invoice_date DATE NOT NULL,

    taxable_value DECIMAL(15,2) NOT NULL,

    cgst DECIMAL(15,2) DEFAULT 0,

    sgst DECIMAL(15,2) DEFAULT 0,

    igst DECIMAL(15,2) DEFAULT 0,

    grand_total DECIMAL(15,2) NOT NULL,

    source VARCHAR(20) DEFAULT 'GSTR2B',

    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(user_id, supplier_gstin, invoice_number)

    );

CREATE INDEX idx_gstr2b_user
    ON gstr2b_invoices(user_id);

CREATE INDEX idx_gstr2b_supplier
    ON gstr2b_invoices(supplier_gstin);

CREATE INDEX idx_gstr2b_invoice
    ON gstr2b_invoices(invoice_number);

CREATE INDEX idx_gstr2b_date
    ON gstr2b_invoices(invoice_date);



-- ==========================================================
-- GSTR-2B Line Items
-- ==========================================================

CREATE TABLE IF NOT EXISTS gstr2b_line_items (

                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    gstr2b_invoice_id UUID NOT NULL
    REFERENCES gstr2b_invoices(id)
    ON DELETE CASCADE,

    description TEXT NOT NULL,

    quantity DECIMAL(10,3),

    unit_price DECIMAL(15,2),

    hsn_code VARCHAR(8),

    gst_rate DECIMAL(5,2),

    taxable_value DECIMAL(15,2),

    cgst_amount DECIMAL(15,2),

    sgst_amount DECIMAL(15,2),

    igst_amount DECIMAL(15,2),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

    );

CREATE INDEX idx_gstr2b_line_invoice
    ON gstr2b_line_items(gstr2b_invoice_id);

CREATE INDEX idx_gstr2b_line_hsn
    ON gstr2b_line_items(hsn_code);