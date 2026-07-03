ALTER TABLE agi_vector_store_config ADD COLUMN IF NOT EXISTS host VARCHAR(500);
ALTER TABLE agi_vector_store_config ADD COLUMN IF NOT EXISTS port INTEGER;
ALTER TABLE agi_vector_store_config ADD COLUMN IF NOT EXISTS database_name VARCHAR(200);
ALTER TABLE agi_vector_store_config ADD COLUMN IF NOT EXISTS namespace_name VARCHAR(200);
ALTER TABLE agi_vector_store_config ADD COLUMN IF NOT EXISTS vector_dimension INTEGER NOT NULL DEFAULT 1536;
ALTER TABLE agi_vector_store_config ADD COLUMN IF NOT EXISTS ssl_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE agi_vector_store_config ADD COLUMN IF NOT EXISTS options_json TEXT NOT NULL DEFAULT '{}';

UPDATE agi_vector_store_config
SET namespace_name = index_name
WHERE namespace_name IS NULL;
