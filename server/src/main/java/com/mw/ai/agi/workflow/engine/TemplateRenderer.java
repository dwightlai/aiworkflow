package com.mw.ai.agi.workflow.engine;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TemplateRenderer {
    private static final Pattern TEMPLATE_TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([^{}\\s]+)\\s*}}|\\$\\{\\s*([^{}\\s]+)\\s*}|\\{\\s*([\\p{L}\\p{N}_.\\-\\[\\]]+)\\s*}");

    private TemplateRenderer() {
    }

    static String render(String template, Map<String, Object> context) {
        Matcher matcher = TEMPLATE_TOKEN_PATTERN.matcher(template == null ? "" : template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String key = firstNonNull(matcher.group(1), matcher.group(2), matcher.group(3));
            Object value = resolvePath(context, key);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static String firstNonNull(String first, String second, String third) {
        if (first != null) {
            return first;
        }
        return second == null ? third : second;
    }

    static Object resolvePath(Map<String, Object> context, String path) {
        if (context.containsKey(path)) {
            return context.get(path);
        }
        Object current = context;
        for (String part : path.split("\\.")) {
            current = resolveSegment(current, part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Object resolveSegment(Object current, String segment) {
        String key = segment;
        Integer index = null;
        int bracketStart = segment.indexOf('[');
        if (bracketStart >= 0 && segment.endsWith("]")) {
            key = segment.substring(0, bracketStart);
            String indexValue = segment.substring(bracketStart + 1, segment.length() - 1);
            if (!indexValue.isBlank()) {
                index = Integer.parseInt(indexValue);
            }
        }
        Object value = key.isBlank() ? current : resolveKey(current, key);
        if (index == null) {
            return value;
        }
        if (value instanceof java.util.List<?> list && index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    private static Object resolveKey(Object current, String key) {
        if (current instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }
}
