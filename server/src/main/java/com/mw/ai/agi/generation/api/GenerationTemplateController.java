package com.mw.ai.agi.generation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.generation.domain.GenerationTemplate;
import com.mw.ai.agi.generation.service.GenerationTemplateService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/generation-templates")
public class GenerationTemplateController {
    private final GenerationTemplateService generationTemplateService;

    public GenerationTemplateController(GenerationTemplateService generationTemplateService) {
        this.generationTemplateService = generationTemplateService;
    }

    @GetMapping
    public ApiResponse<PageResponse<GenerationTemplate>> list() {
        List<GenerationTemplate> templates = generationTemplateService.list();
        return ApiResponse.success(new PageResponse<>(templates, templates.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<GenerationTemplate> get(@PathVariable String id) {
        return ApiResponse.success(generationTemplateService.get(id));
    }

    @GetMapping("/{id}/runtime")
    public ApiResponse<Map<String, Object>> runtime(@PathVariable String id, ObjectMapper objectMapper) {
        GenerationTemplate template = generationTemplateService.get(id);
        return ApiResponse.success(GenerationTemplateRuntimeMapper.toRuntimeView(template, objectMapper));
    }

    @PostMapping
    public ApiResponse<GenerationTemplate> create(@Valid @RequestBody SaveGenerationTemplateRequest request) {
        return ApiResponse.success(generationTemplateService.create(
                request.name(),
                request.code(),
                request.description(),
                request.category(),
                request.ownerUnitId(),
                request.outputType(),
                request.templateSchema(),
                request.workflowId(),
                request.workflowSnapshot(),
                request.status(),
                request.version(),
                request.createdBy()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<GenerationTemplate> update(
            @PathVariable String id,
            @Valid @RequestBody SaveGenerationTemplateRequest request
    ) {
        return ApiResponse.success(generationTemplateService.update(
                id,
                request.name(),
                request.code(),
                request.description(),
                request.category(),
                request.ownerUnitId(),
                request.outputType(),
                request.templateSchema(),
                request.workflowId(),
                request.workflowSnapshot(),
                request.status(),
                request.version()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        generationTemplateService.delete(id);
        return ApiResponse.success(null);
    }

    public record SaveGenerationTemplateRequest(
            @NotBlank String name,
            @NotBlank String code,
            String description,
            @NotBlank String category,
            String ownerUnitId,
            @NotBlank String outputType,
            String templateSchema,
            String workflowId,
            String workflowSnapshot,
            String status,
            int version,
            String createdBy
    ) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
