package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.generation.domain.GenerationTemplate;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class GenerationTemplateService {
    private final GenerationTemplateStore store;
    private final TenantBusinessGuard tenantGuard;
    private final WorkflowApplicationService workflowService;
    private final ObjectMapper objectMapper;

    public GenerationTemplateService() {
        this(new InMemoryGenerationTemplateStore(), null, null, new ObjectMapper());
    }

    @Autowired
    public GenerationTemplateService(
            GenerationTemplateStore store,
            TenantBusinessGuard tenantGuard,
            WorkflowApplicationService workflowService,
            ObjectMapper objectMapper
    ) {
        this.store = store;
        this.tenantGuard = tenantGuard;
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
    }

    public GenerationTemplate create(
            String name,
            String code,
            String description,
            String category,
            String ownerUnitId,
            String outputType,
            String templateSchema,
            String workflowId,
            String workflowSnapshot,
            String status,
            int version,
            String createdBy
    ) {
        Instant now = Instant.now();
        GenerationTemplate template = new GenerationTemplate(
                "gen_tpl_" + UUID.randomUUID(),
                currentTenantId(),
                name,
                code,
                description,
                category,
                ownerUnitId,
                outputType,
                templateSchema,
                blankToNull(workflowId),
                resolveWorkflowSnapshot(workflowId, workflowSnapshot),
                status == null || status.isBlank() ? "DRAFT" : status,
                version <= 0 ? 1 : version,
                createdBy,
                now,
                now
        );
        return store.save(template);
    }

    public GenerationTemplate update(
            String id,
            String name,
            String code,
            String description,
            String category,
            String ownerUnitId,
            String outputType,
            String templateSchema,
            String workflowId,
            String workflowSnapshot,
            String status,
            int version
    ) {
        GenerationTemplate current = get(id);
        return store.save(new GenerationTemplate(
                current.id(),
                current.tenantId(),
                name,
                code,
                description,
                category,
                ownerUnitId,
                outputType,
                templateSchema,
                blankToNull(workflowId),
                resolveWorkflowSnapshot(workflowId, workflowSnapshot),
                status,
                version <= 0 ? current.version() : version,
                current.createdBy(),
                current.createdAt(),
                Instant.now()
        ));
    }

    public List<GenerationTemplate> list() {
        return store.list(currentTenantId());
    }

    public GenerationTemplate get(String id) {
        GenerationTemplate template = store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Generation template not found: " + id));
        assertTenantAccessible(template.tenantId());
        return template;
    }

    public void delete(String id) {
        get(id);
        store.delete(id);
    }

    private String resolveWorkflowSnapshot(String workflowId, String workflowSnapshot) {
        if (workflowId == null || workflowId.isBlank() || workflowService == null) {
            return blankToNull(workflowSnapshot);
        }
        try {
            Workflow workflow = workflowService.getWorkflow(workflowId);
            WorkflowVersion version = workflowService.listVersions(workflowId).stream()
                    .max(Comparator.comparingInt(WorkflowVersion::version))
                    .orElse(null);
            if (version == null) {
                return blankToNull(workflowSnapshot);
            }
            return GenerationWorkflowSnapshotBuilder.build(workflow, version, objectMapper);
        } catch (RuntimeException exception) {
            return blankToNull(workflowSnapshot);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String currentTenantId() {
        return tenantGuard == null ? TenantContext.requireTenantId() : tenantGuard.currentTenantId();
    }

    private void assertTenantAccessible(String resourceTenantId) {
        if (tenantGuard != null) {
            tenantGuard.assertAccessible(resourceTenantId);
        }
    }
}
