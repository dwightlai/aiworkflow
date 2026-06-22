package com.mw.ai.agi.chat.api;

import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.OpenApiRequestContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.chat.service.EmbedTicketService;
import com.mw.ai.agi.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/open/chat")
@Tag(name = "Chat Embed", description = "嵌入 Chat 票据")
public class OpenChatEmbedController {
    private final EmbedTicketService embedTicketService;
    private final IntegrationAppScopeService scopeService;

    public OpenChatEmbedController(EmbedTicketService embedTicketService, IntegrationAppScopeService scopeService) {
        this.embedTicketService = embedTicketService;
        this.scopeService = scopeService;
    }

    @PostMapping("/embed-tickets")
    public ApiResponse<EmbedTicketResponse> issue(@Valid @RequestBody IssueEmbedTicketRequest request, HttpServletRequest servletRequest) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(servletRequest);
        String tenantId = request.tenantId() == null || request.tenantId().isBlank() ? identity.tenantId() : request.tenantId();
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, request.botId());
        if (request.unitId() != null && !request.unitId().isBlank()) {
            scopeService.assertOrganizationAllowed(identity.appId(), tenantId, request.unitId());
        }
        EmbedTicketService.EmbedTicketResult result = embedTicketService.issue(
                identity,
                tenantId,
                request.userId(),
                request.botId(),
                request.expireSeconds() == null ? 300 : request.expireSeconds(),
                mergeBusinessContext(request.businessContext(), request.unitId())
        );
        return ApiResponse.success(new EmbedTicketResponse(result.ticket(), result.expireAt()));
    }

    public record IssueEmbedTicketRequest(
            String tenantId,
            @NotBlank String userId,
            @NotBlank String botId,
            String unitId,
            Integer expireSeconds,
            Map<String, Object> businessContext
    ) {
    }

    private Map<String, Object> mergeBusinessContext(Map<String, Object> businessContext, String unitId) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (businessContext != null) {
            merged.putAll(businessContext);
        }
        if (unitId != null && !unitId.isBlank()) {
            merged.putIfAbsent("unitId", unitId);
        }
        return merged;
    }

    public record EmbedTicketResponse(String ticket, java.time.Instant expireAt) {
    }
}
