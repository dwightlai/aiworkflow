package com.mw.ai.agi.generation.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResearchOutputTemplateRegistry {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ResearchOutputTemplateRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> list(String outputType, String templateCategory) {
        List<Map<String, Object>> templates = new ArrayList<>();
        templates.add(builtin("layout_report_docx", "档案编研普通报告", "report", "DOCX"));
        templates.add(builtin("layout_gallery_docx", "图文展陈版式", "gallery", "DOCX"));
        templates.add(builtin("layout_timeline_docx", "时间轴专题版式", "timeline", "DOCX"));
        templates.add(builtin("layout_topic_collection_docx", "专题汇编 DOCX 母版", "topic_collection", "DOCX"));
        templates.add(builtin("layout_report_html", "普通报告 HTML 预览", "report", "HTML"));
        templates.add(builtin("layout_gallery_html", "图文展陈 HTML 预览", "gallery", "HTML"));
        templates.add(builtin("layout_timeline_html", "时间轴 HTML 预览", "timeline", "HTML"));
        return templates.stream()
                .filter(template -> matches(outputType, stringValue(template.get("outputType"))))
                .filter(template -> matches(templateCategory, stringValue(template.get("templateCategory"))))
                .toList();
    }

    public Map<String, Object> get(String templateId) {
        return list(null, null).stream()
                .filter(template -> templateId.equals(template.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Output template not found: " + templateId));
    }

    public Map<String, Object> resolveDocxConfig(String templateId, String fallbackJson) {
        Map<String, Object> template = get(templateId);
        Map<String, Object> config = readConfigMap(template.get("docxConfig"));
        Map<String, Object> fallback = readMap(fallbackJson);
        Map<String, Object> merged = new LinkedHashMap<>(fallback);
        merged.putAll(config);
        return merged;
    }

    private Map<String, Object> builtin(String id, String name, String category, String outputType) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("id", id);
        template.put("name", name);
        template.put("templateCategory", category);
        template.put("outputType", outputType);
        template.put("docxConfig", defaultDocxConfig(category));
        template.put("layoutConfig", Map.of("defaultTab", "DOCX".equals(outputType) ? "docx" : "html"));
        return template;
    }

    private Map<String, Object> defaultDocxConfig(String category) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("showCover", true);
        config.put("showToc", true);
        config.put("tocDepth", 2);
        config.put("showPageNumber", true);
        config.put("showReferenceSection", true);
        config.put("lineSpacingPt", 28);
        if ("gallery".equals(category)) {
            config.put("galleryTable", true);
        }
        if ("timeline".equals(category)) {
            config.put("timelineStyle", "vertical");
        }
        if ("topic_collection".equals(category)) {
            config.put("masterFile", "research/docx-masters/archive_topic_collection.docx");
            config.put("templateType", "archive_topic_collection");
        }
        return config;
    }

    private boolean matches(String filter, String value) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return filter.equalsIgnoreCase(value);
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readConfigMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> config = new LinkedHashMap<>();
            map.forEach((key, item) -> config.put(String.valueOf(key), item));
            return config;
        }
        return readMap(stringValue(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
