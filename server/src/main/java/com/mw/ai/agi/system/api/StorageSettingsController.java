package com.mw.ai.agi.system.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.config.AgiStorageSettings;
import com.mw.ai.agi.config.AgiStorageSettingsService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/storage-settings")
public class StorageSettingsController {
    private final AgiStorageSettingsService storageSettingsService;

    public StorageSettingsController(AgiStorageSettingsService storageSettingsService) {
        this.storageSettingsService = storageSettingsService;
    }

    @GetMapping
    public ApiResponse<StorageSettingsView> get() {
        return ApiResponse.success(toView(storageSettingsService.getForAdmin()));
    }

    @PutMapping
    public ApiResponse<StorageSettingsView> save(@RequestBody SaveStorageSettingsRequest request) {
        return ApiResponse.success(toView(storageSettingsService.save(
                request.knowledgeDocumentDir(),
                request.knowledgeDocumentSaveOriginal(),
                request.researchOutputDir(),
                request.researchDocxMasterDir()
        )));
    }

    @PostMapping("/reset")
    public ApiResponse<StorageSettingsView> reset() {
        return ApiResponse.success(toView(storageSettingsService.resetToDefaults()));
    }

    private StorageSettingsView toView(AgiStorageSettings settings) {
        return new StorageSettingsView(
                settings.knowledgeDocumentDir(),
                settings.knowledgeDocumentSaveOriginal(),
                settings.researchOutputDir(),
                settings.researchDocxMasterDir(),
                settings.customized()
        );
    }

    public record SaveStorageSettingsRequest(
            @NotBlank String knowledgeDocumentDir,
            boolean knowledgeDocumentSaveOriginal,
            @NotBlank String researchOutputDir,
            @NotBlank String researchDocxMasterDir
    ) {
    }

    public record StorageSettingsView(
            String knowledgeDocumentDir,
            boolean knowledgeDocumentSaveOriginal,
            String researchOutputDir,
            String researchDocxMasterDir,
            boolean customized
    ) {
    }
}
