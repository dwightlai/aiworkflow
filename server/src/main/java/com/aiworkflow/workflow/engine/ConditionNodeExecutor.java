package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
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
        String expectedValue = requiredStringConfig(node, "equals");
        String trueTargetNodeId = requiredStringConfig(node, "trueTargetNodeId");
        String falseTargetNodeId = requiredStringConfig(node, "falseTargetNodeId");

        Object actualValue = context.context().get(contextKey);
        String nextNodeId = expectedValue.equals(String.valueOf(actualValue))
                ? trueTargetNodeId
                : falseTargetNodeId;
        return NodeExecutionResult.branch(nextNodeId);
    }

    private String requiredStringConfig(WorkflowNode node, String key) {
        Object value = node.config().get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("Workflow node " + node.id() + " requires config: " + key);
        }
        return stringValue;
    }
}
