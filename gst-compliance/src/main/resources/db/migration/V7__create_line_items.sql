-- Create line_items table
CREATE TABLE IF NOT EXISTS line_items (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    quantity DECIMAL(10,3) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    hsn_code VARCHAR(8),
    gst_rate DECIMAL(5,2),
    taxable_value DECIMAL(15,2),
    cgst_amount DECIMAL(15,2),
    sgst_amount DECIMAL(15,2),
    igst_amount DECIMAL(15,2),
    hsn_confidence DECIMAL(5,4),
    needs_review BOOLEAN DEFAULT FALSE
    );

-- Create index for faster queries
CREATE INDEX idx_line_items_invoice_id ON line_items(invoice_id);
CREATE INDEX idx_line_items_hsn_code ON line_items(hsn_code);