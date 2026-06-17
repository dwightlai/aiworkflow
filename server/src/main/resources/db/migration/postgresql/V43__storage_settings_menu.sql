INSERT INTO agi_sys_menu (
    id, tenant_id, group_title, menu_key, title, path, sort_order, visible, platform_only, status, created_at, updated_at
) VALUES (
    'menu_storage_settings',
    'tenant_default',
    '系统管理',
    'storage-settings',
    '存储路径配置',
    '/system/storage-settings',
    9,
    TRUE,
    FALSE,
    'ENABLED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
