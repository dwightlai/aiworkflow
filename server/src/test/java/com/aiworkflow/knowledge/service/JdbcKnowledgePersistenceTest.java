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
                "产品知识库",
                "客服资料",
                "embedding-model-1",
                "vector-store-1",
                "SIMPLE_TEXT",
                500,
                50,
                "KEYWORD",
                3
        );
        KnowledgeDocument document = service.addDocument(
                knowledgeBase.id(),
                "faq.txt",
                "发票可以在订单完成后七日内申请。退货需要保留包装。"
        );

        KnowledgeBaseService restoredService = new KnowledgeBaseService(
                new KnowledgeSplitter(),
                new JdbcKnowledgeStore(jdbcTemplate)
        );

        assertThat(restoredService.list())
                .extracting(KnowledgeBase::name)
                .containsExactly("产品知识库");
        assertThat(restoredService.list().getFirst().documentCount()).isEqualTo(1);
        assertThat(restoredService.listDocuments(knowledgeBase.id()))
                .extracting(KnowledgeDocument::id)
                .containsExactly(document.id());
        assertThat(restoredService.listChunks(knowledgeBase.id(), document.id()))
                .hasSize(1)
                .first()
                .extracting("content")
                .isEqualTo("发票可以在订单完成后七日内申请。退货需要保留包装。");
        assertThat(restoredService.search(knowledgeBase.id(), "发票申请", 3))
                .hasSize(1)
                .first()
                .extracting("documentName")
                .isEqualTo("faq.txt");
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
                true
        );

        VectorStoreConfigService restoredService = new VectorStoreConfigService(
                new JdbcVectorStoreConfigStore(jdbcTemplate)
        );

        assertThat(restoredService.list())
                .extracting(VectorStoreConfig::id)
                .containsExactly(config.id());
        assertThat(restoredService.list().getFirst().endpoint()).isEqualTo("http://localhost:9200");
    }
}
