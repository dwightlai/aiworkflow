package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowNodeExecutorRegistry {
    private final Map<WorkflowNodeType, WorkflowNodeExecutor> executors;

    public WorkflowNodeExecutorRegistry(List<WorkflowNodeExecutor> executors) {
        Map<WorkflowNodeType, WorkflowNodeExecutor> registeredExecutors = new EnumMap<>(WorkflowNodeType.class);
        for (WorkflowNodeExecutor executor : executors) {
            registeredExecutors.put(executor.nodeType(), executor);
        }
        this.executors = Map.copyOf(registeredExecutors);
    }

    public WorkflowNodeExecutor getExecutor(WorkflowNodeType nodeType) {
        WorkflowNodeExecutor executor = executors.get(nodeType);
        if (executor == null) {
            throw new IllegalArgumentException("Unsupported workflow node type: " + nodeType);
        }
        return executor;
    }
}
