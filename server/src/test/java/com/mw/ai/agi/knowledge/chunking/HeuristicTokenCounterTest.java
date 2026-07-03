package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import com.mw.ai.agi.knowledge.service.KnowledgeSplitRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicTokenCounterTest {

    private final HeuristicTokenCounter counter = new HeuristicTokenCounter();

    @Test
    void countsChineseAndEnglishWithOneSharedRule() {
        assertThat(counter.count("知识库分块", null)).isGreaterThanOrEqualTo(4);
        assertThat(counter.count("knowledge chunking", null)).isGreaterThanOrEqualTo(2);
    }

    @Test
    void codeLikeTextCostsMoreThanPlainEnglishWords() {
        assertThat(counter.count("{\"chunkSize\": 800}", null))
                .isGreaterThan(counter.count("chunk size", null));
    }

    @Test
    void splitRequestKeepsBackwardCompatibleConstructorAndOverlap() {
        KnowledgeSplitRequest legacy = new KnowledgeSplitRequest("PARAGRAPH", 500, "\n");
        KnowledgeSplitRequest enriched = new KnowledgeSplitRequest("STRUCTURE_AWARE", 800, 80, null);

        assertThat(legacy.chunkOverlap()).isZero();
        assertThat(enriched.effectiveChunkOverlap()).isEqualTo(80);
    }

    @Test
    void chunkPreviewKeepsLegacyConstructorAndCarriesStructureMetadata() {
        KnowledgeChunkPreview legacy = new KnowledgeChunkPreview(0, "content", 2);
        KnowledgeChunkPreview enriched = new KnowledgeChunkPreview(
                1,
                "child content",
                4,
                "PROCEDURE_STEP",
                "第五步",
                List.of("操作手册", "部署流程"),
                "parent_1",
                "group_1",
                "CHILD",
                true,
                "操作手册 部署流程 第五步 child content",
                "{\"profileVersion\":\"v1\"}",
                "procedure-step"
        );

        assertThat(legacy.chunkType()).isEqualTo("PARAGRAPH");
        assertThat(enriched.sectionPath()).containsExactly("操作手册", "部署流程");
        assertThat(enriched.atomic()).isTrue();
        assertThat(enriched.parentChunkId()).isEqualTo("parent_1");
    }
}
