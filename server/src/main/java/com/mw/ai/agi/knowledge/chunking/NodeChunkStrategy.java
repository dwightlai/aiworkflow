package com.mw.ai.agi.knowledge.chunking;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;

import java.util.List;

public interface NodeChunkStrategy {
    boolean supports(DocumentNode node, ChunkProfile profile);

    List<KnowledgeChunkPreview> split(
            DocumentStructure document,
            DocumentNode node,
            ChunkProfile profile,
            int startIndex
    );
}
