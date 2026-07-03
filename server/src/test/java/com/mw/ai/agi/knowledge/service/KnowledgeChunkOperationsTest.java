package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkOperationsTest {
    @Test
    void splitsMergesAndAdjustsChildChunksWhileRefreshingParent() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), store);
        KnowledgeBase base = service.create("kb", null);
        KnowledgeDocument document = service.addDocument(
                base.id(),
                "manual.txt",
                "第一部分内容。第二部分内容。"
        );
        KnowledgeChunk original = store.listChunks(base.id(), document.id()).stream()
                .filter(chunk -> "CHILD".equals(chunk.chunkLevel()))
                .findFirst()
                .orElseThrow();

        List<KnowledgeChunk> split = service.splitChunk(base.id(), original.id(), original.content().indexOf("第二"));

        assertThat(split).hasSize(2);
        KnowledgeChunk parent = store.listChunks(base.id(), document.id()).stream()
                .filter(chunk -> "PARENT".equals(chunk.chunkLevel()))
                .findFirst()
                .orElseThrow();
        assertThat(parent.content()).contains("第一部分内容", "第二部分内容");

        KnowledgeChunk adjusted = service.adjustChunkStructure(
                base.id(),
                split.get(1).id(),
                "第二章 > 处理要求",
                parent.id()
        );
        assertThat(adjusted.sectionPath()).isEqualTo("第二章 > 处理要求");

        KnowledgeChunk merged = service.mergeChunks(
                base.id(),
                List.of(split.get(0).id(), split.get(1).id())
        );
        assertThat(merged.content()).contains("第一部分内容", "第二部分内容");
        assertThat(store.listChunks(base.id(), document.id()).stream()
                .filter(chunk -> "CHILD".equals(chunk.chunkLevel())))
                .hasSize(1);
    }
}
