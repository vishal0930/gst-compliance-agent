DROP INDEX IF EXISTS idx_hsn_embedding;

ALTER TABLE hsn_codes
    ALTER COLUMN embedding TYPE vector(768)
    USING embedding::vector(768);

CREATE INDEX IF NOT EXISTS idx_hsn_embedding
    ON hsn_codes USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
