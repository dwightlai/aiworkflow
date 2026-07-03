package com.mw.ai.agi.knowledge.vector;

import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PgvectorVectorStoreProviderTest {
    @Test
    void buildsIndependentJdbcUrlAndRejectsUnsafeTableNames() {
        VectorStoreConfig config = new VectorStoreConfig(
                "vector_pg", "Pgvector", "PGVECTOR", null, "agi_vectors",
                "10.0.0.9", 5433, "vectors", "agi_vectors", 768,
                true, "{}", "vector_user", "secret", null,
                5000, 30000, true, Instant.now(), Instant.now()
        );

        assertThat(PgvectorVectorStoreProvider.jdbcUrl(config))
                .isEqualTo("jdbc:postgresql://10.0.0.9:5433/vectors?sslmode=require");
        assertThat(PgvectorVectorStoreProvider.validatedIdentifier("agi_vectors"))
                .isEqualTo("agi_vectors");
        assertThatThrownBy(() -> PgvectorVectorStoreProvider.validatedIdentifier("x;drop table users"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
