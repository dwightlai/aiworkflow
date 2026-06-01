package com.aiworkflow.prompt.service;

import com.aiworkflow.prompt.domain.PromptTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcPromptTemplateStore implements PromptTemplateStore {
    private static final String DEFAULT_TENANT_ID = "default";

    private final JdbcTemplate jdbcTemplate;

    public JdbcPromptTemplateStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PromptTemplate save(PromptTemplate template) {
        if (exists(template.id())) {
            jdbcTemplate.update("""
                            UPDATE prompt_template
                            SET tenant_id = ?, name = ?, template = ?, description = ?, created_at = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    DEFAULT_TENANT_ID,
                    template.name(),
                    template.template(),
                    template.description(),
                    Timestamp.from(template.createdAt()),
                    Timestamp.from(template.updatedAt()),
                    template.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO prompt_template
                                (id, tenant_id, name, template, description, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    template.id(),
                    DEFAULT_TENANT_ID,
                    template.name(),
                    template.template(),
                    template.description(),
                    Timestamp.from(template.createdAt()),
                    Timestamp.from(template.updatedAt())
            );
        }
        return template;
    }

    @Override
    public Optional<PromptTemplate> findById(String id) {
        return jdbcTemplate.query("SELECT * FROM prompt_template WHERE id = ?", mapper(), id)
                .stream()
                .findFirst();
    }

    @Override
    public List<PromptTemplate> list() {
        return jdbcTemplate.query("SELECT * FROM prompt_template ORDER BY created_at", mapper());
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM prompt_template WHERE id = ?", id);
    }

    private boolean exists(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prompt_template WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<PromptTemplate> mapper() {
        return (rs, rowNum) -> new PromptTemplate(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("template"),
                rs.getString("description"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
