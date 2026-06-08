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
        Object outputParams = node.config().get("outputParams");
        if (outputParams instanceof List<?> params) {
            Map<String, Object> output = new LinkedHashMap<>();
            for (Object item : params) {
                if (item instanceof Map<?, ?> param) {
                    String name = stringValue(param.get("name"), "");
                    String source = stringValue(param.get("value"), stringValue(param.get("source"), name));
                    if (!name.isBlank()) {
                        output.put(name, resolveValue(context.context(), source));
                    }
                }
            }
            return NodeExecutionResult.output(output);
        }

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

    private Object resolveValue(Map<String, Object> context, String source) {
        Object value = TemplateRenderer.resolvePath(context, source);
        return value == null ? "" : value;
    }

    private String stringValue(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }
}
