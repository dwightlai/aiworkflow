package com.mw.ai.agi.knowledge.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TableDocumentParser {
    public List<TableRow> parseText(String content, String sheetName) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = normalized.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.size() < 2) {
            return List.of();
        }
        String delimiter = lines.stream().anyMatch(line -> line.contains("\t")) ? "\t" : ",";
        List<String> headers = splitLine(lines.get(0), delimiter);
        List<TableRow> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            List<String> values = splitLine(lines.get(index), delimiter);
            Map<String, String> cells = new LinkedHashMap<>();
            boolean hasValue = false;
            for (int column = 0; column < Math.max(headers.size(), values.size()); column++) {
                String header = column < headers.size() && !headers.get(column).isBlank()
                        ? headers.get(column)
                        : "Column " + (column + 1);
                String value = column < values.size() ? values.get(column) : "";
                if (!value.isBlank()) {
                    hasValue = true;
                }
                cells.put(header, value);
            }
            if (hasValue) {
                rows.add(new TableRow(sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName, cells));
            }
        }
        return rows;
    }

    private List<String> splitLine(String line, String delimiter) {
        String[] parts = line.split(java.util.regex.Pattern.quote(delimiter), -1);
        List<String> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            values.add(part.trim());
        }
        return values;
    }

    public record TableRow(String sheetName, Map<String, String> cells) {
    }
}
