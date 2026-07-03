package com.mw.ai.agi.knowledge.vector;

import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MilvusVectorStoreProviderTest {
    @Test
    void buildsUriAndEscapesFilterValues() {
        VectorStoreConfig config = new VectorStoreConfig(
                "vector_m", "Milvus", "MILVUS", null, "agi_vectors",
                "10.0.0.10", 19530, "default", "agi_vectors", 1024,
                false, "{}", "root", "secret", null,
                5000, 30000, true, Instant.now(), Instant.now()
        );

        assertThat(MilvusVectorStoreProvider.uri(config)).isEqualTo("http://10.0.0.10:19530");
        assertThat(MilvusVectorStoreProvider.quoted("kb\"1")).isEqualTo("\"kb\\\"1\"");
        assertThat(MilvusVectorStoreProvider.validatedCollection("agi_vectors"))
                .isEqualTo("agi_vectors");
        assertThatThrownBy(() -> MilvusVectorStoreProvider.validatedCollection("bad-name"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
