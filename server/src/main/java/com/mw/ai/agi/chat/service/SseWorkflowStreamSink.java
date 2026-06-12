package com.mw.ai.agi.chat.service;

import com.mw.ai.agi.workflow.engine.WorkflowStreamSink;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

public final class SseWorkflowStreamSink implements WorkflowStreamSink {
    private final SseEmitter emitter;

    public SseWorkflowStreamSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void emitLlmDelta(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("message.delta").data(Map.of("content", delta)));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to emit message delta", exception);
        }
    }

    @Override
    public void emitCitation(Map<String, Object> citation) {
        if (citation == null || citation.isEmpty()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("citation.added").data(citation));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to emit citation", exception);
        }
    }
}
