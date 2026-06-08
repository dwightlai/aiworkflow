package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StartNodeExecutor implements WorkflowNodeExecutor {
    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.START;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        Map<String, Object> output = new LinkedHashMap<>(context.input());
        output.put("start", context.input());
        return NodeExecutionResult.output(output);
    }
}
