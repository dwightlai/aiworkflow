package com.mw.ai.agi.knowledge.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkProfileSemanticThresholdTest {

    @Test
    void readsAndClampsSemanticThresholdFromProfileConfig() {
        assertThat(ChunkProfileService.semanticSimilarityThreshold(
                "{\"semanticSimilarityThreshold\":0.66}", 0.78
        )).isEqualTo(0.66);
        assertThat(ChunkProfileService.semanticSimilarityThreshold(
                "{\"semanticSimilarityThreshold\":1.4}", 0.78
        )).isEqualTo(1.0);
        assertThat(ChunkProfileService.semanticSimilarityThreshold(
                "{}", 0.78
        )).isEqualTo(0.78);
    }
}
