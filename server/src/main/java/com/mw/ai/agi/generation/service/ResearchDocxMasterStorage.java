package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.config.AgiStorageSettingsService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ResearchDocxMasterStorage {
    private final AgiStorageSettingsService storageSettingsService;

    public ResearchDocxMasterStorage(AgiStorageSettingsService storageSettingsService) {
        this.storageSettingsService = storageSettingsService;
    }

    public String save(String templateId, byte[] content) {
        try {
            Path masterDir = Paths.get(storageSettingsService.getEffective().researchDocxMasterDir());
            Files.createDirectories(masterDir);
            Path file = masterDir.resolve(templateId + ".docx");
            Files.write(file, content);
            return file.toAbsolutePath().toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save DOCX master template.", exception);
        }
    }

    public Path resolve(String masterFile) {
        return Paths.get(masterFile);
    }
}
