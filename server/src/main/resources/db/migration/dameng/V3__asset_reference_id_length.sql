ALTER TABLE agi_integration_app_scope MODIFY id VARCHAR(128) NOT NULL;
ALTER TABLE agi_integration_app_scope MODIFY scope_id VARCHAR(128) NOT NULL;

CREATE UNIQUE INDEX uk_agi_app_scope_app_type_asset ON agi_integration_app_scope(app_id, scope_type, scope_id);

ALTER TABLE agi_asset_grant MODIFY asset_id VARCHAR(128) NOT NULL;
