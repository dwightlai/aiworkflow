package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.config.AgiStorageProperties;
import com.mw.ai.agi.config.AgiStorageSettingsService;
import com.mw.ai.agi.generation.domain.GenerationTemplate;
import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowVersion;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GenerationTemplateService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final GenerationTemplateStore store;
    private final TenantBusinessGuard tenantGuard;
    private final WorkflowApplicationService workflowService;
    private final ObjectMapper objectMapper;
    private final ResearchDocxMasterStorage docxMasterStorage;

    public GenerationTemplateService() {
        this(
                new InMemoryGenerationTemplateStore(),
                null,
                null,
                new ObjectMapper(),
                new ResearchDocxMasterStorage(AgiStorageSettingsService.withDefaults(
                        new AgiStorageProperties(),
                        new ObjectMapper()
                ))
        );
    }

    @Autowired
    public GenerationTemplateService(
            GenerationTemplateStore store,
            TenantBusinessGuard tenantGuard,
            WorkflowApplicationService workflowService,
            ObjectMapper objectMapper,
            ResearchDocxMasterStorage docxMasterStorage
    ) {
        this.store = store;
        this.tenantGuard = tenantGuard;
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
        this.docxMasterStorage = docxMasterStorage;
    }

    public GenerationTemplate create(
            String name,
            String code,
            String description,
            String category,
            String ownerUnitId,
            String outputType,
            String templateCategory,
            String docxConfig,
            String layoutConfig,
            String linkedHtmlTemplateId,
            String templateSchema,
            String workflowId,
            String workflowSnapshot,
            String status,
            int version,
            String createdBy
    ) {
        Instant now = Instant.now();
        String operator = createdBy == null || createdBy.isBlank() ? OperatorContext.currentUserId() : createdBy;
        GenerationTemplate template = new GenerationTemplate(
                "gen_tpl_" + UUID.randomUUID(),
                currentTenantId(),
                name,
                code,
                description,
                category,
                ownerUnitId,
                blankToDefault(outputType, "DOCX"),
                blankToDefault(templateCategory, "report"),
                blankToNull(docxConfig),
                blankToNull(layoutConfig),
                blankToNull(linkedHtmlTemplateId),
                templateSchema,
                blankToNull(workflowId),
                resolveWorkflowSnapshot(workflowId, workflowSnapshot),
                status == null || status.isBlank() ? "DRAFT" : status,
                version <= 0 ? 1 : version,
                operator,
                operator,
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
            String templateCategory,
            String docxConfig,
            String layoutConfig,
            String linkedHtmlTemplateId,
            String templateSchema,
            String workflowId,
            String workflowSnapshot,
            String status,
            int version
    ) {
        GenerationTemplate current = get(id);
        String operator = OperatorContext.currentUserId();
        return store.save(new GenerationTemplate(
                current.id(),
                current.tenantId(),
                name,
                code,
                description,
                category,
                ownerUnitId,
                blankToDefault(outputType, current.outputType()),
                blankToDefault(templateCategory, current.templateCategory()),
                blankToNull(docxConfig) == null ? current.docxConfig() : blankToNull(docxConfig),
                blankToNull(layoutConfig) == null ? current.layoutConfig() : blankToNull(layoutConfig),
                blankToNull(linkedHtmlTemplateId) == null ? current.linkedHtmlTemplateId() : blankToNull(linkedHtmlTemplateId),
                templateSchema,
                blankToNull(workflowId),
                resolveWorkflowSnapshot(workflowId, workflowSnapshot),
                status,
                version <= 0 ? current.version() : version,
                current.createdBy(),
                operator,
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

    public Map<String, Object> uploadDocxMaster(String id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 DOCX 母版文件");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".docx")) {
            throw new IllegalArgumentException("仅支持 .docx 文件");
        }
        GenerationTemplate current = get(id);
        String masterFile = docxMasterStorage.save(current.id(), readBytes(file));
        Map<String, Object> docxConfig = readDocxConfigMap(current.docxConfig());
        docxConfig.put("masterFile", masterFile);
        if ("topic_collection".equalsIgnoreCase(stringValue(current.templateCategory()))) {
            docxConfig.putIfAbsent("templateType", "archive_topic_collection");
        }
        String docxConfigJson = writeJson(docxConfig);
        GenerationTemplate updated = store.save(new GenerationTemplate(
                current.id(),
                current.tenantId(),
                current.name(),
                current.code(),
                current.description(),
                current.category(),
                current.ownerUnitId(),
                current.outputType(),
                current.templateCategory(),
                docxConfigJson,
                current.layoutConfig(),
                current.linkedHtmlTemplateId(),
                current.templateSchema(),
                current.workflowId(),
                current.workflowSnapshot(),
                current.status(),
                current.version(),
                current.createdBy(),
                OperatorContext.currentUserId(),
                current.createdAt(),
                Instant.now()
        ));
        return Map.of(
                "templateId", updated.id(),
                "masterFile", masterFile,
                "fileName", file.getOriginalFilename(),
                "docxConfig", docxConfigJson
        );
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read uploaded DOCX.", exception);
        }
    }

    private Map<String, Object> readDocxConfigMap(String docxConfig) {
        if (docxConfig == null || docxConfig.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(docxConfig, MAP_TYPE);
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write docx config.", exception);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
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
