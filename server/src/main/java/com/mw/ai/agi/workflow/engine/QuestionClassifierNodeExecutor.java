package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.service.ChatModelClient;
import com.mw.ai.agi.model.service.ModelProviderService;
import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuestionClassifierNodeExecutor implements WorkflowNodeExecutor {
    private final ChatModelClient chatModelClient;
    private final ModelProviderService modelProviderService;

    public QuestionClassifierNodeExecutor(ChatModelClient chatModelClient, ModelProviderService modelProviderService) {
        this.chatModelClient = chatModelClient;
        this.modelProviderService = modelProviderService;
    }

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.QUESTION_CLASSIFIER;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String inputKey = optionalStringConfig(node, "inputKey", "question");
        String outputKey = optionalStringConfig(node, "outputKey", "index");
        Map<String, Object> nodeContext = new LinkedHashMap<>(context.context());
        nodeContext.putAll(resolveInputParams(node, context.context()));
        String contentTemplate = optionalStringConfig(node, "contentTemplate", optionalStringConfig(node, "questionTemplate", ""));
        Object inputValue = contentTemplate.isBlank()
                ? TemplateRenderer.resolvePath(nodeContext, inputKey)
                : TemplateRenderer.render(contentTemplate, nodeContext);
        String question = inputValue == null ? "" : String.valueOf(inputValue);
        List<Category> categories = readCategories(node.config().get("categories"));
        Match match = classifyByModel(node, question, categories);
        if (!match.matched()) {
            match = matchCategory(categories, question);
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put(outputKey, match.id());

        return NodeExecutionResult.output(output);
    }

    private Match classifyByModel(WorkflowNode node, String question, List<Category> categories) {
        String providerId = optionalStringConfig(node, "providerId", "");
        if (providerId.isBlank() || question.isBlank() || categories.isEmpty()) {
            return Match.none();
        }
        ModelProvider provider = modelProviderService.get(providerId);
        if (!provider.enabled()) {
            throw new IllegalArgumentException("Model provider is disabled: " + providerId);
        }
        String model = optionalStringConfig(node, "model", provider.model());
        String response = chatModelClient.generate(providerId, model, classifyPrompt(question, categories), Map.of(
                "temperature", 0,
                "maxTokens", 16
        ));
        String normalized = response == null ? "" : response.trim();
        normalized = normalized.replaceAll("^[^0-9A-Za-z\\u4e00-\\u9fff]+|[^0-9A-Za-z\\u4e00-\\u9fff]+$", "");
        for (int index = 0; index < categories.size(); index++) {
            Category category = categories.get(index);
            String ordinal = String.valueOf(index + 1);
            if (normalized.equals(category.id())
                    || normalized.equals(category.name())
                    || normalized.equals(ordinal)
                    || normalized.startsWith(ordinal + ".")
                    || normalized.startsWith(ordinal + ":")
                    || normalized.contains(category.id())
                    || normalized.contains(category.name())) {
                return new Match(category.id(), true);
            }
        }
        return Match.none();
    }

    private String classifyPrompt(String question, List<Category> categories) {
        StringBuilder builder = new StringBuilder();
        builder.append("请对用户内容进行问题分类，只返回最匹配分类的分类编号，不要解释。\n");
        builder.append("用户内容：").append(question).append("\n");
        builder.append("分类：\n");
        for (int index = 0; index < categories.size(); index++) {
            Category category = categories.get(index);
            builder.append(index + 1).append(". ").append(category.id()).append("：").append(category.name()).append("\n");
        }
        return builder.toString();
    }

    private Match matchCategory(List<Category> categories, String question) {
        String normalizedQuestion = question.toLowerCase();
        for (Category category : categories) {
            if (matches(category, question, normalizedQuestion)) {
                return new Match(category.id(), true);
            }
        }
        return Match.none();
    }

    private boolean matches(Category category, String question, String normalizedQuestion) {
        for (String keyword : category.keywords()) {
            if (keyword.isBlank()) {
                continue;
            }
            if ("EQUALS".equals(category.matchMode()) && question.equals(keyword)) {
                return true;
            }
            if (!"EQUALS".equals(category.matchMode()) && normalizedQuestion.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return !category.name().isBlank() && normalizedQuestion.contains(category.name().toLowerCase());
    }

    private List<Category> readCategories(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<Category> categories = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            Object item = values.get(index);
            if (item instanceof Map<?, ?> category) {
                String id = stringValue(category.get("id"), "分类" + (index + 1));
                String name = stringValue(category.get("name"), id);
                String matchMode = stringValue(category.get("matchMode"), "CONTAINS");
                List<String> keywords = new ArrayList<>();
                Object keywordsValue = category.get("keywords");
                if (keywordsValue instanceof List<?> items) {
                    for (Object keywordValue : items) {
                        String keyword = String.valueOf(keywordValue);
                        if (!keyword.isBlank()) {
                            keywords.add(keyword);
                        }
                    }
                } else {
                    String keyword = stringValue(category.get("keyword"), "");
                if (keyword.isBlank()) {
                        keyword = name;
                    }
                    if (!keyword.isBlank()) {
                        keywords.add(keyword);
                    }
                }
                categories.add(new Category(id, name, keywords, matchMode));
            }
        }
        return categories;
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

    private String optionalStringConfig(WorkflowNode node, String key, String defaultValue) {
        return stringValue(node.config().get(key), defaultValue);
    }

    private String stringValue(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }

    private record Category(String id, String name, List<String> keywords, String matchMode) {
    }

    private record Match(String id, boolean matched) {
        static Match none() {
            return new Match("", false);
        }
    }
}
