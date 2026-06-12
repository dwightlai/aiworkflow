ALTER TABLE agi_bot_session ADD COLUMN IF NOT EXISTS pinned BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_agi_bot_session_pinned ON agi_bot_session(bot_id, pinned DESC, updated_at DESC);
