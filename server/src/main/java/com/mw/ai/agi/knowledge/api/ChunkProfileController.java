package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.domain.ChunkProfileVersion;
import com.mw.ai.agi.knowledge.service.ChunkProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/chunk-profiles")
public class ChunkProfileController {
    private final ChunkProfileService service;

    public ChunkProfileController(ChunkProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ChunkProfileVersion>> list(@PathVariable String knowledgeBaseId) {
        return ApiResponse.success(service.list(knowledgeBaseId));
    }

    @PostMapping
    public ApiResponse<ChunkProfileVersion> create(
            @PathVariable String knowledgeBaseId,
            @RequestBody SaveProfileRequest request
    ) {
        return ApiResponse.success(service.create(
                knowledgeBaseId,
                request.name(),
                request.strategy(),
                request.chunkSize(),
                request.chunkOverlap(),
                request.configJson()
        ));
    }

    @PostMapping("/{profileId}/activate")
    public ApiResponse<ChunkProfileVersion> activate(
            @PathVariable String knowledgeBaseId,
            @PathVariable String profileId
    ) {
        return ApiResponse.success(service.activate(knowledgeBaseId, profileId));
    }

    @PostMapping("/compare")
    public ApiResponse<ChunkProfileService.ProfileComparison> compare(
            @PathVariable String knowledgeBaseId,
            @RequestBody CompareProfileRequest request
    ) {
        return ApiResponse.success(service.compare(
                knowledgeBaseId,
                request.documentId(),
                request.leftProfileId(),
                request.rightProfileId()
        ));
    }

    public record SaveProfileRequest(
            String name,
            String strategy,
            int chunkSize,
            int chunkOverlap,
            String configJson
    ) {
    }

    public record CompareProfileRequest(String documentId, String leftProfileId, String rightProfileId) {
    }
}
