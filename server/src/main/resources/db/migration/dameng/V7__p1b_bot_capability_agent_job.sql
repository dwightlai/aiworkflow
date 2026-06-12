CREATE TABLE agi_bot_capability (
    id               VARCHAR(64) PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL,
    bot_id           VARCHAR(64) NOT NULL,
    capability_type  VARCHAR(32) NOT NULL,
    capability_id    VARCHAR(64),
    capability_code  VARCHAR(128),
    routing_keywords CLOB,
    is_primary       NUMBER(1) NOT NULL DEFAULT 0,
    enabled          NUMBER(1) NOT NULL DEFAULT 1,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_agi_bot_capability_bot ON agi_bot_capability(bot_id, enabled);

CREATE TABLE agi_agent_job (
    id               VARCHAR(64) PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL,
    user_id          VARCHAR(64) NOT NULL,
    bot_id           VARCHAR(64),
    conversation_id  VARCHAR(64),
    source_job_id    VARCHAR(64),
    job_type         VARCHAR(64) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    progress         INTEGER,
    current_step     VARCHAR(255),
    result           CLOB,
    error_message    CLOB,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_agi_agent_job_source ON agi_agent_job(source_job_id);
CREATE INDEX idx_agi_agent_job_bot ON agi_agent_job(bot_id, created_at DESC);
