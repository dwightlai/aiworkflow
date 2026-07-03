package com.mw.ai.agi.knowledge.chunking;

import java.util.List;
import java.util.Map;

public record DocumentStructure(
        String documentTitle,
        String sourceType,
        String rawText,
        List<DocumentNode> nodes,
        Map<String, Object> metadata
) {
    public DocumentStructure {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
