package com.mw.ai.agi.knowledge.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.mw.ai.agi.knowledge.service.VectorStoreConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ApiResponse<PageResponse<VectorStoreConfigResponse>> list() {
        List<VectorStoreConfigResponse> configs = vectorStoreConfigService.list().stream()
                .map(VectorStoreConfigResponse::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(configs, configs.size()));
    }

    @PostMapping
    public ApiResponse<VectorStoreConfigResponse> create(@Valid @RequestBody SaveVectorStoreConfigRequest request) {
        return ApiResponse.success(VectorStoreConfigResponse.from(vectorStoreConfigService.create(
                request.name(),
                request.storeType(),
                request.endpoint(),
                request.indexName(),
                request.username(),
                request.password(),
                request.apiKey(),
                request.connectTimeoutMs(),
                request.readTimeoutMs(),
                request.enabled()
        )));
    }

    @PutMapping("/{id}")
    public ApiResponse<VectorStoreConfigResponse> update(
            @PathVariable String id,
            @Valid @RequestBody SaveVectorStoreConfigRequest request
    ) {
        return ApiResponse.success(VectorStoreConfigResponse.from(vectorStoreConfigService.update(
                id,
                request.name(),
                request.storeType(),
                request.endpoint(),
                request.indexName(),
                request.username(),
                request.password(),
                request.apiKey(),
                request.connectTimeoutMs(),
                request.readTimeoutMs(),
                request.enabled()
        )));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        vectorStoreConfigService.delete(id);
        return ApiResponse.success(null);
    }

    public record SaveVectorStoreConfigRequest(
            @NotBlank String name,
            @NotBlank String storeType,
            String endpoint,
            @NotBlank String indexName,
            String username,
            String password,
            String apiKey,
            Integer connectTimeoutMs,
            Integer readTimeoutMs,
            boolean enabled
    ) {
    }

    public record VectorStoreConfigResponse(
            String id,
            String name,
            String storeType,
            String endpoint,
            String indexName,
            String username,
            boolean passwordConfigured,
            boolean apiKeyConfigured,
            int connectTimeoutMs,
            int readTimeoutMs,
            boolean enabled,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {
        static VectorStoreConfigResponse from(VectorStoreConfig config) {
            return new VectorStoreConfigResponse(
                    config.id(),
                    config.name(),
                    config.storeType(),
                    config.endpoint(),
                    config.indexName(),
                    config.username(),
                    config.password() != null && !config.password().isBlank(),
                    config.apiKey() != null && !config.apiKey().isBlank(),
                    config.connectTimeoutMs(),
                    config.readTimeoutMs(),
                    config.enabled(),
                    config.createdAt(),
                    config.updatedAt()
            );
        }
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
