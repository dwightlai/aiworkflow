package com.mw.ai.agi.knowledge.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeBaseSemanticThresholdTest {

    @Test
    void legacyConstructionUsesDefaultSemanticThreshold() {
        KnowledgeBase knowledgeBase = new KnowledgeBase(
                "kb-1",
                "tenant-1",
                "Knowledge",
                null,
                null,
                null,
                null,
                1536,
                "SEMANTIC",
                500,
                50,
                "HYBRID",
                5,
                "ENABLED",
                0,
                0,
                "admin",
                "admin",
                Instant.now(),
                Instant.now()
        );

        assertThat(knowledgeBase.semanticSimilarityThreshold()).isEqualTo(0.78);
    }
}
