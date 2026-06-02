package com.aiworkflow.knowledge.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LocalEmbeddingClient implements EmbeddingClient {
    public static final int DIMENSIONS = 64;

    @Override
    public List<Double> embed(String providerId, String model, String text) {
        double[] vector = new double[DIMENSIONS];
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        normalized.codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(codePoint -> vector[Math.floorMod(codePoint, DIMENSIONS)] += 1.0);
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        List<Double> embedding = new ArrayList<>(DIMENSIONS);
        for (double value : vector) {
            embedding.add(norm == 0.0 ? 0.0 : value / norm);
        }
        return embedding;
    }
}
