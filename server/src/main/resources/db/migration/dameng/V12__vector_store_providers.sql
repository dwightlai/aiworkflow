ALTER TABLE agi_vector_store_config ADD host VARCHAR(500);
ALTER TABLE agi_vector_store_config ADD port INTEGER;
ALTER TABLE agi_vector_store_config ADD database_name VARCHAR(200);
ALTER TABLE agi_vector_store_config ADD namespace_name VARCHAR(200);
ALTER TABLE agi_vector_store_config ADD vector_dimension INTEGER DEFAULT 1536 NOT NULL;
ALTER TABLE agi_vector_store_config ADD ssl_enabled NUMBER(1) DEFAULT 0 NOT NULL;
ALTER TABLE agi_vector_store_config ADD options_json CLOB;

UPDATE agi_vector_store_config
SET namespace_name = index_name
WHERE namespace_name IS NULL;

UPDATE agi_vector_store_config
SET options_json = '{}'
WHERE options_json IS NULL;
