ALTER TABLE agi_knowledge_base
    ADD COLUMN IF NOT EXISTS semantic_similarity_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.78;
