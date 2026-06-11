package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
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
                TenantContext.requireTenantId(),
                request.name(),
                request.description(),
                OperatorContext.currentUserId(),
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

    @PutMapping("/{workflowId}/metadata")
    public ApiResponse<WorkflowResponse> updateMetadata(
            @PathVariable String workflowId,
            @Valid @RequestBody UpdateWorkflowMetadataRequest request
    ) {
        Workflow workflow = workflowService.updateWorkflowMetadata(
                workflowId,
                request.name(),
                request.description()
        );
        return ApiResponse.success(toResponse(workflow));
    }

    @PostMapping("/{workflowId}/publish")
    public ApiResponse<WorkflowResponse> publish(@PathVariable String workflowId) {
        WorkflowVersion publishedVersion = workflowService.publishDraftVersion(workflowId, OperatorContext.currentUserId());
        Workflow workflow = workflowService.getWorkflow(workflowId);
        return ApiResponse.success(WorkflowResponse.from(workflow, publishedVersion));
    }

    @PostMapping("/{workflowId}/archive")
    public ApiResponse<WorkflowResponse> archive(@PathVariable String workflowId) {
        Workflow workflow = workflowService.archiveWorkflow(workflowId);
        return ApiResponse.success(toResponse(workflow));
    }

    @PostMapping("/{workflowId}/restore")
    public ApiResponse<WorkflowResponse> restore(@PathVariable String workflowId) {
        Workflow workflow = workflowService.restoreWorkflow(workflowId);
        return ApiResponse.success(toResponse(workflow));
    }

    @DeleteMapping("/{workflowId}")
    public ApiResponse<Void> delete(@PathVariable String workflowId) {
        workflowService.deleteWorkflow(workflowId);
        return ApiResponse.success(null);
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
