package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.OpenApiRequestContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowStatus;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionRequest;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionService;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/open")
public class OpenWorkflowController {
    private final WorkflowApplicationService workflowService;
    private final WorkflowExecutionService executionService;
    private final IntegrationAppScopeService scopeService;

    public OpenWorkflowController(
            WorkflowApplicationService workflowService,
            WorkflowExecutionService executionService,
            IntegrationAppScopeService scopeService
    ) {
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.scopeService = scopeService;
    }

    @GetMapping("/workflows")
    public ApiResponse<PageResponse<OpenWorkflowView>> list(HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        List<OpenWorkflowView> workflows = workflowService.listWorkflows().stream()
                .filter(workflow -> workflow.status() == WorkflowStatus.PUBLISHED)
                .filter(workflow -> scopeService.isAssetAllowed(
                        identity.appId(),
                        IntegrationAppScopeService.SCOPE_WORKFLOW,
                        workflow.id()
                ))
                .map(OpenWorkflowView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(workflows, workflows.size()));
    }

    @GetMapping("/workflows/{id}")
    public ApiResponse<OpenWorkflowView> get(@PathVariable String id, HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_WORKFLOW, id);
        Workflow workflow = requirePublishedWorkflow(id);
        return ApiResponse.success(OpenWorkflowView.from(workflow));
    }

    @PostMapping("/workflows/{id}/runs")
    public ApiResponse<WorkflowExecutionResponse> run(
            @PathVariable String id,
            @RequestBody(required = false) OpenRunWorkflowRequest body,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = resolveIdentity(
                request,
                body == null ? null : body.userId(),
                body == null ? null : body.unitId(),
                body == null ? null : body.departmentIds(),
                body == null ? null : body.roleIds()
        );
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_WORKFLOW, id);
        requirePublishedWorkflow(id);
        Map<String, Object> input = body == null || body.input() == null ? Map.of() : body.input();
        return ApiResponse.success(WorkflowExecutionResponse.from(executionService.runWorkflow(
                new WorkflowExecutionRequest(id, input, runtimeVariables(identity, input))
        )));
    }

    @GetMapping("/workflow-runs/{executionId}")
    public ApiResponse<WorkflowExecutionResponse> getRun(
            @PathVariable String executionId,
            HttpServletRequest request
    ) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        WorkflowExecutionResponse execution = WorkflowExecutionResponse.from(executionService.getWorkflowExecution(executionId));
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_WORKFLOW, execution.workflowId());
        return ApiResponse.success(execution);
    }

    private Workflow requirePublishedWorkflow(String id) {
        Workflow workflow = workflowService.getWorkflow(id);
        if (workflow.status() != WorkflowStatus.PUBLISHED) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Workflow not found: " + id
            );
        }
        return workflow;
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

    private Map<String, Object> runtimeVariables(RuntimeIdentityContext identity, Map<String, Object> input) {
        Map<String, Object> variables = new LinkedHashMap<>(OpenApiRequestContext.auditContext(identity));
        if (identity.tenantId() != null && !identity.tenantId().isBlank()) {
            variables.put("tenantId", identity.tenantId());
        }
        if (identity.activeUnitId() != null && !identity.activeUnitId().isBlank()) {
            variables.put("unitId", identity.activeUnitId());
            variables.put("activeUnitId", identity.activeUnitId());
        }
        if (!identity.unitIds().isEmpty()) {
            variables.put("unitIds", identity.unitIds());
        }
        if (!identity.departmentIds().isEmpty()) {
            variables.put("departmentIds", identity.departmentIds());
        }
        return variables;
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record OpenWorkflowView(
            String id,
            String name,
            String description,
            WorkflowStatus status
    ) {
        static OpenWorkflowView from(Workflow workflow) {
            return new OpenWorkflowView(
                    workflow.id(),
                    workflow.name(),
                    workflow.description(),
                    workflow.status()
            );
        }
    }

    public record OpenRunWorkflowRequest(
            Map<String, Object> input,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds
    ) {
    }
}
