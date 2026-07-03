package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class HeuristicKnowledgeReranker implements KnowledgeReranker {
    @Override
    public List<KnowledgeSearchResult> rerank(
            String query,
            List<KnowledgeSearchResult> candidates,
            Map<String, KnowledgeChunk> chunksById
    ) {
        Set<String> terms = terms(query);
        return candidates.stream()
                .map(result -> {
                    KnowledgeChunk chunk = chunksById.get(result.id());
                    int boost = chunk == null ? 0 : metadataBoost(chunk, terms);
                    return new KnowledgeSearchResult(
                            result.id(),
                            result.documentName(),
                            result.content(),
                            result.score() + boost
                    );
                })
                .sorted(Comparator.comparingInt(KnowledgeSearchResult::score).reversed())
                .toList();
    }

    private int metadataBoost(KnowledgeChunk chunk, Set<String> terms) {
        String title = normalize(chunk.chunkTitle());
        String path = normalize(chunk.sectionPath());
        String documentName = normalize(chunk.documentName());
        int boost = 0;
        for (String term : terms) {
            if (title.contains(term)) {
                boost += 500;
            }
            if (path.contains(term)) {
                boost += 250;
            }
            if (documentName.contains(term)) {
                boost += 300;
            }
        }
        return boost;
    }

    private Set<String> terms(String query) {
        if (query == null || query.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(normalize(query).split("[\\s,，。！？、]+"))
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
