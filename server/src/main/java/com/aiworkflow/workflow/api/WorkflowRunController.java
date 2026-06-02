package com.aiworkflow.workflow.api;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.workflow.engine.WorkflowExecutionRequest;
import com.aiworkflow.workflow.engine.WorkflowExecutionService;
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

    public WorkflowRunController(WorkflowExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/workflows/{workflowId}/runs")
    public ApiResponse<WorkflowExecutionResponse> run(
            @PathVariable String workflowId,
            @RequestBody(required = false) RunWorkflowRequest request
    ) {
        RunWorkflowRequest safeRequest = request == null ? new RunWorkflowRequest(null) : request;
        return ApiResponse.success(WorkflowExecutionResponse.from(executionService.runWorkflow(
                new WorkflowExecutionRequest(workflowId, safeRequest.input())
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
}
