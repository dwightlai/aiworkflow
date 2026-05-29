package com.aiworkflow.knowledge.api;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.knowledge.domain.VectorStoreConfig;
import com.aiworkflow.knowledge.service.VectorStoreConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vector-store-configs")
public class VectorStoreConfigController {
    private final VectorStoreConfigService vectorStoreConfigService;

    public VectorStoreConfigController(VectorStoreConfigService vectorStoreConfigService) {
        this.vectorStoreConfigService = vectorStoreConfigService;
    }

    @GetMapping
    public ApiResponse<PageResponse<VectorStoreConfig>> list() {
        List<VectorStoreConfig> configs = vectorStoreConfigService.list();
        return ApiResponse.success(new PageResponse<>(configs, configs.size()));
    }

    @PostMapping
    public ApiResponse<VectorStoreConfig> create(@Valid @RequestBody SaveVectorStoreConfigRequest request) {
        return ApiResponse.success(vectorStoreConfigService.create(
                request.name(),
                request.storeType(),
                request.endpoint(),
                request.indexName(),
                request.enabled()
        ));
    }

    public record SaveVectorStoreConfigRequest(
            @NotBlank String name,
            @NotBlank String storeType,
            String endpoint,
            @NotBlank String indexName,
            boolean enabled
    ) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
