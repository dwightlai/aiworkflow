package com.aiworkflow.workflow.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record WorkflowDefinition(
        List<WorkflowNode> nodes,
        List<WorkflowEdge> edges,
        List<WorkflowVariable> variables
) {
    public WorkflowDefinition {
        nodes = copyListOrEmpty(nodes);
        edges = copyListOrEmpty(edges);
        variables = copyListOrEmpty(variables);
    }

    private static <T> List<T> copyListOrEmpty(List<T> items) {
        if (items == null) {
            return List.of();
        }
        try {
            return List.copyOf(items);
        } catch (NullPointerException ignored) {
            return Collections.unmodifiableList(new ArrayList<>(items));
        }
    }
}
