package com.mw.ai.agi.generation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ResearchOutputFileStorage {
    private final Path outputDir;

    public ResearchOutputFileStorage(@Value("${agi.generation.output-dir:./data/generation-outputs}") String outputDir) {
        this.outputDir = Paths.get(outputDir);
    }

    public String saveDocx(String outputId, byte[] content) {
        try {
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
