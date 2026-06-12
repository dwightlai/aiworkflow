package com.mw.ai.agi.model.service;

@FunctionalInterface
public interface ChatModelStreamConsumer {
    void onDelta(String delta);
}
