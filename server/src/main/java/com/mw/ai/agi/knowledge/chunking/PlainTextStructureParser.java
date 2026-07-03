package com.mw.ai.agi.knowledge.chunking;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PlainTextStructureParser implements StructuredDocumentParser {
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".txt");
    }

    @Override
    public DocumentStructure parse(String fileName, String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<DocumentNode> nodes = new ArrayList<>();
        String[] blocks = normalized.split("\\n\\s*\\n");
        for (String block : blocks) {
            String text = block.trim();
            if (text.isBlank()) {
                continue;
            }
            NodeType type = text.matches("(?is)^(Q|问题)\\s*[:：].*\\n(A|答案)\\s*[:：].*")
                    ? NodeType.FAQ
                    : NodeType.PARAGRAPH;
            nodes.add(new DocumentNode(
                    "node_" + UUID.randomUUID(),
                    type,
                    text,
                    List.of(fileName),
                    null,
                    null,
                    nodes.size(),
                    type == NodeType.FAQ ? "faq_" + nodes.size() : null,
                    type == NodeType.FAQ ? Map.of("atomic", true) : Map.of(),
                    List.of()
            ));
        }
        return new DocumentStructure(fileName, "TEXT", normalized, nodes, Map.of("fileName", fileName));
    }
}
