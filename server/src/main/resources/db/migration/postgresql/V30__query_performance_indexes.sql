-- Query-oriented indexes missing from earlier migrations (aligned with Dameng init + runtime query patterns).

-- agi_auth_audit_log: log management filters
CREATE INDEX IF NOT EXISTS idx_agi_auth_audit_tenant_event_time ON agi_auth_audit_log(tenant_id, event_type, occurred_at);
CREATE INDEX IF NOT EXISTS idx_agi_auth_audit_tenant_user_time ON agi_auth_audit_log(tenant_id, user_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_agi_auth_audit_tenant_result_time ON agi_auth_audit_log(tenant_id, result, occurred_at);
CREATE INDEX IF NOT EXISTS idx_agi_auth_audit_actor ON agi_auth_audit_log(user_id, app_id, occurred_at);

-- agi_login_session: refresh token / session validation
CREATE INDEX IF NOT EXISTS idx_agi_login_session_user ON agi_login_session(user_id, expires_at);

-- agi_integration_app_secret: open API key authentication
CREATE INDEX IF NOT EXISTS idx_agi_app_secret_app ON agi_integration_app_secret(app_id, enabled);
CREATE INDEX IF NOT EXISTS idx_agi_app_secret_tenant_app ON agi_integration_app_secret(tenant_id, app_id, created_at);

-- agi_integration_app: tenant admin listing
CREATE INDEX IF NOT EXISTS idx_agi_integration_app_tenant ON agi_integration_app(tenant_id, status, code);

-- agi_user: SSO / external identity lookup & tenant listing
CREATE INDEX IF NOT EXISTS idx_agi_user_external ON agi_user(source_app_id, external_user_id);
CREATE INDEX IF NOT EXISTS idx_agi_user_tenant_status ON agi_user(tenant_id, status);

-- agi_role: tenant role management
CREATE INDEX IF NOT EXISTS idx_agi_role_tenant ON agi_role(tenant_id, organization_id, status);

-- agi_organization: tree listing & child checks
CREATE INDEX IF NOT EXISTS idx_agi_organization_tenant ON agi_organization(tenant_id, status, sort_order);

-- agi_user_organization: membership by organization
CREATE INDEX IF NOT EXISTS idx_agi_user_organization_org ON agi_user_organization(organization_id);

-- agi_workflow_execution: run history by tenant
CREATE INDEX IF NOT EXISTS idx_agi_workflow_execution_tenant_time ON agi_workflow_execution(tenant_id, started_at);

-- agi_knowledge_base: tenant scoped listing
CREATE INDEX IF NOT EXISTS idx_agi_knowledge_base_tenant_status ON agi_knowledge_base(tenant_id, status, updated_at);

-- agi_ai_bot: tenant scoped listing
CREATE INDEX IF NOT EXISTS idx_agi_ai_bot_tenant_status ON agi_ai_bot(tenant_id, status, updated_at);

-- agi_asset_grant: department scoped authorization
CREATE INDEX IF NOT EXISTS idx_agi_asset_grant_department ON agi_asset_grant(department_id, enabled);

-- agi_sys_menu: sidebar navigation
CREATE INDEX IF NOT EXISTS idx_agi_sys_menu_nav ON agi_sys_menu(tenant_id, status, visible, sort_order);

-- agi_data_dictionary: enabled dictionary lookup
CREATE INDEX IF NOT EXISTS idx_agi_data_dictionary_tenant_status ON agi_data_dictionary(tenant_id, status, code);

-- agi_generation_job: tenant job history
CREATE INDEX IF NOT EXISTS idx_agi_generation_job_tenant_status ON agi_generation_job(tenant_id, status, created_at);
