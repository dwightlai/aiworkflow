package com.aiworkflow.prompt.api;

import com.aiworkflow.common.api.ApiResponse;
import com.aiworkflow.prompt.domain.PromptTemplate;
import com.aiworkflow.prompt.service.PromptTemplateService;
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
@RequestMapping("/api/prompts")
public class PromptTemplateController {
    private final PromptTemplateService promptTemplateService;

    public PromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PromptTemplate>> list() {
        List<PromptTemplate> prompts = promptTemplateService.list();
        return ApiResponse.success(new PageResponse<>(prompts, prompts.size()));
    }

    @PostMapping
    public ApiResponse<PromptTemplate> create(@Valid @RequestBody SavePromptTemplateRequest request) {
        return ApiResponse.success(promptTemplateService.create(
                request.name(),
                request.template(),
                request.description()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromptTemplate> update(
            @PathVariable String id,
            @Valid @RequestBody SavePromptTemplateRequest request
    ) {
        return ApiResponse.success(promptTemplateService.update(
                id,
                request.name(),
                request.template(),
                request.description()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        promptTemplateService.delete(id);
        return ApiResponse.success(null);
    }

    public record SavePromptTemplateRequest(
            @NotBlank String name,
            @NotBlank String template,
            String description
    ) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
