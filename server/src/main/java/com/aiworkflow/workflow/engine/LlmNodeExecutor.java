package com.aiworkflow.workflow.engine;

import com.aiworkflow.model.service.ChatModelClient;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LlmNodeExecutor implements WorkflowNodeExecutor {
    private final ChatModelClient chatModelClient;

    public LlmNodeExecutor(ChatModelClient chatModelClient) {
        this.chatModelClient = chatModelClient;
    }

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.LLM;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String providerId = requiredStringConfig(node, "providerId");
        String model = requiredStringConfig(node, "model");
        String promptKey = requiredStringConfig(node, "promptKey");
        String outputKey = requiredStringConfig(node, "outputKey");

        Object promptValue = context.context().get(promptKey);
        String prompt = promptValue == null ? "" : String.valueOf(promptValue);
        String response = chatModelClient.generate(providerId, model, prompt, modelOptions(node));
        return NodeExecutionResult.output(Map.of(outputKey, response));
    }

    private Map<String, Object> modelOptions(WorkflowNode node) {
        Map<String, Object> options = new LinkedHashMap<>();
        copyOption(node, options, "temperature");
        copyOption(node, options, "maxTokens");
        return options;
    }

    private void copyOption(WorkflowNode node, Map<String, Object> options, String key) {
        if (node.config().containsKey(key)) {
            options.put(key, node.config().get(key));
        }
    }

    private String requiredStringConfig(WorkflowNode node, String key) {
        Object value = node.config().get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("Workflow node " + node.id() + " requires config: " + key);
        }
        return stringValue;
    }
}
