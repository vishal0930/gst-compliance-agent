CREATE TABLE IF NOT EXISTS reconciliation_records (
                                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tax_period VARCHAR(7) NOT NULL,
    status VARCHAR(20) DEFAULT 'RUNNING',
    total_invoices INT DEFAULT 0,
    matched_count INT DEFAULT 0,
    mismatch_count INT DEFAULT 0,
    itc_at_risk DECIMAL(15,2) DEFAULT 0,
    report_json JSONB,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_reconciliation_user_period ON reconciliation_records(user_id, tax_period);
CREATE INDEX idx_reconciliation_status ON reconciliation_records(status);