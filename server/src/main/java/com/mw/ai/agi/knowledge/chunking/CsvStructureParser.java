package com.mw.ai.agi.knowledge.chunking;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CsvStructureParser implements StructuredDocumentParser {
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }

    @Override
    public DocumentStructure parse(String fileName, String content) {
        String normalized = content == null
                ? ""
                : content.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<List<String>> records = records(normalized);
        if (records.isEmpty()) {
            return new DocumentStructure(
                    fileName, "CSV", normalized, List.of(), Map.of("fileName", fileName)
            );
        }
        List<String> headers = records.get(0);
        List<DocumentNode> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            List<String> values = records.get(index);
            Map<String, Object> metadata = new LinkedHashMap<>();
            StringBuilder rowText = new StringBuilder();
            for (int column = 0; column < Math.max(headers.size(), values.size()); column++) {
                String header = column < headers.size()
                        ? headers.get(column)
                        : "Column " + (column + 1);
                String value = column < values.size() ? values.get(column) : "";
                metadata.put(header, value);
                if (!rowText.isEmpty()) {
                    rowText.append('\n');
                }
                rowText.append(header).append(": ").append(value);
            }
            rows.add(new DocumentNode(
                    "node_" + UUID.randomUUID(),
                    NodeType.TABLE_ROW,
                    rowText.toString(),
                    List.of(fileName),
                    null,
                    null,
                    index - 1,
                    "table_0",
                    metadata,
                    List.of()
            ));
        }
        return new DocumentStructure(
                fileName,
                "CSV",
                normalized,
                rows,
                Map.of("fileName", fileName, "headers", headers)
        );
    }

    private List<List<String>> records(String content) {
        List<List<String>> result = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (value == '"') {
                if (quoted
                        && index + 1 < content.length()
                        && content.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (value == ',' && !quoted) {
                record.add(field.toString().trim());
                field.setLength(0);
            } else if (value == '\n' && !quoted) {
                appendRecord(result, record, field);
            } else {
                field.append(value);
            }
        }
        appendRecord(result, record, field);
        return List.copyOf(result);
    }

    private void appendRecord(
            List<List<String>> result,
            List<String> record,
            StringBuilder field
    ) {
        record.add(field.toString().trim());
        field.setLength(0);
        if (record.stream().anyMatch(cell -> !cell.isBlank())) {
            result.add(List.copyOf(record));
        }
        record.clear();
    }
}
