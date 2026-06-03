package com.aiworkflow.bot.service;

import com.aiworkflow.bot.domain.AiBot;
import com.aiworkflow.bot.domain.BotMessage;
import com.aiworkflow.bot.domain.BotMessageRole;
import com.aiworkflow.bot.domain.BotSession;
import com.aiworkflow.bot.domain.BotStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcBotStore implements BotStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcBotStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AiBot save(AiBot bot) {
        if (exists(bot.id())) {
            jdbcTemplate.update("""
                            UPDATE agi_ai_bot
                            SET name = ?, description = ?, avatar = ?, workflow_id = ?, model_provider_id = ?,
                                knowledge_base_id = ?, system_prompt = ?, opening_message = ?, status = ?,
                                conversation_count = ?, published_at = ?, created_at = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    bot.name(),
                    bot.description(),
                    bot.avatar(),
                    bot.workflowId(),
                    bot.modelProviderId(),
                    bot.knowledgeBaseId(),
                    bot.systemPrompt(),
                    bot.openingMessage(),
                    bot.status().name(),
                    bot.conversationCount(),
                    timestamp(bot.publishedAt()),
                    Timestamp.from(bot.createdAt()),
                    Timestamp.from(bot.updatedAt()),
                    bot.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO agi_ai_bot
                                (id, name, description, avatar, workflow_id, model_provider_id, knowledge_base_id,
                                 system_prompt, opening_message, status, conversation_count, published_at, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    bot.id(),
                    bot.name(),
                    bot.description(),
                    bot.avatar(),
                    bot.workflowId(),
                    bot.modelProviderId(),
                    bot.knowledgeBaseId(),
                    bot.systemPrompt(),
                    bot.openingMessage(),
                    bot.status().name(),
                    bot.conversationCount(),
                    timestamp(bot.publishedAt()),
                    Timestamp.from(bot.createdAt()),
                    Timestamp.from(bot.updatedAt())
            );
        }
        return bot;
    }

    @Override
    public List<AiBot> list() {
        return jdbcTemplate.query("SELECT * FROM agi_ai_bot ORDER BY created_at", mapper());
    }

    @Override
    public Optional<AiBot> findById(String id) {
        return jdbcTemplate.query("SELECT * FROM agi_ai_bot WHERE id = ?", mapper(), id)
                .stream()
                .findFirst();
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM agi_ai_bot WHERE id = ?", id);
    }

    @Override
    public BotSession saveSession(BotSession session) {
        if (exists("agi_bot_session", session.id())) {
            jdbcTemplate.update("""
                            UPDATE agi_bot_session
                            SET bot_id = ?, title = ?, message_count = ?, created_at = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    session.botId(),
                    session.title(),
                    session.messageCount(),
                    Timestamp.from(session.createdAt()),
                    Timestamp.from(session.updatedAt()),
                    session.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO agi_bot_session (id, bot_id, title, message_count, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    session.id(),
                    session.botId(),
                    session.title(),
                    session.messageCount(),
                    Timestamp.from(session.createdAt()),
                    Timestamp.from(session.updatedAt())
            );
        }
        return session;
    }

    @Override
    public List<BotSession> listSessions(String botId) {
        return jdbcTemplate.query(
                "SELECT * FROM agi_bot_session WHERE bot_id = ? ORDER BY updated_at DESC",
                sessionMapper(),
                botId
        );
    }

    @Override
    public Optional<BotSession> findSessionById(String botId, String sessionId) {
        return jdbcTemplate.query(
                        "SELECT * FROM agi_bot_session WHERE bot_id = ? AND id = ?",
                        sessionMapper(),
                        botId,
                        sessionId
                )
                .stream()
                .findFirst();
    }

    @Override
    public BotMessage saveMessage(BotMessage message) {
        jdbcTemplate.update("""
                        INSERT INTO agi_bot_message (id, session_id, bot_id, role, content, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                message.id(),
                message.sessionId(),
                message.botId(),
                message.role().name(),
                message.content(),
                Timestamp.from(message.createdAt())
        );
        return message;
    }

    @Override
    public List<BotMessage> listMessages(String botId, String sessionId) {
        return jdbcTemplate.query(
                "SELECT * FROM agi_bot_message WHERE bot_id = ? AND session_id = ? ORDER BY created_at",
                messageMapper(),
                botId,
                sessionId
        );
    }

    private boolean exists(String id) {
        return exists("agi_ai_bot", id);
    }

    private boolean exists(String tableName, String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<AiBot> mapper() {
        return (rs, rowNum) -> new AiBot(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("avatar"),
                rs.getString("workflow_id"),
                rs.getString("model_provider_id"),
                rs.getString("knowledge_base_id"),
                rs.getString("system_prompt"),
                rs.getString("opening_message"),
                BotStatus.valueOf(rs.getString("status")),
                rs.getInt("conversation_count"),
                instantOrNull(rs, "published_at"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<BotSession> sessionMapper() {
        return (rs, rowNum) -> new BotSession(
                rs.getString("id"),
                rs.getString("bot_id"),
                rs.getString("title"),
                rs.getInt("message_count"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<BotMessage> messageMapper() {
        return (rs, rowNum) -> new BotMessage(
                rs.getString("id"),
                rs.getString("session_id"),
                rs.getString("bot_id"),
                BotMessageRole.valueOf(rs.getString("role")),
                rs.getString("content"),
                instant(rs, "created_at")
        );
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }

    private Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
