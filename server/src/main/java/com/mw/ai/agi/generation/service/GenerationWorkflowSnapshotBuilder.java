package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowEdge;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GenerationWorkflowSnapshotBuilder {
    private GenerationWorkflowSnapshotBuilder() {
    }

    public static String build(Workflow workflow, WorkflowVersion version, ObjectMapper objectMapper) {
        WorkflowDefinition definition = version.definition();
        List<Map<String, Object>> nodes = new ArrayList<>();
        int order = 1;
        for (WorkflowNode node : definition.nodes()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", node.id());
            item.put("name", node.name());
            item.put("type", node.type().name());
            item.put("order", order++);
            nodes.add(item);
        }
        List<Map<String, Object>> edges = new ArrayList<>();
        for (WorkflowEdge edge : definition.edges()) {
            edges.add(Map.of(
                    "id", edge.id(),
                    "from", edge.sourceNodeId(),
                    "to", edge.targetNodeId()
            ));
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("workflowId", workflow.id());
        snapshot.put("workflowVersionId", version.id());
        snapshot.put("version", version.version());
        snapshot.put("name", workflow.name());
        snapshot.put("nodes", nodes);
        snapshot.put("edges", edges);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build workflow snapshot.", exception);
        }
    }
}
