package com.aiworkflow.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceArchitectureTest {
    @Test
    void productionPersistenceDoesNotUseJdbcTemplateStores() throws IOException {
        Path sourceRoot = Path.of("src/main/java");

        List<String> jdbcReferences;
        try (var paths = Files.walk(sourceRoot)) {
            jdbcReferences = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("\\integration\\"))
                    .filter(path -> contains(path, "JdbcTemplate")
                            || path.getFileName().toString().startsWith("Jdbc"))
                    .map(sourceRoot::relativize)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertThat(jdbcReferences).isEmpty();
    }

    private boolean contains(Path path, String text) {
        try {
            return Files.readString(path).contains(text);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}
