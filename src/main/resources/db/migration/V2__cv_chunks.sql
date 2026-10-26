-- Enable pgvector (safe if already enabled)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE cv_chunks (
                           id              UUID PRIMARY KEY,
                           cv_id           UUID NOT NULL REFERENCES cv_documents(id) ON DELETE CASCADE,
                           candidate_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           kind            VARCHAR(16) NOT NULL,       -- HEADER | RAW | SKILLS
                           chunk_index     INTEGER NOT NULL,           -- ordering within the CV
                           content         TEXT NOT NULL,              -- the actual text
                           token_count     INTEGER NOT NULL,
                           embedding       vector(768),                -- nomic-embed-text dimension
                           created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- Fast candidate/kind lookups and reindex cleanup
CREATE INDEX idx_cv_chunks_cv_id        ON cv_chunks(cv_id);
CREATE INDEX idx_cv_chunks_candidate_id ON cv_chunks(candidate_id);
CREATE INDEX idx_cv_chunks_kind         ON cv_chunks(kind);

-- ANN index for cosine similarity search.
-- ivfflat needs some rows to build meaningfully; 100 lists is fine up to ~1M rows.
-- If the table is empty, this still runs — it just builds an empty index.
CREATE INDEX idx_cv_chunks_embedding
    ON cv_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);