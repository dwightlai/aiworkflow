package com.mw.ai.agi.knowledge.chunking;

import java.util.List;
import java.util.Map;

public record DocumentNode(
        String id,
        NodeType type,
        String text,
        List<String> sectionPath,
        Integer pageStart,
        Integer pageEnd,
        int order,
        String groupId,
        Map<String, Object> metadata,
        List<DocumentNode> children
) {
    public DocumentNode {
        sectionPath = sectionPath == null ? List.of() : List.copyOf(sectionPath);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        children = children == null ? List.of() : List.copyOf(children);
    }
}
