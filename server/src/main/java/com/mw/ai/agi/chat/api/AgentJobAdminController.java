package com.mw.ai.agi.chat.api;

import com.mw.ai.agi.chat.domain.AgentJob;
import com.mw.ai.agi.chat.service.AgentJobService;
import com.mw.ai.agi.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/agent-jobs")
public class AgentJobAdminController {
    private final AgentJobService agentJobService;

    public AgentJobAdminController(AgentJobService agentJobService) {
        this.agentJobService = agentJobService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AgentJobView>> list(
            @RequestParam(required = false) String botId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String conversationId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<AgentJobView> items = agentJobService.list(botId, status, conversationId, limit).stream()
                .map(AgentJobView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<AgentJobView> get(@PathVariable String jobId) {
        return ApiResponse.success(AgentJobView.from(agentJobService.getByAnyId(jobId)));
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record AgentJobView(
            String id,
            String botId,
            String conversationId,
            String sourceJobId,
            String jobType,
            String status,
            Integer progress,
            String currentStep,
            String result,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
        static AgentJobView from(AgentJob job) {
            return new AgentJobView(
                    job.id(),
                    job.botId(),
                    job.conversationId(),
                    job.sourceJobId(),
                    job.jobType(),
                    job.status(),
                    job.progress(),
                    job.currentStep(),
                    job.result(),
                    job.errorMessage(),
                    job.createdAt(),
                    job.updatedAt()
            );
        }
    }
}
