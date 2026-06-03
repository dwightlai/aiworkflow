package com.aiworkflow.workflow.service;

import com.aiworkflow.workflow.domain.Workflow;
import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowEdge;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import com.aiworkflow.workflow.domain.WorkflowStatus;
import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.domain.WorkflowVersionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(properties = "spring.flyway.enabled=false")
class JdbcWorkflowStoreTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JdbcWorkflowStore store;

    @BeforeEach
    void setUp() {
        store = new JdbcWorkflowStore(jdbcTemplate, new ObjectMapper().findAndRegisterModules());
        jdbcTemplate.execute("""
                CREATE TABLE agi_workflow (
                    id VARCHAR(64) PRIMARY KEY,
                    tenant_id VARCHAR(64) NOT NULL,
                    name VARCHAR(200) NOT NULL,
                    description CLOB,
                    status VARCHAR(32) NOT NULL,
                    current_version_id VARCHAR(64),
                    created_by VARCHAR(64) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE agi_workflow_version (
                    id VARCHAR(64) PRIMARY KEY,
                    workflow_id VARCHAR(64) NOT NULL,
                    version INTEGER NOT NULL,
                    definition_json CLOB NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    published_by VARCHAR(64),
                    published_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL
                )
                """);
    }

    @Test
    void savesWorkflowAndVersionThenReadsThemBack() {
        Instant now = Instant.parse("2026-05-28T09:00:00Z");
        Workflow workflow = new Workflow(
                "workflow-1",
                "tenant-default",
                "Support triage",
                "Routes messages",
                WorkflowStatus.DRAFT,
                null,
                "system",
                now,
                now
        );
        WorkflowDefinition definition = new WorkflowDefinition(
                List.of(
                        new WorkflowNode("start", WorkflowNodeType.START, "Start", Map.of()),
                        new WorkflowNode("end", WorkflowNodeType.END, "End", Map.of("outputKeys", List.of("message")))
                ),
                List.of(new WorkflowEdge("edge-1", "start", "end", null)),
                List.of()
        );
        WorkflowVersion version = new WorkflowVersion(
                "version-1",
                workflow.id(),
                1,
                definition,
                WorkflowVersionStatus.DRAFT,
                null,
                null,
                now
        );

        store.saveWorkflow(workflow);
        store.saveVersion(version);

        assertThat(store.findWorkflowById(workflow.id())).contains(workflow);
        assertThat(store.listWorkflows()).containsExactly(workflow);
        assertThat(store.findVersionById(version.id()).orElseThrow().definition().nodes()).hasSize(2);
        assertThat(store.listVersions(workflow.id())).extracting(WorkflowVersion::id).containsExactly("version-1");
    }
}
