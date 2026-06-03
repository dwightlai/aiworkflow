package com.aiworkflow.config;

import com.aiworkflow.bot.service.BotStore;
import com.aiworkflow.bot.service.InMemoryBotStore;
import com.aiworkflow.bot.service.MybatisBotStore;
import com.aiworkflow.bot.persistence.AiBotMapper;
import com.aiworkflow.bot.persistence.BotMessageMapper;
import com.aiworkflow.bot.persistence.BotSessionMapper;
import com.aiworkflow.knowledge.persistence.KnowledgeBaseMapper;
import com.aiworkflow.knowledge.persistence.KnowledgeChunkMapper;
import com.aiworkflow.knowledge.persistence.KnowledgeChunkVectorMapper;
import com.aiworkflow.knowledge.persistence.KnowledgeDocumentMapper;
import com.aiworkflow.knowledge.persistence.VectorStoreConfigMapper;
import com.aiworkflow.knowledge.service.InMemoryKnowledgeStore;
import com.aiworkflow.knowledge.service.InMemoryVectorStoreConfigStore;
import com.aiworkflow.knowledge.service.KnowledgeStore;
import com.aiworkflow.knowledge.service.MybatisKnowledgeStore;
import com.aiworkflow.knowledge.service.MybatisVectorStoreConfigStore;
import com.aiworkflow.knowledge.service.VectorStoreConfigStore;
import com.aiworkflow.model.persistence.ModelProviderMapper;
import com.aiworkflow.model.service.InMemoryModelProviderStore;
import com.aiworkflow.model.service.ModelProviderStore;
import com.aiworkflow.model.service.MybatisModelProviderStore;
import com.aiworkflow.persistence.JsonSupport;
import com.aiworkflow.prompt.persistence.PromptTemplateMapper;
import com.aiworkflow.prompt.service.InMemoryPromptTemplateStore;
import com.aiworkflow.prompt.service.MybatisPromptTemplateStore;
import com.aiworkflow.prompt.service.PromptTemplateStore;
import com.aiworkflow.workflow.engine.InMemoryWorkflowExecutionStore;
import com.aiworkflow.workflow.engine.MybatisWorkflowExecutionStore;
import com.aiworkflow.workflow.engine.WorkflowExecutionStore;
import com.aiworkflow.workflow.persistence.WorkflowExecutionMapper;
import com.aiworkflow.workflow.persistence.WorkflowMapper;
import com.aiworkflow.workflow.persistence.WorkflowNodeExecutionMapper;
import com.aiworkflow.workflow.persistence.WorkflowVersionMapper;
import com.aiworkflow.workflow.service.InMemoryWorkflowStore;
import com.aiworkflow.workflow.service.MybatisWorkflowStore;
import com.aiworkflow.workflow.service.WorkflowStore;
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
            ObjectProvider<BotMessageMapper> messageMapperProvider
    ) {
        AiBotMapper botMapper = botMapperProvider.getIfAvailable();
        BotSessionMapper sessionMapper = sessionMapperProvider.getIfAvailable();
        BotMessageMapper messageMapper = messageMapperProvider.getIfAvailable();
        return botMapper == null || sessionMapper == null || messageMapper == null
                ? new InMemoryBotStore()
                : new MybatisBotStore(botMapper, sessionMapper, messageMapper);
    }
}
