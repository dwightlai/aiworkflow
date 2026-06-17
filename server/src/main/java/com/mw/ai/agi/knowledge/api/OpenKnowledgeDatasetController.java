package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.OpenApiRequestContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.domain.KnowledgeDataset;
import com.mw.ai.agi.knowledge.domain.KnowledgeSourceIndex;
import com.mw.ai.agi.knowledge.service.KnowledgeDatasetService;
import com.mw.ai.agi.knowledge.service.KnowledgeSourceSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/open/knowledge-bases")
@Tag(name = "知识库数据集", description = "专题数据集同步与查询（业务系统开放接口）")
public class OpenKnowledgeDatasetController {
    private final KnowledgeDatasetService datasetService;
    private final KnowledgeSourceSyncService sourceSyncService;
    private final IntegrationAppScopeService scopeService;

    public OpenKnowledgeDatasetController(
            KnowledgeDatasetService datasetService,
            KnowledgeSourceSyncService sourceSyncService,
            IntegrationAppScopeService scopeService
    ) {
        this.datasetService = datasetService;
        this.sourceSyncService = sourceSyncService;
        this.scopeService = scopeService;
    }

    @GetMapping("/{kbId}/datasets")
    public ApiResponse<PageResponse<OpenDatasetView>> listDatasets(
            @PathVariable String kbId,
            HttpServletRequest request
    ) {
        assertKnowledgeBaseAllowed(request, kbId);
        List<OpenDatasetView> items = datasetService.listManageableByKnowledgeBase(kbId).stream()
                .map(OpenDatasetView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @GetMapping("/{kbId}/datasets/{datasetId}/sources")
    public ApiResponse<PageResponse<OpenSourceView>> listSources(
            @PathVariable String kbId,
            @PathVariable String datasetId,
            HttpServletRequest request
    ) {
        assertKnowledgeBaseAllowed(request, kbId);
        KnowledgeDataset dataset = datasetService.getRequired(datasetId);
        if (!kbId.equals(dataset.knowledgeBaseId())) {
            throw new IllegalArgumentException("数据集不属于该知识库");
        }
        List<OpenSourceView> items = sourceSyncService.listByDataset(datasetId).stream()
                .map(OpenSourceView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @PostMapping("/{kbId}/archive-topics/{topicId}/sync")
    public ApiResponse<KnowledgeSourceSyncService.SyncResult> syncArchiveTopic(
            @PathVariable String kbId,
            @PathVariable String topicId,
            @Valid @RequestBody OpenSyncArchiveTopicRequest body,
            HttpServletRequest request
    ) {
        assertKnowledgeBaseAllowed(request, kbId);
        List<KnowledgeSourceSyncService.SourceMaterialPayload> materials = body.materials().stream()
                .map(item -> new KnowledgeSourceSyncService.SourceMaterialPayload(
                        item.sourceRefId(),
                        item.sourceVersion(),
                        item.title(),
                        item.materialSourceType(),
                        item.materialType(),
                        item.sourceArchiveFileId(),
                        item.contentText(),
                        item.metadata()
                ))
                .toList();
        KnowledgeSourceSyncService.ArchiveTopicSyncCommand command = new KnowledgeSourceSyncService.ArchiveTopicSyncCommand(
                body.topicTitle(),
                body.reindexChanged(),
                materials
        );
        return ApiResponse.success(sourceSyncService.syncArchiveTopic(kbId, topicId, command));
    }

    private void assertKnowledgeBaseAllowed(HttpServletRequest request, String kbId) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_KNOWLEDGE_BASE, kbId);
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record OpenDatasetView(
            String id,
            String name,
            String topicId,
            String topicTitle,
            int sourceCount,
            int documentCount,
            int chunkCount,
            String indexStatus
    ) {
        static OpenDatasetView from(KnowledgeDataset dataset) {
            return new OpenDatasetView(
                    dataset.id(),
                    dataset.name(),
                    dataset.topicId(),
                    dataset.topicTitle(),
                    dataset.sourceCount(),
                    dataset.documentCount(),
                    dataset.chunkCount(),
                    dataset.indexStatus()
            );
        }
    }

    public record OpenSourceView(
            String id,
            String sourceRefId,
            String title,
            String materialSourceType,
            String materialType,
            String sourceArchiveFileId,
            String indexStatus
    ) {
        static OpenSourceView from(KnowledgeSourceIndex source) {
            return new OpenSourceView(
                    source.id(),
                    source.sourceRefId(),
                    source.sourceTitleSnapshot(),
                    source.materialSourceType(),
                    source.materialType(),
                    source.sourceArchiveFileId(),
                    source.indexStatus()
            );
        }
    }

    public record OpenSyncArchiveTopicRequest(
            String topicTitle,
            Boolean reindexChanged,
            @NotEmpty List<OpenSourceMaterialRequest> materials
    ) {
    }

    public record OpenSourceMaterialRequest(
            @NotBlank String sourceRefId,
            String sourceVersion,
            String title,
            String materialSourceType,
            String materialType,
            String sourceArchiveFileId,
            String contentText,
            Map<String, Object> metadata
    ) {
    }
}
