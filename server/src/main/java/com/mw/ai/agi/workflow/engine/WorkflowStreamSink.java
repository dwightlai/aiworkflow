package com.mw.ai.agi.workflow.engine;

import java.util.Map;

public interface WorkflowStreamSink {
    void emitLlmDelta(String delta);

    void emitCitation(Map<String, Object> citation);
}
