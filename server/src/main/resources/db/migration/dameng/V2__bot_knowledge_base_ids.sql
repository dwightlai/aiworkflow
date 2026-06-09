ALTER TABLE agi_ai_bot ADD knowledge_base_ids CLOB;

UPDATE agi_ai_bot
SET knowledge_base_ids = '["' || knowledge_base_id || '"]'
WHERE knowledge_base_id IS NOT NULL
  AND knowledge_base_id <> ''
  AND (knowledge_base_ids IS NULL OR knowledge_base_ids = '');

ALTER TABLE agi_ai_bot DROP COLUMN knowledge_base_id;
