package com.aiworkflow.workflow.engine;

import com.aiworkflow.knowledge.service.KnowledgeBaseService;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KnowledgeRetrievalNodeExecutor implements WorkflowNodeExecutor {
    private static final Pattern TEMPLATE_TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeRetrievalNodeExecutor(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.KNOWLEDGE_RETRIEVAL;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String knowledgeBaseId = requiredStringConfig(node, "knowledgeBaseId");
        String queryKey = optionalStringConfig(node, "queryKey", "question");
        String outputKey = outputKey(node);
        int topK = intConfig(node, "fetchCount", intConfig(node, "topK", 3));
        Map<String, Object> nodeContext = new LinkedHashMap<>(context.context());
        nodeContext.putAll(resolveInputParams(node, context.context()));
        String keywordTemplate = optionalStringConfig(node, "keywordTemplate", "");
        Object query = keywordTemplate.isBlank() ? nodeContext.get(queryKey) : renderTemplate(keywordTemplate, nodeContext);
        if (query == null || String.valueOf(query).isBlank()) {
            return NodeExecutionResult.output(Map.of(outputKey, java.util.List.of()));
        }
        return NodeExecutionResult.output(Map.of(
                outputKey,
                knowledgeBaseService.search(knowledgeBaseId, String.valueOf(query), topK)
        ));
    }

    private Map<String, Object> resolveInputParams(WorkflowNode node, Map<String, Object> context) {
        Object inputParams = node.config().get("inputParams");
        if (!(inputParams instanceof List<?> params)) {
            return Map.of();
        }
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Object item : params) {
            if (item instanceof Map<?, ?> param) {
                Object name = param.get("name");
                Object value = param.get("value");
                if (name instanceof String nameValue && !nameValue.isBlank()) {
                    resolved.put(nameValue, resolveParamValue(value, context));
                }
            }
        }
        return resolved;
    }

    private Object resolveParamValue(Object value, Map<String, Object> context) {
        if (value instanceof String stringValue && context.containsKey(stringValue)) {
            return context.get(stringValue);
        }
        return value == null ? "" : value;
    }

    private String renderTemplate(String template, Map<String, Object> context) {
        Matcher matcher = TEMPLATE_TOKEN_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            Object value = context.get(matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String outputKey(WorkflowNode node) {
        Object outputParams = node.config().get("outputParams");
        if (outputParams instanceof List<?> params && !params.isEmpty() && params.get(0) instanceof Map<?, ?> first) {
            Object name = first.get("name");
            if (name instanceof String nameValue && !nameValue.isBlank()) {
                return nameValue;
            }
        }
        return optionalStringConfig(node, "outputKey", "documents");
    }

    private String requiredStringConfig(WorkflowNode node, String key) {
        Object value = node.config().get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("Workflow node " + node.id() + " requires config: " + key);
        }
        return stringValue;
    }

    private String optionalStringConfig(WorkflowNode node, String key, String defaultValue) {
        Object value = node.config().get(key);
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
