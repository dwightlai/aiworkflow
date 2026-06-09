ALTER TABLE agi_workflow
    ADD COLUMN IF NOT EXISTS owner_unit_id VARCHAR(64);

ALTER TABLE agi_model_provider
    ADD COLUMN IF NOT EXISTS owner_unit_id VARCHAR(64);
