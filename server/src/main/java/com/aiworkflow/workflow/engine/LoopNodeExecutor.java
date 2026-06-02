package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LoopNodeExecutor implements WorkflowNodeExecutor {
    private final ContentTemplateNodeExecutor contentTemplateNodeExecutor;
    private final HttpToolNodeExecutor httpToolNodeExecutor;

    public LoopNodeExecutor(ObjectMapper objectMapper) {
        this.contentTemplateNodeExecutor = new ContentTemplateNodeExecutor(objectMapper);
        this.httpToolNodeExecutor = new HttpToolNodeExecutor(objectMapper);
    }

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.LOOP;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String loopVar = optionalStringConfig(node, "loopVar", "items");
        String itemVar = optionalStringConfig(node, "itemVar", "loopItem");
        String indexVar = optionalStringConfig(node, "indexVar", "index");
        String outputKey = optionalStringConfig(node, "outputKey", "loopResults");
        int maxIterations = intConfig(node, "maxIterations", 100);
        List<?> items = resolveItems(loopVar, context.context());
        List<Map<String, Object>> steps = steps(node);
        List<Object> results = new ArrayList<>();

        int count = Math.min(items.size(), maxIterations);
        for (int index = 0; index < count; index++) {
            Map<String, Object> loopContext = new LinkedHashMap<>(context.context());
            loopContext.put(itemVar, items.get(index));
            loopContext.put(indexVar, index);
            if (steps.isEmpty()) {
                results.add(items.get(index));
                continue;
            }

            Map<String, Object> iterationOutput = new LinkedHashMap<>();
            for (Map<String, Object> step : steps) {
                Map<String, Object> stepOutput = executeStep(step, loopContext);
                loopContext.putAll(stepOutput);
                iterationOutput.putAll(stepOutput);
            }
            results.add(iterationOutput);
        }

        return NodeExecutionResult.output(Map.of(outputKey, results));
    }

    private Map<String, Object> executeStep(Map<String, Object> step, Map<String, Object> context) {
        String type = String.valueOf(step.getOrDefault("type", "CONTENT_TEMPLATE")).toUpperCase();
        if (type.equals("HTTP") || type.equals("HTTP_TOOL")) {
            String outputKey = stringValue(step.get("outputKey"), "httpResult");
            return Map.of(outputKey, httpToolNodeExecutor.executeInline(step, context));
        }
        return contentTemplateNodeExecutor.executeInline(step, context);
    }

    private List<?> resolveItems(String loopVar, Map<String, Object> context) {
        Object value = TemplateRenderer.resolvePath(context, loopVar);
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Object[] array) {
            return List.of(array);
        }
        if (value == null) {
            return List.of();
        }
        return List.of(value);
    }

    private List<Map<String, Object>> steps(WorkflowNode node) {
        Object value = node.config().get("loopSteps");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> steps = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> step = new LinkedHashMap<>();
                map.forEach((key, stepValue) -> {
                    if (key instanceof String keyValue) {
                        step.put(keyValue, stepValue);
                    }
                });
                steps.add(step);
            }
        }
        return steps;
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

    private int intConfig(WorkflowNode node, String key, int defaultValue) {
        Object value = node.config().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Integer.parseInt(stringValue);
        }
        return defaultValue;
    }
}
