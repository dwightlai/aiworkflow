ALTER TABLE agi_bot_session ADD COLUMN IF NOT EXISTS user_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_agi_bot_session_user ON agi_bot_session(bot_id, user_id, updated_at DESC);
