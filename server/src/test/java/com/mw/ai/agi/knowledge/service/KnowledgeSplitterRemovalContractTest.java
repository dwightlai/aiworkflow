package com.mw.ai.agi.knowledge.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeSplitterRemovalContractTest {

    @Test
    void productionSourcesNoLongerContainLegacyKnowledgeSplitter() throws Exception {
        Path sourceRoot = Path.of("src", "main", "java");
        try (var paths = Files.walk(sourceRoot)) {
            var references = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("KnowledgeSplitter");
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();
            assertThat(references).isEmpty();
        }
    }
}
