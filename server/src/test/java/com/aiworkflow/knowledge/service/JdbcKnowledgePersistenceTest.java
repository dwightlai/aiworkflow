package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import com.aiworkflow.knowledge.domain.VectorStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcKnowledgePersistenceTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("db/migration/V3__knowledge_schema.sql")
                .addScript("db/migration/V7__knowledge_base_vector_dimension.sql")
                .addScript("db/migration/V6__knowledge_chunk_vectors.sql")
                .addScript("db/migration/V10__vector_store_es_connection.sql")
                .addScript("db/migration/V12__agi_table_prefix.sql")
                .build();
        jdbcTemplate = new JdbcTemplate(database);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void persistsKnowledgeBasesDocumentsAndChunksAcrossServiceInstances() {
        KnowledgeBaseService service = new KnowledgeBaseService(
                new KnowledgeSplitter(),
                new JdbcKnowledgeStore(jdbcTemplate)
        );
        KnowledgeBase knowledgeBase = service.create(
                "Product knowledge base",
                "Customer support material",
                "embedding-model-1",
                "vector-store-1",
                "SIMPLE_TEXT",
                500,
                50,
                "HYBRID",
                3
        );
        KnowledgeDocument document = service.addDocument(
                knowledgeBase.id(),
                "faq.txt",
                "Invoices can be requested within seven days after an order is completed. Returns require original packaging."
        );

        KnowledgeBaseService restoredService = new KnowledgeBaseService(
                new KnowledgeSplitter(),
                new JdbcKnowledgeStore(jdbcTemplate)
        );

        assertThat(restoredService.list())
                .extracting(KnowledgeBase::name)
                .containsExactly("Product knowledge base");
        assertThat(restoredService.list().get(0).documentCount()).isEqualTo(1);
        assertThat(restoredService.listDocuments(knowledgeBase.id()))
                .extracting(KnowledgeDocument::id)
                .containsExactly(document.id());
        assertThat(restoredService.listChunks(knowledgeBase.id(), document.id()))
                .hasSize(1)
                .first()
                .extracting("content")
                .isEqualTo("Invoices can be requested within seven days after an order is completed. Returns require original packaging.");
        assertThat(restoredService.search(knowledgeBase.id(), "invoice request", 3))
                .hasSize(1)
                .first()
                .extracting("documentName")
                .isEqualTo("faq.txt");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agi_knowledge_chunk_vector", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void persistsVectorStoreConfigsAcrossServiceInstances() {
        VectorStoreConfigService service = new VectorStoreConfigService(
                new JdbcVectorStoreConfigStore(jdbcTemplate)
        );
        VectorStoreConfig config = service.create(
                "Elastic dev",
                "ELASTICSEARCH",
                "http://localhost:9200",
                "kb_dev",
                "elastic",
                "secret",
                null,
                7000,
                45000,
                true
        );

        VectorStoreConfigService restoredService = new VectorStoreConfigService(
                new JdbcVectorStoreConfigStore(jdbcTemplate)
        );

        assertThat(restoredService.list())
                .extracting(VectorStoreConfig::id)
                .containsExactly(config.id());
        assertThat(restoredService.list().get(0).endpoint()).isEqualTo("http://localhost:9200");
        assertThat(restoredService.list().get(0).username()).isEqualTo("elastic");
        assertThat(restoredService.list().get(0).password()).isEqualTo("secret");
        assertThat(restoredService.list().get(0).connectTimeoutMs()).isEqualTo(7000);
    }
}
