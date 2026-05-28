package com.aiworkflow.workflow.service;

import com.aiworkflow.workflow.domain.Workflow;
import com.aiworkflow.workflow.domain.WorkflowDefinition;
import com.aiworkflow.workflow.domain.WorkflowStatus;
import com.aiworkflow.workflow.domain.WorkflowVersion;
import com.aiworkflow.workflow.domain.WorkflowVersionStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkflowApplicationService {
    private final WorkflowStore store;
    private final DagValidator dagValidator;

    public WorkflowApplicationService(WorkflowStore store, DagValidator dagValidator) {
        this.store = store;
        this.dagValidator = dagValidator;
    }

    public Workflow createWorkflow(
            String tenantId,
            String name,
            String description,
            String createdBy,
            WorkflowDefinition definition
    ) {
        dagValidator.validate(definition);

        Instant now = Instant.now();
        Workflow workflow = new Workflow(
                UUID.randomUUID().toString(),
                tenantId,
                name,
                description,
                WorkflowStatus.DRAFT,
                null,
                createdBy,
                now,
                now
        );
        WorkflowVersion draftVersion = new WorkflowVersion(
                UUID.randomUUID().toString(),
                workflow.id(),
                1,
                definition,
                WorkflowVersionStatus.DRAFT,
                null,
                null,
                now
        );

        store.saveWorkflow(workflow);
        store.saveVersion(draftVersion);
        return workflow;
    }

    public WorkflowVersion updateDraftDefinition(String workflowId, WorkflowDefinition definition) {
        getWorkflow(workflowId);
        dagValidator.validate(definition);

        WorkflowVersion draftVersion = findDraftVersion(workflowId);
        WorkflowVersion updatedDraft = new WorkflowVersion(
                draftVersion.id(),
                draftVersion.workflowId(),
                draftVersion.version(),
                definition,
                WorkflowVersionStatus.DRAFT,
                null,
                null,
                draftVersion.createdAt()
        );
        return store.saveVersion(updatedDraft);
    }

    public WorkflowVersion publishDraftVersion(String workflowId, String publishedBy) {
        Workflow workflow = getWorkflow(workflowId);
        WorkflowVersion draftVersion = findDraftVersion(workflowId);
        Instant publishedAt = Instant.now();
        WorkflowVersion publishedVersion = new WorkflowVersion(
                draftVersion.id(),
                draftVersion.workflowId(),
                draftVersion.version(),
                draftVersion.definition(),
                WorkflowVersionStatus.PUBLISHED,
                publishedBy,
                publishedAt,
                draftVersion.createdAt()
        );
        store.saveVersion(publishedVersion);

        Instant updatedAt = publishedAt.isAfter(workflow.updatedAt())
                ? publishedAt
                : workflow.updatedAt().plusNanos(1);
        store.saveWorkflow(new Workflow(
                workflow.id(),
                workflow.tenantId(),
                workflow.name(),
                workflow.description(),
                WorkflowStatus.PUBLISHED,
                publishedVersion.id(),
                workflow.createdBy(),
                workflow.createdAt(),
                updatedAt
        ));
        return publishedVersion;
    }

    public List<Workflow> listWorkflows() {
        return store.listWorkflows();
    }

    public Workflow getWorkflow(String workflowId) {
        return store.findWorkflowById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    public List<WorkflowVersion> listVersions(String workflowId) {
        getWorkflow(workflowId);
        return store.listVersions(workflowId);
    }

    private WorkflowVersion findDraftVersion(String workflowId) {
        return store.listVersions(workflowId).stream()
                .filter(version -> version.status() == WorkflowVersionStatus.DRAFT)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Workflow has no draft version: " + workflowId));
    }
}
