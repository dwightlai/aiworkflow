package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DefaultChunkStrategyRouter implements ChunkStrategyRouter {
    private final List<NodeChunkStrategy> strategies;
    private final TokenCounter tokenCounter;

    public DefaultChunkStrategyRouter(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
        this.strategies = List.of(new DefaultNodeChunkStrategy(tokenCounter));
    }

    @Override
    public List<KnowledgeChunkPreview> split(DocumentStructure document, ChunkProfile profile) {
        List<KnowledgeChunkPreview> result = new ArrayList<>();
        List<DocumentNode> prepared = aggregateSmallEmbeddedTables(document, profile);
        for (DocumentNode node : aggregateShortSections(prepared, profile)) {
            NodeChunkStrategy strategy = strategies.stream()
                    .filter(candidate -> candidate.supports(node, profile))
                    .findFirst()
                    .orElse(null);
            if (strategy != null) {
                result.addAll(strategy.split(document, node, profile, result.size()));
            }
        }
        return result;
    }

    private List<DocumentNode> aggregateSmallEmbeddedTables(
            DocumentStructure document,
            ChunkProfile profile
    ) {
        if ("XLSX".equalsIgnoreCase(document.sourceType())) {
            return document.nodes();
        }
        List<DocumentNode> result = new ArrayList<>();
        List<DocumentNode> nodes = document.nodes();
        for (int index = 0; index < nodes.size(); ) {
            DocumentNode first = nodes.get(index);
            if (first.type() != NodeType.TABLE_ROW || first.groupId() == null) {
                result.add(first);
                index++;
                continue;
            }
            List<DocumentNode> group = new ArrayList<>();
            int tokens = 0;
            int cursor = index;
            while (cursor < nodes.size()) {
                DocumentNode candidate = nodes.get(cursor);
                if (candidate.type() != NodeType.TABLE_ROW
                        || !first.groupId().equals(candidate.groupId())
                        || !first.sectionPath().equals(candidate.sectionPath())) {
                    break;
                }
                group.add(candidate);
                tokens += tokenCounter.count(candidate.text(), null);
                cursor++;
            }
            if (group.size() > 1 && tokens <= profile.maxTokens()) {
                result.add(mergeTable(group, result.size()));
            } else {
                result.addAll(group);
            }
            index = cursor;
        }
        return result;
    }

    private DocumentNode mergeTable(List<DocumentNode> nodes, int order) {
        DocumentNode first = nodes.get(0);
        Map<String, Object> metadata = new LinkedHashMap<>(first.metadata());
        metadata.put("atomic", true);
        metadata.put("tableRowCount", nodes.size());
        metadata.put(
                "rows",
                nodes.stream().map(node -> node.metadata().get("data")).toList()
        );
        return new DocumentNode(
                "node_" + UUID.randomUUID(),
                NodeType.TABLE,
                nodes.stream()
                        .map(DocumentNode::text)
                        .collect(java.util.stream.Collectors.joining("\n\n")),
                first.sectionPath(),
                first.pageStart(),
                nodes.get(nodes.size() - 1).pageEnd(),
                order,
                first.groupId(),
                metadata,
                List.of()
        );
    }

    private List<DocumentNode> aggregateShortSections(List<DocumentNode> nodes, ChunkProfile profile) {
        List<DocumentNode> result = new ArrayList<>();
        for (int index = 0; index < nodes.size(); ) {
            DocumentNode first = nodes.get(index);
            if (!isShortSectionContent(first, profile)) {
                result.add(first);
                index++;
                continue;
            }
            List<DocumentNode> group = new ArrayList<>();
            int tokens = 0;
            int cursor = index;
            while (cursor < nodes.size()) {
                DocumentNode candidate = nodes.get(cursor);
                int candidateTokens = tokenCounter.count(candidate.text(), null);
                if (!isShortSectionContent(candidate, profile)
                        || !candidate.sectionPath().equals(first.sectionPath())
                        || (!group.isEmpty() && tokens + candidateTokens > profile.targetTokens())) {
                    break;
                }
                group.add(candidate);
                tokens += candidateTokens;
                cursor++;
            }
            if (group.size() == 1) {
                result.add(first);
            } else {
                result.add(merge(group, result.size()));
            }
            index = cursor;
        }
        return result;
    }

    private boolean isShortSectionContent(DocumentNode node, ChunkProfile profile) {
        return !node.sectionPath().isEmpty()
                && (node.type() == NodeType.PARAGRAPH || node.type() == NodeType.LIST)
                && !Boolean.TRUE.equals(node.metadata().get("atomic"))
                && tokenCounter.count(node.text(), null) < profile.minTokens();
    }

    private DocumentNode merge(List<DocumentNode> nodes, int order) {
        DocumentNode first = nodes.get(0);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("aggregated", true);
        metadata.put("sourceNodeCount", nodes.size());
        metadata.put(
                "sourceMetadata",
                nodes.stream().map(DocumentNode::metadata).toList()
        );
        return new DocumentNode(
                "node_" + UUID.randomUUID(),
                nodes.stream().allMatch(node -> node.type() == NodeType.LIST)
                        ? NodeType.LIST
                        : NodeType.PARAGRAPH,
                nodes.stream().map(DocumentNode::text).collect(java.util.stream.Collectors.joining("\n")),
                first.sectionPath(),
                first.pageStart(),
                nodes.get(nodes.size() - 1).pageEnd(),
                order,
                first.groupId(),
                metadata,
                List.of()
        );
    }
}
