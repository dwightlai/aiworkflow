package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import com.mw.ai.agi.knowledge.domain.KnowledgeDocument;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import com.mw.ai.agi.knowledge.service.KnowledgeBaseService;
import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RequestIdentitySupport identitySupport;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService, RequestIdentitySupport identitySupport) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.identitySupport = identitySupport;
    }

    @GetMapping
    public ApiResponse<PageResponse<KnowledgeBase>> list(HttpServletRequest request) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.list(identitySupport.grantContext(request));
        return ApiResponse.success(new PageResponse<>(knowledgeBases, knowledgeBases.size()));
    }

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@Valid @RequestBody SaveKnowledgeBaseRequest body, HttpServletRequest request) {
        return ApiResponse.success(knowledgeBaseService.create(
                body.name(),
                body.description(),
                identitySupport.resolveOwnerUnitId(body.ownerUnitId(), request),
                body.embeddingModelId(),
                body.vectorStoreConfigId(),
                body.vectorDimension(),
                body.splitterType(),
                body.chunkSize(),
                body.chunkOverlap(),
                body.retrievalMode(),
                body.topK(),
                body.kbType(),
                body.datasetMode()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBase> update(
            @PathVariable String id,
            @Valid @RequestBody SaveKnowledgeBaseRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.update(
                id,
                body.name(),
                body.description(),
                body.ownerUnitId(),
                body.embeddingModelId(),
                body.vectorStoreConfigId(),
                body.vectorDimension(),
                body.splitterType(),
                body.chunkSize(),
                body.chunkOverlap(),
                body.retrievalMode(),
                body.topK()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeBaseService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/embeddings/backfill")
    public ApiResponse<BackfillEmbeddingsResponse> backfillEmbeddings(@PathVariable String id) {
        return ApiResponse.success(new BackfillEmbeddingsResponse(
                knowledgeBaseService.backfillEmbeddingsIfConfigured(id)
        ));
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

    @PostMapping(value = "/documents/text/upload/preview", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeBaseService.UploadedDocumentPreview> previewUploadedTextDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "FIXED_LENGTH") String splitterType,
            @RequestParam(defaultValue = "200") int chunkSize,
            @RequestParam(required = false) String separator
    ) throws IOException {
        return ApiResponse.success(knowledgeBaseService.previewTextDocumentFile(
                fileName(file),
                file.getContentType(),
                file.getInputStream(),
                new com.mw.ai.agi.knowledge.service.KnowledgeSplitRequest(splitterType, chunkSize, separator)
        ));
    }

    @PostMapping(value = "/documents/table/upload/preview", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeBaseService.UploadedDocumentPreview> previewUploadedTableDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "200") int chunkSize
    ) throws IOException {
        return ApiResponse.success(knowledgeBaseService.previewTableDocumentFile(
                fileName(file),
                file.getContentType(),
                file.getInputStream(),
                new com.mw.ai.agi.knowledge.service.KnowledgeSplitRequest("STRUCTURED_TABLE", chunkSize, null)
        ));
    }

    @GetMapping("/{id}/documents")
    public ApiResponse<PageResponse<KnowledgeDocument>> documents(
            @PathVariable String id,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String datasetId
    ) {
        List<KnowledgeDocument> documents = knowledgeBaseService.listDocuments(id, datasetId);
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
                request.chunkOverlap(),
                request.datasetId()
        ));
    }

    @PostMapping("/{id}/documents/manual")
    public ApiResponse<KnowledgeDocument> addManualDataset(
            @PathVariable String id,
            @Valid @RequestBody AddManualDatasetRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.addManualDataset(
                id,
                request.entries().stream()
                        .map(entry -> new KnowledgeBaseService.ManualDatasetEntry(
                                entry.title(),
                                entry.content(),
                                entry.tags(),
                                entry.category(),
                                entry.source()
                        ))
                        .toList(),
                request.datasetId()
        ));
    }

    @PostMapping(value = "/{id}/documents/upload", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeDocument> uploadDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String splitterType,
            @RequestParam(defaultValue = "0") int chunkSize,
            @RequestParam(defaultValue = "-1") int chunkOverlap,
            @RequestParam(required = false) String datasetId
    ) throws IOException {
        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "uploaded-document"
                : file.getOriginalFilename();
        return ApiResponse.success(knowledgeBaseService.addTextDocumentFile(
                id,
                fileName,
                file.getContentType(),
                file.getInputStream(),
                new com.mw.ai.agi.knowledge.service.KnowledgeSplitRequest(
                        splitterType == null || splitterType.isBlank() ? "FIXED_LENGTH" : splitterType,
                        chunkSize <= 0 ? 200 : chunkSize,
                        null
                ),
                datasetId
        ));
    }

    @PostMapping(value = "/{id}/documents/text/upload", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeDocument> uploadTextDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "FIXED_LENGTH") String splitterType,
            @RequestParam(defaultValue = "200") int chunkSize,
            @RequestParam(required = false) String separator,
            @RequestParam(required = false) String datasetId
    ) throws IOException {
        return ApiResponse.success(knowledgeBaseService.addTextDocumentFile(
                id,
                fileName(file),
                file.getContentType(),
                file.getInputStream(),
                new com.mw.ai.agi.knowledge.service.KnowledgeSplitRequest(splitterType, chunkSize, separator),
                datasetId
        ));
    }

    @PostMapping(value = "/{id}/documents/table/upload", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeDocument> uploadTableDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "200") int chunkSize,
            @RequestParam(required = false) String datasetId
    ) throws IOException {
        return ApiResponse.success(knowledgeBaseService.addTableDocumentFile(
                id,
                fileName(file),
                file.getContentType(),
                file.getInputStream(),
                new com.mw.ai.agi.knowledge.service.KnowledgeSplitRequest("STRUCTURED_TABLE", chunkSize, null),
                datasetId
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

    @PostMapping("/{id}/documents/{documentId}/reparse")
    public ApiResponse<KnowledgeDocument> reparseDocument(
            @PathVariable String id,
            @PathVariable String documentId
    ) {
        return ApiResponse.success(knowledgeBaseService.reparseDocument(id, documentId));
    }

    @GetMapping("/{id}/documents/{documentId}/chunks")
    public ApiResponse<PageResponse<KnowledgeChunk>> chunks(
            @PathVariable String id,
            @PathVariable String documentId
    ) {
        List<KnowledgeChunk> chunks = knowledgeBaseService.listChunks(id, documentId);
        return ApiResponse.success(new PageResponse<>(chunks, chunks.size()));
    }

    @PostMapping("/{id}/documents/{documentId}/search")
    public ApiResponse<List<KnowledgeSearchResult>> searchDocument(
            @PathVariable String id,
            @PathVariable String documentId,
            @Valid @RequestBody SearchKnowledgeBaseRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.searchDocument(
                id,
                documentId,
                body.query(),
                body.topK(),
                identitySupport.grantContext(request)
        ));
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
            @Valid @RequestBody SearchKnowledgeBaseRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.search(
                id,
                body.query(),
                body.topK(),
                identitySupport.grantContext(request)
        ));
    }

    public record SaveKnowledgeBaseRequest(
            @NotBlank String name,
            String description,
            String ownerUnitId,
            String embeddingModelId,
            String vectorStoreConfigId,
            int vectorDimension,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String retrievalMode,
            int topK,
            String kbType,
            String datasetMode
    ) {
    }

    public record AddKnowledgeDocumentRequest(
            @NotBlank String name,
            @NotBlank String content,
            String splitterType,
            int chunkSize,
            int chunkOverlap,
            String datasetId
    ) {
    }

    public record AddManualDatasetRequest(List<ManualDatasetEntryRequest> entries, String datasetId) {
    }

    public record ManualDatasetEntryRequest(
            @NotBlank String title,
            @NotBlank String content,
            String tags,
            String category,
            String source
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

    public record BackfillEmbeddingsResponse(int created) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    private String fileName(MultipartFile file) {
        return file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "uploaded-document"
                : file.getOriginalFilename();
    }
}
