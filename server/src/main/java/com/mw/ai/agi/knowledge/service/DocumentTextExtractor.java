package com.mw.ai.agi.knowledge.service;

import org.apache.tika.Tika;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DocumentTextExtractor {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "csv", "html", "htm", "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"
    );

    private final Tika tika = new Tika();

    public String extract(String fileName, String contentType, InputStream inputStream) {
        String extension = extensionOf(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported knowledge document type: " + extension);
        }
        try {
            byte[] bytes = inputStream.readAllBytes();
            String text = switch (extension) {
                case "txt", "md", "markdown", "csv" -> new String(bytes, StandardCharsets.UTF_8);
                case "docx" -> extractDocx(bytes);
                case "xlsx" -> extractXlsx(bytes);
                case "pdf" -> extractPdf(bytes);
                default -> tika.parseToString(new ByteArrayInputStream(bytes));
            };
            String normalized = normalize(text);
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("Uploaded document has no extractable text: " + fileName);
            }
            return normalized;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read uploaded document: " + fileName, exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to extract uploaded document text: " + fileName, exception);
        }
    }

    public boolean supports(String fileName) {
        return SUPPORTED_EXTENSIONS.contains(extensionOf(fileName));
    }

    private String normalize(String text) {
        return text == null ? "" : text
                .replace("\uFEFF", "")
                .replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String extractDocx(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        boolean firstParagraph = true;
        int numberedListIndex = 0;
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (IBodyElement element : document.getBodyElements()) {
                if (element.getElementType() == BodyElementType.PARAGRAPH) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
                    if (text.isBlank()) {
                        continue;
                    }
                    int headingLevel = headingLevel(paragraph, text, firstParagraph);
                    if (headingLevel > 0) {
                        result.append("#".repeat(headingLevel)).append(' ');
                    }
                    if (isNumberedList(paragraph)) {
                        result.append(++numberedListIndex).append(". ");
                    } else {
                        numberedListIndex = 0;
                    }
                    result.append(text).append("\n\n");
                    firstParagraph = false;
                } else if (element.getElementType() == BodyElementType.TABLE) {
                    numberedListIndex = 0;
                    appendDocxTable(result, (XWPFTable) element);
                }
            }
        }
        return result.toString();
    }

    private boolean isNumberedList(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        return paragraph.getNumID() != null
                || (style != null && style.toLowerCase(Locale.ROOT).contains("listnumber"));
    }

    private int headingLevel(XWPFParagraph paragraph, String text, boolean firstParagraph) {
        String style = paragraph.getStyle();
        if (style != null) {
            java.util.regex.Matcher matcher = Pattern.compile("(?i)(?:heading|标题)\\s*([1-6])").matcher(style);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            if ("title".equalsIgnoreCase(style)) {
                return 1;
            }
        }
        if (firstParagraph) {
            return 1;
        }
        if (text.matches("^[一二三四五六七八九十]+、.+")) {
            return 2;
        }
        if (text.matches("^\\d+(?:\\.\\d+)*[、.．]\\s*.+")) {
            return Math.min(6, 2 + (int) text.chars().filter(ch -> ch == '.').count());
        }
        return 0;
    }

    private void appendDocxTable(StringBuilder result, XWPFTable table) {
        List<List<String>> rows = table.getRows().stream()
                .map(row -> row.getTableCells().stream()
                        .map(cell -> cell.getText().replace('\n', ' ').trim())
                        .toList())
                .filter(row -> row.stream().anyMatch(value -> !value.isBlank()))
                .toList();
        appendMarkdownTable(result, rows);
    }

    private String extractXlsx(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            for (Sheet sheet : workbook) {
                result.append("# 工作表：").append(sheet.getSheetName()).append("\n\n");
                List<List<String>> rows = new ArrayList<>();
                int maxColumns = 0;
                for (Row row : sheet) {
                    maxColumns = Math.max(maxColumns, row.getLastCellNum());
                }
                for (Row row : sheet) {
                    List<String> values = new ArrayList<>();
                    for (int column = 0; column < Math.max(maxColumns, 0); column++) {
                        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                    }
                    if (values.stream().anyMatch(value -> !value.isBlank())) {
                        rows.add(values);
                    }
                }
                appendMarkdownTable(result, rows);
            }
        }
        return result.toString();
    }

    private void appendMarkdownTable(StringBuilder result, List<List<String>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        int columns = rows.stream().mapToInt(List::size).max().orElse(1);
        appendMarkdownRow(result, rows.get(0), columns);
        result.append('|').append(" --- |".repeat(columns)).append('\n');
        for (int index = 1; index < rows.size(); index++) {
            appendMarkdownRow(result, rows.get(index), columns);
        }
        result.append('\n');
    }

    private void appendMarkdownRow(StringBuilder result, List<String> row, int columns) {
        result.append('|');
        for (int index = 0; index < columns; index++) {
            String value = index < row.size() ? row.get(index) : "";
            result.append(' ').append(value.replace("|", "\\|")).append(" |");
        }
        result.append('\n');
    }

    private String extractPdf(byte[] bytes) throws Exception {
        List<List<String>> pages = new ArrayList<>();
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                List<String> lines = stripper.getText(document).lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .toList();
                pages.add(lines);
            }
        }
        Map<String, Integer> edgeFrequency = new HashMap<>();
        for (List<String> page : pages) {
            for (String line : edgeLines(page)) {
                edgeFrequency.merge(line, 1, Integer::sum);
            }
        }
        StringBuilder result = new StringBuilder();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            result.append("# 第 ").append(pageIndex + 1).append(" 页\n\n");
            List<String> pageLines = pages.get(pageIndex);
            for (int lineIndex = 0; lineIndex < pageLines.size(); lineIndex++) {
                String line = pageLines.get(lineIndex);
                if (isNumericPageMarker(line, lineIndex, pageLines.size())) {
                    continue;
                }
                if (edgeFrequency.getOrDefault(line, 0) >= 2) {
                    continue;
                }
                result.append(line).append('\n');
            }
            result.append('\n');
        }
        return result.toString();
    }

    private boolean isNumericPageMarker(String line, int lineIndex, int lineCount) {
        boolean atPageEdge = lineIndex == 0 || lineIndex == lineCount - 1;
        return atPageEdge && line.matches("\\d{1,4}");
    }

    private List<String> edgeLines(List<String> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        result.add(lines.get(0));
        if (lines.size() > 1) {
            result.add(lines.get(lines.size() - 1));
        }
        return result;
    }

    private String extensionOf(String fileName) {
        String normalized = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dotIndex + 1);
    }
}
