package com.mw.ai.agi.model.service;

import java.util.Map;

public interface ChatModelClient {
    String generate(String providerId, String model, String prompt, Map<String, Object> options);

    default String generateStream(
            String providerId,
            String model,
            String prompt,
            Map<String, Object> options,
            ChatModelStreamConsumer consumer
    ) {
        String full = generate(providerId, model, prompt, options);
        if (consumer != null && full != null && !full.isEmpty()) {
            consumer.onDelta(full);
        }
        return full == null ? "" : full;
    }
}

