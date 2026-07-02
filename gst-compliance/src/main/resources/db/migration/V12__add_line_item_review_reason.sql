ALTER TABLE line_items
    ADD COLUMN IF NOT EXISTS review_reason VARCHAR(100);
