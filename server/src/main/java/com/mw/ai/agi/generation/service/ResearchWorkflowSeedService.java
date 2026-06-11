package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.generation.persistence.GenerationTemplateMapper;
import com.mw.ai.agi.model.persistence.ModelProviderEntity;
import com.mw.ai.agi.model.persistence.ModelProviderMapper;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowStatus;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;
import com.mw.ai.agi.workflow.domain.WorkflowVersionStatus;
import com.mw.ai.agi.workflow.persistence.WorkflowEntity;
import com.mw.ai.agi.workflow.persistence.WorkflowMapper;
import com.mw.ai.agi.workflow.persistence.WorkflowVersionEntity;
import com.mw.ai.agi.workflow.persistence.WorkflowVersionMapper;
import com.mw.ai.agi.workflow.service.DagValidator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Component
@Order(20)
public class ResearchWorkflowSeedService implements ApplicationRunner {
    private static final String TEMPLATE_ID = "template_research_001";
    private static final String TENANT_ID = "tenant_default";

    private final Optional<WorkflowMapper> workflowMapper;
    private final Optional<WorkflowVersionMapper> workflowVersionMapper;
    private final Optional<GenerationTemplateMapper> generationTemplateMapper;
    private final Optional<ModelProviderMapper> modelProviderMapper;
    private final DagValidator dagValidator;
    private final ObjectMapper objectMapper;

    public ResearchWorkflowSeedService(
            Optional<WorkflowMapper> workflowMapper,
            Optional<WorkflowVersionMapper> workflowVersionMapper,
            Optional<GenerationTemplateMapper> generationTemplateMapper,
            Optional<ModelProviderMapper> modelProviderMapper,
            DagValidator dagValidator,
            ObjectMapper objectMapper
    ) {
        this.workflowMapper = workflowMapper;
        this.workflowVersionMapper = workflowVersionMapper;
        this.generationTemplateMapper = generationTemplateMapper;
        this.modelProviderMapper = modelProviderMapper;
        this.dagValidator = dagValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (workflowMapper.isEmpty() || workflowVersionMapper.isEmpty()) {
            return;
        }
        WorkflowDefinition definition = ResearchWorkflowDefinitionBuilder.build();
        dagValidator.validate(definition);
        Instant now = Instant.now();
        seedModelProvider(now);
        seedWorkflow(definition, now);
        bindGenerationTemplate(now);
    }

    private void seedModelProvider(Instant now) {
        if (modelProviderMapper.isEmpty()) {
            return;
        }
        ModelProviderMapper mapper = modelProviderMapper.get();
        if (mapper.selectById(ResearchWorkflowDefinitionBuilder.MODEL_PROVIDER_ID) != null) {
            return;
        }
        ModelProviderEntity entity = new ModelProviderEntity();
        entity.setId(ResearchWorkflowDefinitionBuilder.MODEL_PROVIDER_ID);
        entity.setTenantId(TENANT_ID);
        entity.setName("编研默认模型");
        entity.setModelType("Stub");
        entity.setModelUsage("CHAT");
        entity.setDescription("智能编研工作流默认模型占位，可在模型管理中替换为真实模型。");
        entity.setVisionSupport(false);
        entity.setPricePerMillionTokens(BigDecimal.ZERO);
        entity.setBaseUrl("http://127.0.0.1:8080");
        entity.setModel("research-stub");
        entity.setApiKeyRef("stub");
        entity.setEnabled(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
    }

    private void seedWorkflow(WorkflowDefinition definition, Instant now) {
        WorkflowMapper workflowStore = workflowMapper.get();
        WorkflowVersionMapper versionStore = workflowVersionMapper.get();
        WorkflowEntity workflowEntity = workflowStore.selectById(ResearchWorkflowDefinitionBuilder.WORKFLOW_ID);
        if (workflowEntity == null) {
            workflowEntity = new WorkflowEntity();
            workflowEntity.setId(ResearchWorkflowDefinitionBuilder.WORKFLOW_ID);
            workflowEntity.setTenantId(TENANT_ID);
            workflowEntity.setOwnerUnitId("org_default_unit");
            workflowEntity.setName("档案智能编研工作流");
            workflowEntity.setDescription("HTTP 拉取模板与资料、知识库检索、大模型生成大纲与分章正文");
            workflowEntity.setStatus(WorkflowStatus.PUBLISHED.name());
            workflowEntity.setCurrentVersionId(ResearchWorkflowDefinitionBuilder.WORKFLOW_VERSION_ID);
            workflowEntity.setCreatedBy("user_admin");
            workflowEntity.setCreatedAt(now);
            workflowEntity.setUpdatedAt(now);
            workflowStore.insert(workflowEntity);
        } else {
            workflowEntity.setName("档案智能编研工作流");
            workflowEntity.setDescription("HTTP 拉取模板与资料、知识库检索、大模型生成大纲与分章正文");
            workflowEntity.setStatus(WorkflowStatus.PUBLISHED.name());
            workflowEntity.setCurrentVersionId(ResearchWorkflowDefinitionBuilder.WORKFLOW_VERSION_ID);
            workflowEntity.setUpdatedAt(now);
            workflowStore.updateById(workflowEntity);
        }

        WorkflowVersionEntity versionEntity = versionStore.selectById(ResearchWorkflowDefinitionBuilder.WORKFLOW_VERSION_ID);
        String definitionJson = ResearchWorkflowDefinitionBuilder.buildDefinitionJson(objectMapper);
        if (versionEntity == null) {
            versionEntity = new WorkflowVersionEntity();
            versionEntity.setId(ResearchWorkflowDefinitionBuilder.WORKFLOW_VERSION_ID);
            versionEntity.setWorkflowId(ResearchWorkflowDefinitionBuilder.WORKFLOW_ID);
            versionEntity.setVersion(1);
            versionEntity.setDefinitionJson(definitionJson);
            versionEntity.setStatus(WorkflowVersionStatus.PUBLISHED.name());
            versionEntity.setPublishedBy("user_admin");
            versionEntity.setPublishedAt(now);
            versionEntity.setCreatedAt(now);
            versionStore.insert(versionEntity);
            return;
        }
        upgradeResearchWorkflowDefinition(versionEntity);
        versionEntity.setStatus(WorkflowVersionStatus.PUBLISHED.name());
        versionStore.updateById(versionEntity);
    }

    private void upgradeResearchWorkflowDefinition(WorkflowVersionEntity versionEntity) {
        String definitionJson = versionEntity.getDefinitionJson();
        if (definitionJson == null) {
            return;
        }
        String updated = definitionJson
                .replace("\"model\":\"research-stub\"", "\"model\":\"\"")
                .replace("\"model\": \"research-stub\"", "\"model\": \"\"")
                .replace(
                        "/api/generation-templates/template_research_001/runtime",
                        "/api/generation-templates/{{templateId}}/runtime"
                );
        if (!updated.equals(definitionJson)) {
            versionEntity.setDefinitionJson(updated);
        }
    }

    private void bindGenerationTemplate(Instant now) {
        if (generationTemplateMapper.isEmpty()) {
            return;
        }
        var entity = generationTemplateMapper.get().selectById(TEMPLATE_ID);
        if (entity == null) {
            return;
        }
        entity.setWorkflowId(ResearchWorkflowDefinitionBuilder.WORKFLOW_ID);
        entity.setUpdatedAt(now);
        try {
            Workflow workflow = new Workflow(
                    ResearchWorkflowDefinitionBuilder.WORKFLOW_ID,
                    TENANT_ID,
                    "org_default_unit",
                    "档案智能编研工作流",
                    entity.getDescription(),
                    WorkflowStatus.PUBLISHED,
                    ResearchWorkflowDefinitionBuilder.WORKFLOW_VERSION_ID,
                    "user_admin",
                    "user_admin",
                    now,
                    now
            );
            WorkflowVersion version = new WorkflowVersion(
                    ResearchWorkflowDefinitionBuilder.WORKFLOW_VERSION_ID,
                    ResearchWorkflowDefinitionBuilder.WORKFLOW_ID,
                    1,
                    ResearchWorkflowDefinitionBuilder.build(),
                    WorkflowVersionStatus.PUBLISHED,
                    "user_admin",
                    now,
                    now
            );
            entity.setWorkflowSnapshot(GenerationWorkflowSnapshotBuilder.build(workflow, version, objectMapper));
        } catch (RuntimeException ignored) {
            entity.setWorkflowSnapshot(null);
        }
        generationTemplateMapper.get().updateById(entity);
    }
}
