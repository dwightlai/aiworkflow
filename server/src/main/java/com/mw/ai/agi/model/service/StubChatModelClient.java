package com.mw.ai.agi.model.service;

import java.util.Map;

public class StubChatModelClient implements ChatModelClient {
    @Override
    public String generate(String providerId, String model, String prompt, Map<String, Object> options) {
        return "model response";
    }
}
