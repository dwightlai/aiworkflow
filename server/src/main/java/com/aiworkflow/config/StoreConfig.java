package com.aiworkflow.config;

import com.aiworkflow.knowledge.service.InMemoryKnowledgeStore;
import com.aiworkflow.knowledge.service.InMemoryVectorStoreConfigStore;
import com.aiworkflow.knowledge.service.JdbcKnowledgeStore;
import com.aiworkflow.knowledge.service.JdbcVectorStoreConfigStore;
import com.aiworkflow.knowledge.service.KnowledgeStore;
import com.aiworkflow.knowledge.service.VectorStoreConfigStore;
import com.aiworkflow.model.service.InMemoryModelProviderStore;
import com.aiworkflow.model.service.JdbcModelProviderStore;
import com.aiworkflow.model.service.ModelProviderStore;
import com.aiworkflow.prompt.service.InMemoryPromptTemplateStore;
import com.aiworkflow.prompt.service.JdbcPromptTemplateStore;
import com.aiworkflow.prompt.service.PromptTemplateStore;
import com.aiworkflow.workflow.engine.InMemoryWorkflowExecutionStore;
import com.aiworkflow.workflow.engine.JdbcWorkflowExecutionStore;
import com.aiworkflow.workflow.engine.WorkflowExecutionStore;
import com.aiworkflow.workflow.service.InMemoryWorkflowStore;
import com.aiworkflow.workflow.service.JdbcWorkflowStore;
import com.aiworkflow.workflow.service.WorkflowStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class StoreConfig {
    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public WorkflowStore jdbcWorkflowStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcWorkflowStore(jdbcTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowStore.class)
    public WorkflowStore inMemoryWorkflowStore() {
        return new InMemoryWorkflowStore();
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public WorkflowExecutionStore jdbcWorkflowExecutionStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcWorkflowExecutionStore(jdbcTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowExecutionStore.class)
    public WorkflowExecutionStore inMemoryWorkflowExecutionStore() {
        return new InMemoryWorkflowExecutionStore();
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public KnowledgeStore jdbcKnowledgeStore(JdbcTemplate jdbcTemplate) {
        return new JdbcKnowledgeStore(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(KnowledgeStore.class)
    public KnowledgeStore inMemoryKnowledgeStore() {
        return new InMemoryKnowledgeStore();
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public VectorStoreConfigStore jdbcVectorStoreConfigStore(JdbcTemplate jdbcTemplate) {
        return new JdbcVectorStoreConfigStore(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(VectorStoreConfigStore.class)
    public VectorStoreConfigStore inMemoryVectorStoreConfigStore() {
        return new InMemoryVectorStoreConfigStore();
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public ModelProviderStore jdbcModelProviderStore(JdbcTemplate jdbcTemplate) {
        return new JdbcModelProviderStore(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ModelProviderStore.class)
    public ModelProviderStore inMemoryModelProviderStore() {
        return new InMemoryModelProviderStore();
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public PromptTemplateStore jdbcPromptTemplateStore(JdbcTemplate jdbcTemplate) {
        return new JdbcPromptTemplateStore(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(PromptTemplateStore.class)
    public PromptTemplateStore inMemoryPromptTemplateStore() {
        return new InMemoryPromptTemplateStore();
    }
}
