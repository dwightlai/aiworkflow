package com.mw.ai.agi.knowledge.service;

import java.util.List;

public interface EmbeddingClient {
    List<Double> embed(String providerId, String model, String text);

    default List<List<Double>> embedAll(String providerId, String model, List<String> texts) {
        return texts.stream()
                .map(text -> embed(providerId, model, text))
                .toList();
    }
}
