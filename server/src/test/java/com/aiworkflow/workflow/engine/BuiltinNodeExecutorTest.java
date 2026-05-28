package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinNodeExecutorTest {

    @Test
    void startNodePassesInputThrough() {
        StartNodeExecutor executor = new StartNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada")
        );

        NodeExecutionResult result = executor.execute(node("start", WorkflowNodeType.START, Map.of()), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("name", "Ada"));
        assertThat(result.nextNodeId()).isEmpty();
    }

    @Test
    void endNodeReturnsConfiguredOutputKeys() {
        EndNodeExecutor executor = new EndNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada", "message", "Hello Ada", "internal", "hidden")
        );

        NodeExecutionResult result = executor.execute(node(
                "end",
                WorkflowNodeType.END,
                Map.of("outputKeys", List.of("message", "missing"))
        ), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("message", "Hello Ada"));
        assertThat(result.nextNodeId()).isEmpty();
    }

    @Test
    void textTransformNodeRendersTemplateFromContext() {
        TextTransformNodeExecutor executor = new TextTransformNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("name", "Ada"),
                Map.of("name", "Ada", "company", "Lovelace Labs")
        );

        NodeExecutionResult result = executor.execute(node(
                "transform",
                WorkflowNodeType.TEXT_TRANSFORM,
                Map.of("outputKey", "message", "template", "Hello {{name}} from {{company}}")
        ), context);

        assertThat(result.output()).containsExactlyEntriesOf(Map.of("message", "Hello Ada from Lovelace Labs"));
        assertThat(result.nextNodeId()).isEmpty();
    }

    @Test
    void conditionNodeSelectsMatchedBranch() {
        ConditionNodeExecutor executor = new ConditionNodeExecutor();
        NodeExecutionContext context = new NodeExecutionContext(
                Map.of("tier", "vip"),
                Map.of("tier", "vip")
        );

        NodeExecutionResult result = executor.execute(node(
                "condition",
                WorkflowNodeType.CONDITION,
                Map.of(
                        "contextKey", "tier",
                        "equals", "vip",
                        "trueTargetNodeId", "vip-path",
                        "falseTargetNodeId", "normal-path"
                )
        ), context);

        assertThat(result.output()).isEmpty();
        assertThat(result.nextNodeId()).contains("vip-path");
    }

    private WorkflowNode node(String id, WorkflowNodeType type, Map<String, Object> config) {
        return new WorkflowNode(id, type, id, config);
    }
}
