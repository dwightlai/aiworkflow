package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.workflow.domain.WorkflowNode;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuestionClassifierNodeExecutor implements WorkflowNodeExecutor {
    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.QUESTION_CLASSIFIER;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String inputKey = optionalStringConfig(node, "inputKey", "question");
        String outputKey = optionalStringConfig(node, "outputKey", "questionCategory");
        Object inputValue = TemplateRenderer.resolvePath(context.context(), inputKey);
        String question = inputValue == null ? "" : String.valueOf(inputValue);
        Match match = matchCategory(node.config().get("categories"), question);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put(outputKey, match.id());
        output.put("categoryName", match.name());
        output.put("categoryMatched", match.matched());
        output.put("question", question);

        return NodeExecutionResult.output(output);
    }

    private Match matchCategory(Object value, String question) {
        if (!(value instanceof List<?> categories)) {
            return Match.none();
        }
        String normalizedQuestion = question.toLowerCase();
        for (Object item : categories) {
            if (item instanceof Map<?, ?> category && matches(category, question, normalizedQuestion)) {
                return new Match(
                        stringValue(category.get("id"), stringValue(category.get("name"), "")),
                        stringValue(category.get("name"), stringValue(category.get("id"), "")),
                        true
                );
            }
        }
        return Match.none();
    }

    private boolean matches(Map<?, ?> category, String question, String normalizedQuestion) {
        String matchMode = stringValue(category.get("matchMode"), "CONTAINS");
        Object keywordsValue = category.get("keywords");
        if (keywordsValue instanceof List<?> keywords) {
            for (Object keywordValue : keywords) {
                String keyword = String.valueOf(keywordValue);
                if (keyword.isBlank()) {
                    continue;
                }
                if ("EQUALS".equals(matchMode) && question.equals(keyword)) {
                    return true;
                }
                if (!"EQUALS".equals(matchMode) && normalizedQuestion.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }
        String keyword = stringValue(category.get("keyword"), "");
        return !keyword.isBlank() && normalizedQuestion.contains(keyword.toLowerCase());
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

    private record Match(String id, String name, boolean matched) {
        static Match none() {
            return new Match("other", "其他", false);
        }
    }
}
