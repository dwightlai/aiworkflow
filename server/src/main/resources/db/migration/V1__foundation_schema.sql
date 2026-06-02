CREATE TABLE IF NOT EXISTS workflow (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    current_version_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS workflow_version (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL REFERENCES workflow(id),
    version INTEGER NOT NULL,
    definition_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    published_by VARCHAR(64),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (workflow_id, version)
);

CREATE TABLE IF NOT EXISTS workflow_execution (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL REFERENCES workflow(id),
    workflow_version_id VARCHAR(64) NOT NULL REFERENCES workflow_version(id),
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json TEXT NOT NULL,
    context_json TEXT NOT NULL,
    output_json TEXT,
    error_code VARCHAR(100),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS workflow_node_execution (
    id VARCHAR(64) PRIMARY KEY,
    workflow_execution_id VARCHAR(64) NOT NULL REFERENCES workflow_execution(id),
    node_id VARCHAR(100) NOT NULL,
    node_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json TEXT,
    output_json TEXT,
    error_code VARCHAR(100),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS integration_app (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS webhook_subscription (
    id VARCHAR(64) PRIMARY KEY,
    integration_app_id VARCHAR(64) NOT NULL REFERENCES integration_app(id),
    event_type VARCHAR(100) NOT NULL,
    target_url TEXT NOT NULL,
    secret_ref VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE workflow
    ADD CONSTRAINT fk_workflow_current_version
        FOREIGN KEY (current_version_id) REFERENCES workflow_version(id);

CREATE INDEX IF NOT EXISTS idx_workflow_tenant_status ON workflow(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_workflow_execution_workflow ON workflow_execution(workflow_id, created_at);
CREATE INDEX IF NOT EXISTS idx_node_execution_run ON workflow_node_execution(workflow_execution_id);
