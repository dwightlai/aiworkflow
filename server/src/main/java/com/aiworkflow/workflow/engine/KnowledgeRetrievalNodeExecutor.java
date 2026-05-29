package com.aiworkflow.workflow.engine;

import com.aiworkflow.knowledge.service.KnowledgeBaseService;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.Map;

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
        String knowledgeBaseId = requiredStringConfig(node, "knowledgeBaseId");
        String queryKey = requiredStringConfig(node, "queryKey");
        String outputKey = requiredStringConfig(node, "outputKey");
        int topK = intConfig(node, "topK", 3);
        Object query = context.context().get(queryKey);
        if (query == null || String.valueOf(query).isBlank()) {
            return NodeExecutionResult.output(Map.of(outputKey, java.util.List.of()));
        }
        return NodeExecutionResult.output(Map.of(
                outputKey,
                knowledgeBaseService.search(knowledgeBaseId, String.valueOf(query), topK)
        ));
    }

    private String requiredStringConfig(WorkflowNode node, String key) {
        Object value = node.config().get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("Workflow node " + node.id() + " requires config: " + key);
        }
        return stringValue;
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
