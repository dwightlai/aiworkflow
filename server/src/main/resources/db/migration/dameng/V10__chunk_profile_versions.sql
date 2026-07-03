CREATE TABLE agi_chunk_profile_version (
    id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL,
    name VARCHAR(200) NOT NULL,
    strategy VARCHAR(64) NOT NULL,
    chunk_size INTEGER NOT NULL,
    chunk_overlap INTEGER NOT NULL,
    config_json CLOB,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chunk_profile_base FOREIGN KEY (knowledge_base_id)
        REFERENCES agi_knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT uk_chunk_profile_version UNIQUE (knowledge_base_id, version)
);

CREATE INDEX idx_chunk_profile_status
    ON agi_chunk_profile_version(knowledge_base_id, status, version);
