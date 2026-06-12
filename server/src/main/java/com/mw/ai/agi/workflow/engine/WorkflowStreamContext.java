package com.mw.ai.agi.workflow.engine;

import java.util.Optional;
import java.util.function.Supplier;

public final class WorkflowStreamContext {
    private static final ThreadLocal<WorkflowStreamSink> SINK = new ThreadLocal<>();

    private WorkflowStreamContext() {
    }

    public static <T> T callWith(WorkflowStreamSink sink, Supplier<T> supplier) {
        if (sink == null) {
            return supplier.get();
        }
        SINK.set(sink);
        try {
            return supplier.get();
        } finally {
            SINK.remove();
        }
    }

    public static Optional<WorkflowStreamSink> current() {
        return Optional.ofNullable(SINK.get());
    }
}
