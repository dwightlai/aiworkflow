package com.mw.ai.agi.workflow.engine;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryWorkflowExecutionStore implements WorkflowExecutionStore {
    private final ConcurrentMap<String, WorkflowExecution> workflowExecutions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<NodeExecution>> nodeExecutionsByWorkflowExecutionId = new ConcurrentHashMap<>();

    @Override
    public WorkflowExecution saveWorkflowExecution(WorkflowExecution execution) {
        workflowExecutions.put(execution.id(), execution);
        return execution;
    }

    @Override
    public Optional<WorkflowExecution> findWorkflowExecutionById(String executionId) {
        return Optional.ofNullable(workflowExecutions.get(executionId));
    }

    @Override
    public List<WorkflowExecution> listWorkflowExecutions() {
        return workflowExecutions.values().stream()
                .sorted(Comparator.comparing(WorkflowExecution::startedAt).reversed())
                .toList();
    }

    @Override
    public NodeExecution saveNodeExecution(NodeExecution nodeExecution) {
        nodeExecutionsByWorkflowExecutionId.compute(nodeExecution.workflowExecutionId(), (ignored, existing) -> {
            List<NodeExecution> executions = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            executions.add(nodeExecution);
            return List.copyOf(executions);
        });
        return nodeExecution;
    }

    @Override
    public List<NodeExecution> listNodeExecutions(String workflowExecutionId) {
        return nodeExecutionsByWorkflowExecutionId.getOrDefault(workflowExecutionId, List.of());
    }
}
