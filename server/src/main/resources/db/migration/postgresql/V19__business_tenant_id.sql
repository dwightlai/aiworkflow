UPDATE agi_workflow SET tenant_id = 'tenant_default' WHERE tenant_id IS NULL OR tenant_id IN ('tenant-default', 'default');
UPDATE agi_workflow_execution SET tenant_id = 'tenant_default' WHERE tenant_id IS NULL OR tenant_id IN ('tenant-default', 'default');
UPDATE agi_model_provider SET tenant_id = 'tenant_default' WHERE tenant_id IS NULL OR tenant_id IN ('tenant-default', 'default');

ALTER TABLE agi_knowledge_base ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
UPDATE agi_knowledge_base SET tenant_id = 'tenant_default' WHERE tenant_id IS NULL;
ALTER TABLE agi_knowledge_base ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE agi_ai_bot ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
UPDATE agi_ai_bot SET tenant_id = 'tenant_default' WHERE tenant_id IS NULL;
ALTER TABLE agi_ai_bot ALTER COLUMN tenant_id SET NOT NULL;

UPDATE agi_prompt_template SET tenant_id = 'tenant_default' WHERE tenant_id IS NULL OR tenant_id IN ('tenant-default', 'default');

CREATE INDEX IF NOT EXISTS idx_agi_knowledge_base_tenant ON agi_knowledge_base(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agi_ai_bot_tenant ON agi_ai_bot(tenant_id);
