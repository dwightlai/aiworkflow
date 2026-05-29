package com.aiworkflow.knowledge.api;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import com.aiworkflow.knowledge.domain.KnowledgeSearchResult;
import com.aiworkflow.knowledge.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {
    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    public ApiResponse<PageResponse<KnowledgeBase>> list() {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.list();
        return ApiResponse.success(new PageResponse<>(knowledgeBases, knowledgeBases.size()));
    }

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@Valid @RequestBody SaveKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.create(request.name(), request.description()));
    }

    @GetMapping("/{id}/documents")
    public ApiResponse<PageResponse<KnowledgeDocument>> documents(@PathVariable String id) {
        List<KnowledgeDocument> documents = knowledgeBaseService.listDocuments(id);
        return ApiResponse.success(new PageResponse<>(documents, documents.size()));
    }

    @PostMapping("/{id}/documents")
    public ApiResponse<KnowledgeDocument> addDocument(
            @PathVariable String id,
            @Valid @RequestBody AddKnowledgeDocumentRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.addDocument(id, request.name(), request.content()));
    }

    @PostMapping("/{id}/search")
    public ApiResponse<List<KnowledgeSearchResult>> search(
            @PathVariable String id,
            @Valid @RequestBody SearchKnowledgeBaseRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.search(id, request.query(), request.topK()));
    }

    public record SaveKnowledgeBaseRequest(@NotBlank String name, String description) {
    }

    public record AddKnowledgeDocumentRequest(@NotBlank String name, @NotBlank String content) {
    }

    public record SearchKnowledgeBaseRequest(@NotBlank String query, int topK) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
