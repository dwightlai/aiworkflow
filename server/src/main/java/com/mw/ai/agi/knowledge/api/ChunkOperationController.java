package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/chunk-operations")
public class ChunkOperationController {
    private final KnowledgeBaseService service;

    public ChunkOperationController(KnowledgeBaseService service) {
        this.service = service;
    }

    @PostMapping("/{chunkId}/split")
    public ApiResponse<List<KnowledgeChunk>> split(
            @PathVariable String knowledgeBaseId,
            @PathVariable String chunkId,
            @RequestBody SplitChunkRequest request
    ) {
        return ApiResponse.success(service.splitChunk(knowledgeBaseId, chunkId, request.offset()));
    }

    @PostMapping("/merge")
    public ApiResponse<KnowledgeChunk> merge(
            @PathVariable String knowledgeBaseId,
            @RequestBody MergeChunksRequest request
    ) {
        return ApiResponse.success(service.mergeChunks(knowledgeBaseId, request.chunkIds()));
    }

    @PutMapping("/{chunkId}/structure")
    public ApiResponse<KnowledgeChunk> adjust(
            @PathVariable String knowledgeBaseId,
            @PathVariable String chunkId,
            @RequestBody AdjustChunkRequest request
    ) {
        return ApiResponse.success(service.adjustChunkStructure(
                knowledgeBaseId,
                chunkId,
                request.sectionPath(),
                request.parentChunkId()
        ));
    }

    public record SplitChunkRequest(int offset) {
    }

    public record MergeChunksRequest(List<String> chunkIds) {
    }

    public record AdjustChunkRequest(String sectionPath, String parentChunkId) {
    }
}
