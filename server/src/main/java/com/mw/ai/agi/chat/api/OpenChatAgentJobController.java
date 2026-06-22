package com.mw.ai.agi.chat.api;

import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.OpenApiRequestContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.chat.domain.AgentJob;
import com.mw.ai.agi.chat.service.AgentJobService;
import com.mw.ai.agi.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/open/chat")
@Tag(name = "Chat Agent Job", description = "通用异步任务状态回写")
public class OpenChatAgentJobController {
    private final AgentJobService agentJobService;
    private final IntegrationAppScopeService scopeService;

    public OpenChatAgentJobController(AgentJobService agentJobService, IntegrationAppScopeService scopeService) {
        this.agentJobService = agentJobService;
        this.scopeService = scopeService;
    }

    @PatchMapping("/agent-jobs/{jobId}")
    public ApiResponse<OpenAgentJobView> update(
            @PathVariable String jobId,
            @RequestBody UpdateAgentJobRequest body,
            HttpServletRequest servletRequest
    ) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(servletRequest);
        AgentJob current = agentJobService.getByAnyId(jobId);
        if (current.botId() != null && !current.botId().isBlank()) {
            scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_BOT, current.botId());
        }
        Map<String, Object> patch = new LinkedHashMap<>();
        if (body.status() != null) {
            patch.put("status", body.status());
        }
        if (body.progress() != null) {
            patch.put("progress", body.progress());
        }
        if (body.currentStep() != null) {
            patch.put("currentStep", body.currentStep());
        }
        if (body.errorMessage() != null) {
            patch.put("errorMessage", body.errorMessage());
        }
        if (body.result() != null) {
            patch.put("result", body.result());
        }
        AgentJob updated = agentJobService.applyOpenUpdate(jobId, patch);
        return ApiResponse.success(OpenAgentJobView.from(updated));
    }

    public record UpdateAgentJobRequest(
            String status,
            Integer progress,
            String currentStep,
            String errorMessage,
            Object result
    ) {
    }

    public record OpenAgentJobView(
            @NotBlank String id,
            String status,
            Integer progress,
            String currentStep,
            String errorMessage,
            String result
    ) {
        static OpenAgentJobView from(AgentJob job) {
            return new OpenAgentJobView(
                    job.id(),
                    job.status(),
                    job.progress(),
                    job.currentStep(),
                    job.errorMessage(),
                    job.result()
            );
        }
    }
}
