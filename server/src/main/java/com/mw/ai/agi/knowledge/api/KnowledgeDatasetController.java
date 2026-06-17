package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeDataset;
import com.mw.ai.agi.knowledge.domain.KnowledgeSourceIndex;
import com.mw.ai.agi.knowledge.service.KnowledgeDatasetService;
import com.mw.ai.agi.knowledge.service.KnowledgeBaseService;
import com.mw.ai.agi.knowledge.service.KnowledgeSourceSyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class KnowledgeDatasetController {
    private final KnowledgeDatasetService datasetService;
    private final KnowledgeSourceSyncService sourceSyncService;
    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeDatasetController(
            KnowledgeDatasetService datasetService,
            KnowledgeSourceSyncService sourceSyncService,
            KnowledgeBaseService knowledgeBaseService
    ) {
        this.datasetService = datasetService;
        this.sourceSyncService = sourceSyncService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/knowledge-bases/{kbId}/datasets")
    public ApiResponse<PageResponse<KnowledgeDataset>> listDatasets(@PathVariable String kbId) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureDefaultDataset(knowledgeBaseService.getKnowledgeBase(kbId));
        List<KnowledgeDataset> items = datasetService.listForUi(knowledgeBase);
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @PostMapping("/knowledge-bases/{kbId}/datasets")
    public ApiResponse<KnowledgeDataset> createDataset(
            @PathVariable String kbId,
            @Valid @RequestBody CreateKnowledgeDatasetRequest request
    ) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(kbId);
        KnowledgeDataset dataset = datasetService.createManualDataset(
                knowledgeBase,
                request.name(),
                request.code(),
                request.description(),
                request.topicId(),
                request.topicTitle()
        );
        knowledgeBaseService.updateDatasetCount(kbId, datasetService.listByKnowledgeBase(kbId).size());
        return ApiResponse.success(dataset);
    }

    @PutMapping("/knowledge-datasets/{datasetId}")
    public ApiResponse<KnowledgeDataset> updateDataset(
            @PathVariable String datasetId,
            @Valid @RequestBody UpdateKnowledgeDatasetRequest request
    ) {
        return ApiResponse.success(datasetService.updateDataset(datasetId, request.name(), request.description()));
    }

    @DeleteMapping("/knowledge-datasets/{datasetId}")
    public ApiResponse<Void> deleteDataset(@PathVariable String datasetId) {
        datasetService.deleteDataset(datasetId);
        return ApiResponse.success(null);
    }

    @GetMapping("/knowledge-datasets/{datasetId}")
    public ApiResponse<KnowledgeDataset> getDataset(@PathVariable String datasetId) {
        return ApiResponse.success(datasetService.getRequired(datasetId));
    }

    @GetMapping("/knowledge-datasets/{datasetId}/sources")
    public ApiResponse<PageResponse<KnowledgeSourceIndex>> listSources(@PathVariable String datasetId) {
        List<KnowledgeSourceIndex> items = sourceSyncService.listByDataset(datasetId);
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @PostMapping("/knowledge-datasets/{datasetId}/sources/manual")
    public ApiResponse<KnowledgeSourceIndex> addManualSource(
            @PathVariable String datasetId,
            @Valid @RequestBody AddManualSourceRequest request
    ) {
        return ApiResponse.success(sourceSyncService.addManualTextSource(datasetId, request.title(), request.content()));
    }

    @PostMapping("/knowledge-sources/{sourceIndexId}/reindex")
    public ApiResponse<Void> reindexSource(@PathVariable String sourceIndexId) {
        knowledgeBaseService.indexSourceMaterial(sourceIndexId);
        return ApiResponse.success(null);
    }

    public record PageResponse<T>(List<T> items, int total) {
    }

    public record CreateKnowledgeDatasetRequest(
            @NotBlank String name,
            String code,
            String description,
            String topicId,
            String topicTitle
    ) {
    }

    public record UpdateKnowledgeDatasetRequest(
            @NotBlank String name,
            String description
    ) {
    }

    public record AddManualSourceRequest(
            @NotBlank String title,
            @NotBlank String content
    ) {
    }
}
