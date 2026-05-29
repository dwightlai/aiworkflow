package com.aiworkflow.model.api;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.model.domain.ModelProvider;
import com.aiworkflow.model.service.ModelProviderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/model-providers")
public class ModelProviderController {
    private final ModelProviderService modelProviderService;

    public ModelProviderController(ModelProviderService modelProviderService) {
        this.modelProviderService = modelProviderService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ModelProvider>> list() {
        List<ModelProvider> providers = modelProviderService.list();
        return ApiResponse.success(new PageResponse<>(providers, providers.size()));
    }

    @PostMapping
    public ApiResponse<ModelProvider> create(@Valid @RequestBody SaveModelProviderRequest request) {
        return ApiResponse.success(modelProviderService.create(
                request.name(),
                request.modelType(),
                request.description(),
                request.visionSupport(),
                request.pricePerMillionTokens(),
                request.baseUrl(),
                request.model(),
                request.apiKeyRef(),
                request.enabled()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelProvider> update(
            @PathVariable String id,
            @Valid @RequestBody SaveModelProviderRequest request
    ) {
        return ApiResponse.success(modelProviderService.update(
                id,
                request.name(),
                request.modelType(),
                request.description(),
                request.visionSupport(),
                request.pricePerMillionTokens(),
                request.baseUrl(),
                request.model(),
                request.apiKeyRef(),
                request.enabled()
        ));
    }

    public record SaveModelProviderRequest(
            @NotBlank String name,
            @NotBlank String modelType,
            String description,
            boolean visionSupport,
            BigDecimal pricePerMillionTokens,
            @NotBlank String baseUrl,
            @NotBlank String model,
            @NotBlank String apiKeyRef,
            boolean enabled
    ) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
