package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkStrategyReviewRegressionTest {

    private final TokenCounter characterTokens = (text, modelId) ->
            text == null ? 0 : text.replaceAll("\\s+", "").length();

    @Test
    void defaultsUseConfiguredChunkSizeAsTargetTokens() {
        ChunkProfile profile = ChunkProfile.defaults("STRUCTURE_AWARE", 1000, 100);

        assertThat(profile.targetTokens()).isEqualTo(1000);
        assertThat(profile.maxTokens()).isEqualTo(1000);
        assertThat(profile.minTokens()).isEqualTo(250);
    }

    @Test
    void structuredSentenceChunksRespectMaximumAndConfiguredOverlap() {
        DocumentStructure document = new DocumentStructure(
                "制度",
                "MD",
                "",
                List.of(),
                Map.of("fileName", "policy.md")
        );
        DocumentNode node = new DocumentNode(
                "node-1",
                NodeType.PARAGRAPH,
                "甲乙丙丁。戊己庚辛。壬癸子丑。",
                List.of("第一章"),
                null,
                null,
                0,
                null,
                Map.of(),
                List.of()
        );
        ChunkProfile profile = ChunkProfile.defaults("STRUCTURE_AWARE", 10, 5);

        List<KnowledgeChunkPreview> chunks =
                new DefaultNodeChunkStrategy(characterTokens).split(document, node, profile, 0);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.tokenEstimate()).isLessThanOrEqualTo(10));
        assertThat(chunks.get(1).content()).startsWith("戊己庚辛。");
    }

    @Test
    void oversizedSingleSentenceFallsBackToTokenWindows() {
        DocumentStructure document = new DocumentStructure(
                "制度", "MD", "", List.of(), Map.of()
        );
        DocumentNode node = new DocumentNode(
                "node-1",
                NodeType.PARAGRAPH,
                "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳",
                List.of("第一章"),
                null,
                null,
                0,
                null,
                Map.of(),
                List.of()
        );

        List<KnowledgeChunkPreview> chunks = new DefaultNodeChunkStrategy(characterTokens)
                .split(document, node, ChunkProfile.defaults("STRUCTURE_AWARE", 6, 1), 0);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.tokenEstimate()).isLessThanOrEqualTo(6));
    }
}
