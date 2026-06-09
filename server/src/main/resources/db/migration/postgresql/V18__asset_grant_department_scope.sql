ALTER TABLE agi_asset_grant
    ADD COLUMN IF NOT EXISTS department_scope VARCHAR(32) NOT NULL DEFAULT 'SELF';

ALTER TABLE agi_asset_grant
    ALTER COLUMN unit_id DROP NOT NULL;
