package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;

import java.util.List;

public record ChunkingQualityReport(
        int chunkCount,
        int totalTokens,
        int minTokens,
        int maxTokens,
        double averageTokens,
        int atomicChunkCount,
        double atomicChunkRatio,
        int parentLinkedChunkCount,
        double parentCoverageRatio
) {
    public static ChunkingQualityReport from(List<KnowledgeChunkPreview> chunks) {
        List<KnowledgeChunkPreview> values = chunks == null ? List.of() : List.copyOf(chunks);
        if (values.isEmpty()) {
            return new ChunkingQualityReport(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        int total = values.stream().mapToInt(KnowledgeChunkPreview::tokenEstimate).sum();
        int atomic = (int) values.stream().filter(KnowledgeChunkPreview::atomic).count();
        int parentLinked = (int) values.stream()
                .filter(chunk -> chunk.parentChunkId() != null && !chunk.parentChunkId().isBlank())
                .count();
        return new ChunkingQualityReport(
                values.size(),
                total,
                values.stream().mapToInt(KnowledgeChunkPreview::tokenEstimate).min().orElse(0),
                values.stream().mapToInt(KnowledgeChunkPreview::tokenEstimate).max().orElse(0),
                (double) total / values.size(),
                atomic,
                ratio(atomic, values.size()),
                parentLinked,
                ratio(parentLinked, values.size())
        );
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0 : (double) numerator / denominator;
    }
}
