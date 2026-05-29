package com.aiworkflow.model.api;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.model.domain.ModelProvider;
import com.aiworkflow.model.service.ModelProviderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ApiResponse<ModelProvider> create(@Valid @RequestBody CreateModelProviderRequest request) {
        return ApiResponse.success(modelProviderService.create(
                request.name(),
                request.baseUrl(),
                request.model(),
                request.apiKeyRef(),
                request.enabled()
        ));
    }

    public record CreateModelProviderRequest(
            @NotBlank String name,
            @NotBlank String baseUrl,
            @NotBlank String model,
            @NotBlank String apiKeyRef,
            boolean enabled
    ) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
