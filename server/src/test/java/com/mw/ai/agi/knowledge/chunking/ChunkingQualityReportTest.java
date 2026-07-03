package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingQualityReportTest {
    @Test
    void summarizesTokenAtomicAndParentCoverageMetrics() {
        List<KnowledgeChunkPreview> chunks = List.of(
                preview(0, 10, true, "parent-1"),
                preview(1, 30, false, "parent-1")
        );

        ChunkingQualityReport report = ChunkingQualityReport.from(chunks);

        assertThat(report.chunkCount()).isEqualTo(2);
        assertThat(report.totalTokens()).isEqualTo(40);
        assertThat(report.minTokens()).isEqualTo(10);
        assertThat(report.maxTokens()).isEqualTo(30);
        assertThat(report.averageTokens()).isEqualTo(20);
        assertThat(report.atomicChunkRatio()).isEqualTo(0.5);
        assertThat(report.parentCoverageRatio()).isEqualTo(1);
    }

    private KnowledgeChunkPreview preview(int index, int tokens, boolean atomic, String parentId) {
        return new KnowledgeChunkPreview(
                index,
                "content-" + index,
                tokens,
                "PARAGRAPH",
                null,
                List.of(),
                parentId,
                null,
                "CHILD",
                atomic,
                "content-" + index,
                "{}",
                "test"
        );
    }
}
