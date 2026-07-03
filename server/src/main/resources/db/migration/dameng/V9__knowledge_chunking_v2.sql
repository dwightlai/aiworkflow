ALTER TABLE agi_knowledge_chunk ADD logical_chunk_id VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD parent_chunk_id VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD group_id VARCHAR(64);
ALTER TABLE agi_knowledge_chunk ADD chunk_level VARCHAR(16);
ALTER TABLE agi_knowledge_chunk ADD section_path VARCHAR(1000);

UPDATE agi_knowledge_chunk
SET logical_chunk_id = id
WHERE logical_chunk_id IS NULL;

UPDATE agi_knowledge_chunk
SET chunk_level = 'CHILD'
WHERE chunk_level IS NULL;

CREATE INDEX idx_chunk_logical ON agi_knowledge_chunk(knowledge_base_id, logical_chunk_id);
CREATE INDEX idx_chunk_parent ON agi_knowledge_chunk(knowledge_base_id, parent_chunk_id);
CREATE INDEX idx_chunk_group ON agi_knowledge_chunk(knowledge_base_id, group_id);
