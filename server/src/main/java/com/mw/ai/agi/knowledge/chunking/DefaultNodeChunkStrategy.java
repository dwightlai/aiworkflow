package com.mw.ai.agi.knowledge.chunking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultNodeChunkStrategy implements NodeChunkStrategy {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SENTENCE_BOUNDARY =
            "(?<=[。！？；：!?;:])\\s*|\\R+|(?<!\\d\\.)(?<=[.!?])\\s+";

    private final TokenCounter tokenCounter;
    private final TokenWindowSplitter tokenWindowSplitter;

    public DefaultNodeChunkStrategy(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
        this.tokenWindowSplitter = new TokenWindowSplitter(tokenCounter);
    }

    @Override
    public boolean supports(DocumentNode node, ChunkProfile profile) {
        return node.type() != NodeType.TITLE && node.type() != NodeType.DOCUMENT;
    }

    @Override
    public List<KnowledgeChunkPreview> split(
            DocumentStructure document,
            DocumentNode node,
            ChunkProfile profile,
            int startIndex
    ) {
        boolean atomic = profile.preserveAtomicBlocks() && (
                Boolean.TRUE.equals(node.metadata().get("atomic"))
                        || node.type() == NodeType.CODE_BLOCK
                        || node.type() == NodeType.TABLE
                        || node.type() == NodeType.FAQ
                        || node.type() == NodeType.TABLE_ROW
        );
        List<String> contents = atomic || tokenCounter.count(node.text(), null) <= profile.maxTokens()
                ? List.of(node.text())
                : splitAtSentenceBoundaries(
                        node.text(),
                        profile.maxTokens(),
                        profile.overlapTokens()
                );
        List<KnowledgeChunkPreview> result = new ArrayList<>();
        String parentId = parentId(document, node);
        List<String> sectionPath = profile.includeSectionPath() ? node.sectionPath() : List.of();
        String title = sectionPath.isEmpty()
                ? document.documentTitle()
                : sectionPath.get(sectionPath.size() - 1);
        for (String content : contents) {
            String embeddingContent = embeddingContent(
                    document.documentTitle(), sectionPath, node, content
            );
            result.add(new KnowledgeChunkPreview(
                    startIndex + result.size(),
                    content,
                    tokenCounter.count(content, null),
                    node.type().name(),
                    title,
                    sectionPath,
                    parentId,
                    node.groupId(),
                    "CHILD",
                    atomic,
                    embeddingContent,
                    metadataJson(document, node),
                    splitReason(node.type(), atomic)
            ));
        }
        return result;
    }

    private List<String> splitAtSentenceBoundaries(
            String text,
            int maxTokens,
            int overlapTokens
    ) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : text.split(SENTENCE_BOUNDARY)) {
            String value = sentence.trim();
            if (value.isBlank()) {
                continue;
            }
            int sentenceTokens = tokenCounter.count(value, null);
            if (sentenceTokens > maxTokens) {
                if (!current.isEmpty()) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                }
                List<String> windows =
                        tokenWindowSplitter.split(value, maxTokens, overlapTokens, null);
                if (windows.size() > 1) {
                    chunks.addAll(windows.subList(0, windows.size() - 1));
                }
                current.append(windows.get(windows.size() - 1));
                continue;
            }
            if (!current.isEmpty()
                    && tokenCounter.count(current + " " + value, null) > maxTokens) {
                String completed = current.toString().trim();
                chunks.add(completed);
                current.setLength(0);
                int carryTokens = Math.min(
                        overlapTokens,
                        Math.max(0, maxTokens - sentenceTokens)
                );
                current.append(tokenWindowSplitter.suffix(completed, carryTokens, null));
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(value);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks.isEmpty()
                ? tokenWindowSplitter.split(text, maxTokens, overlapTokens, null)
                : List.copyOf(chunks);
    }

    private String parentId(DocumentStructure document, DocumentNode node) {
        String key = document.documentTitle() + "|" + String.join("/", node.sectionPath());
        return "parent_" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private String embeddingContent(
            String documentTitle,
            List<String> sectionPath,
            DocumentNode node,
            String content
    ) {
        if (node.type() == NodeType.TABLE_ROW) {
            String sheetName = String.valueOf(node.metadata().getOrDefault("sheetName", ""));
            String prefix = sheetName.isBlank() ? "表格记录" : sheetName + "记录";
            return prefix + "：\n" + content;
        }
        StringBuilder result = new StringBuilder();
        if (documentTitle != null && !documentTitle.isBlank()) {
            result.append("文档：").append(documentTitle).append('\n');
        }
        if (!sectionPath.isEmpty()) {
            result.append("章节：").append(String.join(" > ", sectionPath)).append('\n');
        }
        result.append("类型：")
                .append(node.type().name())
                .append("\n\n")
                .append(content);
        return result.toString();
    }

    private String metadataJson(DocumentStructure document, DocumentNode node) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        Object fileName = document.metadata().get("fileName");
        if (fileName != null) {
            metadata.put("sourceFile", fileName);
        }
        metadata.putAll(node.metadata());
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize chunk metadata", exception);
        }
    }

    private String splitReason(NodeType type, boolean atomic) {
        return atomic
                ? "atomic-" + type.name().toLowerCase()
                : "structure-" + type.name().toLowerCase();
    }
}
