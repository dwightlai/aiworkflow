package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryWorkflowExecutionStoreTest {

    private final WorkflowExecutionStore store = new InMemoryWorkflowExecutionStore();

    @Test
    void savesAndFindsWorkflowExecution() {
        WorkflowExecution execution = workflowExecution("execution-1", WorkflowExecutionStatus.RUNNING);

        WorkflowExecution saved = store.saveWorkflowExecution(execution);

        assertThat(saved).isEqualTo(execution);
        assertThat(store.findWorkflowExecutionById("execution-1")).contains(execution);
        assertThat(store.findWorkflowExecutionById("missing")).isEmpty();
    }

    @Test
    void savesAndListsNodeExecutionsInCreatedOrder() {
        NodeExecution first = nodeExecution("node-execution-1", "execution-1", "start", WorkflowNodeType.START);
        NodeExecution second = nodeExecution("node-execution-2", "execution-1", "transform", WorkflowNodeType.TEXT_TRANSFORM);

        store.saveNodeExecution(first);
        store.saveNodeExecution(second);

        assertThat(store.listNodeExecutions("execution-1")).containsExactly(first, second);
        assertThat(store.listNodeExecutions("other-execution")).isEmpty();
    }

    @Test
    void returnsImmutableSnapshots() {
        Map<String, Object> input = new HashMap<>();
        input.put("name", "Ada");
        WorkflowExecution execution = new WorkflowExecution(
                "execution-1",
                "workflow-1",
                "version-1",
                WorkflowExecutionStatus.SUCCEEDED,
                input,
                Map.of("message", "Hello Ada"),
                null,
                Instant.parse("2026-05-28T01:00:00Z"),
                Instant.parse("2026-05-28T01:00:01Z")
        );

        store.saveWorkflowExecution(execution);
        input.put("name", "Grace");

        WorkflowExecution saved = store.findWorkflowExecutionById("execution-1").orElseThrow();
        assertThat(saved.input()).containsEntry("name", "Ada");
        assertThatThrownBy(() -> saved.input().put("name", "Grace"))
                .isInstanceOf(UnsupportedOperationException.class);

        List<NodeExecution> nodeExecutions = store.listNodeExecutions("execution-1");
        assertThatThrownBy(() -> nodeExecutions.add(nodeExecution(
                "node-execution-1",
                "execution-1",
                "start",
                WorkflowNodeType.START
        ))).isInstanceOf(UnsupportedOperationException.class);
    }

    private WorkflowExecution workflowExecution(String id, WorkflowExecutionStatus status) {
        return new WorkflowExecution(
                id,
                "workflow-1",
                "version-1",
                status,
                Map.of("name", "Ada"),
                Map.of(),
                null,
                Instant.parse("2026-05-28T01:00:00Z"),
                null
        );
    }

    private NodeExecution nodeExecution(
            String id,
            String workflowExecutionId,
            String nodeId,
            WorkflowNodeType nodeType
    ) {
        return new NodeExecution(
                id,
                workflowExecutionId,
                nodeId,
                nodeType,
                NodeExecutionStatus.SUCCEEDED,
                Map.of("name", "Ada"),
                Map.of("name", "Ada"),
                null,
                Instant.parse("2026-05-28T01:00:00Z"),
                Instant.parse("2026-05-28T01:00:01Z")
        );
    }
}
