CREATE TABLE IF NOT EXISTS agi_tenant (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE agi_integration_app ADD COLUMN IF NOT EXISTS code VARCHAR(100);
ALTER TABLE agi_integration_app ADD COLUMN IF NOT EXISTS app_type VARCHAR(64) NOT NULL DEFAULT 'OTHER';
ALTER TABLE agi_integration_app ADD COLUMN IF NOT EXISTS auth_type VARCHAR(64) NOT NULL DEFAULT 'API_KEY';
UPDATE agi_integration_app SET code = id WHERE code IS NULL;
ALTER TABLE agi_integration_app ALTER COLUMN code SET NOT NULL;

CREATE TABLE IF NOT EXISTS agi_unit (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    code VARCHAR(100) NOT NULL,
    external_unit_id VARCHAR(200),
    name VARCHAR(200) NOT NULL,
    unit_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_department (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    unit_id VARCHAR(64) NOT NULL REFERENCES agi_unit(id),
    code VARCHAR(100) NOT NULL,
    external_department_id VARCHAR(200),
    parent_id VARCHAR(64),
    name VARCHAR(200) NOT NULL,
    sort_order INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_agi_department_parent FOREIGN KEY (parent_id) REFERENCES agi_department(id)
);

CREATE TABLE IF NOT EXISTS agi_role (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    unit_id VARCHAR(64),
    external_role_id VARCHAR(200),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    role_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_agi_role_unit FOREIGN KEY (unit_id) REFERENCES agi_unit(id)
);

CREATE TABLE IF NOT EXISTS agi_user (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),
    display_name VARCHAR(200) NOT NULL,
    mobile VARCHAR(50),
    email VARCHAR(200),
    user_type VARCHAR(64) NOT NULL,
    source_app_id VARCHAR(64),
    external_user_id VARCHAR(200),
    status VARCHAR(32) NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_agi_user_app FOREIGN KEY (source_app_id) REFERENCES agi_integration_app(id)
);

CREATE TABLE IF NOT EXISTS agi_user_unit (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    user_id VARCHAR(64) NOT NULL REFERENCES agi_user(id) ON DELETE CASCADE,
    unit_id VARCHAR(64) NOT NULL REFERENCES agi_unit(id),
    primary_unit BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_user_department (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    user_id VARCHAR(64) NOT NULL REFERENCES agi_user(id) ON DELETE CASCADE,
    department_id VARCHAR(64) NOT NULL REFERENCES agi_department(id),
    primary_department BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_user_role (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    user_id VARCHAR(64) NOT NULL REFERENCES agi_user(id) ON DELETE CASCADE,
    role_id VARCHAR(64) NOT NULL REFERENCES agi_role(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_integration_app_secret (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    app_id VARCHAR(64) NOT NULL REFERENCES agi_integration_app(id) ON DELETE CASCADE,
    secret_hash VARCHAR(255) NOT NULL,
    secret_prefix VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_integration_app_scope (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    app_id VARCHAR(64) NOT NULL REFERENCES agi_integration_app(id) ON DELETE CASCADE,
    scope_type VARCHAR(64) NOT NULL,
    scope_id VARCHAR(100) NOT NULL,
    permission VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_login_session (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    user_id VARCHAR(64) NOT NULL REFERENCES agi_user(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL,
    user_agent TEXT,
    client_ip VARCHAR(100),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS agi_auth_audit_log (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    user_id VARCHAR(200),
    app_id VARCHAR(64),
    unit_id VARCHAR(200),
    department_ids TEXT,
    role_ids TEXT,
    client_ip VARCHAR(100),
    user_agent TEXT,
    result VARCHAR(32) NOT NULL,
    error_code VARCHAR(100),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_tenant_code ON agi_tenant(code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_unit_tenant_code ON agi_unit(tenant_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_department_unit_code ON agi_department(unit_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_role_tenant_code ON agi_role(tenant_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_user_tenant_username ON agi_user(tenant_id, username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_integration_app_tenant_code ON agi_integration_app(tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_agi_user_unit_user ON agi_user_unit(user_id, unit_id);
CREATE INDEX IF NOT EXISTS idx_agi_user_department_user ON agi_user_department(user_id, department_id);
CREATE INDEX IF NOT EXISTS idx_agi_user_role_user ON agi_user_role(user_id, role_id);
CREATE INDEX IF NOT EXISTS idx_agi_app_scope_app ON agi_integration_app_scope(app_id, scope_type, scope_id);
CREATE INDEX IF NOT EXISTS idx_agi_auth_audit_tenant_time ON agi_auth_audit_log(tenant_id, occurred_at);

INSERT INTO agi_tenant (id, code, name, status, created_at, updated_at)
VALUES ('tenant_default', 'default', 'Default Tenant', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO agi_unit (id, tenant_id, code, external_unit_id, name, unit_type, status, created_at, updated_at)
VALUES ('unit_default', 'tenant_default', 'default_unit', NULL, 'Default Unit', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO agi_department (id, tenant_id, unit_id, code, external_department_id, parent_id, name, sort_order, status, created_at, updated_at)
VALUES ('dept_default', 'tenant_default', 'unit_default', 'default_dept', NULL, NULL, 'Default Department', 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO agi_role (id, tenant_id, unit_id, external_role_id, code, name, role_type, status, created_at, updated_at)
VALUES
    ('role_platform_admin', 'tenant_default', NULL, NULL, 'platform_admin', 'Platform Administrator', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role_unit_admin', 'tenant_default', NULL, NULL, 'unit_admin', 'Unit Administrator', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role_asset_manager', 'tenant_default', NULL, NULL, 'asset_manager', 'Asset Manager', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role_app_user', 'tenant_default', NULL, NULL, 'app_user', 'Application User', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role_integration_admin', 'tenant_default', NULL, NULL, 'integration_admin', 'Integration Administrator', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role_auditor', 'tenant_default', NULL, NULL, 'auditor', 'Auditor', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO agi_user (id, tenant_id, username, password_hash, display_name, mobile, email, user_type, source_app_id, external_user_id, status, last_login_at, created_at, updated_at)
VALUES ('user_admin', 'tenant_default', 'admin', NULL, 'Platform Administrator', NULL, NULL, 'LOCAL', NULL, NULL, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO agi_user_unit (id, tenant_id, user_id, unit_id, primary_unit, created_at)
VALUES ('user_admin_unit_default', 'tenant_default', 'user_admin', 'unit_default', TRUE, CURRENT_TIMESTAMP);

INSERT INTO agi_user_department (id, tenant_id, user_id, department_id, primary_department, created_at)
VALUES ('user_admin_dept_default', 'tenant_default', 'user_admin', 'dept_default', TRUE, CURRENT_TIMESTAMP);

INSERT INTO agi_user_role (id, tenant_id, user_id, role_id, created_at)
VALUES ('user_admin_role_platform_admin', 'tenant_default', 'user_admin', 'role_platform_admin', CURRENT_TIMESTAMP);
