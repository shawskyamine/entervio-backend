-- Add a generated tsvector column for BM25 keyword search.
-- 'simple' config: lowercase + tokenize only (no stemming, no stopwords)
-- so it works across mixed French / English / tech-vocabulary content.
ALTER TABLE cv_chunks
    ADD COLUMN content_tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;

-- GIN index — the standard index type for tsvector.
CREATE INDEX idx_cv_chunks_tsv ON cv_chunks USING GIN (content_tsv);