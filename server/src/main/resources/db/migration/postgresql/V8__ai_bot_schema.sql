CREATE TABLE IF NOT EXISTS ai_bot (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    avatar VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    model_provider_id VARCHAR(64),
    knowledge_base_id VARCHAR(64),
    system_prompt TEXT,
    opening_message TEXT,
    status VARCHAR(32) NOT NULL,
    conversation_count INTEGER NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_bot_status ON ai_bot(status, updated_at);
CREATE INDEX IF NOT EXISTS idx_ai_bot_workflow ON ai_bot(workflow_id);
