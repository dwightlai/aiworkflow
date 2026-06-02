CREATE TABLE IF NOT EXISTS bot_session (
    id VARCHAR(64) PRIMARY KEY,
    bot_id VARCHAR(64) NOT NULL REFERENCES ai_bot(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS bot_message (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES bot_session(id) ON DELETE CASCADE,
    bot_id VARCHAR(64) NOT NULL REFERENCES ai_bot(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bot_session_bot ON bot_session(bot_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_bot_message_session ON bot_message(bot_id, session_id, created_at);
