package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.auth.service.JwtTokenService;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionRequest;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WorkflowRunController {
    private final WorkflowExecutionService executionService;
    private final JwtTokenService jwtTokenService;

    public WorkflowRunController(WorkflowExecutionService executionService, JwtTokenService jwtTokenService) {
        this.executionService = executionService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/workflows/{workflowId}/runs")
    public ApiResponse<WorkflowExecutionResponse> run(
            @PathVariable String workflowId,
            @RequestBody(required = false) RunWorkflowRequest request,
            HttpServletRequest servletRequest
    ) {
        RunWorkflowRequest safeRequest = request == null ? new RunWorkflowRequest(null) : request;
        return ApiResponse.success(WorkflowExecutionResponse.from(executionService.runWorkflow(
                new WorkflowExecutionRequest(workflowId, safeRequest.input(), systemVariables(servletRequest))
        )));
    }

    @GetMapping("/workflow-runs/{executionId}")
    public ApiResponse<WorkflowExecutionResponse> get(@PathVariable String executionId) {
        return ApiResponse.success(WorkflowExecutionResponse.from(executionService.getWorkflowExecution(executionId)));
    }

    @GetMapping("/workflow-runs")
    public ApiResponse<PageResponse<WorkflowExecutionResponse>> list() {
        List<WorkflowExecutionResponse> executions = executionService.listWorkflowExecutions().stream()
                .map(WorkflowExecutionResponse::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(executions, executions.size()));
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    private java.util.Map<String, Object> systemVariables(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return java.util.Map.of();
        }
        try {
            JwtTokenService.JwtClaims claims = jwtTokenService.verify(authorization.substring("Bearer ".length()), "ACCESS");
            return java.util.Map.of("userId", claims.subject());
        } catch (RuntimeException ignored) {
            return java.util.Map.of();
        }
    }
}
