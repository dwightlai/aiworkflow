package com.mw.ai.agi.knowledge.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Component
public class DocumentTextExtractor {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "html", "htm", "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"
    );

    private final Tika tika = new Tika();

    public String extract(String fileName, String contentType, InputStream inputStream) {
        String extension = extensionOf(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported knowledge document type: " + extension);
        }
        try {
            String text = switch (extension) {
                case "txt", "md", "markdown" -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                default -> tika.parseToString(inputStream);
            };
            String normalized = normalize(text);
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("Uploaded document has no extractable text: " + fileName);
            }
            return normalized;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read uploaded document: " + fileName, exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to extract uploaded document text: " + fileName, exception);
        }
    }

    public boolean supports(String fileName) {
        return SUPPORTED_EXTENSIONS.contains(extensionOf(fileName));
    }

    private String normalize(String text) {
        return text == null ? "" : text
                .replace("\uFEFF", "")
                .replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String extensionOf(String fileName) {
        String normalized = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dotIndex + 1);
    }
}
