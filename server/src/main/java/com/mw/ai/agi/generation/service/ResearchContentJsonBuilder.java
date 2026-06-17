package com.mw.ai.agi.generation.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ResearchContentJsonBuilder {
    private static final Pattern TIMELINE_LINE = Pattern.compile("^[-*]?\\s*(\\d{4}[^：:]*)[：:](.+)$");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ResearchContentJsonBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> build(
            String templateCategory,
            String title,
            String topic,
            String audience,
            List<Map<String, Object>> sections,
            List<Map<String, Object>> sectionOutputs,
            List<Map<String, Object>> citations
    ) {
        return build(templateCategory, title, topic, audience, sections, sectionOutputs, citations, List.of());
    }

    public Map<String, Object> build(
            String templateCategory,
            String title,
            String topic,
            String audience,
            List<Map<String, Object>> sections,
            List<Map<String, Object>> sectionOutputs,
            List<Map<String, Object>> citations,
            List<Map<String, Object>> corpusItems
    ) {
        if (isTopicCollection(templateCategory)) {
            return buildTopicCollection(title, topic, sections, sectionOutputs, citations, corpusItems);
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("templateCategory", templateCategory == null || templateCategory.isBlank() ? "report" : templateCategory);
        content.put("title", title);
        content.put("topic", topic);
        content.put("audience", audience);
        content.put("sections", buildSections(sections, sectionOutputs, corpusItems));
        content.put("citations", citations == null ? List.of() : citations);
        content.put("references", normalizeReferences(citations));
        return content;
    }

    private boolean isTopicCollection(String templateCategory) {
        return "topic_collection".equalsIgnoreCase(templateCategory)
                || "archive_topic_collection".equalsIgnoreCase(templateCategory);
    }

    private Map<String, Object> buildTopicCollection(
            String title,
            String topic,
            List<Map<String, Object>> sections,
            List<Map<String, Object>> sectionOutputs,
            List<Map<String, Object>> citations,
            List<Map<String, Object>> corpusItems
    ) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("templateType", "archive_topic_collection");
        content.put("templateCode", "archive_topic_collection_docx");
        content.put("title", title == null || title.isBlank() ? topic + "专题资料汇编" : title);
        content.put("topic", topic);
        content.put("cover", Map.of(
                "organization", "某某集团档案中心",
                "publishDate", java.time.YearMonth.now().toString(),
                "securityLevel", "内部资料"
        ));
        content.put("editorNote", "围绕" + topic + "主题，对立项文件、会议纪要、实施方案、验收材料和图片资料进行分类整理。");
        content.put("summary", "本汇编用于支撑" + topic + "过程回顾、成果展示和经验总结。");
        content.put("sections", buildSections(sections, sectionOutputs, corpusItems));
        content.put("references", buildTopicReferences(citations, corpusItems));
        return content;
    }

    private List<Map<String, Object>> buildTopicReferences(
            List<Map<String, Object>> citations,
            List<Map<String, Object>> corpusItems
    ) {
        List<Map<String, Object>> references = new ArrayList<>();
        int refNo = 1;
        if (citations != null) {
            for (Map<String, Object> citation : citations) {
                Map<String, Object> reference = new LinkedHashMap<>();
                reference.put("refNo", refNo++);
                reference.put("title", citation.getOrDefault("title", citation.get("sourceId")));
                reference.put("sourceType", citation.getOrDefault("sourceType", "档案文件"));
                reference.put("archiveCode", citation.getOrDefault("archiveCode", ""));
                reference.put("quote", citation.getOrDefault("quote", ""));
                references.add(reference);
            }
        }
        if (references.isEmpty() && corpusItems != null) {
            for (Map<String, Object> item : corpusItems) {
                Map<String, Object> reference = new LinkedHashMap<>();
                reference.put("refNo", refNo++);
                reference.put("title", item.get("title"));
                reference.put("sourceType", "档案文件");
                reference.put("archiveCode", item.getOrDefault("archiveCode", item.get("archiveItemCode")));
                reference.put("quote", item.getOrDefault("summary", ""));
                references.add(reference);
            }
        }
        return references;
    }

    public String writeJson(Map<String, Object> content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write content_json.", exception);
        }
    }

    public Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read content_json.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildSections(
            List<Map<String, Object>> sections,
            List<Map<String, Object>> sectionOutputs,
            List<Map<String, Object>> corpusItems
    ) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < sections.size(); index++) {
            Map<String, Object> section = sections.get(index);
            Map<String, Object> output = index < sectionOutputs.size() ? sectionOutputs.get(index) : Map.of();
            String key = stringValue(section.get("key"), "section_" + index);
            String title = stringValue(section.get("title"), key);
            String outputFormat = stringValue(section.get("outputFormat"), "text");
            String markdown = stringValue(output.get("contentMarkdown"), "");
            String plain = stripMarkdownHeadings(markdown);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", key);
            entry.put("title", title);
            entry.put("type", mapSectionType(outputFormat));
            entry.put("contentMarkdown", markdown);

            if ("document_collection".equalsIgnoreCase(outputFormat)) {
                List<Map<String, Object>> items = parseDocumentCollectionItems(plain);
                if (items.isEmpty()) {
                    items = mapCorpusToDocumentItems(corpusItems);
                }
                entry.put("items", items);
            } else if ("gallery".equalsIgnoreCase(outputFormat)) {
                entry.put("items", parseGalleryItems(plain, output.get("citations")));
            } else if ("timeline".equalsIgnoreCase(outputFormat)) {
                entry.put("events", parseTimelineEvents(plain, output.get("citations")));
                entry.put("content", plain);
            } else if ("analysis".equalsIgnoreCase(outputFormat)) {
                entry.put("content", plain);
            } else {
                entry.put("content", plain);
            }

            Object sectionCitations = output.get("citations");
            if (sectionCitations != null) {
                entry.put("citations", sectionCitations);
            }
            result.add(entry);
        }
        return result;
    }

    private String mapSectionType(String outputFormat) {
        if ("timeline".equalsIgnoreCase(outputFormat)) {
            return "timeline";
        }
        if ("gallery".equalsIgnoreCase(outputFormat)) {
            return "gallery";
        }
        if ("document_collection".equalsIgnoreCase(outputFormat)) {
            return "document_collection";
        }
        if ("analysis".equalsIgnoreCase(outputFormat)) {
            return "analysis";
        }
        return "text";
    }

    private List<Map<String, Object>> parseDocumentCollectionItems(String plain) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (String block : plain.split("\n\n")) {
            String trimmed = block.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", extractMeta(trimmed, "题名"));
            if (stringValue(item.get("title"), "").isBlank()) {
                item.put("title", firstLine(trimmed));
            }
            item.put("archiveCode", firstNonBlank(
                    extractMeta(trimmed, "档号"),
                    extractMeta(trimmed, "archiveCode")
            ));
            item.put("formationDate", firstNonBlank(
                    extractMeta(trimmed, "形成时间"),
                    extractMeta(trimmed, "日期")
            ));
            item.put("responsibleOrg", firstNonBlank(
                    extractMeta(trimmed, "责任单位"),
                    extractMeta(trimmed, "责任者"),
                    extractMeta(trimmed, "来源")
            ));
            item.put("summary", firstNonBlank(
                    extractMeta(trimmed, "摘要"),
                    extractMeta(trimmed, "说明")
            ));
            if (stringValue(item.get("summary"), "").isBlank()) {
                item.put("summary", trimmed);
            }
            items.add(item);
        }
        return items;
    }

    private List<Map<String, Object>> mapCorpusToDocumentItems(List<Map<String, Object>> corpusItems) {
        if (corpusItems == null || corpusItems.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> corpusItem : corpusItems) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", corpusItem.get("title"));
            item.put("archiveCode", firstNonBlank(
                    stringValue(corpusItem.get("archiveCode"), ""),
                    stringValue(corpusItem.get("archiveItemCode"), "")
            ));
            item.put("formationDate", corpusItem.get("formationDate"));
            item.put("responsibleOrg", firstNonBlank(
                    stringValue(corpusItem.get("responsibleUnit"), ""),
                    stringValue(corpusItem.get("sourceUnit"), "")
            ));
            item.put("summary", corpusItem.get("summary"));
            items.add(item);
        }
        return items;
    }

    private List<Map<String, Object>> parseTimelineEvents(String plain, Object citations) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (String line : plain.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            Matcher matcher = TIMELINE_LINE.matcher(trimmed);
            if (matcher.matches()) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("date", matcher.group(1).trim());
                event.put("title", matcher.group(2).trim());
                event.put("description", matcher.group(2).trim());
                events.add(event);
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("date", "");
                event.put("title", trimmed.substring(2).trim());
                event.put("description", trimmed.substring(2).trim());
                events.add(event);
            } else {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("date", "");
                event.put("title", trimmed);
                event.put("description", trimmed);
                events.add(event);
            }
        }
        if (events.isEmpty() && !plain.isBlank()) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("date", "");
            event.put("title", plain.trim());
            event.put("description", plain.trim());
            events.add(event);
        }
        if (citations instanceof List<?> items && !items.isEmpty()) {
            events.get(events.size() - 1).put("citations", items);
        }
        return events;
    }

    private List<Map<String, Object>> parseGalleryItems(String plain, Object citations) {
        List<Map<String, Object>> items = new ArrayList<>();
        String[] blocks = plain.split("\n\n");
        int index = 1;
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", index++);
            item.put("title", firstLine(trimmed));
            item.put("caption", firstLine(trimmed));
            item.put("description", trimmed);
            item.put("archiveCode", extractMeta(trimmed, "档号"));
            item.put("formationDate", extractMeta(trimmed, "形成时间"));
            item.put("source", extractMeta(trimmed, "来源"));
            item.put("imageRef", firstNonBlank(
                    extractMeta(trimmed, "图片引用"),
                    extractMeta(trimmed, "imageRef"),
                    extractMeta(trimmed, "图片")
            ));
            item.put("date", extractMeta(trimmed, "日期"));
            if (citations instanceof List<?> citationItems && !citationItems.isEmpty()) {
                item.put("citations", citationItems);
            }
            items.add(item);
        }
        if (items.isEmpty() && !plain.isBlank()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", 1);
            item.put("title", firstLine(plain));
            item.put("caption", firstLine(plain));
            item.put("description", plain.trim());
            items.add(item);
        }
        return items;
    }

    private List<Map<String, Object>> normalizeReferences(List<Map<String, Object>> citations) {
        if (citations == null || citations.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> references = new ArrayList<>();
        int refNo = 1;
        for (Map<String, Object> citation : citations) {
            Map<String, Object> reference = new LinkedHashMap<>();
            reference.put("refNo", refNo++);
            reference.put("sourceTitle", citation.getOrDefault("title", citation.get("sourceId")));
            reference.put("sourceType", citation.getOrDefault("sourceType", "档案文件"));
            reference.put("quote", citation.getOrDefault("quote", ""));
            references.add(reference);
        }
        return references;
    }

    private String stripMarkdownHeadings(String markdown) {
        if (markdown == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String line : markdown.replace("\r\n", "\n").split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                int space = trimmed.indexOf(' ');
                builder.append(space > 0 ? trimmed.substring(space + 1).trim() : "").append('\n');
            } else {
                builder.append(line).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private String firstLine(String text) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                return trimmed.replaceAll("^#+\\s*", "");
            }
        }
        return "";
    }

    private String extractMeta(String text, String label) {
        for (String line : text.split("\n")) {
            if (line.contains(label + "：") || line.contains(label + ":")) {
                int index = line.indexOf(label);
                String value = line.substring(index + label.length()).replaceFirst("^[：:]\\s*", "").trim();
                return value;
            }
        }
        return "";
    }

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
