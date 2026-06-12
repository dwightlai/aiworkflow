package com.mw.ai.agi.workflow.engine;

import com.mw.ai.agi.persistence.JsonSupport;
import com.mw.ai.agi.workflow.domain.WorkflowNodeType;
import com.mw.ai.agi.workflow.persistence.WorkflowExecutionEntity;
import com.mw.ai.agi.workflow.persistence.WorkflowExecutionMapper;
import com.mw.ai.agi.workflow.persistence.WorkflowNodeExecutionEntity;
import com.mw.ai.agi.workflow.persistence.WorkflowNodeExecutionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mw.ai.agi.auth.service.TenantContext;

public class MybatisWorkflowExecutionStore implements WorkflowExecutionStore {
    private final WorkflowExecutionMapper executionMapper;
    private final WorkflowNodeExecutionMapper nodeExecutionMapper;
    private final JsonSupport jsonSupport;

    public MybatisWorkflowExecutionStore(
            WorkflowExecutionMapper executionMapper,
            WorkflowNodeExecutionMapper nodeExecutionMapper,
            JsonSupport jsonSupport
    ) {
        this.executionMapper = executionMapper;
        this.nodeExecutionMapper = nodeExecutionMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public WorkflowExecution saveWorkflowExecution(WorkflowExecution execution) {
        WorkflowExecutionEntity entity = toEntity(execution);
        if (executionMapper.selectById(execution.id()) == null) {
            executionMapper.insert(entity);
        } else {
            executionMapper.updateById(entity);
        }
        return execution;
    }

    @Override
    public Optional<WorkflowExecution> findWorkflowExecutionById(String executionId) {
        return Optional.ofNullable(executionMapper.selectById(executionId)).map(this::toDomain);
    }

    @Override
    public List<WorkflowExecution> listWorkflowExecutions(String tenantId) {
        LambdaQueryWrapper<WorkflowExecutionEntity> wrapper = new LambdaQueryWrapper<WorkflowExecutionEntity>()
                .orderByDesc(WorkflowExecutionEntity::getStartedAt);
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(WorkflowExecutionEntity::getTenantId, tenantId);
        }
        return executionMapper.selectList(wrapper)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public NodeExecution saveNodeExecution(NodeExecution nodeExecution) {
        nodeExecutionMapper.insert(toEntity(nodeExecution));
        return nodeExecution;
    }

    @Override
    public List<NodeExecution> listNodeExecutions(String workflowExecutionId) {
        return nodeExecutionMapper.selectList(new LambdaQueryWrapper<WorkflowNodeExecutionEntity>()
                        .eq(WorkflowNodeExecutionEntity::getWorkflowExecutionId, workflowExecutionId)
                        .orderByAsc(WorkflowNodeExecutionEntity::getStartedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private WorkflowExecutionEntity toEntity(WorkflowExecution execution) {
        WorkflowExecutionEntity entity = new WorkflowExecutionEntity();
        entity.setId(execution.id());
        entity.setWorkflowId(execution.workflowId());
        entity.setWorkflowVersionId(execution.workflowVersionId());
        entity.setTenantId(TenantContext.requireTenantId());
        entity.setStatus(execution.status().name());
        entity.setInputJson(jsonSupport.write(execution.input()));
        entity.setContextJson(jsonSupport.write(contextPayload(execution)));
        entity.setOutputJson(jsonSupport.write(execution.output()));
        entity.setErrorCode(null);
        entity.setErrorMessage(execution.errorMessage());
        entity.setStartedAt(execution.startedAt());
        entity.setFinishedAt(execution.finishedAt());
        entity.setCreatedAt(execution.startedAt() == null ? Instant.now() : execution.startedAt());
        return entity;
    }

    private WorkflowExecution toDomain(WorkflowExecutionEntity entity) {
        Map<String, Object> contextPayload = jsonSupport.readMap(entity.getContextJson());
        String currentNodeId = contextPayload.containsKey("__currentNodeId")
                ? String.valueOf(contextPayload.remove("__currentNodeId"))
                : null;
        if (currentNodeId != null && currentNodeId.isBlank()) {
            currentNodeId = null;
        }
        return new WorkflowExecution(
                entity.getId(),
                entity.getWorkflowId(),
                entity.getWorkflowVersionId(),
                WorkflowExecutionStatus.valueOf(entity.getStatus()),
                jsonSupport.readMap(entity.getInputJson()),
                jsonSupport.readMap(entity.getOutputJson()),
                entity.getErrorMessage(),
                contextPayload,
                currentNodeId,
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    private Map<String, Object> contextPayload(WorkflowExecution execution) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>(execution.context());
        if (execution.currentNodeId() != null && !execution.currentNodeId().isBlank()) {
            payload.put("__currentNodeId", execution.currentNodeId());
        }
        return payload;
    }

    private WorkflowNodeExecutionEntity toEntity(NodeExecution nodeExecution) {
        WorkflowNodeExecutionEntity entity = new WorkflowNodeExecutionEntity();
        entity.setId(nodeExecution.id());
        entity.setWorkflowExecutionId(nodeExecution.workflowExecutionId());
        entity.setNodeId(nodeExecution.nodeId());
        entity.setNodeType(nodeExecution.nodeType().name());
        entity.setStatus(nodeExecution.status().name());
        entity.setInputJson(jsonSupport.write(nodeExecution.input()));
        entity.setOutputJson(jsonSupport.write(nodeExecution.output()));
        entity.setErrorCode(null);
        entity.setErrorMessage(nodeExecution.errorMessage());
        entity.setStartedAt(nodeExecution.startedAt());
        entity.setFinishedAt(nodeExecution.finishedAt());
        entity.setCreatedAt(nodeExecution.startedAt() == null ? Instant.now() : nodeExecution.startedAt());
        return entity;
    }

    private NodeExecution toDomain(WorkflowNodeExecutionEntity entity) {
        return new NodeExecution(
                entity.getId(),
                entity.getWorkflowExecutionId(),
                entity.getNodeId(),
                WorkflowNodeType.valueOf(entity.getNodeType()),
                NodeExecutionStatus.valueOf(entity.getStatus()),
                jsonSupport.readMap(entity.getInputJson()),
                jsonSupport.readMap(entity.getOutputJson()),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }
}
