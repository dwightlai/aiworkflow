package com.mw.ai.agi.knowledge.chunking;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class OfficeStructureParser implements StructuredDocumentParser {
    private static final Pattern INTEGER = Pattern.compile("[-+]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("[-+]?\\d+\\.\\d+");
    private static final Pattern DATE = Pattern.compile(
            "\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?"
    );
    private static final Pattern MARKDOWN_HEADING =
            Pattern.compile("(?m)^#{1,6}\\s+\\S+");
    private static final Pattern MARKDOWN_TABLE =
            Pattern.compile("(?m)^\\s*\\|.+\\|\\s*$");
    private final MarkdownStructureParser markdownParser = new MarkdownStructureParser();
    private final PlainTextStructureParser plainTextParser = new PlainTextStructureParser();

    @Override
    public boolean supports(String fileName) {
        String value = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return value.endsWith(".docx") || value.endsWith(".xlsx") || value.endsWith(".pdf");
    }

    @Override
    public DocumentStructure parse(String fileName, String content) {
        if (!hasMarkdownStructure(content)) {
            DocumentStructure plain = plainTextParser.parse(fileName, content);
            return new DocumentStructure(
                    plain.documentTitle(),
                    extension(fileName).toUpperCase(Locale.ROOT),
                    plain.rawText(),
                    plain.nodes(),
                    Map.of(
                            "fileName", fileName,
                            "structurePreserved", false,
                            "parserFallback", "PLAIN_TEXT"
                    )
            );
        }
        DocumentStructure markdown = markdownParser.parse(fileName + ".md", content);
        List<DocumentNode> nodes = new ArrayList<>();
        for (DocumentNode node : markdown.nodes()) {
            if (node.type() != NodeType.TABLE) {
                nodes.add(withOrder(node, nodes.size()));
                continue;
            }
            expandTableRows(node, nodes);
        }
        String sourceType = extension(fileName).toUpperCase(Locale.ROOT);
        return new DocumentStructure(
                markdown.documentTitle(),
                sourceType,
                markdown.rawText(),
                nodes,
                Map.of("fileName", fileName, "structurePreserved", true)
        );
    }

    private void expandTableRows(DocumentNode table, List<DocumentNode> target) {
        List<String> lines = table.text().lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.size() < 3) {
            target.add(withOrder(table, target.size()));
            return;
        }
        List<String> schema = parseMarkdownRow(lines.get(0));
        if (schema.isEmpty()) {
            target.add(withOrder(table, target.size()));
            return;
        }
        String groupId = table.groupId() == null ? "table_" + UUID.randomUUID() : table.groupId();
        int logicalRowIndex = 0;
        for (int index = 2; index < lines.size(); index++) {
            String row = lines.get(index);
            if (isSeparator(row)) {
                continue;
            }
            List<String> values = parseMarkdownRow(row);
            Map<String, Object> data = new LinkedHashMap<>();
            Map<String, String> displayData = new LinkedHashMap<>();
            Map<String, String> fieldTypes = new LinkedHashMap<>();
            StringBuilder content = new StringBuilder();
            for (int column = 0; column < schema.size(); column++) {
                String field = schema.get(column).trim();
                if (field.isBlank()) {
                    continue;
                }
                String value = column < values.size() ? values.get(column).trim() : "";
                Object typedValue = typedValue(value);
                data.put(field, typedValue);
                displayData.put(field, value);
                fieldTypes.put(field, fieldType(value, typedValue));
                if (!value.isBlank()) {
                    if (!content.isEmpty()) {
                        content.append('\n');
                    }
                    content.append(field).append(": ").append(value);
                }
            }
            if (content.isEmpty()) {
                continue;
            }
            logicalRowIndex++;
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("atomic", true);
            metadata.put("sheetName", sheetName(table.sectionPath()));
            metadata.put("rowIndex", logicalRowIndex);
            metadata.put("schema", List.copyOf(schema));
            metadata.put("data", data);
            metadata.put("displayData", displayData);
            metadata.put("fieldTypes", fieldTypes);
            target.add(new DocumentNode(
                    "node_" + UUID.randomUUID(),
                    NodeType.TABLE_ROW,
                    content.toString(),
                    table.sectionPath(),
                    table.pageStart(),
                    table.pageEnd(),
                    target.size(),
                    groupId,
                    metadata,
                    List.of()
            ));
        }
    }

    private List<String> parseMarkdownRow(String line) {
        String value = line == null ? "" : line.trim();
        if (value.startsWith("|")) {
            value = value.substring(1);
        }
        if (value.endsWith("|")) {
            value = value.substring(0, value.length() - 1);
        }
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (escaped) {
                current.append(character);
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '|') {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (escaped) {
            current.append('\\');
        }
        cells.add(current.toString().trim());
        return cells;
    }

    private Object typedValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String numeric = value.trim().replaceAll("[,\\s\\u00a5\\uffe5$€£]", "");
        if (INTEGER.matcher(numeric).matches()) {
            try {
                return Long.parseLong(numeric);
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
        if (DECIMAL.matcher(numeric).matches()) {
            try {
                return Double.parseDouble(numeric);
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
        return value;
    }

    private String fieldType(String original, Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return "NUMBER";
        }
        if (DATE.matcher(original.trim()).matches()) {
            return "DATE";
        }
        return "STRING";
    }

    private String sheetName(List<String> sectionPath) {
        if (sectionPath == null || sectionPath.isEmpty()) {
            return "";
        }
        String value = sectionPath.get(sectionPath.size() - 1).trim();
        for (String prefix : List.of("\u5de5\u4f5c\u8868\uff1a", "\u5de5\u4f5c\u8868:")) {
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length()).trim();
            }
        }
        return value;
    }

    private DocumentNode withOrder(DocumentNode node, int order) {
        return new DocumentNode(
                node.id(),
                node.type(),
                node.text(),
                node.sectionPath(),
                node.pageStart(),
                node.pageEnd(),
                order,
                node.groupId(),
                node.metadata(),
                node.children()
        );
    }

    private boolean isSeparator(String row) {
        return row.replace("|", "").replace("-", "").replace(":", "").isBlank();
    }

    private String extension(String fileName) {
        String value = fileName == null ? "" : fileName;
        int dot = value.lastIndexOf('.');
        return dot < 0 ? "OFFICE" : value.substring(dot + 1);
    }

    private boolean hasMarkdownStructure(String content) {
        String value = content == null ? "" : content;
        return MARKDOWN_HEADING.matcher(value).find()
                || MARKDOWN_TABLE.matcher(value).find();
    }
}
