package com.aiworkflow.workflow.engine;

import com.aiworkflow.workflow.domain.WorkflowNode;
import com.aiworkflow.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptNodeExecutor implements WorkflowNodeExecutor {
    private static final Pattern TEMPLATE_TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    @Override
    public WorkflowNodeType nodeType() {
        return WorkflowNodeType.PROMPT;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        String outputKey = requiredStringConfig(node, "outputKey");
        String template = requiredStringConfig(node, "template");
        return NodeExecutionResult.output(Map.of(outputKey, renderTemplate(template, context)));
    }

    private String renderTemplate(String template, NodeExecutionContext context) {
        Matcher matcher = TEMPLATE_TOKEN_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            Object value = context.context().get(matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String requiredStringConfig(WorkflowNode node, String key) {
        Object value = node.config().get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException("Workflow node " + node.id() + " requires config: " + key);
        }
        return stringValue;
    }
}
