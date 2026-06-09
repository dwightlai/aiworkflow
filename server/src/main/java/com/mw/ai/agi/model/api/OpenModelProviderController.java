package com.mw.ai.agi.model.api;

import com.mw.ai.agi.auth.service.IntegrationAppScopeService;
import com.mw.ai.agi.auth.service.OpenApiRequestContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.model.domain.ModelProvider;
import com.mw.ai.agi.model.service.ModelProviderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/open/model-providers")
@Tag(name = "大模型", description = "大模型 Provider 列表（不含敏感配置）")
public class OpenModelProviderController {
    private final ModelProviderService modelProviderService;
    private final IntegrationAppScopeService scopeService;

    public OpenModelProviderController(ModelProviderService modelProviderService, IntegrationAppScopeService scopeService) {
        this.modelProviderService = modelProviderService;
        this.scopeService = scopeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OpenModelProviderView>> list(HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        List<OpenModelProviderView> providers = modelProviderService.list().stream()
                .filter(ModelProvider::enabled)
                .filter(provider -> scopeService.isAssetAllowed(
                        identity.appId(),
                        IntegrationAppScopeService.SCOPE_MODEL_PROVIDER,
                        provider.id()
                ))
                .map(OpenModelProviderView::from)
                .toList();
        return ApiResponse.success(new PageResponse<>(providers, providers.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<OpenModelProviderView> get(@PathVariable String id, HttpServletRequest request) {
        RuntimeIdentityContext identity = OpenApiRequestContext.require(request);
        scopeService.assertAssetAllowed(identity.appId(), IntegrationAppScopeService.SCOPE_MODEL_PROVIDER, id);
        ModelProvider provider = modelProviderService.get(id);
        if (!provider.enabled()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Model provider not found: " + id
            );
        }
        return ApiResponse.success(OpenModelProviderView.from(provider));
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record OpenModelProviderView(
            String id,
            String name,
            String modelType,
            String modelUsage,
            String description,
            boolean visionSupport,
            String model,
            boolean enabled
    ) {
        static OpenModelProviderView from(ModelProvider provider) {
            return new OpenModelProviderView(
                    provider.id(),
                    provider.name(),
                    provider.modelType(),
                    provider.modelUsage(),
                    provider.description(),
                    provider.visionSupport(),
                    provider.model(),
                    provider.enabled()
            );
        }
    }
}
