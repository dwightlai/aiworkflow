package com.aiworkflow.model.service;

import com.aiworkflow.model.domain.ModelProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcModelProviderStore implements ModelProviderStore {
    private static final String DEFAULT_TENANT_ID = "default";

    private final JdbcTemplate jdbcTemplate;

    public JdbcModelProviderStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ModelProvider save(ModelProvider provider) {
        if (exists(provider.id())) {
            jdbcTemplate.update("""
                            UPDATE model_provider
                            SET tenant_id = ?, name = ?, model_type = ?, model_usage = ?, description = ?, vision_support = ?,
                                price_per_million_tokens = ?, base_url = ?, model = ?, api_key_ref = ?,
                                enabled = ?, created_at = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    DEFAULT_TENANT_ID,
                    provider.name(),
                    provider.modelType(),
                    provider.modelUsage(),
                    provider.description(),
                    provider.visionSupport(),
                    provider.pricePerMillionTokens(),
                    provider.baseUrl(),
                    provider.model(),
                    provider.apiKeyRef(),
                    provider.enabled(),
                    Timestamp.from(provider.createdAt()),
                    Timestamp.from(provider.updatedAt()),
                    provider.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO model_provider
                                (id, tenant_id, name, model_type, model_usage, description, vision_support, price_per_million_tokens,
                                 base_url, model, api_key_ref, enabled, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    provider.id(),
                    DEFAULT_TENANT_ID,
                    provider.name(),
                    provider.modelType(),
                    provider.modelUsage(),
                    provider.description(),
                    provider.visionSupport(),
                    provider.pricePerMillionTokens(),
                    provider.baseUrl(),
                    provider.model(),
                    provider.apiKeyRef(),
                    provider.enabled(),
                    Timestamp.from(provider.createdAt()),
                    Timestamp.from(provider.updatedAt())
            );
        }
        return provider;
    }

    @Override
    public Optional<ModelProvider> findById(String id) {
        return jdbcTemplate.query("SELECT * FROM model_provider WHERE id = ?", mapper(), id)
                .stream()
                .findFirst();
    }

    @Override
    public List<ModelProvider> list() {
        return jdbcTemplate.query("SELECT * FROM model_provider ORDER BY created_at", mapper());
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM model_provider WHERE id = ?", id);
    }

    private boolean exists(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM model_provider WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<ModelProvider> mapper() {
        return (rs, rowNum) -> new ModelProvider(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("model_type"),
                rs.getString("model_usage"),
                rs.getString("description"),
                rs.getBoolean("vision_support"),
                rs.getBigDecimal("price_per_million_tokens"),
                rs.getString("base_url"),
                rs.getString("model"),
                rs.getString("api_key_ref"),
                rs.getBoolean("enabled"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
