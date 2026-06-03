package com.aiworkflow.workflow.service;

import com.aiworkflow.persistence.JsonSupport;
import com.aiworkflow.workflow.domain.Workflow;
import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowStatus;
import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.domain.WorkflowVersionStatus;
import com.aiworkflow.workflow.persistence.WorkflowEntity;
import com.aiworkflow.workflow.persistence.WorkflowMapper;
import com.aiworkflow.workflow.persistence.WorkflowVersionEntity;
import com.aiworkflow.workflow.persistence.WorkflowVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

public class MybatisWorkflowStore implements WorkflowStore {
    private final WorkflowMapper workflowMapper;
    private final WorkflowVersionMapper versionMapper;
    private final JsonSupport jsonSupport;

    public MybatisWorkflowStore(WorkflowMapper workflowMapper, WorkflowVersionMapper versionMapper, JsonSupport jsonSupport) {
        this.workflowMapper = workflowMapper;
        this.versionMapper = versionMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public Workflow saveWorkflow(Workflow workflow) {
        WorkflowEntity entity = toEntity(workflow);
        if (workflowMapper.selectById(workflow.id()) == null) {
            workflowMapper.insert(entity);
        } else {
            workflowMapper.updateById(entity);
        }
        return workflow;
    }

    @Override
    public Optional<Workflow> findWorkflowById(String workflowId) {
        return Optional.ofNullable(workflowMapper.selectById(workflowId)).map(this::toDomain);
    }

    @Override
    public List<Workflow> listWorkflows() {
        return workflowMapper.selectList(new LambdaQueryWrapper<WorkflowEntity>()
                        .orderByAsc(WorkflowEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public WorkflowVersion saveVersion(WorkflowVersion version) {
        WorkflowVersionEntity entity = toEntity(version);
        if (versionMapper.selectById(version.id()) == null) {
            versionMapper.insert(entity);
        } else {
            versionMapper.updateById(entity);
        }
        return version;
    }

    @Override
    public Optional<WorkflowVersion> findVersionById(String versionId) {
        return Optional.ofNullable(versionMapper.selectById(versionId)).map(this::toDomain);
    }

    @Override
    public List<WorkflowVersion> listVersions(String workflowId) {
        return versionMapper.selectList(new LambdaQueryWrapper<WorkflowVersionEntity>()
                        .eq(WorkflowVersionEntity::getWorkflowId, workflowId)
                        .orderByAsc(WorkflowVersionEntity::getVersion))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private WorkflowEntity toEntity(Workflow workflow) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(workflow.id());
        entity.setTenantId(workflow.tenantId());
        entity.setName(workflow.name());
        entity.setDescription(workflow.description());
        entity.setStatus(workflow.status().name());
        entity.setCurrentVersionId(workflow.currentVersionId());
        entity.setCreatedBy(workflow.createdBy());
        entity.setCreatedAt(workflow.createdAt());
        entity.setUpdatedAt(workflow.updatedAt());
        return entity;
    }

    private Workflow toDomain(WorkflowEntity entity) {
        return new Workflow(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                WorkflowStatus.valueOf(entity.getStatus()),
                entity.getCurrentVersionId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private WorkflowVersionEntity toEntity(WorkflowVersion version) {
        WorkflowVersionEntity entity = new WorkflowVersionEntity();
        entity.setId(version.id());
        entity.setWorkflowId(version.workflowId());
        entity.setVersion(version.version());
        entity.setDefinitionJson(jsonSupport.write(version.definition()));
        entity.setStatus(version.status().name());
        entity.setPublishedBy(version.publishedBy());
        entity.setPublishedAt(version.publishedAt());
        entity.setCreatedAt(version.createdAt());
        return entity;
    }

    private WorkflowVersion toDomain(WorkflowVersionEntity entity) {
        return new WorkflowVersion(
                entity.getId(),
                entity.getWorkflowId(),
                entity.getVersion(),
                jsonSupport.read(entity.getDefinitionJson(), WorkflowDefinition.class),
                WorkflowVersionStatus.valueOf(entity.getStatus()),
                entity.getPublishedBy(),
                entity.getPublishedAt(),
                entity.getCreatedAt()
        );
    }
}
