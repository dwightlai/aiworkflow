package com.aiworkflow.config;

import com.aiworkflow.bot.service.BotStore;
import com.aiworkflow.bot.service.InMemoryBotStore;
import com.aiworkflow.bot.service.JdbcBotStore;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class StoreConfig {
    @Bean
    public WorkflowStore workflowStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider, ObjectMapper objectMapper) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        return jdbcTemplate == null ? new InMemoryWorkflowStore() : new JdbcWorkflowStore(jdbcTemplate, objectMapper);
    }

    @Bean
    public WorkflowExecutionStore workflowExecutionStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider, ObjectMapper objectMapper) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        return jdbcTemplate == null
                ? new InMemoryWorkflowExecutionStore()
                : new JdbcWorkflowExecutionStore(jdbcTemplate, objectMapper);
    }

    @Bean
    public KnowledgeStore knowledgeStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        return jdbcTemplate == null ? new InMemoryKnowledgeStore() : new JdbcKnowledgeStore(jdbcTemplate);
    }

    @Bean
    public VectorStoreConfigStore vectorStoreConfigStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        return jdbcTemplate == null ? new InMemoryVectorStoreConfigStore() : new JdbcVectorStoreConfigStore(jdbcTemplate);
    }

    @Bean
    public ModelProviderStore modelProviderStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        return jdbcTemplate == null ? new InMemoryModelProviderStore() : new JdbcModelProviderStore(jdbcTemplate);
    }

    @Bean
    public PromptTemplateStore promptTemplateStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        return jdbcTemplate == null ? new InMemoryPromptTemplateStore() : new JdbcPromptTemplateStore(jdbcTemplate);
    }

    @Bean
    public BotStore botStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        return jdbcTemplate == null ? new InMemoryBotStore() : new JdbcBotStore(jdbcTemplate);
    }
}
