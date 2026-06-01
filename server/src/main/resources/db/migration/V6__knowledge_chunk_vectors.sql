CREATE TABLE IF NOT EXISTS knowledge_chunk_vector (
    chunk_id VARCHAR(64) PRIMARY KEY REFERENCES knowledge_chunk(id) ON DELETE CASCADE,
    knowledge_base_id VARCHAR(64) NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    document_id VARCHAR(64) NOT NULL REFERENCES knowledge_document(id) ON DELETE CASCADE,
    embedding_model_id VARCHAR(64) NOT NULL,
    embedding TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_vector_base ON knowledge_chunk_vector(knowledge_base_id, embedding_model_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_vector_document ON knowledge_chunk_vector(knowledge_base_id, document_id);
