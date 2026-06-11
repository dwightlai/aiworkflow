ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_ai_bot ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE agi_ai_bot ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_model_provider ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE agi_model_provider ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_prompt_template ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE agi_prompt_template ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_sys_menu ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE agi_sys_menu ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_data_dictionary ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE agi_data_dictionary ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_data_dictionary_item ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE agi_data_dictionary_item ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_integration_app ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE agi_integration_app ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_generation_template ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

ALTER TABLE agi_workflow ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);
