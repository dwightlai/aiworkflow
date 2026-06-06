ALTER TABLE agi_user ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_agi_user_sort_order ON agi_user(tenant_id, sort_order, username);
