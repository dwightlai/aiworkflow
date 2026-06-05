package com.mw.ai.agi.integration.api;

import com.mw.ai.agi.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integration-apps")
public class IntegrationAppController {
    @GetMapping
    public ApiResponse<List<IntegrationAppResponse>> list() {
        return ApiResponse.success(List.of());
    }

    public record IntegrationAppResponse(String id, String name, String status) {
    }
}
