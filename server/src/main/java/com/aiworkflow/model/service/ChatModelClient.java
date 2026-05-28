package com.aiworkflow.model.service;

import java.util.Map;

public interface ChatModelClient {
    String generate(String providerId, String model, String prompt, Map<String, Object> options);
}
