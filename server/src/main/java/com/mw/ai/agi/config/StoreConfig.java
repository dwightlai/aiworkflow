package com.mw.ai.agi.config;

import com.mw.ai.agi.bot.service.BotStore;
import com.mw.ai.agi.bot.service.InMemoryBotStore;
import com.mw.ai.agi.bot.service.MybatisBotStore;
import com.mw.ai.agi.bot.persistence.AiBotMapper;
import com.mw.ai.agi.bot.persistence.BotMessageMapper;
import com.mw.ai.agi.bot.persistence.BotSessionMapper;
import com.mw.ai.agi.knowledge.persistence.KnowledgeBaseMapper;
import com.mw.ai.agi.knowledge.persistence.KnowledgeChunkMapper;
import com.mw.ai.agi.knowledge.persistence.KnowledgeChunkVectorMapper;
import com.mw.ai.agi.knowledge.persistence.KnowledgeDocumentMapper;
import com.mw.ai.agi.knowledge.persistence.VectorStoreConfigMapper;
import com.mw.ai.agi.knowledge.service.InMemoryKnowledgeStore;
import com.mw.ai.agi.knowledge.service.InMemoryVectorStoreConfigStore;
import com.mw.ai.agi.knowledge.service.KnowledgeStore;
import com.mw.ai.agi.knowledge.service.MybatisKnowledgeStore;
import com.mw.ai.agi.knowledge.service.MybatisVectorStoreConfigStore;
import com.mw.ai.agi.knowledge.service.VectorStoreConfigStore;
import com.mw.ai.agi.model.persistence.ModelProviderMapper;
import com.mw.ai.agi.model.service.InMemoryModelProviderStore;
import com.mw.ai.agi.model.service.ModelProviderStore;
import com.mw.ai.agi.model.service.MybatisModelProviderStore;
import com.mw.ai.agi.persistence.JsonSupport;
import com.mw.ai.agi.prompt.persistence.PromptTemplateMapper;
import com.mw.ai.agi.prompt.service.InMemoryPromptTemplateStore;
import com.mw.ai.agi.prompt.service.MybatisPromptTemplateStore;
import com.mw.ai.agi.prompt.service.PromptTemplateStore;
import com.mw.ai.agi.workflow.engine.InMemoryWorkflowExecutionStore;
import com.mw.ai.agi.workflow.engine.MybatisWorkflowExecutionStore;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionStore;
import com.mw.ai.agi.workflow.persistence.WorkflowExecutionMapper;
import com.mw.ai.agi.workflow.persistence.WorkflowMapper;
import com.mw.ai.agi.workflow.persistence.WorkflowNodeExecutionMapper;
import com.mw.ai.agi.workflow.persistence.WorkflowVersionMapper;
import com.mw.ai.agi.workflow.service.InMemoryWorkflowStore;
import com.mw.ai.agi.workflow.service.MybatisWorkflowStore;
import com.mw.ai.agi.workflow.service.WorkflowStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreConfig {
    @Bean
    public WorkflowStore workflowStore(
            ObjectProvider<WorkflowMapper> workflowMapperProvider,
            ObjectProvider<WorkflowVersionMapper> versionMapperProvider,
            ObjectMapper objectMapper
    ) {
        WorkflowMapper workflowMapper = workflowMapperProvider.getIfAvailable();
        WorkflowVersionMapper versionMapper = versionMapperProvider.getIfAvailable();
        return workflowMapper == null || versionMapper == null
                ? new InMemoryWorkflowStore()
                : new MybatisWorkflowStore(workflowMapper, versionMapper, new JsonSupport(objectMapper));
    }

    @Bean
    public WorkflowExecutionStore workflowExecutionStore(
            ObjectProvider<WorkflowExecutionMapper> executionMapperProvider,
            ObjectProvider<WorkflowNodeExecutionMapper> nodeExecutionMapperProvider,
            ObjectMapper objectMapper
    ) {
        WorkflowExecutionMapper executionMapper = executionMapperProvider.getIfAvailable();
        WorkflowNodeExecutionMapper nodeExecutionMapper = nodeExecutionMapperProvider.getIfAvailable();
        return executionMapper == null || nodeExecutionMapper == null
                ? new InMemoryWorkflowExecutionStore()
                : new MybatisWorkflowExecutionStore(executionMapper, nodeExecutionMapper, new JsonSupport(objectMapper));
    }

    @Bean
    public KnowledgeStore knowledgeStore(
            ObjectProvider<KnowledgeBaseMapper> knowledgeBaseMapperProvider,
            ObjectProvider<KnowledgeDocumentMapper> documentMapperProvider,
            ObjectProvider<KnowledgeChunkMapper> chunkMapperProvider,
            ObjectProvider<KnowledgeChunkVectorMapper> chunkVectorMapperProvider,
            ObjectMapper objectMapper
    ) {
        KnowledgeBaseMapper knowledgeBaseMapper = knowledgeBaseMapperProvider.getIfAvailable();
        KnowledgeDocumentMapper documentMapper = documentMapperProvider.getIfAvailable();
        KnowledgeChunkMapper chunkMapper = chunkMapperProvider.getIfAvailable();
        KnowledgeChunkVectorMapper chunkVectorMapper = chunkVectorMapperProvider.getIfAvailable();
        return knowledgeBaseMapper == null || documentMapper == null || chunkMapper == null || chunkVectorMapper == null
                ? new InMemoryKnowledgeStore()
                : new MybatisKnowledgeStore(
                knowledgeBaseMapper,
                documentMapper,
                chunkMapper,
                chunkVectorMapper,
                new JsonSupport(objectMapper)
        );
    }

    @Bean
    public VectorStoreConfigStore vectorStoreConfigStore(ObjectProvider<VectorStoreConfigMapper> mapperProvider) {
        VectorStoreConfigMapper mapper = mapperProvider.getIfAvailable();
        return mapper == null ? new InMemoryVectorStoreConfigStore() : new MybatisVectorStoreConfigStore(mapper);
    }

    @Bean
    public ModelProviderStore modelProviderStore(ObjectProvider<ModelProviderMapper> mapperProvider) {
        ModelProviderMapper mapper = mapperProvider.getIfAvailable();
        return mapper == null ? new InMemoryModelProviderStore() : new MybatisModelProviderStore(mapper);
    }

    @Bean
    public PromptTemplateStore promptTemplateStore(ObjectProvider<PromptTemplateMapper> mapperProvider) {
        PromptTemplateMapper mapper = mapperProvider.getIfAvailable();
        return mapper == null ? new InMemoryPromptTemplateStore() : new MybatisPromptTemplateStore(mapper);
    }

    @Bean
    public BotStore botStore(
            ObjectProvider<AiBotMapper> botMapperProvider,
            ObjectProvider<BotSessionMapper> sessionMapperProvider,
            ObjectProvider<BotMessageMapper> messageMapperProvider,
            ObjectMapper objectMapper
    ) {
        AiBotMapper botMapper = botMapperProvider.getIfAvailable();
        BotSessionMapper sessionMapper = sessionMapperProvider.getIfAvailable();
        BotMessageMapper messageMapper = messageMapperProvider.getIfAvailable();
        return botMapper == null || sessionMapper == null || messageMapper == null
                ? new InMemoryBotStore()
                : new MybatisBotStore(botMapper, sessionMapper, messageMapper, new JsonSupport(objectMapper));
    }
}
