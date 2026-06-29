-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS hsn_codes (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hsn_code VARCHAR(8) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    gst_rate DECIMAL(5,2) NOT NULL,
    chapter VARCHAR(2),
    heading VARCHAR(4),
    sub_heading VARCHAR(6),
    embedding vector(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_hsn_codes_code ON hsn_codes(hsn_code);
CREATE INDEX idx_hsn_codes_chapter ON hsn_codes(chapter);
CREATE INDEX idx_hsn_embedding ON hsn_codes USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);