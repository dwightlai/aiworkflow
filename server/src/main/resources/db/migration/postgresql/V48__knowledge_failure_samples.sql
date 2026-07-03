CREATE TABLE IF NOT EXISTS agi_knowledge_failure_sample (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    knowledge_base_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    role_snapshot TEXT,
    seed_chunk_ids TEXT,
    evidence_group_json TEXT,
    expected_evidence TEXT,
    expected_answer_points TEXT,
    failure_flags_json TEXT,
    parser_version VARCHAR(64),
    profile_version VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_failure_sample_base FOREIGN KEY (knowledge_base_id)
        REFERENCES agi_knowledge_base(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_failure_sample_status
    ON agi_knowledge_failure_sample(tenant_id, knowledge_base_id, status, created_at);
