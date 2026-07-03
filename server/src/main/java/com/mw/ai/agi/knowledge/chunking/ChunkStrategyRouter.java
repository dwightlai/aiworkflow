package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;

import java.util.List;

public interface ChunkStrategyRouter {
    List<KnowledgeChunkPreview> split(DocumentStructure document, ChunkProfile profile);
}
