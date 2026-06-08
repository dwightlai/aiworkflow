package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import com.mw.ai.agi.knowledge.service.KnowledgeBaseService;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KnowledgeRetrievalNodeExecutor implements WorkflowNodeExecutor {
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
        List<String> knowledgeBaseIds = knowledgeBaseIds(node);
        String queryKey = optionalStringConfig(node, "queryKey", "question");
        String outputKey = optionalStringConfig(node, "outputKey", "documents");
        int topK = intConfig(node, "fetchCount", intConfig(node, "topK", 3));
        double similarityThreshold = doubleConfig(node, "similarityThreshold", 0.0d);
        Map<String, Object> nodeContext = new LinkedHashMap<>(context.context());
        nodeContext.putAll(resolveInputParams(node, context.context()));
        String queryTemplate = optionalStringConfig(node, "queryText", optionalStringConfig(node, "keywordTemplate", ""));
        Object query = queryTemplate.isBlank() ? nodeContext.get(queryKey) : TemplateRenderer.render(queryTemplate, nodeContext);
        String queryText = query == null ? "" : String.valueOf(query);
        if (queryText.isBlank()) {
            return NodeExecutionResult.output(knowledgeOutput(outputKey, queryText, List.of()));
        }
        List<KnowledgeSearchResult> results = knowledgeBaseIds.stream()
                .flatMap(knowledgeBaseId -> knowledgeBaseService.search(
                        knowledgeBaseId,
                        queryText,
                        topK,
                        grantContext(context.context())
                ).stream())
                .filter(result -> similarityThreshold <= 0 || result.score() >= Math.round(similarityThreshold * 1000))
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .limit(topK)
                .toList();
        return NodeExecutionResult.output(knowledgeOutput(outputKey, queryText, results));
    }

    private Map<String, Object> grantContext(Map<String, Object> context) {
        Map<String, Object> grantContext = new LinkedHashMap<>();
        copyIfPresent(context, grantContext, "userId");
        copyIfPresent(context, grantContext, "activeUnitId");
        copyIfPresent(context, grantContext, "unitId");
        copyIfPresent(context, grantContext, "unitIds");
        copyIfPresent(context, grantContext, "departmentIds");
        return grantContext;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key) && source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }

    private List<String> knowledgeBaseIds(WorkflowNode node) {
        Object value = node.config().get("knowledgeBaseIds");
        List<String> ids = value instanceof List<?> values
                ? values.stream().map(String::valueOf).filter(item -> !item.isBlank()).toList()
                : List.of();
        if (!ids.isEmpty()) {
            return ids;
        }
        return List.of(requiredStringConfig(node, "knowledgeBaseId"));
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
        if (value instanceof String stringValue) {
            Object resolved = TemplateRenderer.resolvePath(context, stringValue);
            return resolved == null ? stringValue : resolved;
        }
        return value == null ? "" : value;
    }

    private Map<String, Object> knowledgeOutput(String legacyOutputKey, String query, List<KnowledgeSearchResult> results) {
        String content = results.stream()
                .map(KnowledgeSearchResult::content)
                .collect(Collectors.joining("\n\n"));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("content", content);
        output.put("sources", results);
        output.put("query", query);
        if (!"sources".equals(legacyOutputKey) && !"content".equals(legacyOutputKey) && !"query".equals(legacyOutputKey)) {
            output.put(legacyOutputKey, results);
        }
        return output;
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

    private double doubleConfig(WorkflowNode node, String key, double defaultValue) {
        Object value = node.config().get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Double.parseDouble(stringValue);
        }
        return defaultValue;
    }
}
