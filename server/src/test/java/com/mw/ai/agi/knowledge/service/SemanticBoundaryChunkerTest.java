package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticBoundaryChunkerTest {
    @Test
    void splitsAtEmbeddingTopicShiftInsteadOfEverySentence() {
        List<String> sentences = List.of(
                "报销申请需要主管审批。",
                "审批通过后提交发票。",
                "GPU服务器用于模型推理。",
                "服务器部署在内网机房。"
        );
        Map<String, List<Double>> vectors = Map.of(
                "报销申请需要主管审批。", List.of(1.0, 0.0),
                "审批通过后提交发票。", List.of(0.95, 0.05),
                "GPU服务器用于模型推理。", List.of(0.0, 1.0),
                "服务器部署在内网机房。", List.of(0.05, 0.95)
        );
        EmbeddingClient embeddingClient = (provider, model, text) -> vectors.get(text);
        SemanticBoundaryChunker chunker = new SemanticBoundaryChunker(
                embeddingClient,
                new HeuristicTokenCounter()
        );

        List<String> chunks = chunker.split(
                String.join("", sentences),
                new KnowledgeSplitRequest("SEMANTIC", 200, 0, null),
                "embedding-model"
        );

        assertThat(chunks).containsExactly(
                "报销申请需要主管审批。审批通过后提交发票。",
                "GPU服务器用于模型推理。服务器部署在内网机房。"
        );
    }

    @Test
    void respectsMaximumTokenLimitEvenWithoutTopicShift() {
        EmbeddingClient embeddingClient = (provider, model, text) -> List.of(1.0, 0.0);
        SemanticBoundaryChunker chunker = new SemanticBoundaryChunker(
                embeddingClient,
                new HeuristicTokenCounter()
        );

        List<String> chunks = chunker.split(
                "第一句内容较长。第二句内容较长。第三句内容较长。",
                new KnowledgeSplitRequest("SEMANTIC", 12, 0, null),
                "embedding-model"
        );

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(16));
    }

    @Test
    void comparesCurrentSentenceWithAccumulatedTopicVector() {
        List<String> sentences = List.of(
                "主题甲第一句。",
                "主题甲第二句。",
                "过渡说明一句。",
                "主题乙正式开始。",
                "主题乙继续说明。"
        );
        Map<String, List<Double>> vectors = Map.of(
                sentences.get(0), List.of(1.0, 0.0),
                sentences.get(1), List.of(1.0, 0.0),
                sentences.get(2), List.of(0.85, 0.527),
                sentences.get(3), List.of(0.4, 0.916),
                sentences.get(4), List.of(0.0, 1.0)
        );
        SemanticBoundaryChunker chunker = new SemanticBoundaryChunker(
                (provider, model, text) -> vectors.get(text),
                new HeuristicTokenCounter()
        );

        List<String> chunks = chunker.split(
                String.join("", sentences),
                new KnowledgeSplitRequest("SEMANTIC", 200, 0, null),
                "embedding-model"
        );

        assertThat(chunks).containsExactly(
                String.join("", sentences.subList(0, 3)),
                String.join("", sentences.subList(3, 5))
        );
    }

    @Test
    void batchesEmbeddingsForLargeDocuments() {
        List<Integer> batchSizes = new ArrayList<>();
        EmbeddingClient embeddingClient = new EmbeddingClient() {
            @Override
            public List<Double> embed(String providerId, String model, String text) {
                return List.of(1.0, 0.0);
            }

            @Override
            public List<List<Double>> embedAll(
                    String providerId,
                    String model,
                    List<String> texts
            ) {
                batchSizes.add(texts.size());
                return texts.stream().map(ignored -> List.of(1.0, 0.0)).toList();
            }
        };
        SemanticBoundaryChunker chunker = new SemanticBoundaryChunker(
                embeddingClient,
                new HeuristicTokenCounter()
        );
        String content = java.util.stream.IntStream.range(0, 130)
                .mapToObj(index -> "第" + index + "句。")
                .collect(java.util.stream.Collectors.joining());

        chunker.split(
                content,
                new KnowledgeSplitRequest("SEMANTIC", 1000, 0, null),
                "embedding-model"
        );

        assertThat(batchSizes).hasSize(3).allMatch(size -> size <= 64);
    }
}
