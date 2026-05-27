package com.aiworkflow.workflow.api;

import com.aiworkflow.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    @GetMapping
    public ApiResponse<PageResponse<WorkflowSummaryResponse>> list() {
        return ApiResponse.success(new PageResponse<>(List.of(), 0));
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record WorkflowSummaryResponse(String id, String name, String status) {
    }
}
