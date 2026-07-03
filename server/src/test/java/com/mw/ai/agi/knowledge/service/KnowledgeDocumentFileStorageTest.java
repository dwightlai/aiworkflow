package com.mw.ai.agi.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.config.AgiStorageProperties;
import com.mw.ai.agi.config.AgiStorageSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeDocumentFileStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesOnlyExistingFilesInsideConfiguredOriginalDirectory() {
        AgiStorageProperties properties = new AgiStorageProperties();
        properties.setKnowledgeDocumentDir(tempDir.toString());
        KnowledgeDocumentFileStorage storage = new KnowledgeDocumentFileStorage(
                AgiStorageSettingsService.withDefaults(properties, new ObjectMapper())
        );
        String stored = storage.save("kb_1", "doc_1", "制度.docx", new byte[] {1, 2, 3})
                .orElseThrow();

        assertThat(storage.resolveExisting(stored)).isPresent();
        assertThat(storage.resolveExisting(tempDir.resolve("..").resolve("outside.txt").toString()))
                .isEmpty();
    }
}
