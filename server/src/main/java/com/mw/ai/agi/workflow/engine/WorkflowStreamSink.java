package com.mw.ai.agi.workflow.engine;

import java.util.Map;

public interface WorkflowStreamSink {
    void emitLlmDelta(String delta);

    void emitCitation(Map<String, Object> citation);

    default void emitToolStarted(String connectorCode, String operationCode) {
    }

    default void emitToolCompleted(String connectorCode, String operationCode) {
    }

    default void emitToolFailed(String connectorCode, String operationCode, String message) {
    }
}
