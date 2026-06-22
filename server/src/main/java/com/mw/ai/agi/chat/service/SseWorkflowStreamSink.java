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

    @Override
    public void emitToolStarted(String connectorCode, String operationCode) {
        emitToolEvent("tool.started", connectorCode, operationCode, null);
    }

    @Override
    public void emitToolCompleted(String connectorCode, String operationCode) {
        emitToolEvent("tool.completed", connectorCode, operationCode, null);
    }

    @Override
    public void emitToolFailed(String connectorCode, String operationCode, String message) {
        emitToolEvent("tool.failed", connectorCode, operationCode, message);
    }

    private void emitToolEvent(String eventName, String connectorCode, String operationCode, String message) {
        try {
            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("connectorCode", connectorCode);
            data.put("operationCode", operationCode);
            data.put("name", operationCode == null || operationCode.isBlank() ? connectorCode : operationCode);
            if (message != null && !message.isBlank()) {
                data.put("message", message);
            }
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to emit " + eventName, exception);
        }
    }
}
