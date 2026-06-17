package com.mw.ai.agi.generation.service;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ResearchDocxExporter {
    private final ResearchDocxMasterRenderer masterRenderer;

    public ResearchDocxExporter(ResearchDocxMasterRenderer masterRenderer) {
        this.masterRenderer = masterRenderer;
    }

    public byte[] export(String title, String markdown) {
        return exportFromContentJson(Map.of(
                "title", title == null ? "" : title,
                "sections", List.of(Map.of(
                        "type", "text",
                        "title", "",
                        "content", markdown == null ? "" : markdown
                ))
        ), Map.of());
    }

    @SuppressWarnings("unchecked")
    public byte[] exportFromContentJson(Map<String, Object> contentJson, Map<String, Object> docxConfig) {
        String masterFile = stringValue(docxConfig.get("masterFile"), "");
        if (!masterFile.isBlank()) {
            return masterRenderer.renderFromMasterFile(masterFile, contentJson);
        }
        boolean showCover = bool(docxConfig.get("showCover"), true);
        boolean showToc = bool(docxConfig.get("showToc"), true);
        boolean showReferenceSection = bool(docxConfig.get("showReferenceSection"), true);
        int lineSpacing = intValue(docxConfig.get("lineSpacingPt"), 28);
        String title = stringValue(contentJson.get("title"), "编研成果");

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (showCover) {
                addCover(document, title, stringValue(contentJson.get("topic"), ""), stringValue(contentJson.get("audience"), ""));
                document.createParagraph();
            }
            Object sections = contentJson.get("sections");
            if (showToc && sections instanceof List<?> sectionItems && !sectionItems.isEmpty()) {
                addHeading(document, "目录", 16, true, ParagraphAlignment.LEFT);
                for (Object item : sectionItems) {
                    if (item instanceof Map<?, ?> section) {
                        addBody(document, stringValue(section.get("title"), ""), lineSpacing);
                    }
                }
                document.createParagraph();
            }
            if (sections instanceof List<?> sectionItems) {
                for (Object item : sectionItems) {
                    if (item instanceof Map<?, ?> section) {
                        renderSection(document, (Map<String, Object>) section, lineSpacing, docxConfig);
                    }
                }
            }
            if (showReferenceSection) {
                Object references = contentJson.get("references");
                if (references instanceof List<?> refItems && !refItems.isEmpty()) {
                    addHeading(document, "参考文献 / 资料来源", 14, true, ParagraphAlignment.LEFT);
                    int index = 1;
                    for (Object item : refItems) {
                        if (item instanceof Map<?, ?> ref) {
                            Object refNo = ref.get("refNo");
                            String line = "[" + (refNo != null ? refNo : index) + "] "
                                    + stringValue(ref.get("sourceTitle"), "");
                            addBody(document, line, lineSpacing);
                            index++;
                        }
                    }
                }
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to export DOCX.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderSection(XWPFDocument document, Map<String, Object> section, int lineSpacing, Map<String, Object> docxConfig) {
        String type = stringValue(section.get("type"), "text");
        addHeading(document, stringValue(section.get("title"), ""), 14, true, ParagraphAlignment.LEFT);
        if ("timeline".equals(type)) {
            Object events = section.get("events");
            if (events instanceof List<?> eventItems) {
                for (Object item : eventItems) {
                    if (item instanceof Map<?, ?> event) {
                        String date = stringValue(event.get("date"), "");
                        String description = stringValue(event.get("description"), stringValue(event.get("title"), ""));
                        String line = date.isBlank() ? description : date + "  " + description;
                        addBody(document, line, lineSpacing);
                    }
                }
            }
            return;
        }
        if ("gallery".equals(type)) {
            boolean galleryTable = bool(docxConfig.get("galleryTable"), true);
            Object items = section.get("items");
            if (galleryTable && items instanceof List<?> galleryItems && !galleryItems.isEmpty()) {
                var table = document.createTable(galleryItems.size() + 1, 4);
                table.getRow(0).getCell(0).setText("序号");
                table.getRow(0).getCell(1).setText("题名");
                table.getRow(0).getCell(2).setText("档号");
                table.getRow(0).getCell(3).setText("说明");
                int row = 1;
                for (Object item : galleryItems) {
                    if (item instanceof Map<?, ?> galleryItem) {
                        Object itemIndex = galleryItem.get("index");
                        table.getRow(row).getCell(0).setText(String.valueOf(itemIndex != null ? itemIndex : row));
                        table.getRow(row).getCell(1).setText(stringValue(galleryItem.get("title"), ""));
                        table.getRow(row).getCell(2).setText(stringValue(galleryItem.get("archiveCode"), ""));
                        table.getRow(row).getCell(3).setText(stringValue(galleryItem.get("description"), ""));
                        row++;
                    }
                }
                document.createParagraph();
                return;
            }
        }
        String content = stringValue(section.get("content"), stringValue(section.get("contentMarkdown"), ""));
        for (String line : splitLines(content)) {
            if (line.isBlank()) {
                document.createParagraph();
                continue;
            }
            if (line.startsWith("# ")) {
                addHeading(document, line.substring(2).trim(), 16, true, ParagraphAlignment.LEFT);
            } else if (line.startsWith("## ")) {
                addHeading(document, line.substring(3).trim(), 14, true, ParagraphAlignment.LEFT);
            } else if (line.startsWith("### ")) {
                addHeading(document, line.substring(4).trim(), 12, true, ParagraphAlignment.LEFT);
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                addBody(document, "• " + line.substring(2).trim(), lineSpacing);
            } else {
                addBody(document, line, lineSpacing);
            }
        }
    }

    private void addCover(XWPFDocument document, String title, String topic, String audience) {
        document.createParagraph();
        XWPFParagraph titleParagraph = document.createParagraph();
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(22);
        titleRun.setText(title);
        if (!topic.isBlank()) {
            XWPFParagraph topicParagraph = document.createParagraph();
            topicParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun topicRun = topicParagraph.createRun();
            topicRun.setFontSize(14);
            topicRun.setText("主题：" + topic);
        }
        if (!audience.isBlank()) {
            XWPFParagraph audienceParagraph = document.createParagraph();
            audienceParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun audienceRun = audienceParagraph.createRun();
            audienceRun.setFontSize(12);
            audienceRun.setText("面向对象：" + audience);
        }
    }

    private void addHeading(XWPFDocument document, String text, int fontSize, boolean bold, ParagraphAlignment alignment) {
        if (text == null || text.isBlank()) {
            return;
        }
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setText(text);
    }

    private void addBody(XWPFDocument document, String text, int lineSpacing) {
        if (text == null || text.isBlank()) {
            return;
        }
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBetween(lineSpacing / 12.0);
        XWPFRun run = paragraph.createRun();
        run.setFontSize(12);
        run.setText(text);
    }

    private List<String> splitLines(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        return List.of(markdown.replace("\r\n", "\n").split("\n"));
    }

    private boolean bool(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
