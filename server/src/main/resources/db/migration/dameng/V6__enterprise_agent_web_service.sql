ALTER TABLE agi_bot_message ADD metadata CLOB;
ALTER TABLE agi_bot_message ADD message_type VARCHAR(32);

ALTER TABLE agi_bot_session ADD pinned NUMBER(1) DEFAULT 0 NOT NULL;
ALTER TABLE agi_bot_session ADD user_id VARCHAR(64);

ALTER TABLE agi_ai_bot ADD capability_hint CLOB;
ALTER TABLE agi_ai_bot ADD suggested_questions CLOB;

CREATE TABLE agi_connector (
    id             VARCHAR(64) PRIMARY KEY,
    tenant_id      VARCHAR(64) NOT NULL,
    name           VARCHAR(128) NOT NULL,
    code           VARCHAR(128) NOT NULL,
    type           VARCHAR(64) NOT NULL,
    access_type    VARCHAR(32) NOT NULL,
    base_url       VARCHAR(512),
    auth_mode      VARCHAR(64),
    auth_config    CLOB,
    enabled        NUMBER(1) NOT NULL,
    description    CLOB,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_agi_connector_tenant_code ON agi_connector(tenant_id, code);

CREATE TABLE agi_connector_operation (
    id                       VARCHAR(64) PRIMARY KEY,
    tenant_id                VARCHAR(64) NOT NULL,
    connector_id             VARCHAR(64) NOT NULL,
    name                     VARCHAR(128) NOT NULL,
    code                     VARCHAR(128) NOT NULL,
    method                   VARCHAR(16),
    path                     VARCHAR(512),
    operation_type           VARCHAR(32) NOT NULL,
    risk_level               VARCHAR(32) NOT NULL,
    need_confirm             NUMBER(1) NOT NULL,
    confirm_summary_template CLOB,
    request_template         CLOB,
    enabled                  NUMBER(1) NOT NULL,
    description              CLOB,
    created_at               TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_agi_connector_operation_code ON agi_connector_operation(tenant_id, connector_id, code);

CREATE TABLE agi_embed_ticket (
    id                VARCHAR(64) PRIMARY KEY,
    tenant_id         VARCHAR(64) NOT NULL,
    user_id           VARCHAR(64) NOT NULL,
    bot_id            VARCHAR(64) NOT NULL,
    ticket            VARCHAR(256) NOT NULL,
    status            VARCHAR(32) NOT NULL,
    expire_at         TIMESTAMP NOT NULL,
    used_at           TIMESTAMP,
    business_context  CLOB,
    source_app_id     VARCHAR(64),
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_agi_embed_ticket_ticket ON agi_embed_ticket(ticket);

CREATE TABLE agi_human_confirm_task (
    id                VARCHAR(64) PRIMARY KEY,
    tenant_id         VARCHAR(64) NOT NULL,
    conversation_id   VARCHAR(64),
    bot_id            VARCHAR(64),
    workflow_run_id   VARCHAR(64),
    node_id           VARCHAR(64),
    user_id           VARCHAR(64) NOT NULL,
    title             VARCHAR(255),
    summary           CLOB,
    payload_snapshot  CLOB,
    connector_code    VARCHAR(64),
    operation_code    VARCHAR(64),
    payload_hash      VARCHAR(128),
    status            VARCHAR(32) NOT NULL,
    expire_at         TIMESTAMP,
    created_at        TIMESTAMP NOT NULL,
    confirmed_at      TIMESTAMP
);

CREATE TABLE agi_agent_audit_log (
    id               VARCHAR(64) PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL,
    user_id          VARCHAR(64),
    bot_id           VARCHAR(64),
    conversation_id  VARCHAR(64),
    message_id       VARCHAR(64),
    connector_code   VARCHAR(64),
    operation_code   VARCHAR(64),
    event_type       VARCHAR(64) NOT NULL,
    request_summary  CLOB,
    response_summary CLOB,
    status           VARCHAR(32),
    error_message    CLOB,
    trace_id         VARCHAR(64),
    created_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_agi_bot_session_user ON agi_bot_session(bot_id, user_id, updated_at DESC);
CREATE INDEX idx_agi_bot_session_pinned ON agi_bot_session(bot_id, pinned DESC, updated_at DESC);
