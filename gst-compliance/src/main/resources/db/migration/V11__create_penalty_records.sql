-- Create penalty_records table
CREATE TABLE IF NOT EXISTS penalty_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tax_period VARCHAR(7) NOT NULL, -- Format: MM-YYYY
    return_type VARCHAR(20) NOT NULL,
    due_date DATE NOT NULL,
    filing_date DATE,
    delay_days INTEGER,
    interest_rate DECIMAL(5, 2),
    interest_amount DECIMAL(15, 2),
    late_fee_per_day DECIMAL(10, 2),
    late_fee_total DECIMAL(15, 2),
    total_penalty DECIMAL(15, 2),
    tax_liability DECIMAL(15, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    notes VARCHAR(500)
);

-- Create indexes for penalty_records
CREATE INDEX IF NOT EXISTS idx_penalty_user ON penalty_records(user_id);
CREATE INDEX IF NOT EXISTS idx_penalty_period ON penalty_records(tax_period);
CREATE INDEX IF NOT EXISTS idx_penalty_return_type ON penalty_records(return_type);
CREATE INDEX IF NOT EXISTS idx_penalty_status ON penalty_records(status);
CREATE INDEX IF NOT EXISTS idx_penalty_due_date ON penalty_records(due_date);
CREATE INDEX IF NOT EXISTS idx_penalty_calculated ON penalty_records(calculated_at DESC);

-- Add comment
COMMENT ON TABLE penalty_records IS 'Stores penalty calculations for late GST filings including late fees and interest';
