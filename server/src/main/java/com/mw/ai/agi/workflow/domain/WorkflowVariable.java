package com.mw.ai.agi.workflow.domain;

public record WorkflowVariable(
        String name,
        WorkflowVariableType type,
        boolean required
) {
}
