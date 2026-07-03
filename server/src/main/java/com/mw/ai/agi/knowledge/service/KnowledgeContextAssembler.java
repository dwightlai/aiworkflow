package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.chunking.TokenCounter;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class KnowledgeContextAssembler {
    private static final org.slf4j.Logger QUALITY_LOG =
            org.slf4j.LoggerFactory.getLogger("knowledge.retrieval.quality");
    private final TokenCounter tokenCounter;

    public KnowledgeContextAssembler(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }

    public List<KnowledgeSearchResult> expand(
            List<KnowledgeSearchResult> seeds,
            List<KnowledgeChunk> authorizedChunks,
            int maxTokens,
            int maxResults
    ) {
        if (seeds == null || seeds.isEmpty()) {
            return List.of();
        }
        int tokenBudget = Math.max(maxTokens, 1);
        int resultLimit = Math.max(maxResults, 1);
        Map<String, KnowledgeChunk> chunksById = authorizedChunks.stream()
                .collect(Collectors.toMap(KnowledgeChunk::id, Function.identity(), (left, right) -> left));
        Map<String, Integer> documentCounts = new HashMap<>();
        Set<String> expandedParentIds = new HashSet<>();
        List<KnowledgeSearchResult> result = new ArrayList<>();
        int usedTokens = 0;
        int expandedParentCount = 0;
        int trimmedCount = 0;

        for (KnowledgeSearchResult seed : seeds) {
            if (result.size() >= resultLimit) {
                break;
            }
            KnowledgeChunk child = chunksById.get(seed.id());
            if (child == null || !"CHILD".equalsIgnoreCase(child.chunkLevel())) {
                continue;
            }
            String content = child.content();
            KnowledgeChunk parent = chunksById.get(child.parentChunkId());
            if (isAuthorizedParent(parent, child) && expandedParentIds.add(parent.id())) {
                int parentTokens = tokenCounter.count(parent.content(), null);
                if (usedTokens + parentTokens <= tokenBudget) {
                    content = parent.content();
                    expandedParentCount++;
                }
            }
            int contentTokens = tokenCounter.count(content, null);
            if (usedTokens + contentTokens > tokenBudget && !result.isEmpty()) {
                trimmedCount++;
                continue;
            }
            int countForDocument = documentCounts.getOrDefault(child.documentId(), 0);
            if (countForDocument >= Math.max(1, (resultLimit + 1) / 2)) {
                continue;
            }
            result.add(new KnowledgeSearchResult(seed.id(), seed.documentName(), content, seed.score()));
            documentCounts.put(child.documentId(), countForDocument + 1);
            usedTokens += contentTokens;
        }
        QUALITY_LOG.info(
                "event=context_assembly candidateChunkCount={} seedChunkCount={} expandedParentCount={} trimmedChunkCount={} finalEvidenceGroupCount={} finalContextTokens={}",
                authorizedChunks.size(),
                seeds.size(),
                expandedParentCount,
                trimmedCount,
                result.size(),
                usedTokens
        );
        return List.copyOf(result);
    }

    private boolean isAuthorizedParent(KnowledgeChunk parent, KnowledgeChunk child) {
        return parent != null
                && parent.enabled()
                && "PARENT".equalsIgnoreCase(parent.chunkLevel())
                && parent.knowledgeBaseId().equals(child.knowledgeBaseId())
                && parent.documentId().equals(child.documentId());
    }
}
