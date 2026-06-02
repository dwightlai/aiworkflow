package com.aiworkflow.workflow.engine;

import com.aiworkflow.model.domain.ModelProvider;
import com.aiworkflow.model.service.ChatModelClient;
import com.aiworkflow.model.service.ModelProviderService;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LlmNodeExecutor implements WorkflowNodeExecutor {
    private final ChatModelClient chatModelClient;
    private final ModelProviderService modelProviderService;

    public LlmNodeExecutor(ChatModelClient chatModelClient, ModelProviderService modelProviderService) {
        this.chatModelClient = chatModelClient;
        this.modelProviderService = modelProviderService;
    }

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.LLM;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String providerId = requiredStringConfig(node, "providerId");
        ModelProvider provider = modelProviderService.get(providerId);
        if (!provider.enabled()) {
            throw new IllegalArgumentException("Model provider is disabled: " + providerId);
        }
        String model = optionalStringConfig(node, "model", provider.model());
        String promptKey = requiredStringConfig(node, "promptKey");
        String outputKey = requiredStringConfig(node, "outputKey");

        Object promptValue = context.context().get(promptKey);
        String prompt = promptValue == null ? "" : String.valueOf(promptValue);
        String systemPrompt = optionalStringConfig(node, "systemPrompt", "");
        if (!systemPrompt.isBlank()) {
            prompt = systemPrompt + "\n\n" + prompt;
        }
        String response = chatModelClient.generate(providerId, model, prompt, modelOptions(node));
        return NodeExecutionResult.output(Map.of(outputKey, response));
    }

    private Map<String, Object> modelOptions(WorkflowNode node) {
        Map<String, Object> options = new LinkedHashMap<>();
        copyOption(node, options, "temperature");
        copyOption(node, options, "topP");
        copyOption(node, options, "maxTokens");
        copyOption(node, options, "responseFormat");
        copyOption(node, options, "stream");
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

    private String optionalStringConfig(WorkflowNode node, String key, String defaultValue) {
        Object value = node.config().get(key);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }
}
