CREATE TABLE IF NOT EXISTS vector_store_config (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    store_type VARCHAR(64) NOT NULL,
    endpoint TEXT,
    index_name VARCHAR(200) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS knowledge_base (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    embedding_model_id VARCHAR(64),
    vector_store_config_id VARCHAR(64),
    splitter_type VARCHAR(64) NOT NULL,
    chunk_size INTEGER NOT NULL,
    chunk_overlap INTEGER NOT NULL,
    retrieval_mode VARCHAR(64) NOT NULL,
    top_k INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    document_count INTEGER NOT NULL,
    chunk_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS knowledge_document (
    id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    name VARCHAR(300) NOT NULL,
    chunk_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    document_id VARCHAR(64) NOT NULL REFERENCES knowledge_document(id) ON DELETE CASCADE,
    document_name VARCHAR(300) NOT NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL,
    token_estimate INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_knowledge_base_status ON knowledge_base(status, updated_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_document_base ON knowledge_document(knowledge_base_id, created_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_document ON knowledge_chunk(knowledge_base_id, document_id, chunk_index);
CREATE INDEX IF NOT EXISTS idx_vector_store_enabled ON vector_store_config(enabled, updated_at);
