package com.aiworkflow.workflow.service;

import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowEdge;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DagValidator {
    private static final String DEFINITION_REQUIRED = "Workflow definition is required.";
    private static final String EXACTLY_ONE_START = "Workflow definition must contain exactly one START node.";
    private static final String AT_LEAST_ONE_END = "Workflow definition must contain at least one END node.";
    private static final String MISSING_EDGE_NODE = "Workflow edge references missing node.";
    private static final String ACYCLIC = "Workflow definition must be acyclic.";

    public void validate(WorkflowDefinition definition) {
        if (definition == null) {
            throw new DagValidationException(DEFINITION_REQUIRED);
        }

        List<WorkflowNode> nodes = listOrEmpty(definition.nodes());
        List<WorkflowEdge> edges = listOrEmpty(definition.edges());

        long startCount = nodes.stream()
                .filter(node -> node.type() == WorkflowNodeType.START)
                .count();
        if (startCount != 1) {
            throw new DagValidationException(EXACTLY_ONE_START);
        }

        boolean hasEndNode = nodes.stream()
                .anyMatch(node -> node.type() == WorkflowNodeType.END);
        if (!hasEndNode) {
            throw new DagValidationException(AT_LEAST_ONE_END);
        }

        Map<String, List<String>> adjacency = buildAdjacency(nodes, edges);
        rejectCycles(adjacency);
    }

    private Map<String, List<String>> buildAdjacency(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        Set<String> nodeIds = new HashSet<>();
        Map<String, List<String>> adjacency = new HashMap<>();
        for (WorkflowNode node : nodes) {
            nodeIds.add(node.id());
            adjacency.putIfAbsent(node.id(), new ArrayList<>());
        }

        for (WorkflowEdge edge : edges) {
            if (!nodeIds.contains(edge.sourceNodeId()) || !nodeIds.contains(edge.targetNodeId())) {
                throw new DagValidationException(MISSING_EDGE_NODE);
            }
            adjacency.get(edge.sourceNodeId()).add(edge.targetNodeId());
        }

        return adjacency;
    }

    private void rejectCycles(Map<String, List<String>> adjacency) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (String nodeId : adjacency.keySet()) {
            if (hasCycle(nodeId, adjacency, visiting, visited)) {
                throw new DagValidationException(ACYCLIC);
            }
        }
    }

    private boolean hasCycle(
            String nodeId,
            Map<String, List<String>> adjacency,
            Set<String> visiting,
            Set<String> visited
    ) {
        if (visited.contains(nodeId)) {
            return false;
        }
        if (!visiting.add(nodeId)) {
            return true;
        }

        for (String targetNodeId : adjacency.getOrDefault(nodeId, List.of())) {
            if (hasCycle(targetNodeId, adjacency, visiting, visited)) {
                return true;
            }
        }

        visiting.remove(nodeId);
        visited.add(nodeId);
        return false;
    }

    private static <T> List<T> listOrEmpty(List<T> items) {
        return items == null ? List.of() : items;
    }
}
