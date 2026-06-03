package com.aiworkflow.config;

import com.aiworkflow.bot.persistence.AiBotEntity;
import com.aiworkflow.bot.persistence.BotMessageEntity;
import com.aiworkflow.bot.persistence.BotSessionEntity;
import com.aiworkflow.knowledge.persistence.KnowledgeBaseEntity;
import com.aiworkflow.knowledge.persistence.KnowledgeChunkEntity;
import com.aiworkflow.knowledge.persistence.KnowledgeChunkVectorEntity;
import com.aiworkflow.knowledge.persistence.KnowledgeDocumentEntity;
import com.aiworkflow.knowledge.persistence.VectorStoreConfigEntity;
import com.aiworkflow.model.persistence.ModelProviderEntity;
import com.aiworkflow.prompt.persistence.PromptTemplateEntity;
import com.aiworkflow.workflow.persistence.WorkflowEntity;
import com.aiworkflow.workflow.persistence.WorkflowExecutionEntity;
import com.aiworkflow.workflow.persistence.WorkflowNodeExecutionEntity;
import com.aiworkflow.workflow.persistence.WorkflowVersionEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DamengSchemaMigrationContractTest {
    private static final List<Class<?>> PERSISTENCE_ENTITIES = List.of(
            AiBotEntity.class,
            BotMessageEntity.class,
            BotSessionEntity.class,
            KnowledgeBaseEntity.class,
            KnowledgeChunkEntity.class,
            KnowledgeChunkVectorEntity.class,
            KnowledgeDocumentEntity.class,
            VectorStoreConfigEntity.class,
            ModelProviderEntity.class,
            PromptTemplateEntity.class,
            WorkflowEntity.class,
            WorkflowExecutionEntity.class,
            WorkflowNodeExecutionEntity.class,
            WorkflowVersionEntity.class
    );

    private static final List<String> POSTGRES_ONLY_PATTERNS = List.of(
            "IF NOT EXISTS",
            "TIMESTAMP WITH TIME ZONE",
            " BOOLEAN",
            "ADD COLUMN IF NOT EXISTS",
            "ALTER COLUMN",
            " RENAME TO "
    );

    @Test
    void damengMigrationDefinesEveryMybatisTable() throws Exception {
        String sql = damengSql().toLowerCase(Locale.ROOT);

        List<String> tableNames = PERSISTENCE_ENTITIES.stream()
                .map(entity -> entity.getAnnotation(TableName.class).value())
                .toList();

        assertThat(tableNames).allSatisfy(tableName ->
                assertThat(sql).contains("create table " + tableName.toLowerCase(Locale.ROOT)));
    }

    @Test
    void damengMigrationUsesNumberOneForBooleanColumns() throws Exception {
        String sql = damengSql().toLowerCase(Locale.ROOT);

        assertThat(sql).doesNotContain(" boolean");
        assertThat(sql).containsPattern(Pattern.compile("\\benabled\\s+number\\(1\\)\\s+not\\s+null"));
        assertThat(sql).containsPattern(Pattern.compile("\\bvision_support\\s+number\\(1\\)\\s+not\\s+null"));
    }

    @Test
    void damengMigrationAvoidsPostgresqlOnlySyntax() throws Exception {
        String sql = damengSql().toUpperCase(Locale.ROOT);

        assertThat(POSTGRES_ONLY_PATTERNS)
                .allSatisfy(pattern -> assertThat(sql).doesNotContain(pattern));
    }

    private String damengSql() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath:db/migration/dameng/*.sql");
        assertThat(resources).isNotEmpty();

        StringBuilder builder = new StringBuilder();
        for (var resource : resources) {
            builder.append(resource.getContentAsString(StandardCharsets.UTF_8)).append('\n');
        }
        return builder.toString();
    }
}
