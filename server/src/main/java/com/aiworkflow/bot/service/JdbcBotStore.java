package com.aiworkflow.bot.service;

import com.aiworkflow.bot.domain.AiBot;
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
                            UPDATE ai_bot
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
                            INSERT INTO ai_bot
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
        return jdbcTemplate.query("SELECT * FROM ai_bot ORDER BY created_at", mapper());
    }

    @Override
    public Optional<AiBot> findById(String id) {
        return jdbcTemplate.query("SELECT * FROM ai_bot WHERE id = ?", mapper(), id)
                .stream()
                .findFirst();
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM ai_bot WHERE id = ?", id);
    }

    private boolean exists(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_bot WHERE id = ?", Integer.class, id);
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
