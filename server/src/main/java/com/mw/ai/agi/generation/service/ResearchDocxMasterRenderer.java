package com.mw.ai.agi.generation.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
public class ResearchDocxMasterRenderer {
    private static final Pattern GALLERY_LOOP = Pattern.compile(
            "\\{\\{#gallery\\.items\\}\\}(.*?)\\{\\{/gallery\\.items\\}\\}",
            Pattern.DOTALL
    );
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^{}#/]+)\\}\\}");
    private static final Pattern ELLIPSIS = Pattern.compile("\\{\\{\\.\\.\\.\\}\\}");

    public byte[] renderFromMasterFile(String masterFile, Map<String, Object> contentJson) {
        try (InputStream inputStream = openMaster(masterFile)) {
            return render(inputStream, contentJson);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to render DOCX master: " + masterFile, exception);
        }
    }

    public byte[] render(InputStream masterInput, Map<String, Object> contentJson) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(masterInput)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                byte[] data = zipInputStream.readAllBytes();
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = renderDocumentXml(new String(data, StandardCharsets.UTF_8), contentJson);
                    data = xml.getBytes(StandardCharsets.UTF_8);
                }
                entries.put(entry.getName(), data);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, byte[]> item : entries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(item.getKey()));
                zipOutputStream.write(item.getValue());
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }

    private InputStream openMaster(String masterFile) throws Exception {
        if (masterFile == null || masterFile.isBlank()) {
            throw new IllegalArgumentException("masterFile is required.");
        }
        if (masterFile.startsWith("classpath:")) {
            return new ClassPathResource(masterFile.substring("classpath:".length())).getInputStream();
        }
        Path path = Path.of(masterFile);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }
        return new ClassPathResource(masterFile).getInputStream();
    }

    String renderDocumentXml(String xml, Map<String, Object> contentJson) {
        String rendered = expandGalleryLoop(xml, contentJson);
        rendered = expandEllipsis(rendered, buildEllipsisValues(contentJson));
        rendered = replacePlaceholders(rendered, contentJson);
        return rendered;
    }

    private String expandGalleryLoop(String xml, Map<String, Object> contentJson) {
        Matcher matcher = GALLERY_LOOP.matcher(xml);
        if (!matcher.find()) {
            return xml;
        }
        String block = matcher.group(1);
        StringBuilder expanded = new StringBuilder();
        for (Map<String, Object> item : findGalleryItems(contentJson)) {
            String row = block;
            row = row.replace("{{title}}", escapeXml(stringValue(item.get("title"), "")));
            row = row.replace("{{imageRef}}", escapeXml(stringValue(item.get("imageRef"), "")));
            row = row.replace("{{description}}", escapeXml(stringValue(item.get("description"), "")));
            expanded.append(row);
        }
        return matcher.replaceFirst(Matcher.quoteReplacement(expanded.toString()));
    }

    private String expandEllipsis(String xml, Deque<String> values) {
        Matcher matcher = ELLIPSIS.matcher(xml);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String value = values.isEmpty() ? "" : values.poll();
            matcher.appendReplacement(builder, Matcher.quoteReplacement(escapeXml(value)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String replacePlaceholders(String xml, Map<String, Object> contentJson) {
        Matcher matcher = PLACEHOLDER.matcher(xml);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = resolvePlaceholder(key, contentJson);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(escapeXml(value)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String resolvePlaceholder(String key, Map<String, Object> contentJson) {
        if ("toc".equals(key)) {
            return buildToc(contentJson);
        }
        if (key.startsWith("sections.") && key.endsWith(".content")) {
            String sectionId = key.substring("sections.".length(), key.length() - ".content".length());
            return findSectionField(contentJson, sectionId, "content");
        }
        return resolveDotPath(contentJson, key);
    }

    @SuppressWarnings("unchecked")
    private Deque<String> buildEllipsisValues(Map<String, Object> contentJson) {
        Deque<String> values = new ArrayDeque<>();
        Object sections = contentJson.get("sections");
        if (!(sections instanceof List<?> sectionItems)) {
            appendReferences(values, contentJson.get("references"));
            return values;
        }
        for (Object item : sectionItems) {
            if (!(item instanceof Map<?, ?> section)) {
                continue;
            }
            String type = stringValue(section.get("type"), "text");
            if ("document_collection".equals(type)) {
                appendDocumentCollection(values, section.get("items"));
            } else if ("timeline".equals(type)) {
                appendTimeline(values, section.get("events"));
            }
        }
        appendReferences(values, contentJson.get("references"));
        return values;
    }

    @SuppressWarnings("unchecked")
    private void appendDocumentCollection(Deque<String> values, Object items) {
        if (!(items instanceof List<?> collectionItems)) {
            return;
        }
        for (Object item : collectionItems) {
            if (!(item instanceof Map<?, ?> document)) {
                continue;
            }
            values.add(stringValue(document.get("title"), ""));
            values.add(stringValue(document.get("archiveCode"), ""));
            values.add(stringValue(document.get("formationDate"), ""));
            values.add(stringValue(document.get("responsibleOrg"), ""));
            values.add(stringValue(document.get("summary"), ""));
        }
    }

    @SuppressWarnings("unchecked")
    private void appendTimeline(Deque<String> values, Object events) {
        if (!(events instanceof List<?> eventItems)) {
            return;
        }
        for (Object item : eventItems) {
            if (!(item instanceof Map<?, ?> event)) {
                continue;
            }
            values.add(stringValue(event.get("date"), ""));
            values.add(stringValue(event.get("title"), ""));
            values.add(stringValue(event.get("description"), stringValue(event.get("title"), "")));
        }
    }

    @SuppressWarnings("unchecked")
    private void appendReferences(Deque<String> values, Object references) {
        if (!(references instanceof List<?> refItems)) {
            return;
        }
        for (Object item : refItems) {
            if (!(item instanceof Map<?, ?> reference)) {
                continue;
            }
            values.add("[" + stringValue(reference.get("refNo"), "") + "] "
                    + stringValue(reference.get("title"), stringValue(reference.get("sourceTitle"), "")));
            values.add(stringValue(reference.get("archiveCode"), ""));
            values.add(stringValue(reference.get("sourceType"), ""));
            values.add(stringValue(reference.get("quote"), ""));
            values.add(stringValue(reference.get("pageRange"), stringValue(reference.get("page"), "")));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> findGalleryItems(Map<String, Object> contentJson) {
        List<Map<String, Object>> items = new ArrayList<>();
        Object sections = contentJson.get("sections");
        if (!(sections instanceof List<?> sectionItems)) {
            return items;
        }
        for (Object item : sectionItems) {
            if (!(item instanceof Map<?, ?> section)) {
                continue;
            }
            if (!"gallery".equals(stringValue(section.get("type"), ""))) {
                continue;
            }
            Object galleryItems = section.get("items");
            if (galleryItems instanceof List<?> list) {
                for (Object galleryItem : list) {
                    if (galleryItem instanceof Map<?, ?> map) {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        map.forEach((key, value) -> entry.put(String.valueOf(key), value));
                        items.add(entry);
                    }
                }
            }
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private String findSectionField(Map<String, Object> contentJson, String sectionId, String field) {
        Object sections = contentJson.get("sections");
        if (!(sections instanceof List<?> sectionItems)) {
            return "";
        }
        for (Object item : sectionItems) {
            if (item instanceof Map<?, ?> section && sectionId.equals(stringValue(section.get("id"), ""))) {
                return stringValue(section.get(field), "");
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String buildToc(Map<String, Object> contentJson) {
        Object sections = contentJson.get("sections");
        if (!(sections instanceof List<?> sectionItems) || sectionItems.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object item : sectionItems) {
            if (item instanceof Map<?, ?> section) {
                String title = stringValue(section.get("title"), "");
                if (!title.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(title);
                }
            }
        }
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private String resolveDotPath(Map<String, Object> root, String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return "";
            }
            current = map.get(part);
        }
        return stringValue(current, "");
    }

    private String escapeXml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
