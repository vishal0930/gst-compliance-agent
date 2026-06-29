-- Create compliance_briefs table
CREATE TABLE IF NOT EXISTS compliance_briefs (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tax_period VARCHAR(7) NOT NULL,
    brief_text TEXT,
    total_sales DECIMAL(15,2),
    total_gst DECIMAL(15,2),
    total_itc DECIMAL(15,2),
    tax_liability DECIMAL(15,2),
    itc_at_risk DECIMAL(15,2),
    action_items JSONB,
    gstr3b_draft JSONB,
    is_complete BOOLEAN DEFAULT FALSE,
    is_approved BOOLEAN DEFAULT FALSE,
    generated_at TIMESTAMP,
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create index for faster queries
CREATE INDEX idx_compliance_briefs_user_id ON compliance_briefs(user_id);
CREATE INDEX idx_compliance_briefs_tax_period ON compliance_briefs(tax_period);
CREATE INDEX idx_compliance_briefs_is_approved ON compliance_briefs(is_approved);