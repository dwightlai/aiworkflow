package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.config.AgiStorageSettingsService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class KnowledgeDocumentFileStorage {
    private final AgiStorageSettingsService storageSettingsService;

    public KnowledgeDocumentFileStorage(AgiStorageSettingsService storageSettingsService) {
        this.storageSettingsService = storageSettingsService;
    }

    public Optional<String> save(String knowledgeBaseId, String documentId, String fileName, byte[] content) {
        if (!storageSettingsService.getEffective().knowledgeDocumentSaveOriginal()) {
            return Optional.empty();
        }
        try {
            Path directory = Paths.get(storageSettingsService.getEffective().knowledgeDocumentDir(), knowledgeBaseId);
            Files.createDirectories(directory);
            Path file = directory.resolve(documentId + "_" + sanitizeFileName(fileName));
            Files.write(file, content);
            return Optional.of(file.toAbsolutePath().toString());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save knowledge document file.", exception);
        }
    }

    public Path resolve(String storagePath) {
        return Paths.get(storagePath);
    }

    public Optional<Path> resolveExisting(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return Optional.empty();
        }
        Path root = Paths.get(storageSettingsService.getEffective().knowledgeDocumentDir())
                .toAbsolutePath()
                .normalize();
        Path candidate = Paths.get(storagePath).toAbsolutePath().normalize();
        if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    private String sanitizeFileName(String fileName) {
        String normalized = fileName == null || fileName.isBlank() ? "uploaded-document.docx" : fileName.trim();
        return normalized.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
