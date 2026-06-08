package com.mw.ai.agi.workflow.service;

import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowStatus;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryWorkflowStore implements WorkflowStore {
    private final ConcurrentMap<String, Workflow> workflows = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WorkflowVersion> versions = new ConcurrentHashMap<>();

    @Override
    public Workflow saveWorkflow(Workflow workflow) {
        workflows.put(workflow.id(), workflow);
        return workflow;
    }

    @Override
    public Optional<Workflow> findWorkflowById(String workflowId) {
        return Optional.ofNullable(workflows.get(workflowId));
    }

    @Override
    public List<Workflow> listWorkflows() {
        return workflows.values().stream()
                .filter(workflow -> workflow.status() != WorkflowStatus.DELETED)
                .sorted(Comparator.comparing(Workflow::createdAt))
                .toList();
    }

    @Override
    public WorkflowVersion saveVersion(WorkflowVersion version) {
        versions.put(version.id(), version);
        return version;
    }

    @Override
    public Optional<WorkflowVersion> findVersionById(String versionId) {
        return Optional.ofNullable(versions.get(versionId));
    }

    @Override
    public List<WorkflowVersion> listVersions(String workflowId) {
        return versions.values().stream()
                .filter(version -> version.workflowId().equals(workflowId))
                .sorted(Comparator.comparingInt(WorkflowVersion::version))
                .toList();
    }
}
