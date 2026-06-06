package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.service.ChatModelClient;
import com.mw.ai.agi.model.service.ModelProviderService;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
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
        String promptKey = optionalStringConfig(node, "promptKey", "question");
        String outputKey = optionalStringConfig(node, "outputKey", "content");

        Map<String, Object> nodeContext = new LinkedHashMap<>(context.context());
        nodeContext.putAll(resolveInputParams(node, context.context()));
        String userPrompt = optionalStringConfig(node, "userMessage", optionalStringConfig(node, "userPrompt", ""));
        Object promptValue = userPrompt.isBlank() ? TemplateRenderer.resolvePath(nodeContext, promptKey) : TemplateRenderer.render(userPrompt, nodeContext);
        String prompt = promptValue == null ? "" : String.valueOf(promptValue);
        String systemMessage = optionalStringConfig(node, "systemMessage", optionalStringConfig(node, "systemPrompt", ""));
        if (!systemMessage.isBlank()) {
            prompt = TemplateRenderer.render(systemMessage, nodeContext) + "\n\n" + prompt;
        }
        String response = chatModelClient.generate(providerId, model, prompt, modelOptions(node));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("content", response);
        output.put("reasoning_content", "");
        if (!"content".equals(outputKey) && !"reasoning_content".equals(outputKey)) {
            output.put(outputKey, response);
        }
        return NodeExecutionResult.output(output);
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

    private Map<String, Object> modelOptions(WorkflowNode node) {
        Map<String, Object> options = new LinkedHashMap<>();
        copyOption(node, options, "temperature");
        copyOption(node, options, "topP");
        copyOption(node, options, "topK");
        copyOption(node, options, "maxTokens");
        copyOption(node, options, "responseFormat");
        copyOption(node, options, "outputFormat");
        copyOption(node, options, "stream");
        copyOption(node, options, "streaming");
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
