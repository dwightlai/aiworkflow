package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.service.KnowledgeFailureSampleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/failure-samples")
public class KnowledgeFailureSampleController {
    private final KnowledgeFailureSampleService service;

    public KnowledgeFailureSampleController(KnowledgeFailureSampleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeFailureSampleService.FailureSample>> list(
            @PathVariable String knowledgeBaseId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(service.list(knowledgeBaseId, status));
    }

    @PostMapping
    public ApiResponse<KnowledgeFailureSampleService.FailureSample> archive(
            @PathVariable String knowledgeBaseId,
            @RequestBody KnowledgeFailureSampleService.ArchiveRequest request
    ) {
        return ApiResponse.success(service.archive(knowledgeBaseId, request));
    }

    @PatchMapping("/{sampleId}/status")
    public ApiResponse<KnowledgeFailureSampleService.FailureSample> updateStatus(
            @PathVariable String knowledgeBaseId,
            @PathVariable String sampleId,
            @RequestBody UpdateStatusRequest request
    ) {
        return ApiResponse.success(service.updateStatus(knowledgeBaseId, sampleId, request.status()));
    }

    public record UpdateStatusRequest(String status) {
    }
}
