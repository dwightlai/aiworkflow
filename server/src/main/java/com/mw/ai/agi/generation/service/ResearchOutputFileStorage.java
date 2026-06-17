package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.config.AgiStorageSettingsService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ResearchOutputFileStorage {
    private final AgiStorageSettingsService storageSettingsService;

    public ResearchOutputFileStorage(AgiStorageSettingsService storageSettingsService) {
        this.storageSettingsService = storageSettingsService;
    }

    public String saveDocx(String outputId, byte[] content) {
        try {
            Path outputDir = Paths.get(storageSettingsService.getEffective().researchOutputDir());
            Files.createDirectories(outputDir);
            Path file = outputDir.resolve(outputId + ".docx");
            Files.write(file, content);
            return file.toAbsolutePath().toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save DOCX output.", exception);
        }
    }

    public Path resolveDocxPath(String contentDocxPath) {
        return Paths.get(contentDocxPath);
    }
}
