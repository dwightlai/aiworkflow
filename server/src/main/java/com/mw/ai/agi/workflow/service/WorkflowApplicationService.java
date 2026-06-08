package com.mw.ai.agi.workflow.service;

import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowDefinition;
import com.mw.ai.agi.workflow.domain.WorkflowStatus;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;
import com.mw.ai.agi.workflow.domain.WorkflowVersionStatus;
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

    public synchronized Workflow createWorkflow(
            String tenantId,
            String name,
            String description,
            String createdBy,
            WorkflowDefinition definition
    ) {
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

    public synchronized WorkflowVersion updateDraftDefinition(String workflowId, WorkflowDefinition definition) {
        getWorkflow(workflowId);
        dagValidator.validateDraft(definition);

        WorkflowVersion draftVersion = findDraftVersion(workflowId)
                .orElseGet(() -> createNextDraftVersion(workflowId, definition));
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

    public synchronized WorkflowVersion publishDraftVersion(String workflowId, String publishedBy) {
        Workflow workflow = getWorkflow(workflowId);
        WorkflowVersion draftVersion = findDraftVersion(workflowId)
                .orElseGet(() -> store.findVersionById(workflow.currentVersionId())
                        .orElseThrow(() -> WorkflowNotFoundException.publishedVersionNotFound(workflowId)));
        if (draftVersion.status() == WorkflowVersionStatus.PUBLISHED) {
            return draftVersion;
        }
        dagValidator.validate(draftVersion.definition());
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

    public synchronized Workflow updateWorkflowMetadata(String workflowId, String name, String description) {
        Workflow workflow = getWorkflow(workflowId);
        Workflow updated = new Workflow(
                workflow.id(),
                workflow.tenantId(),
                name,
                description,
                workflow.status(),
                workflow.currentVersionId(),
                workflow.createdBy(),
                workflow.createdAt(),
                Instant.now()
        );
        return store.saveWorkflow(updated);
    }

    public synchronized Workflow archiveWorkflow(String workflowId) {
        Workflow workflow = getWorkflow(workflowId);
        Workflow archived = new Workflow(
                workflow.id(),
                workflow.tenantId(),
                workflow.name(),
                workflow.description(),
                WorkflowStatus.ARCHIVED,
                workflow.currentVersionId(),
                workflow.createdBy(),
                workflow.createdAt(),
                Instant.now()
        );
        return store.saveWorkflow(archived);
    }

    public synchronized void deleteWorkflow(String workflowId) {
        Workflow workflow = store.findWorkflowById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        if (workflow.status() == WorkflowStatus.DELETED) {
            return;
        }
        store.saveWorkflow(new Workflow(
                workflow.id(),
                workflow.tenantId(),
                workflow.name(),
                workflow.description(),
                WorkflowStatus.DELETED,
                workflow.currentVersionId(),
                workflow.createdBy(),
                workflow.createdAt(),
                Instant.now()
        ));
    }

    public Workflow getWorkflow(String workflowId) {
        Workflow workflow = store.findWorkflowById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        if (workflow.status() == WorkflowStatus.DELETED) {
            throw new WorkflowNotFoundException(workflowId);
        }
        return workflow;
    }

    public List<WorkflowVersion> listVersions(String workflowId) {
        getWorkflow(workflowId);
        return store.listVersions(workflowId);
    }

    public WorkflowVersion getPublishedVersion(String workflowId) {
        Workflow workflow = getWorkflow(workflowId);
        if (workflow.currentVersionId() == null) {
            throw WorkflowNotFoundException.publishedVersionNotFound(workflowId);
        }
        return store.findVersionById(workflow.currentVersionId())
                .orElseThrow(() -> WorkflowNotFoundException.publishedVersionNotFound(workflowId));
    }

    private java.util.Optional<WorkflowVersion> findDraftVersion(String workflowId) {
        return store.listVersions(workflowId).stream()
                .filter(version -> version.status() == WorkflowVersionStatus.DRAFT)
                .findFirst();
    }

    private WorkflowVersion createNextDraftVersion(String workflowId, WorkflowDefinition definition) {
        int nextVersion = store.listVersions(workflowId).stream()
                .mapToInt(WorkflowVersion::version)
                .max()
                .orElse(0) + 1;
        return new WorkflowVersion(
                UUID.randomUUID().toString(),
                workflowId,
                nextVersion,
                definition,
                WorkflowVersionStatus.DRAFT,
                null,
                null,
                Instant.now()
        );
    }
}
