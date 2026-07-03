package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicKnowledgeRerankerTest {
    @Test
    void boostsCandidatesWhoseSectionTitleMatchesTheQuery() {
        KnowledgeChunk generic = chunk("generic", "其他说明");
        KnowledgeChunk matching = chunk("matching", "付款节点");
        List<KnowledgeSearchResult> candidates = List.of(
                new KnowledgeSearchResult(generic.id(), "合同.docx", "付款比例 30%", 1000),
                new KnowledgeSearchResult(matching.id(), "合同.docx", "上线款比例 40%", 800)
        );

        List<KnowledgeSearchResult> reranked = new HeuristicKnowledgeReranker().rerank(
                "付款节点",
                candidates,
                Map.of(generic.id(), generic, matching.id(), matching)
        );

        assertThat(reranked).extracting(KnowledgeSearchResult::id)
                .containsExactly("matching", "generic");
    }

    private KnowledgeChunk chunk(String id, String title) {
        return new KnowledgeChunk(id, "kb", "doc", "合同.docx", "内容", 0, true, 10)
                .withStructure(id, null, null, "CHILD", title, title, "PARAGRAPH", "{}");
    }
}
