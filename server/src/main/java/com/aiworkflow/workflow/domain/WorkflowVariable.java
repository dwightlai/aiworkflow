package com.aiworkflow.workflow.domain;

public record WorkflowVariable(
        String name,
        WorkflowVariableType type,
        boolean required
) {
}
