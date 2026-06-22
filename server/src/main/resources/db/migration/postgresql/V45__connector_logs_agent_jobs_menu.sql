INSERT INTO agi_sys_menu (id, tenant_id, group_title, menu_key, title, path, sort_order, visible, platform_only, status, created_at, updated_at)
SELECT 'menu_connector_logs', 'tenant_default', 'AI 功能', 'connector-logs', '调用日志', '/connector-logs', 7, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agi_sys_menu WHERE tenant_id = 'tenant_default' AND menu_key = 'connector-logs');

INSERT INTO agi_sys_menu (id, tenant_id, group_title, menu_key, title, path, sort_order, visible, platform_only, status, created_at, updated_at)
SELECT 'menu_agent_jobs', 'tenant_default', 'AI 功能', 'agent-jobs', '异步任务', '/agent-jobs', 9, TRUE, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agi_sys_menu WHERE tenant_id = 'tenant_default' AND menu_key = 'agent-jobs');
