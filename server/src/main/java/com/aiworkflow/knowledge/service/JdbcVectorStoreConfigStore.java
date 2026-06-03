package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.VectorStoreConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcVectorStoreConfigStore implements VectorStoreConfigStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcVectorStoreConfigStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public VectorStoreConfig save(VectorStoreConfig config) {
        if (exists(config.id())) {
            jdbcTemplate.update("""
                            UPDATE agi_vector_store_config
                            SET name = ?, store_type = ?, endpoint = ?, index_name = ?, username = ?, password = ?,
                                api_key = ?, connect_timeout_ms = ?, read_timeout_ms = ?, enabled = ?, created_at = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    config.name(),
                    config.storeType(),
                    config.endpoint(),
                    config.indexName(),
                    config.username(),
                    config.password(),
                    config.apiKey(),
                    config.connectTimeoutMs(),
                    config.readTimeoutMs(),
                    config.enabled(),
                    Timestamp.from(config.createdAt()),
                    Timestamp.from(config.updatedAt()),
                    config.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO agi_vector_store_config
                                (id, name, store_type, endpoint, index_name, username, password, api_key,
                                 connect_timeout_ms, read_timeout_ms, enabled, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    config.id(),
                    config.name(),
                    config.storeType(),
                    config.endpoint(),
                    config.indexName(),
                    config.username(),
                    config.password(),
                    config.apiKey(),
                    config.connectTimeoutMs(),
                    config.readTimeoutMs(),
                    config.enabled(),
                    Timestamp.from(config.createdAt()),
                    Timestamp.from(config.updatedAt())
            );
        }
        return config;
    }

    @Override
    public Optional<VectorStoreConfig> findById(String id) {
        return jdbcTemplate.query("SELECT * FROM agi_vector_store_config WHERE id = ?", mapper(), id)
                .stream()
                .findFirst();
    }

    @Override
    public List<VectorStoreConfig> list() {
        return jdbcTemplate.query("SELECT * FROM agi_vector_store_config ORDER BY created_at", mapper());
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM agi_vector_store_config WHERE id = ?", id);
    }

    private boolean exists(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agi_vector_store_config WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<VectorStoreConfig> mapper() {
        return (rs, rowNum) -> new VectorStoreConfig(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("store_type"),
                rs.getString("endpoint"),
                rs.getString("index_name"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("api_key"),
                rs.getInt("connect_timeout_ms"),
                rs.getInt("read_timeout_ms"),
                rs.getBoolean("enabled"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
