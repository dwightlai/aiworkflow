package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ContentTemplateNodeExecutor implements WorkflowNodeExecutor {
    private final ObjectMapper objectMapper;

    public ContentTemplateNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.CONTENT_TEMPLATE;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String outputKey = optionalStringConfig(node, "outputKey", "output");
        String template = requiredStringConfig(node, "template");
        String rendered = TemplateRenderer.render(template, context.context());
        String outputFormat = optionalStringConfig(node, "outputFormat", "TEXT");
        return NodeExecutionResult.output(Map.of(outputKey, parseRenderedOutput(rendered, outputFormat)));
    }

    Map<String, Object> executeInline(Map<String, Object> step, Map<String, Object> context) {
        String outputKey = stringValue(step.get("outputKey"), "output");
        String template = stringValue(step.get("template"), "");
        String rendered = TemplateRenderer.render(template, context);
        String outputFormat = stringValue(step.get("outputFormat"), "TEXT");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(outputKey, parseRenderedOutput(rendered, outputFormat));
        return output;
    }

    private Object parseRenderedOutput(String rendered, String outputFormat) {
        if ("JSON".equalsIgnoreCase(outputFormat)) {
            try {
                return objectMapper.readValue(rendered, Object.class);
            } catch (JsonProcessingException ex) {
                throw new IllegalArgumentException("Content template output is not valid JSON", ex);
            }
        }
        return rendered;
    }

    private String requiredStringConfig(WorkflowNode node, String key) {
        Object value = node.config().get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("Workflow node " + node.id() + " requires config: " + key);
        }
        return stringValue;
    }

    private String optionalStringConfig(WorkflowNode node, String key, String defaultValue) {
        return stringValue(node.config().get(key), defaultValue);
    }

    private String stringValue(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }
}
