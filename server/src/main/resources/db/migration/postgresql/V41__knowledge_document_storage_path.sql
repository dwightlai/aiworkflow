ALTER TABLE agi_knowledge_document
    ADD COLUMN IF NOT EXISTS storage_path VARCHAR(512);
