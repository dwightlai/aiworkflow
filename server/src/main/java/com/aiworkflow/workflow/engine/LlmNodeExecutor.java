package com.aiworkflow.workflow.engine;

import com.aiworkflow.model.domain.ModelProvider;
import com.aiworkflow.model.service.ChatModelClient;
import com.aiworkflow.model.service.ModelProviderService;
import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LlmNodeExecutor implements WorkflowNodeExecutor {
    private static final Pattern TEMPLATE_TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
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
        String outputKey = outputKey(node);

        Map<String, Object> nodeContext = new LinkedHashMap<>(context.context());
        nodeContext.putAll(resolveInputParams(node, context.context()));
        String userPrompt = optionalStringConfig(node, "userPrompt", "");
        Object promptValue = userPrompt.isBlank() ? nodeContext.get(promptKey) : renderTemplate(userPrompt, nodeContext);
        String prompt = promptValue == null ? "" : String.valueOf(promptValue);
        String systemPrompt = optionalStringConfig(node, "systemPrompt", "");
        if (!systemPrompt.isBlank()) {
            prompt = systemPrompt + "\n\n" + prompt;
        }
        String response = chatModelClient.generate(providerId, model, prompt, modelOptions(node));
        return NodeExecutionResult.output(Map.of(outputKey, response));
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
        return requiredStringConfig(node, "outputKey");
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
