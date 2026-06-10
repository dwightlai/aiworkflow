package com.mw.ai.agi.generation.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.generation.domain.GenerationTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GenerationTemplateRuntimeMapper {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private GenerationTemplateRuntimeMapper() {
    }

    public static Map<String, Object> toRuntimeView(GenerationTemplate template, ObjectMapper objectMapper) {
        Map<String, Object> schema = readMap(template.templateSchema(), objectMapper);
        List<Map<String, Object>> sections = readSections(schema);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", template.id());
        view.put("name", template.name());
        view.put("outputType", template.outputType());
        view.put("sections", sections);
        view.put("sectionCount", sections.size());
        view.put("outlineSections", buildOutlineSectionsText(sections));
        view.put("variables", schema.getOrDefault("variables", List.of()));
        return view;
    }

    private static List<Map<String, Object>> readSections(Map<String, Object> schema) {
        Object sections = schema.get("sections");
        if (!(sections instanceof List<?> items)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> section = new LinkedHashMap<>();
                map.forEach((key, value) -> section.put(String.valueOf(key), value));
                result.add(section);
            }
        }
        return result;
    }

    private static String buildOutlineSectionsText(List<Map<String, Object>> sections) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, Object> section : sections) {
            builder.append("- ").append(stringValue(section.get("title"), "章节")).append('\n');
        }
        return builder.toString().trim();
    }

    private static Map<String, Object> readMap(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read generation template schema.", exception);
        }
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
