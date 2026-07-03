package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.chunking.TokenCounter;
import com.mw.ai.agi.knowledge.chunking.TokenWindowSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SemanticBoundaryChunker {
    private static final int EMBEDDING_BATCH_SIZE = 64;
    private static final int MIN_TOPIC_CHUNK_TOKENS = 8;
    private static final String SENTENCE_BOUNDARY =
            "(?<=[。！？；：!?;:])\\s*|\\R+|(?<!\\d\\.)(?<=[.!?])\\s+";

    private final EmbeddingClient embeddingClient;
    private final TokenCounter tokenCounter;
    private final TokenWindowSplitter tokenWindowSplitter;

    public SemanticBoundaryChunker(
            EmbeddingClient embeddingClient,
            TokenCounter tokenCounter
    ) {
        this.embeddingClient = embeddingClient;
        this.tokenCounter = tokenCounter;
        this.tokenWindowSplitter = new TokenWindowSplitter(tokenCounter);
    }

    public List<String> split(
            String content,
            KnowledgeSplitRequest request,
            String embeddingModelId
    ) {
        if (embeddingModelId == null || embeddingModelId.isBlank()) {
            throw new IllegalArgumentException(
                    "SEMANTIC splitting requires embeddingModelId"
            );
        }
        List<String> sentences = sentences(content);
        if (sentences.size() <= 1) {
            return tokenWindowSplitter.split(
                    content,
                    request.effectiveChunkSize(),
                    request.effectiveChunkOverlap(),
                    embeddingModelId
            );
        }
        List<List<Double>> vectors = embedInBatches(embeddingModelId, sentences);
        if (vectors.size() != sentences.size()) {
            throw new IllegalStateException(
                    "Embedding provider returned an unexpected vector count"
            );
        }

        int maxTokens = request.effectiveChunkSize();
        double threshold = request.effectiveSemanticSimilarityThreshold();
        List<String> semanticChunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        List<Double> vectorSum = new ArrayList<>();
        int sentenceCount = 0;
        for (int index = 0; index < sentences.size(); index++) {
            String sentence = sentences.get(index);
            List<Double> vector = vectors.get(index);
            boolean exceedsMax = !current.isEmpty()
                    && tokenCounter.count(
                            current + " " + sentence,
                            embeddingModelId
                    ) > maxTokens;
            boolean topicShift = sentenceCount >= 2
                    && tokenCounter.count(current.toString(), embeddingModelId)
                    >= MIN_TOPIC_CHUNK_TOKENS
                    && cosineSimilarity(average(vectorSum, sentenceCount), vector)
                    < threshold;
            if (exceedsMax || topicShift) {
                semanticChunks.add(current.toString().trim());
                current.setLength(0);
                vectorSum.clear();
                sentenceCount = 0;
            }
            current.append(sentence);
            add(vectorSum, vector);
            sentenceCount++;
        }
        if (!current.isEmpty()) {
            semanticChunks.add(current.toString().trim());
        }

        List<String> result = new ArrayList<>();
        for (String chunk : semanticChunks) {
            result.addAll(tokenWindowSplitter.split(
                    chunk,
                    maxTokens,
                    request.effectiveChunkOverlap(),
                    embeddingModelId
            ));
        }
        return List.copyOf(result);
    }

    private List<List<Double>> embedInBatches(
            String embeddingModelId,
            List<String> sentences
    ) {
        Map<String, List<Double>> cache = new LinkedHashMap<>();
        List<String> unique = sentences.stream().distinct().toList();
        for (int start = 0; start < unique.size(); start += EMBEDDING_BATCH_SIZE) {
            List<String> batch = unique.subList(
                    start,
                    Math.min(start + EMBEDDING_BATCH_SIZE, unique.size())
            );
            List<List<Double>> vectors =
                    embeddingClient.embedAll(embeddingModelId, embeddingModelId, batch);
            if (vectors.size() != batch.size()) {
                throw new IllegalStateException(
                        "Embedding provider returned an unexpected vector count"
                );
            }
            for (int index = 0; index < batch.size(); index++) {
                cache.put(batch.get(index), vectors.get(index));
            }
        }
        return sentences.stream().map(cache::get).toList();
    }

    private List<String> sentences(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : content.trim().split(SENTENCE_BOUNDARY)) {
            if (!value.isBlank()) {
                result.add(value.trim());
            }
        }
        return result.isEmpty() ? List.of(content.trim()) : List.copyOf(result);
    }

    private void add(List<Double> sum, List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return;
        }
        while (sum.size() < vector.size()) {
            sum.add(0.0);
        }
        for (int index = 0; index < vector.size(); index++) {
            sum.set(index, sum.get(index) + vector.get(index));
        }
    }

    private List<Double> average(List<Double> sum, int count) {
        if (sum.isEmpty() || count <= 0) {
            return List.of();
        }
        return sum.stream().map(value -> value / count).toList();
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null
                || right == null
                || left.isEmpty()
                || left.size() != right.size()) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.size(); index++) {
            double l = left.get(index);
            double r = right.get(index);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
