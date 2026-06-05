package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/openapi/v1")
public class OpenWorkflowRunController {
    @PostMapping("/workflows/{workflowId}/runs")
    public ApiResponse<Map<String, String>> createRun(@PathVariable String workflowId) {
        return ApiResponse.success(Map.of("workflowId", workflowId, "runId", "run_" + UUID.randomUUID()));
    }

    @GetMapping("/workflow-runs/{runId}")
    public ApiResponse<Map<String, String>> getRun(@PathVariable String runId) {
        return ApiResponse.success(Map.of("runId", runId, "status", "PENDING"));
    }
}
