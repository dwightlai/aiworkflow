package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionRequest;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RequestIdentitySupport identitySupport;

    public WorkflowRunController(WorkflowExecutionService executionService, RequestIdentitySupport identitySupport) {
        this.executionService = executionService;
        this.identitySupport = identitySupport;
    }

    @PostMapping("/workflows/{workflowId}/runs")
    public ApiResponse<WorkflowExecutionResponse> run(
            @PathVariable String workflowId,
            @RequestBody(required = false) RunWorkflowRequest request,
            HttpServletRequest servletRequest
    ) {
        RunWorkflowRequest safeRequest = request == null ? new RunWorkflowRequest(null) : request;
        return ApiResponse.success(WorkflowExecutionResponse.from(executionService.runWorkflow(
                new WorkflowExecutionRequest(workflowId, safeRequest.input(), runtimeVariables(servletRequest))
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

    private java.util.Map<String, Object> runtimeVariables(HttpServletRequest request) {
        java.util.Map<String, Object> variables = new java.util.LinkedHashMap<>(identitySupport.grantContext(request));
        variables.putAll(identitySupport.resolve(request)
                .map(identity -> java.util.Map.<String, Object>of("userId", identity.userId()))
                .orElse(java.util.Map.of()));
        return variables;
    }
}
