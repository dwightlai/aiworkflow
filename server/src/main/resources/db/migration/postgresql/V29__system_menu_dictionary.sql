CREATE TABLE IF NOT EXISTS agi_sys_menu (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    group_title VARCHAR(100) NOT NULL,
    menu_key VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    path VARCHAR(300) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    platform_only BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_sys_menu_tenant_key ON agi_sys_menu(tenant_id, menu_key);
CREATE INDEX IF NOT EXISTS idx_agi_sys_menu_tenant_group ON agi_sys_menu(tenant_id, group_title, sort_order);

CREATE TABLE IF NOT EXISTS agi_data_dictionary (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_data_dictionary_tenant_code ON agi_data_dictionary(tenant_id, code);

CREATE TABLE IF NOT EXISTS agi_data_dictionary_item (
    id VARCHAR(64) PRIMARY KEY,
    dictionary_id VARCHAR(64) NOT NULL REFERENCES agi_data_dictionary(id) ON DELETE CASCADE,
    label VARCHAR(200) NOT NULL,
    item_value VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_agi_data_dictionary_item_dict ON agi_data_dictionary_item(dictionary_id, sort_order);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_data_dictionary_item_value ON agi_data_dictionary_item(dictionary_id, item_value);

INSERT INTO agi_sys_menu (id, tenant_id, group_title, menu_key, title, path, sort_order, visible, platform_only, status, created_at, updated_at)
VALUES
    ('menu_dashboard', 'tenant_default', 'AI 功能', 'dashboard', '工作台', '/', 1, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_bots', 'tenant_default', 'AI 功能', 'bots', '智能体 Bots', '/bots', 2, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_workflows', 'tenant_default', 'AI 功能', 'workflows', '工作流', '/workflows', 3, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_workflow_runs', 'tenant_default', 'AI 功能', 'workflow-runs', '运行监控', '/workflow-runs', 4, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_knowledge', 'tenant_default', 'AI 功能', 'knowledge', '知识库', '/knowledge', 5, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_models', 'tenant_default', 'AI 功能', 'models', '模型配置', '/models', 6, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_generation_templates', 'tenant_default', 'AI 功能', 'generation-templates', '编研模板', '/research/templates', 7, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_research', 'tenant_default', 'AI 功能', 'research', '智能编研', '/research/compile', 8, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_tenants', 'tenant_default', '系统管理', 'tenants', '租户管理', '/system/tenants', 1, TRUE, TRUE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_identity', 'tenant_default', '系统管理', 'identity', '组织用户', '/system/identity', 2, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_asset_grants', 'tenant_default', '系统管理', 'asset-grants', '资产授权', '/system/asset-grants', 3, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_integration_apps', 'tenant_default', '系统管理', 'integration-apps', '第三方应用', '/system/integration-apps', 4, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_open_api_docs', 'tenant_default', '系统管理', 'open-api-docs', '开放 API 文档', '/system/open-api-docs', 5, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_menus', 'tenant_default', '系统管理', 'menus', '菜单管理', '/system/menus', 6, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_dictionary', 'tenant_default', '系统管理', 'dictionary', '数据字典', '/system/dictionary', 7, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu_logs', 'tenant_default', '系统管理', 'logs', '日志管理', '/system/logs', 8, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO agi_data_dictionary (id, tenant_id, code, name, description, status, created_at, updated_at)
VALUES
    ('dict_common_status', 'tenant_default', 'common_status', '通用状态', '启用/停用', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_app_type', 'tenant_default', 'integration_app_type', '第三方应用类型', 'Integration App 类型', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_bot_status', 'tenant_default', 'bot_status', '智能体状态', 'Bot 状态枚举', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO agi_data_dictionary_item (id, dictionary_id, label, item_value, description, sort_order, status, created_at, updated_at)
VALUES
    ('dict_common_status_enabled', 'dict_common_status', '启用', 'ENABLED', NULL, 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_common_status_disabled', 'dict_common_status', '停用', 'DISABLED', NULL, 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_app_type_archive', 'dict_app_type', '数字档案馆', 'ARCHIVE_SYSTEM', NULL, 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_app_type_oa', 'dict_app_type', 'OA 系统', 'OA_SYSTEM', NULL, 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_app_type_business', 'dict_app_type', '业务系统', 'BUSINESS_SYSTEM', NULL, 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_app_type_other', 'dict_app_type', '其他', 'OTHER', NULL, 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_bot_status_enabled', 'dict_bot_status', '启用', 'ENABLED', NULL, 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dict_bot_status_disabled', 'dict_bot_status', '停用', 'DISABLED', NULL, 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
