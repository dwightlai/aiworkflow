package com.aiworkflow.model.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StubChatModelClient implements ChatModelClient {
    @Override
    public String generate(String providerId, String model, String prompt, Map<String, Object> options) {
        return "model response";
    }
}
