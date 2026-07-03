package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;

import java.util.List;
import java.util.Map;

public interface KnowledgeReranker {
    List<KnowledgeSearchResult> rerank(
            String query,
            List<KnowledgeSearchResult> candidates,
            Map<String, KnowledgeChunk> chunksById
    );
}
