ALTER TABLE agi_integration_app_scope ALTER COLUMN id TYPE VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_app_scope_app_type_asset
    ON agi_integration_app_scope(app_id, scope_type, scope_id);
