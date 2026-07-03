package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VectorStoreConfigExtendedFieldsTest {
    @Test
    void carriesIndependentExternalStoreConnectionSettings() {
        Instant now = Instant.now();
        VectorStoreConfig config = new VectorStoreConfig(
                "vector_1", "Milvus prod", "MILVUS", null, "knowledge_chunks",
                "10.0.0.8", 19530, "default", "knowledge_chunks", 1024,
                true, "{\"consistencyLevel\":\"Bounded\"}",
                "root", "secret", null, 5000, 30000, true, now, now
        );

        assertThat(config.host()).isEqualTo("10.0.0.8");
        assertThat(config.port()).isEqualTo(19530);
        assertThat(config.databaseName()).isEqualTo("default");
        assertThat(config.namespaceName()).isEqualTo("knowledge_chunks");
        assertThat(config.vectorDimension()).isEqualTo(1024);
        assertThat(config.sslEnabled()).isTrue();
    }
}
