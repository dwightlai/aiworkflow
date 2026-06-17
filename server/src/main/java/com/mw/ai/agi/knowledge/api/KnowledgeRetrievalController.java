package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.domain.KnowledgeRetrievalItem;
import com.mw.ai.agi.knowledge.service.KnowledgeBaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeRetrievalController {
    private final KnowledgeBaseService knowledgeBaseService;
    private final RequestIdentitySupport identitySupport;

    public KnowledgeRetrievalController(KnowledgeBaseService knowledgeBaseService, RequestIdentitySupport identitySupport) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.identitySupport = identitySupport;
    }

    @PostMapping("/retrieval")
    public ApiResponse<RetrievalResponse> retrieve(@Valid @RequestBody RetrievalRequest request, HttpServletRequest servletRequest) {
        List<KnowledgeRetrievalItem> items = knowledgeBaseService.retrieve(
                request.knowledgeBaseId(),
                request.datasetId(),
                request.query(),
                request.topK() == null ? 0 : request.topK(),
                request.retrievalMode(),
                request.filters(),
                identitySupport.grantContext(servletRequest)
        );
        return ApiResponse.success(new RetrievalResponse(items));
    }

    public record RetrievalRequest(
            @NotBlank String knowledgeBaseId,
            String datasetId,
            @NotBlank String query,
            String retrievalMode,
            Integer topK,
            Map<String, Object> filters
    ) {
    }

    public record RetrievalResponse(List<KnowledgeRetrievalItem> items) {
    }
}
