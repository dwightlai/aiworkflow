package com.aiworkflow.model.service;

import com.aiworkflow.model.domain.ModelProvider;
import com.aiworkflow.prompt.domain.PromptTemplate;
import com.aiworkflow.prompt.service.JdbcPromptTemplateStore;
import com.aiworkflow.prompt.service.PromptTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcModelAndPromptPersistenceTest {
    private EmbeddedDatabase database;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("db/migration/V2__ai_studio_schema.sql")
                .addScript("db/migration/V4__model_provider_columns.sql")
                .build();
        jdbcTemplate = new JdbcTemplate(database);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void persistsModelProvidersAcrossServiceInstances() {
        ModelProviderService service = new ModelProviderService(new JdbcModelProviderStore(jdbcTemplate));
        ModelProvider provider = service.create(
                "DeepSeek",
                "DeepSeek",
                "国产大模型",
                false,
                new BigDecimal("2.5"),
                "https://api.deepseek.com/v1",
                "deepseek-chat",
                "deepseek-key",
                true
        );

        ModelProviderService restoredService = new ModelProviderService(new JdbcModelProviderStore(jdbcTemplate));

        assertThat(restoredService.list())
                .extracting(ModelProvider::id)
                .containsExactly(provider.id());
        assertThat(restoredService.get(provider.id()).model()).isEqualTo("deepseek-chat");
        assertThat(restoredService.get(provider.id()).pricePerMillionTokens()).isEqualByComparingTo("2.5");
    }

    @Test
    void persistsPromptTemplatesAcrossServiceInstances() {
        PromptTemplateService service = new PromptTemplateService(new JdbcPromptTemplateStore(jdbcTemplate));
        PromptTemplate template = service.create(
                "意图识别",
                "请识别用户输入的意图：{{input}}",
                "客服分类"
        );

        PromptTemplateService restoredService = new PromptTemplateService(new JdbcPromptTemplateStore(jdbcTemplate));

        assertThat(restoredService.list())
                .extracting(PromptTemplate::id)
                .containsExactly(template.id());
        assertThat(restoredService.list().getFirst().template()).contains("{{input}}");
    }
}
