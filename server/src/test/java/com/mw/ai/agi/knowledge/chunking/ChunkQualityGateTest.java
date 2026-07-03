package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkQualityGateTest {

    @Test
    void rejectsOversizedNonAtomicChunksBeforePersistence() {
        assertThatThrownBy(() -> new ChunkQualityGate().validate(
                List.of(preview(120, false)),
                100
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("120", "100");
    }

    @Test
    void permitsOversizedAtomicChunks() {
        assertThatCode(() -> new ChunkQualityGate().validate(
                List.of(preview(120, true)),
                100
        )).doesNotThrowAnyException();
    }

    private KnowledgeChunkPreview preview(int tokens, boolean atomic) {
        return new KnowledgeChunkPreview(
                0,
                "content",
                tokens,
                "PARAGRAPH",
                null,
                List.of(),
                null,
                null,
                "CHILD",
                atomic,
                "content",
                "{}",
                "test"
        );
    }
}
