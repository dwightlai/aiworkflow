package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

@Component
public class ConditionNodeExecutor implements WorkflowNodeExecutor {
    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.CONDITION;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String contextKey = requiredStringConfig(node, "contextKey");
        String operator = optionalStringConfig(node, "operator", "EQUALS");
        String expectedValue = optionalStringConfig(node, "compareValue", optionalStringConfig(node, "equals", ""));
        String trueTargetNodeId = requiredStringConfig(node, "trueTargetNodeId");
        String falseTargetNodeId = requiredStringConfig(node, "falseTargetNodeId");

        Object actualValue = context.context().get(contextKey);
        String nextNodeId = matches(operator, actualValue, expectedValue) ? trueTargetNodeId : falseTargetNodeId;
        return NodeExecutionResult.branch(nextNodeId);
    }

    private boolean matches(String operator, Object actualValue, String expectedValue) {
        String actual = actualValue == null ? "" : String.valueOf(actualValue);
        return switch (operator) {
            case "NOT_EQUALS" -> !actual.equals(expectedValue);
            case "CONTAINS" -> actual.contains(expectedValue);
            case "IS_EMPTY" -> actual.isBlank();
            case "IS_NOT_EMPTY" -> !actual.isBlank();
            case "EQUALS" -> actual.equals(expectedValue);
            default -> throw new IllegalArgumentException("Unsupported condition operator: " + operator);
        };
    }

    private String requiredStringConfig(WorkflowNode node, String key) {
        Object value = node.config().get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("Workflow node " + node.id() + " requires config: " + key);
        }
        return stringValue;
    }

    private String optionalStringConfig(WorkflowNode node, String key, String defaultValue) {
        Object value = node.config().get(key);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }
}
