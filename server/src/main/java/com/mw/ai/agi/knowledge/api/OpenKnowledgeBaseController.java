package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.OpenApiRequestContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import com.mw.ai.agi.knowledge.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/open/knowledge-bases")
@Tag(name = "知识库", description = "知识库列表与检索")
public class OpenKnowledgeBaseController {
    private final KnowledgeBaseService knowledgeBaseService;
    private final IntegrationAppScopeService scopeService;

    public OpenKnowledgeBaseController(KnowledgeBaseService knowledgeBaseService, IntegrationAppScopeService scopeService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.scopeService = scopeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OpenKnowledgeBaseView>> list(HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        List<OpenKnowledgeBaseView> knowledgeBases = knowledgeBaseService.list().stream()
                .filter(knowledgeBase -> isAvailableKnowledgeBase(knowledgeBase.status()))
                .filter(knowledgeBase -> scopeService.isAssetAllowed(
                        identity.appId(),
                        IntegrationAppScopeService.SCOPE_KNOWLEDGE_BASE,
                        knowledgeBase.id()
                ))
                .map(OpenKnowledgeBaseView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(knowledgeBases, knowledgeBases.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<OpenKnowledgeBaseView> get(@PathVariable String id, HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_KNOWLEDGE_BASE, id);
        KnowledgeBase knowledgeBase = knowledgeBaseService.list().stream()
                .filter(item -> id.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Knowledge base not found: " + id
                ));
        if (!isAvailableKnowledgeBase(knowledgeBase.status())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Knowledge base not found: " + id
            );
        }
        return ApiResponse.success(OpenKnowledgeBaseView.from(knowledgeBase));
    }

    @PostMapping("/{id}/search")
    public ApiResponse<List<KnowledgeSearchResult>> search(
            @PathVariable String id,
            @Valid @RequestBody OpenSearchKnowledgeBaseRequest body,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = resolveIdentity(
                request,
                body.userId(),
                body.unitId(),
                body.departmentIds(),
                body.roleIds()
        );
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_KNOWLEDGE_BASE, id);
        return ApiResponse.success(knowledgeBaseService.search(
                id,
                body.query(),
                body.topK(),
                OpenApiRequestContext.auditContext(identity)
        ));
    }

    private RuntimeIdentityContext resolveIdentity(
            HttpServletRequest request,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds
    ) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        if (userId == null && unitId == null && (departmentIds == null || departmentIds.isEmpty()) && (roleIds == null || roleIds.isEmpty())) {
            return identity;
        }
        return new RuntimeIdentityContext(
                identity.tenantId(),
                identity.appId(),
                firstNonBlank(userId, identity.userId()),
                identity.unitIds(),
                firstNonBlank(unitId, identity.activeUnitId()),
                departmentIds == null || departmentIds.isEmpty() ? identity.departmentIds() : departmentIds,
                roleIds == null || roleIds.isEmpty() ? identity.roleIds() : roleIds,
                identity.authType(),
                identity.source()
        );
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private boolean isAvailableKnowledgeBase(String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String normalized = status.trim().toUpperCase();
        return "READY".equals(normalized) || "ACTIVE".equals(normalized);
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record OpenKnowledgeBaseView(
            String id,
            String name,
            String description,
            String retrievalMode,
            int topK
    ) {
        static OpenKnowledgeBaseView from(KnowledgeBase knowledgeBase) {
            return new OpenKnowledgeBaseView(
                    knowledgeBase.id(),
                    knowledgeBase.name(),
                    knowledgeBase.description(),
                    knowledgeBase.retrievalMode(),
                    knowledgeBase.topK()
            );
        }
    }

    public record OpenSearchKnowledgeBaseRequest(
            @NotBlank String query,
            int topK,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds
    ) {
    }
}
