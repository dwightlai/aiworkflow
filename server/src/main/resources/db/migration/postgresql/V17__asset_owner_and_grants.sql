ALTER TABLE agi_knowledge_base
    ADD COLUMN IF NOT EXISTS owner_unit_id VARCHAR(64);

ALTER TABLE agi_ai_bot
    ADD COLUMN IF NOT EXISTS owner_unit_id VARCHAR(64);

CREATE TABLE IF NOT EXISTS agi_asset_grant (
    id VARCHAR(64) PRIMARY KEY,
    asset_type VARCHAR(64) NOT NULL,
    asset_id VARCHAR(64) NOT NULL,
    permission VARCHAR(32) NOT NULL,
    unit_id VARCHAR(64) NOT NULL,
    unit_scope VARCHAR(32) NOT NULL DEFAULT 'SELF',
    department_id VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_agi_asset_grant_asset ON agi_asset_grant(asset_type, asset_id, permission, enabled);
CREATE INDEX IF NOT EXISTS idx_agi_asset_grant_unit ON agi_asset_grant(unit_id, unit_scope);
