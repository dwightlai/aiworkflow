CREATE TABLE IF NOT EXISTS agi_bot_capability (
    id               VARCHAR(64) PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL,
    bot_id           VARCHAR(64) NOT NULL,
    capability_type  VARCHAR(32) NOT NULL,
    capability_id    VARCHAR(64),
    capability_code  VARCHAR(128),
    routing_keywords TEXT,
    is_primary       BOOLEAN NOT NULL DEFAULT FALSE,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_agi_bot_capability_bot ON agi_bot_capability(bot_id, enabled);

CREATE TABLE IF NOT EXISTS agi_agent_job (
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
    result           TEXT,
    error_message    TEXT,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_agi_agent_job_source ON agi_agent_job(source_job_id);
CREATE INDEX IF NOT EXISTS idx_agi_agent_job_bot ON agi_agent_job(bot_id, created_at DESC);

INSERT INTO agi_sys_menu (id, tenant_id, group_title, menu_key, title, path, sort_order, visible, platform_only, status, created_at, updated_at)
SELECT 'menu_agent_audit', 'tenant_default', 'AI 功能', 'agent-audit', '智能体审计', '/agent-audit', 8, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agi_sys_menu WHERE tenant_id = 'tenant_default' AND menu_key = 'agent-audit');
