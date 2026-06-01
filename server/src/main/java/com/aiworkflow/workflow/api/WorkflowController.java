package com.aiworkflow.workflow.api;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.workflow.domain.Workflow;
import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.service.WorkflowApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private static final String DEFAULT_TENANT_ID = "tenant-default";
    private static final String DEFAULT_USER_ID = "system";

    private final WorkflowApplicationService workflowService;

    public WorkflowController(WorkflowApplicationService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public ApiResponse<PageResponse<WorkflowResponse>> list() {
        List<WorkflowResponse> workflows = workflowService.listWorkflows().stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(new PageResponse<>(workflows, workflows.size()));
    }

    @PostMapping
    public ApiResponse<WorkflowResponse> create(@Valid @RequestBody CreateWorkflowRequest request) {
        Workflow workflow = workflowService.createWorkflow(
                DEFAULT_TENANT_ID,
                request.name(),
                request.description(),
                DEFAULT_USER_ID,
                request.definition()
        );
        return ApiResponse.success(toResponse(workflow));
    }

    @GetMapping("/{workflowId}")
    public ApiResponse<WorkflowResponse> get(@PathVariable String workflowId) {
        return ApiResponse.success(toResponse(workflowService.getWorkflow(workflowId)));
    }

    @PutMapping("/{workflowId}/draft")
    public ApiResponse<WorkflowResponse> updateDraft(
            @PathVariable String workflowId,
            @Valid @RequestBody UpdateWorkflowDraftRequest request
    ) {
        workflowService.updateDraftDefinition(workflowId, request.definition());
        return ApiResponse.success(toResponse(workflowService.getWorkflow(workflowId)));
    }

    @PostMapping("/{workflowId}/publish")
    public ApiResponse<WorkflowResponse> publish(@PathVariable String workflowId) {
        WorkflowVersion publishedVersion = workflowService.publishDraftVersion(workflowId, DEFAULT_USER_ID);
        Workflow workflow = workflowService.getWorkflow(workflowId);
        return ApiResponse.success(WorkflowResponse.from(workflow, publishedVersion));
    }

    @PostMapping("/{workflowId}/archive")
    public ApiResponse<WorkflowResponse> archive(@PathVariable String workflowId) {
        Workflow workflow = workflowService.archiveWorkflow(workflowId);
        return ApiResponse.success(toResponse(workflow));
    }

    private WorkflowResponse toResponse(Workflow workflow) {
        WorkflowVersion latestVersion = workflowService.listVersions(workflow.id()).stream()
                .max(Comparator.comparingInt(WorkflowVersion::version))
                .orElse(null);
        return WorkflowResponse.from(workflow, latestVersion);
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
