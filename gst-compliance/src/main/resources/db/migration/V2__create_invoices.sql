CREATE TABLE IF NOT EXISTS invoices (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vendor_name VARCHAR(500) NOT NULL,
    vendor_gstin VARCHAR(15) NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    total_gst DECIMAL(15,2) NOT NULL,
    file_key VARCHAR(500) NOT NULL,
    parse_status VARCHAR(20) DEFAULT 'PENDING',
    raw_json JSONB,
    confidence_score DECIMAL(5,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, invoice_number, vendor_gstin)
    );

CREATE INDEX idx_invoices_user_id ON invoices(user_id);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_vendor_gstin ON invoices(vendor_gstin);
CREATE INDEX idx_invoices_status ON invoices(parse_status);