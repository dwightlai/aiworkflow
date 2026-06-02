package com.aiworkflow.knowledge.api;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeChunkPreview;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import com.aiworkflow.knowledge.domain.KnowledgeSearchResult;
import com.aiworkflow.knowledge.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
        return ApiResponse.success(knowledgeBaseService.create(
                request.name(),
                request.description(),
                request.embeddingModelId(),
                request.vectorStoreConfigId(),
                request.vectorDimension(),
                request.splitterType(),
                request.chunkSize(),
                request.chunkOverlap(),
                request.retrievalMode(),
                request.topK()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBase> update(
            @PathVariable String id,
            @Valid @RequestBody SaveKnowledgeBaseRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.update(
                id,
                request.name(),
                request.description(),
                request.embeddingModelId(),
                request.vectorStoreConfigId(),
                request.vectorDimension(),
                request.splitterType(),
                request.chunkSize(),
                request.chunkOverlap(),
                request.retrievalMode(),
                request.topK()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeBaseService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/chunks/preview")
    public ApiResponse<List<KnowledgeChunkPreview>> previewChunks(@Valid @RequestBody PreviewChunksRequest request) {
        return ApiResponse.success(knowledgeBaseService.previewChunks(
                request.content(),
                request.splitterType(),
                request.chunkSize(),
                request.chunkOverlap()
        ));
    }

    @PostMapping(value = "/documents/upload/preview", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeBaseService.UploadedDocumentPreview> previewUploadedDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String splitterType,
            @RequestParam(defaultValue = "0") int chunkSize,
            @RequestParam(defaultValue = "-1") int chunkOverlap
    ) throws IOException {
        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "uploaded-document"
                : file.getOriginalFilename();
        return ApiResponse.success(knowledgeBaseService.previewDocumentFile(
                fileName,
                file.getContentType(),
                file.getInputStream(),
                splitterType,
                chunkSize,
                chunkOverlap
        ));
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
        return ApiResponse.success(knowledgeBaseService.addDocument(
                id,
                request.name(),
                request.content(),
                request.splitterType(),
                request.chunkSize(),
                request.chunkOverlap()
        ));
    }

    @PostMapping(value = "/{id}/documents/upload", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeDocument> uploadDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String splitterType,
            @RequestParam(defaultValue = "0") int chunkSize,
            @RequestParam(defaultValue = "-1") int chunkOverlap
    ) throws IOException {
        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "uploaded-document"
                : file.getOriginalFilename();
        return ApiResponse.success(knowledgeBaseService.addDocumentFile(
                id,
                fileName,
                file.getContentType(),
                file.getInputStream(),
                splitterType,
                chunkSize,
                chunkOverlap
        ));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable String id,
            @PathVariable String documentId
    ) {
        knowledgeBaseService.deleteDocument(id, documentId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/documents/{documentId}/chunks")
    public ApiResponse<PageResponse<KnowledgeChunk>> chunks(
            @PathVariable String id,
            @PathVariable String documentId
    ) {
        List<KnowledgeChunk> chunks = knowledgeBaseService.listChunks(id, documentId);
        return ApiResponse.success(new PageResponse<>(chunks, chunks.size()));
    }

    @PutMapping("/{id}/chunks/{chunkId}")
    public ApiResponse<KnowledgeChunk> updateChunk(
            @PathVariable String id,
            @PathVariable String chunkId,
            @Valid @RequestBody UpdateKnowledgeChunkRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.updateChunk(id, chunkId, request.content(), request.enabled()));
    }

    @PostMapping("/{id}/search")
    public ApiResponse<List<KnowledgeSearchResult>> search(
            @PathVariable String id,
            @Valid @RequestBody SearchKnowledgeBaseRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.search(id, request.query(), request.topK()));
    }

    public record SaveKnowledgeBaseRequest(
            @NotBlank String name,
            String description,
            String embeddingModelId,
            String vectorStoreConfigId,
            int vectorDimension,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String retrievalMode,
            int topK
    ) {
    }

    public record AddKnowledgeDocumentRequest(
            @NotBlank String name,
            @NotBlank String content,
            String splitterType,
            int chunkSize,
            int chunkOverlap
    ) {
    }

    public record PreviewChunksRequest(
            @NotBlank String content,
            String splitterType,
            int chunkSize,
            int chunkOverlap
    ) {
    }

    public record UpdateKnowledgeChunkRequest(
            @NotBlank String content,
            boolean enabled
    ) {
    }

    public record SearchKnowledgeBaseRequest(@NotBlank String query, int topK) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
