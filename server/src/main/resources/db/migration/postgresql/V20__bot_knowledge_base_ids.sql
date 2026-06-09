ALTER TABLE agi_ai_bot ADD COLUMN IF NOT EXISTS knowledge_base_ids TEXT;

UPDATE agi_ai_bot
SET knowledge_base_ids = to_json(ARRAY[knowledge_base_id])::text
WHERE knowledge_base_id IS NOT NULL
  AND knowledge_base_id <> ''
  AND (knowledge_base_ids IS NULL OR knowledge_base_ids = '');

ALTER TABLE agi_ai_bot DROP COLUMN IF EXISTS knowledge_base_id;
