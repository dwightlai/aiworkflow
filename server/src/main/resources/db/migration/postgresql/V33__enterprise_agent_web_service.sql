ALTER TABLE agi_bot_message ADD COLUMN IF NOT EXISTS metadata TEXT;
ALTER TABLE agi_bot_message ADD COLUMN IF NOT EXISTS message_type VARCHAR(32);

CREATE TABLE IF NOT EXISTS agi_connector (
    id             VARCHAR(64) PRIMARY KEY,
    tenant_id      VARCHAR(64) NOT NULL,
    name           VARCHAR(128) NOT NULL,
    code           VARCHAR(128) NOT NULL,
    type           VARCHAR(64) NOT NULL,
    access_type    VARCHAR(32) NOT NULL,
    base_url       VARCHAR(512),
    auth_mode      VARCHAR(64),
    auth_config    TEXT,
    enabled        BOOLEAN NOT NULL,
    description    TEXT,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_connector_tenant_code ON agi_connector(tenant_id, code);

CREATE TABLE IF NOT EXISTS agi_connector_operation (
    id                       VARCHAR(64) PRIMARY KEY,
    tenant_id                VARCHAR(64) NOT NULL,
    connector_id             VARCHAR(64) NOT NULL,
    name                     VARCHAR(128) NOT NULL,
    code                     VARCHAR(128) NOT NULL,
    method                   VARCHAR(16),
    path                     VARCHAR(512),
    operation_type           VARCHAR(32) NOT NULL,
    risk_level               VARCHAR(32) NOT NULL,
    need_confirm             BOOLEAN NOT NULL,
    confirm_summary_template TEXT,
    request_template         TEXT,
    enabled                  BOOLEAN NOT NULL,
    description              TEXT,
    created_at               TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_connector_operation_code ON agi_connector_operation(tenant_id, connector_id, code);

CREATE TABLE IF NOT EXISTS agi_embed_ticket (
    id                VARCHAR(64) PRIMARY KEY,
    tenant_id         VARCHAR(64) NOT NULL,
    user_id           VARCHAR(64) NOT NULL,
    bot_id            VARCHAR(64) NOT NULL,
    ticket            VARCHAR(256) NOT NULL,
    status            VARCHAR(32) NOT NULL,
    expire_at         TIMESTAMP NOT NULL,
    used_at           TIMESTAMP,
    business_context  TEXT,
    source_app_id     VARCHAR(64),
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_embed_ticket_ticket ON agi_embed_ticket(ticket);

CREATE TABLE IF NOT EXISTS agi_human_confirm_task (
    id                VARCHAR(64) PRIMARY KEY,
    tenant_id         VARCHAR(64) NOT NULL,
    conversation_id   VARCHAR(64),
    bot_id            VARCHAR(64),
    workflow_run_id   VARCHAR(64),
    node_id           VARCHAR(64),
    user_id           VARCHAR(64) NOT NULL,
    title             VARCHAR(255),
    summary           TEXT,
    payload_snapshot  TEXT,
    connector_code    VARCHAR(64),
    operation_code    VARCHAR(64),
    payload_hash      VARCHAR(128),
    status            VARCHAR(32) NOT NULL,
    expire_at         TIMESTAMP,
    created_at        TIMESTAMP NOT NULL,
    confirmed_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agi_agent_audit_log (
    id               VARCHAR(64) PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL,
    user_id          VARCHAR(64),
    bot_id           VARCHAR(64),
    conversation_id  VARCHAR(64),
    message_id       VARCHAR(64),
    connector_code   VARCHAR(64),
    operation_code   VARCHAR(64),
    event_type       VARCHAR(64) NOT NULL,
    request_summary  TEXT,
    response_summary TEXT,
    status           VARCHAR(32),
    error_message    TEXT,
    trace_id         VARCHAR(64),
    created_at       TIMESTAMP NOT NULL
);

INSERT INTO agi_sys_menu (id, tenant_id, group_title, menu_key, title, path, sort_order, visible, platform_only, status, created_at, updated_at)
SELECT 'menu_connectors', 'tenant_default', 'AI 功能', 'connectors', '连接器', '/connectors', 7, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agi_sys_menu WHERE tenant_id = 'tenant_default' AND menu_key = 'connectors');

UPDATE agi_sys_menu SET sort_order = sort_order + 1, updated_at = CURRENT_TIMESTAMP
WHERE tenant_id = 'tenant_default' AND group_title = 'AI 功能' AND sort_order >= 7 AND menu_key != 'connectors';

UPDATE agi_sys_menu SET sort_order = 7, updated_at = CURRENT_TIMESTAMP
WHERE tenant_id = 'tenant_default' AND menu_key = 'connectors';
