package com.mw.ai.agi.knowledge.chunking;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownStructureParser implements StructuredDocumentParser {
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern PROCEDURE_HEADING = Pattern.compile(".*第[一二三四五六七八九十\\d]+步.*");

    @Override
    public boolean supports(String fileName) {
        String value = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return value.endsWith(".md") || value.endsWith(".markdown");
    }

    @Override
    public DocumentStructure parse(String fileName, String content) {
        String normalized = normalize(content);
        List<DocumentNode> nodes = new ArrayList<>();
        List<String> headingStack = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        List<String> paragraphPath = List.of();
        String[] lines = normalized.split("\\n", -1);

        for (int index = 0; index < lines.length; ) {
            String line = lines[index];
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                flushParagraph(nodes, paragraph, paragraphPath);
                int level = heading.group(1).length();
                String title = heading.group(2).trim();
                while (headingStack.size() >= level) {
                    headingStack.remove(headingStack.size() - 1);
                }
                headingStack.add(title);
                NodeType type = detectHeadingType(title);
                nodes.add(node(type, title, headingStack, nodes.size(), null, Map.of("headingLevel", level)));
                index++;
                continue;
            }

            if (line.stripLeading().startsWith("```")) {
                flushParagraph(nodes, paragraph, paragraphPath);
                String language = line.strip().substring(3).trim();
                StringBuilder code = new StringBuilder(line).append('\n');
                index++;
                while (index < lines.length) {
                    code.append(lines[index]);
                    if (lines[index].strip().startsWith("```")) {
                        index++;
                        break;
                    }
                    code.append('\n');
                    index++;
                }
                nodes.add(node(
                        NodeType.CODE_BLOCK,
                        code.toString().trim(),
                        headingStack,
                        nodes.size(),
                        null,
                        Map.of("atomic", true, "language", language)
                ));
                continue;
            }

            if (isTableLine(line)) {
                flushParagraph(nodes, paragraph, paragraphPath);
                StringBuilder table = new StringBuilder();
                while (index < lines.length && isTableLine(lines[index])) {
                    if (!table.isEmpty()) {
                        table.append('\n');
                    }
                    table.append(lines[index].trim());
                    index++;
                }
                nodes.add(node(
                        NodeType.TABLE,
                        table.toString(),
                        headingStack,
                        nodes.size(),
                        "table_" + nodes.size(),
                        Map.of("atomic", true)
                ));
                continue;
            }

            if (isQuestion(line)) {
                flushParagraph(nodes, paragraph, paragraphPath);
                StringBuilder faq = new StringBuilder(line.trim());
                index++;
                while (index < lines.length && !lines[index].isBlank() && !isQuestion(lines[index]) && !HEADING.matcher(lines[index]).matches()) {
                    faq.append('\n').append(lines[index].trim());
                    index++;
                }
                nodes.add(node(
                        NodeType.FAQ,
                        faq.toString(),
                        headingStack,
                        nodes.size(),
                        "faq_" + nodes.size(),
                        Map.of("atomic", true)
                ));
                continue;
            }

            if (line.isBlank()) {
                flushParagraph(nodes, paragraph, paragraphPath);
                index++;
                continue;
            }

            if (paragraph.isEmpty()) {
                paragraphPath = List.copyOf(headingStack);
            } else {
                paragraph.append('\n');
            }
            paragraph.append(line.trim());
            index++;
        }
        flushParagraph(nodes, paragraph, paragraphPath);

        String documentTitle = nodes.stream()
                .filter(node -> node.type() == NodeType.TITLE)
                .map(DocumentNode::text)
                .findFirst()
                .orElseGet(() -> stripExtension(fileName));
        return new DocumentStructure(documentTitle, "MARKDOWN", normalized, nodes, Map.of("fileName", fileName));
    }

    private NodeType detectHeadingType(String title) {
        if (PROCEDURE_HEADING.matcher(title).matches()) {
            return NodeType.PROCEDURE_STEP;
        }
        if (title.startsWith("议题") || title.contains("会议议题")) {
            return NodeType.MEETING_TOPIC;
        }
        if (title.startsWith("附录") || title.startsWith("附件")) {
            return NodeType.APPENDIX;
        }
        return NodeType.TITLE;
    }

    private void flushParagraph(List<DocumentNode> nodes, StringBuilder paragraph, List<String> sectionPath) {
        if (paragraph.isEmpty()) {
            return;
        }
        String text = paragraph.toString().trim();
        NodeType type = isList(text) ? NodeType.LIST : NodeType.PARAGRAPH;
        nodes.add(node(type, text, sectionPath, nodes.size(), null, Map.of()));
        paragraph.setLength(0);
    }

    private DocumentNode node(
            NodeType type,
            String text,
            List<String> sectionPath,
            int order,
            String groupId,
            Map<String, Object> metadata
    ) {
        return new DocumentNode(
                "node_" + UUID.randomUUID(),
                type,
                text,
                List.copyOf(sectionPath),
                null,
                null,
                order,
                groupId,
                new LinkedHashMap<>(metadata),
                List.of()
        );
    }

    private boolean isTableLine(String line) {
        String value = line == null ? "" : line.trim();
        return value.startsWith("|") && value.endsWith("|") && value.length() > 2;
    }

    private boolean isQuestion(String line) {
        String value = line == null ? "" : line.trim();
        return value.matches("(?i)^(Q|问题)\\s*[:：].+");
    }

    private boolean isList(String text) {
        return text.lines().allMatch(line -> line.matches("^\\s*(?:[-*+] |\\d+[.、]\\s*).+"));
    }

    private String normalize(String content) {
        return content == null ? "" : content
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }

    private String stripExtension(String fileName) {
        String value = fileName == null ? "document" : fileName;
        int dot = value.lastIndexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }
}
