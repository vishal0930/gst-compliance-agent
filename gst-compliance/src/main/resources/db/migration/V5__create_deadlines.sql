CREATE TABLE IF NOT EXISTS filing_deadlines (
                                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    form_type VARCHAR(20) NOT NULL,
    due_date DATE NOT NULL,
    penalty_per_day DECIMAL(10,2) DEFAULT 50.00,
    severity VARCHAR(10) DEFAULT 'MEDIUM',
    notified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, form_type, due_date)
    );

CREATE INDEX idx_deadlines_user ON filing_deadlines(user_id);
CREATE INDEX idx_deadlines_due_date ON filing_deadlines(due_date);
CREATE INDEX idx_deadlines_notified ON filing_deadlines(notified);