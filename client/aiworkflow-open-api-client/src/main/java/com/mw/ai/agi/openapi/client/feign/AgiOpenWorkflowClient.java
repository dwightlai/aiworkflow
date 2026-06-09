package com.mw.ai.agi.openapi.client.feign;

import com.mw.ai.agi.openapi.client.model.ApiResponse;
import com.mw.ai.agi.openapi.client.model.PageResponse;
import com.mw.ai.agi.openapi.client.model.workflow.OpenRunWorkflowRequest;
import com.mw.ai.agi.openapi.client.model.workflow.OpenWorkflowView;
import com.mw.ai.agi.openapi.client.model.workflow.WorkflowExecutionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        contextId = "agiOpenWorkflowClient",
        name = "${agi.openapi.service-name:aiworkflow-server}",
        url = "${agi.openapi.base-url:}"
)
public interface AgiOpenWorkflowClient {

    @GetMapping("/api/open/workflows")
    ApiResponse<PageResponse<OpenWorkflowView>> listWorkflows();

    @GetMapping("/api/open/workflows/{id}")
    ApiResponse<OpenWorkflowView> getWorkflow(@PathVariable("id") String id);

    @PostMapping("/api/open/workflows/{id}/runs")
    ApiResponse<WorkflowExecutionResponse> runWorkflow(
            @PathVariable("id") String id,
            @RequestBody(required = false) OpenRunWorkflowRequest request
    );

    @GetMapping("/api/open/workflow-runs/{executionId}")
    ApiResponse<WorkflowExecutionResponse> getWorkflowRun(@PathVariable("executionId") String executionId);
}
