package com.mw.ai.agi.workflow.service;

import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;

import java.util.List;
import java.util.Optional;

public interface WorkflowStore {
    Workflow saveWorkflow(Workflow workflow);

    Optional<Workflow> findWorkflowById(String workflowId);

    List<Workflow> listWorkflows(String tenantId);

    WorkflowVersion saveVersion(WorkflowVersion version);

    Optional<WorkflowVersion> findVersionById(String versionId);

    List<WorkflowVersion> listVersions(String workflowId);
}
