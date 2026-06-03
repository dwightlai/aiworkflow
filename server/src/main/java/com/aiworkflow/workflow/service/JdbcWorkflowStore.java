package com.aiworkflow.workflow.service;

import com.aiworkflow.workflow.domain.Workflow;
import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowStatus;
import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.domain.WorkflowVersionStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcWorkflowStore implements WorkflowStore {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcWorkflowStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Workflow saveWorkflow(Workflow workflow) {
        if (exists("agi_workflow", workflow.id())) {
            jdbcTemplate.update("""
                            UPDATE agi_workflow
                            SET tenant_id = ?, name = ?, description = ?, status = ?, current_version_id = ?,
                                created_by = ?, created_at = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    workflow.tenantId(),
                    workflow.name(),
                    workflow.description(),
                    workflow.status().name(),
                    workflow.currentVersionId(),
                    workflow.createdBy(),
                    Timestamp.from(workflow.createdAt()),
                    Timestamp.from(workflow.updatedAt()),
                    workflow.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO agi_workflow
                                (id, tenant_id, name, description, status, current_version_id, created_by, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    workflow.id(),
                    workflow.tenantId(),
                    workflow.name(),
                    workflow.description(),
                    workflow.status().name(),
                    workflow.currentVersionId(),
                    workflow.createdBy(),
                    Timestamp.from(workflow.createdAt()),
                    Timestamp.from(workflow.updatedAt())
            );
        }
        return workflow;
    }

    @Override
    public Optional<Workflow> findWorkflowById(String workflowId) {
        return jdbcTemplate.query("SELECT * FROM agi_workflow WHERE id = ?", workflowMapper(), workflowId)
                .stream()
                .findFirst();
    }

    @Override
    public List<Workflow> listWorkflows() {
        return jdbcTemplate.query("SELECT * FROM agi_workflow ORDER BY created_at", workflowMapper());
    }

    @Override
    public WorkflowVersion saveVersion(WorkflowVersion version) {
        String definitionJson = writeJson(version.definition());
        if (exists("agi_workflow_version", version.id())) {
            jdbcTemplate.update("""
                            UPDATE agi_workflow_version
                            SET workflow_id = ?, version = ?, definition_json = ?, status = ?,
                                published_by = ?, published_at = ?, created_at = ?
                            WHERE id = ?
                            """,
                    version.workflowId(),
                    version.version(),
                    definitionJson,
                    version.status().name(),
                    version.publishedBy(),
                    timestampOrNull(version.publishedAt()),
                    Timestamp.from(version.createdAt()),
                    version.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO agi_workflow_version
                                (id, workflow_id, version, definition_json, status, published_by, published_at, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    version.id(),
                    version.workflowId(),
                    version.version(),
                    definitionJson,
                    version.status().name(),
                    version.publishedBy(),
                    timestampOrNull(version.publishedAt()),
                    Timestamp.from(version.createdAt())
            );
        }
        return version;
    }

    @Override
    public Optional<WorkflowVersion> findVersionById(String versionId) {
        return jdbcTemplate.query("SELECT * FROM agi_workflow_version WHERE id = ?", versionMapper(), versionId)
                .stream()
                .findFirst();
    }

    @Override
    public List<WorkflowVersion> listVersions(String workflowId) {
        return jdbcTemplate.query(
                "SELECT * FROM agi_workflow_version WHERE workflow_id = ? ORDER BY version",
                versionMapper(),
                workflowId
        );
    }

    private boolean exists(String tableName, String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<Workflow> workflowMapper() {
        return (rs, rowNum) -> new Workflow(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("name"),
                rs.getString("description"),
                WorkflowStatus.valueOf(rs.getString("status")),
                rs.getString("current_version_id"),
                rs.getString("created_by"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<WorkflowVersion> versionMapper() {
        return (rs, rowNum) -> new WorkflowVersion(
                rs.getString("id"),
                rs.getString("workflow_id"),
                rs.getInt("version"),
                readDefinition(rs.getString("definition_json")),
                WorkflowVersionStatus.valueOf(rs.getString("status")),
                rs.getString("published_by"),
                instantOrNull(rs, "published_at"),
                instant(rs, "created_at")
        );
    }

    private WorkflowDefinition readDefinition(String value) {
        try {
            return objectMapper.readValue(value, WorkflowDefinition.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read workflow definition JSON.", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to write JSON.", ex);
        }
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }

    private Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Timestamp timestampOrNull(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
