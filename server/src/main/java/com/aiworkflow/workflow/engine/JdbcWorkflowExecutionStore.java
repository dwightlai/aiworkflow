package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNodeType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcWorkflowExecutionStore implements WorkflowExecutionStore {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcWorkflowExecutionStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public WorkflowExecution saveWorkflowExecution(WorkflowExecution execution) {
        if (exists("workflow_execution", execution.id())) {
            jdbcTemplate.update("""
                            UPDATE workflow_execution
                            SET workflow_id = ?, workflow_version_id = ?, status = ?, input_json = ?, context_json = ?,
                                output_json = ?, error_message = ?, started_at = ?, finished_at = ?
                            WHERE id = ?
                            """,
                    execution.workflowId(),
                    execution.workflowVersionId(),
                    execution.status().name(),
                    writeJson(execution.input()),
                    "{}",
                    writeJson(execution.output()),
                    execution.errorMessage(),
                    timestampOrNull(execution.startedAt()),
                    timestampOrNull(execution.finishedAt()),
                    execution.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO workflow_execution
                                (id, workflow_id, workflow_version_id, tenant_id, status, input_json, context_json,
                                 output_json, error_code, error_message, started_at, finished_at, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    execution.id(),
                    execution.workflowId(),
                    execution.workflowVersionId(),
                    "tenant-default",
                    execution.status().name(),
                    writeJson(execution.input()),
                    "{}",
                    writeJson(execution.output()),
                    null,
                    execution.errorMessage(),
                    timestampOrNull(execution.startedAt()),
                    timestampOrNull(execution.finishedAt()),
                    Timestamp.from(Instant.now())
            );
        }
        return execution;
    }

    @Override
    public Optional<WorkflowExecution> findWorkflowExecutionById(String executionId) {
        return jdbcTemplate.query("SELECT * FROM workflow_execution WHERE id = ?", workflowExecutionMapper(), executionId)
                .stream()
                .findFirst();
    }

    @Override
    public List<WorkflowExecution> listWorkflowExecutions() {
        return jdbcTemplate.query("SELECT * FROM workflow_execution ORDER BY started_at DESC", workflowExecutionMapper());
    }

    @Override
    public NodeExecution saveNodeExecution(NodeExecution nodeExecution) {
        jdbcTemplate.update("""
                        INSERT INTO workflow_node_execution
                            (id, workflow_execution_id, node_id, node_type, status, input_json, output_json,
                             error_code, error_message, started_at, finished_at, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                nodeExecution.id(),
                nodeExecution.workflowExecutionId(),
                nodeExecution.nodeId(),
                nodeExecution.nodeType().name(),
                nodeExecution.status().name(),
                writeJson(nodeExecution.input()),
                writeJson(nodeExecution.output()),
                null,
                nodeExecution.errorMessage(),
                timestampOrNull(nodeExecution.startedAt()),
                timestampOrNull(nodeExecution.finishedAt()),
                Timestamp.from(Instant.now())
        );
        return nodeExecution;
    }

    @Override
    public List<NodeExecution> listNodeExecutions(String workflowExecutionId) {
        return jdbcTemplate.query(
                "SELECT * FROM workflow_node_execution WHERE workflow_execution_id = ? ORDER BY started_at",
                nodeExecutionMapper(),
                workflowExecutionId
        );
    }

    private RowMapper<WorkflowExecution> workflowExecutionMapper() {
        return (rs, rowNum) -> new WorkflowExecution(
                rs.getString("id"),
                rs.getString("workflow_id"),
                rs.getString("workflow_version_id"),
                WorkflowExecutionStatus.valueOf(rs.getString("status")),
                readMap(rs.getString("input_json")),
                readMap(rs.getString("output_json")),
                rs.getString("error_message"),
                instantOrNull(rs, "started_at"),
                instantOrNull(rs, "finished_at")
        );
    }

    private RowMapper<NodeExecution> nodeExecutionMapper() {
        return (rs, rowNum) -> new NodeExecution(
                rs.getString("id"),
                rs.getString("workflow_execution_id"),
                rs.getString("node_id"),
                WorkflowNodeType.valueOf(rs.getString("node_type")),
                NodeExecutionStatus.valueOf(rs.getString("status")),
                readMap(rs.getString("input_json")),
                readMap(rs.getString("output_json")),
                rs.getString("error_message"),
                instantOrNull(rs, "started_at"),
                instantOrNull(rs, "finished_at")
        );
    }

    private boolean exists(String tableName, String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read execution JSON.", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to write execution JSON.", ex);
        }
    }

    private Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Timestamp timestampOrNull(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
