package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeContextAssemblerTest {
    private final KnowledgeContextAssembler assembler =
            new KnowledgeContextAssembler(new HeuristicTokenCounter());

    @Test
    void expandsASeedToItsParentWithoutCrossingDocumentBoundary() {
        KnowledgeChunk parent = chunk("parent-1", "doc-1", "完整制度条款", 10, "PARENT", null);
        KnowledgeChunk seed = chunk("child-1", "doc-1", "付款期限", 0, "CHILD", "parent-1");
        KnowledgeChunk otherDocument = chunk("parent-2", "doc-2", "不应回填", 10, "PARENT", null);

        List<KnowledgeSearchResult> result = assembler.expand(
                List.of(new KnowledgeSearchResult(seed.id(), seed.documentName(), seed.content(), 900)),
                List.of(seed, parent, otherDocument),
                500,
                5
        );

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(seed.id());
            assertThat(item.content()).isEqualTo("完整制度条款");
            assertThat(item.score()).isEqualTo(900);
        });
    }

    @Test
    void fallsBackToSeedWhenParentWouldExceedBudget() {
        KnowledgeChunk parent = chunk("parent-1", "doc-1", "很长的完整条款".repeat(100), 10, "PARENT", null);
        KnowledgeChunk seed = chunk("child-1", "doc-1", "付款期限", 0, "CHILD", "parent-1");

        List<KnowledgeSearchResult> result = assembler.expand(
                List.of(new KnowledgeSearchResult(seed.id(), seed.documentName(), seed.content(), 900)),
                List.of(seed, parent),
                5,
                5
        );

        assertThat(result).singleElement()
                .extracting(KnowledgeSearchResult::content)
                .isEqualTo("付款期限");
    }

    private KnowledgeChunk chunk(
            String id,
            String documentId,
            String content,
            int index,
            String level,
            String parentId
    ) {
        return new KnowledgeChunk(id, "kb-1", documentId, documentId + ".docx", content, index, true, 10)
                .withStructure(id, parentId, null, level, "第一章", "第一章", "PARAGRAPH", "{}");
    }
}
