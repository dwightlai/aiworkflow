package com.mw.ai.agi.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPersistenceStyleTest {
    @Test
    void authProductionCodeUsesMybatisInsteadOfJdbcTemplate() throws IOException {
        Path authSourceRoot = Path.of("src/main/java/com/mw/ai/agi/auth");

        try (var files = Files.walk(authSourceRoot)) {
            assertThat(files
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .filter(content -> content.contains("JdbcTemplate")))
                    .as("auth module production code must not reference JdbcTemplate")
                    .isEmpty();
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
