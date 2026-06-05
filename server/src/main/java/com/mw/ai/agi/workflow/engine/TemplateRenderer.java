package com.mw.ai.agi.workflow.engine;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TemplateRenderer {
    private static final Pattern TEMPLATE_TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private TemplateRenderer() {
    }

    static String render(String template, Map<String, Object> context) {
        Matcher matcher = TEMPLATE_TOKEN_PATTERN.matcher(template == null ? "" : template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            Object value = resolvePath(context, matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    static Object resolvePath(Map<String, Object> context, String path) {
        if (context.containsKey(path)) {
            return context.get(path);
        }
        Object current = context;
        for (String part : path.split("\\.")) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
