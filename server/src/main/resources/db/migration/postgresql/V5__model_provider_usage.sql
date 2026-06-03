ALTER TABLE model_provider
    ADD COLUMN IF NOT EXISTS model_usage VARCHAR(40) NOT NULL DEFAULT 'CHAT';

CREATE INDEX IF NOT EXISTS idx_model_provider_usage ON model_provider(tenant_id, model_usage, enabled);
