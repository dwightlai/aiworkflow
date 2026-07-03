CREATE TABLE agi_knowledge_failure_sample (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    knowledge_base_id VARCHAR(64) NOT NULL,
    question CLOB NOT NULL,
    role_snapshot CLOB,
    seed_chunk_ids CLOB,
    evidence_group_json CLOB,
    expected_evidence CLOB,
    expected_answer_points CLOB,
    failure_flags_json CLOB,
    parser_version VARCHAR(64),
    profile_version VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_failure_sample_base FOREIGN KEY (knowledge_base_id)
        REFERENCES agi_knowledge_base(id) ON DELETE CASCADE
);

CREATE INDEX idx_failure_sample_status
    ON agi_knowledge_failure_sample(tenant_id, knowledge_base_id, status, created_at);
