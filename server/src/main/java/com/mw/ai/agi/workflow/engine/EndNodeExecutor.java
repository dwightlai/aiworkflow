package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EndNodeExecutor implements WorkflowNodeExecutor {
    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.END;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        Object outputKeys = node.config().get("outputKeys");
        if (!(outputKeys instanceof List<?> keys)) {
            return NodeExecutionResult.output(context.context());
        }

        Map<String, Object> output = new LinkedHashMap<>();
        for (Object key : keys) {
            if (key instanceof String stringKey && context.context().containsKey(stringKey)) {
                output.put(stringKey, context.context().get(stringKey));
            }
        }
        return NodeExecutionResult.output(output);
    }
}
