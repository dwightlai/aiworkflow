package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchDocxMasterRendererTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResearchDocxMasterRenderer renderer = new ResearchDocxMasterRenderer();

    @Test
    void rendersTopicCollectionMasterFromSampleJson() throws Exception {
        Map<String, Object> contentJson;
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("research/samples/archive_topic_collection_content.json")) {
            contentJson = objectMapper.readValue(inputStream, Map.class);
        }
        byte[] docxBytes = renderer.renderFromMasterFile(
                "research/docx-masters/archive_topic_collection.docx",
                contentJson
        );
        assertThat(docxBytes).isNotEmpty();
        assertThat(docxBytes[0]).isEqualTo((byte) 'P');
        assertThat(docxBytes[1]).isEqualTo((byte) 'K');

        Path outputPath = Path.of("target/test-outputs/topic_collection_demo.docx");
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, docxBytes);
        assertThat(Files.size(outputPath)).isGreaterThan(10_000);
    }
}
