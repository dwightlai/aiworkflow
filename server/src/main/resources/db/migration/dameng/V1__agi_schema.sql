CREATE TABLE agi_workflow (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    status VARCHAR(32) NOT NULL,
    current_version_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE agi_workflow_version (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL,
    definition_json CLOB NOT NULL,
    status VARCHAR(32) NOT NULL,
    published_by VARCHAR(64),
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_agi_workflow_version UNIQUE (workflow_id, version),
    CONSTRAINT fk_agi_workflow_version_workflow FOREIGN KEY (workflow_id) REFERENCES agi_workflow(id)
);

CREATE TABLE agi_workflow_execution (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    workflow_version_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json CLOB NOT NULL,
    context_json CLOB NOT NULL,
    output_json CLOB,
    error_code VARCHAR(100),
    error_message CLOB,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_workflow_execution_workflow FOREIGN KEY (workflow_id) REFERENCES agi_workflow(id),
    CONSTRAINT fk_agi_workflow_execution_version FOREIGN KEY (workflow_version_id) REFERENCES agi_workflow_version(id)
);

CREATE TABLE agi_workflow_node_execution (
    id VARCHAR(64) PRIMARY KEY,
    workflow_execution_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(100) NOT NULL,
    node_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json CLOB,
    output_json CLOB,
    error_code VARCHAR(100),
    error_message CLOB,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_node_execution_run FOREIGN KEY (workflow_execution_id) REFERENCES agi_workflow_execution(id)
);

CREATE TABLE agi_integration_app (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE agi_webhook_subscription (
    id VARCHAR(64) PRIMARY KEY,
    integration_app_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    target_url CLOB NOT NULL,
    secret_ref VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_webhook_app FOREIGN KEY (integration_app_id) REFERENCES agi_integration_app(id)
);

CREATE TABLE agi_prompt_template (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    template CLOB NOT NULL,
    description CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE agi_model_provider (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    base_url CLOB NOT NULL,
    api_key_ref VARCHAR(200) NOT NULL,
    enabled NUMBER(1) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    model_type VARCHAR(100) NOT NULL DEFAULT 'CUSTOM',
    description CLOB,
    vision_support NUMBER(1) NOT NULL DEFAULT 0,
    price_per_million_tokens NUMERIC(18, 6) NOT NULL DEFAULT 0,
    model VARCHAR(200) NOT NULL DEFAULT '',
    model_usage VARCHAR(40) NOT NULL DEFAULT 'CHAT'
);

CREATE TABLE agi_vector_store_config (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    store_type VARCHAR(64) NOT NULL,
    endpoint CLOB,
    index_name VARCHAR(200) NOT NULL,
    enabled NUMBER(1) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    username VARCHAR(200),
    password CLOB,
    api_key CLOB,
    connect_timeout_ms INTEGER NOT NULL DEFAULT 5000,
    read_timeout_ms INTEGER NOT NULL DEFAULT 30000
);

CREATE TABLE agi_knowledge_base (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
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
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    vector_dimension INTEGER NOT NULL DEFAULT 1536
);

CREATE TABLE agi_knowledge_document (
    id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL,
    name VARCHAR(300) NOT NULL,
    chunk_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    dataset_type VARCHAR(32) NOT NULL DEFAULT 'TEXT_DOCUMENT',
    processing_status VARCHAR(32) NOT NULL DEFAULT 'READY',
    tags CLOB,
    category VARCHAR(255),
    source CLOB,
    row_count INTEGER NOT NULL DEFAULT 0,
    parser_type VARCHAR(64),
    splitter_type VARCHAR(64),
    splitter_config CLOB,
    raw_content CLOB,
    error_message CLOB,
    CONSTRAINT fk_agi_knowledge_document_base FOREIGN KEY (knowledge_base_id) REFERENCES agi_knowledge_base(id) ON DELETE CASCADE
);

CREATE TABLE agi_knowledge_chunk (
    id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    document_name VARCHAR(300) NOT NULL,
    content CLOB NOT NULL,
    chunk_index INTEGER NOT NULL,
    enabled NUMBER(1) NOT NULL,
    token_estimate INTEGER NOT NULL,
    CONSTRAINT fk_agi_knowledge_chunk_base FOREIGN KEY (knowledge_base_id) REFERENCES agi_knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT fk_agi_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES agi_knowledge_document(id) ON DELETE CASCADE
);

CREATE TABLE agi_knowledge_chunk_vector (
    chunk_id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    embedding_model_id VARCHAR(64) NOT NULL,
    embedding CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_chunk_vector_chunk FOREIGN KEY (chunk_id) REFERENCES agi_knowledge_chunk(id) ON DELETE CASCADE,
    CONSTRAINT fk_agi_chunk_vector_base FOREIGN KEY (knowledge_base_id) REFERENCES agi_knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT fk_agi_chunk_vector_document FOREIGN KEY (document_id) REFERENCES agi_knowledge_document(id) ON DELETE CASCADE
);

CREATE TABLE agi_ai_bot (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    avatar VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64),
    model_provider_id VARCHAR(64),
    knowledge_base_id VARCHAR(64),
    system_prompt CLOB,
    opening_message CLOB,
    status VARCHAR(32) NOT NULL,
    conversation_count INTEGER NOT NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE agi_bot_session (
    id VARCHAR(64) PRIMARY KEY,
    bot_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_bot_session_bot FOREIGN KEY (bot_id) REFERENCES agi_ai_bot(id) ON DELETE CASCADE
);

CREATE TABLE agi_bot_message (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    bot_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agi_bot_message_session FOREIGN KEY (session_id) REFERENCES agi_bot_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_agi_bot_message_bot FOREIGN KEY (bot_id) REFERENCES agi_ai_bot(id) ON DELETE CASCADE
);

ALTER TABLE agi_workflow ADD CONSTRAINT fk_agi_workflow_current_version FOREIGN KEY (current_version_id) REFERENCES agi_workflow_version(id);

CREATE INDEX idx_agi_workflow_tenant_status ON agi_workflow(tenant_id, status);
CREATE INDEX idx_agi_workflow_execution_workflow ON agi_workflow_execution(workflow_id, created_at);
CREATE INDEX idx_agi_node_execution_run ON agi_workflow_node_execution(workflow_execution_id);
CREATE INDEX idx_agi_prompt_template_tenant ON agi_prompt_template(tenant_id, name);
CREATE INDEX idx_agi_model_provider_tenant ON agi_model_provider(tenant_id, enabled);
CREATE INDEX idx_agi_model_provider_usage ON agi_model_provider(tenant_id, model_usage, enabled);
CREATE INDEX idx_agi_knowledge_base_status ON agi_knowledge_base(status, updated_at);
CREATE INDEX idx_agi_knowledge_document_base ON agi_knowledge_document(knowledge_base_id, created_at);
CREATE INDEX idx_agi_knowledge_chunk_document ON agi_knowledge_chunk(knowledge_base_id, document_id, chunk_index);
CREATE INDEX idx_agi_vector_store_enabled ON agi_vector_store_config(enabled, updated_at);
CREATE INDEX idx_agi_chunk_vector_base ON agi_knowledge_chunk_vector(knowledge_base_id, embedding_model_id);
CREATE INDEX idx_agi_chunk_vector_document ON agi_knowledge_chunk_vector(knowledge_base_id, document_id);
CREATE INDEX idx_agi_ai_bot_status ON agi_ai_bot(status, updated_at);
CREATE INDEX idx_agi_ai_bot_workflow ON agi_ai_bot(workflow_id);
CREATE INDEX idx_agi_bot_session_bot ON agi_bot_session(bot_id, updated_at);
CREATE INDEX idx_agi_bot_message_session ON agi_bot_message(bot_id, session_id, created_at);
