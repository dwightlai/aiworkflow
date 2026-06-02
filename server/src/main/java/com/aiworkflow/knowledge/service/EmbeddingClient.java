package com.aiworkflow.knowledge.service;

import java.util.List;

public interface EmbeddingClient {
    List<Double> embed(String providerId, String model, String text);
}
