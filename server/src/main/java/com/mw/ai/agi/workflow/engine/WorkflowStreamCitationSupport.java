package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowStreamCitationSupport {
    private WorkflowStreamCitationSupport() {
    }

    public static void emitKnowledgeResults(List<KnowledgeSearchResult> results) {
        WorkflowStreamContext.current().ifPresent(sink -> emitKnowledgeResults(sink, results));
    }

    public static void emitKnowledgeResults(WorkflowStreamSink sink, List<KnowledgeSearchResult> results) {
        if (sink == null || results == null || results.isEmpty()) {
            return;
        }
        for (KnowledgeSearchResult result : results) {
            sink.emitCitation(toCitation(result));
        }
    }

    public static Map<String, Object> toCitation(KnowledgeSearchResult result) {
        Map<String, Object> citation = new LinkedHashMap<>();
        citation.put("sourceId", result.id());
        citation.put("title", result.documentName());
        citation.put("excerpt", truncate(result.content(), 240));
        citation.put("sourceType", "KNOWLEDGE");
        citation.put("score", result.score());
        return citation;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}
