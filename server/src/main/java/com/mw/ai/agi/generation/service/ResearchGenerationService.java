package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.generation.domain.GenerationJob;
import com.mw.ai.agi.generation.domain.GenerationOutput;
import com.mw.ai.agi.generation.domain.GenerationTemplate;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionRequest;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionStatus;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ResearchGenerationService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final GenerationTemplateStore templateStore;
    private final GenerationJobStore jobStore;
    private final GenerationOutputStore outputStore;
    private final ObjectMapper objectMapper;
    private final TenantBusinessGuard tenantGuard;
    private final WorkflowExecutionService workflowExecutionService;
    private final ResearchDocxExporter docxExporter;
    private final ResearchOutputFileStorage outputFileStorage;

    public ResearchGenerationService() {
        this(
                new InMemoryGenerationTemplateStore(),
                new InMemoryGenerationJobStore(),
                new InMemoryGenerationOutputStore(),
                new ObjectMapper(),
                null,
                null,
                new ResearchDocxExporter(),
                new ResearchOutputFileStorage("./target/test-outputs")
        );
    }

    @Autowired
    public ResearchGenerationService(
            GenerationTemplateStore templateStore,
            GenerationJobStore jobStore,
            GenerationOutputStore outputStore,
            ObjectMapper objectMapper,
            TenantBusinessGuard tenantGuard,
            @Autowired(required = false) WorkflowExecutionService workflowExecutionService,
            ResearchDocxExporter docxExporter,
            ResearchOutputFileStorage outputFileStorage
    ) {
        this.templateStore = templateStore;
        this.jobStore = jobStore;
        this.outputStore = outputStore;
        this.objectMapper = objectMapper;
        this.tenantGuard = tenantGuard;
        this.workflowExecutionService = workflowExecutionService;
        this.docxExporter = docxExporter;
        this.outputFileStorage = outputFileStorage;
    }

    public GenerationJob run(
            String botId,
            String templateId,
            String unitId,
            String userId,
            List<String> knowledgeBaseIds,
            Map<String, Object> externalCorpus,
            Map<String, Object> variables
    ) {
        Instant startedAt = Instant.now();
        String tenantId = currentTenantId();
        GenerationTemplate template = getTemplate(templateId);
        assertWorkflowReady(template);
        Map<String, Object> runtimeVariables = variables == null ? Map.of() : variables;
        String topic = stringValue(runtimeVariables.get("topic"), "").trim();
        if (topic.isBlank()) {
            throw new IllegalArgumentException("编研主题不能为空");
        }
        String audience = stringValue(runtimeVariables.get("audience"), "档案管理人员");
        String themeLibraryId = resolveThemeLibraryId(externalCorpus);
        return runViaWorkflow(
                botId,
                template,
                unitId,
                userId,
                knowledgeBaseIds,
                externalCorpus,
                runtimeVariables,
                topic,
                audience,
                themeLibraryId,
                startedAt,
                tenantId
        );
    }

    private void assertWorkflowReady(GenerationTemplate template) {
        if (template.workflowId() == null || template.workflowId().isBlank()) {
            throw new IllegalStateException("编研模板未绑定工作流，请先在编研模板中绑定工作流。");
        }
        if (workflowExecutionService == null) {
            throw new IllegalStateException("工作流执行服务不可用。");
        }
    }

    private GenerationJob runViaWorkflow(
            String botId,
            GenerationTemplate template,
            String unitId,
            String userId,
            List<String> knowledgeBaseIds,
            Map<String, Object> externalCorpus,
            Map<String, Object> variables,
            String topic,
            String audience,
            String themeLibraryId,
            Instant startedAt,
            String tenantId
    ) {
        Map<String, Object> workflowInput = buildWorkflowInput(
                template.id(),
                themeLibraryId,
                topic,
                audience,
                knowledgeBaseIds,
                externalCorpus,
                variables
        );
        WorkflowExecutionResult executionResult = workflowExecutionService.runWorkflow(new WorkflowExecutionRequest(
                template.workflowId(),
                workflowInput,
                Map.of(
                        "userId", userId == null ? "" : userId,
                        "unitId", unitId == null ? "" : unitId
                )
        ));
        if (executionResult.execution().status() != WorkflowExecutionStatus.SUCCEEDED) {
            throw new IllegalStateException(executionResult.execution().errorMessage());
        }

        Map<String, Object> workflowOutput = executionResult.execution().output();
        List<Map<String, Object>> sections = readSectionsFromWorkflow(workflowOutput, template);
        List<Map<String, Object>> sectionOutputs = mapWorkflowSectionOutputs(sections, workflowOutput.get("sectionOutputs"));
        String outlineText = stringValue(workflowOutput.get("outlineText"), "");
        Map<String, Object> outline = buildOutline(topic, sections);
        String contentMarkdown = mergeMarkdown(sectionOutputs);
        if (!outlineText.isBlank()) {
            contentMarkdown = outlineText + "\n\n" + contentMarkdown;
        }
        List<Map<String, Object>> citations = collectCitations(sectionOutputs);
        String outputType = stringValue(workflowOutput.get("outputType"), template.outputType());
        Instant completedAt = Instant.now();
        String jobId = "gen_job_" + UUID.randomUUID();
        String workflowRunSnapshot = writeJson(Map.of(
                "workflowExecutionId", executionResult.execution().id(),
                "workflowId", executionResult.execution().workflowId(),
                "workflowVersionId", executionResult.execution().workflowVersionId(),
                "status", executionResult.execution().status().name()
        ));

        GenerationJob job = new GenerationJob(
                jobId,
                tenantId,
                botId,
                template.id(),
                template.workflowId(),
                unitId,
                userId,
                writeJson(knowledgeBaseIds == null ? List.of() : knowledgeBaseIds),
                writeJson(externalCorpus == null ? Map.of() : externalCorpus),
                writeJson(variables),
                "COMPLETED",
                writeJson(outline),
                writeJson(sectionOutputs),
                workflowRunSnapshot,
                null,
                startedAt,
                completedAt,
                startedAt
        );
        jobStore.save(job);

        String outputId = "gen_out_" + UUID.randomUUID();
        String contentDocxPath = null;
        if (isDocxOutput(outputType)) {
            byte[] docxBytes = docxExporter.export(topic + "编研成果", contentMarkdown);
            contentDocxPath = outputFileStorage.saveDocx(outputId, docxBytes);
        }

        GenerationOutput output = new GenerationOutput(
                outputId,
                tenantId,
                jobId,
                topic + "编研成果",
                outputType,
                contentMarkdown,
                contentDocxPath,
                writeJson(citations),
                writeJson(Map.of(
                        "corpusItems", workflowOutput.get("corpusItems"),
                        "workflowOutput", workflowOutput
                )),
                "DRAFT",
                completedAt
        );
        outputStore.save(output);
        return job;
    }

    private Map<String, Object> buildWorkflowInput(
            String templateId,
            String themeLibraryId,
            String topic,
            String audience,
            List<String> knowledgeBaseIds,
            Map<String, Object> externalCorpus,
            Map<String, Object> variables
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("templateId", templateId);
        input.put("themeLibraryId", themeLibraryId);
        input.put("baseUrl", "http://127.0.0.1:8080");
        input.put("topic", topic);
        input.put("audience", audience);
        input.put("knowledgeBaseIds", knowledgeBaseIds == null ? List.of() : knowledgeBaseIds);
        if (externalCorpus != null && !externalCorpus.isEmpty()) {
            input.put("externalCorpus", externalCorpus);
        }
        if (variables != null) {
            input.putAll(variables);
        }
        return input;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readSectionsFromWorkflow(Map<String, Object> workflowOutput, GenerationTemplate template) {
        Object templateSections = workflowOutput.get("templateSections");
        if (templateSections instanceof List<?> items && !items.isEmpty()) {
            List<Map<String, Object>> sections = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> section = new LinkedHashMap<>();
                    map.forEach((key, value) -> section.put(String.valueOf(key), value));
                    sections.add(section);
                }
            }
            if (!sections.isEmpty()) {
                return sections;
            }
        }
        return readSections(readMap(template.templateSchema()));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapWorkflowSectionOutputs(
            List<Map<String, Object>> sections,
            Object rawSectionOutputs
    ) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        List<?> loopOutputs = rawSectionOutputs instanceof List<?> items ? items : List.of();
        for (int index = 0; index < sections.size(); index++) {
            Map<String, Object> section = sections.get(index);
            String key = stringValue(section.get("key"), "section");
            String title = stringValue(section.get("title"), key);
            String contentMarkdown = "## " + title;
            if (index < loopOutputs.size() && loopOutputs.get(index) instanceof Map<?, ?> loopOutput) {
                contentMarkdown = stringValue(loopOutput.get("sectionMarkdown"), contentMarkdown);
            }
            outputs.add(Map.of(
                    "key", key,
                    "title", title,
                    "contentMarkdown", contentMarkdown,
                    "citations", List.of()
            ));
        }
        return outputs;
    }

    public List<GenerationJob> listJobs() {
        return jobStore.list(currentTenantId());
    }

    public GenerationJob getJob(String id) {
        GenerationJob job = jobStore.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Generation job not found: " + id));
        assertTenantAccessible(job.tenantId());
        return job;
    }

    public List<GenerationOutput> listOutputs(String jobId) {
        GenerationJob job = getJob(jobId);
        return outputStore.listByJobId(job.id());
    }

    public GenerationOutput getFirstOutput(String jobId) {
        List<GenerationOutput> outputs = listOutputs(jobId);
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("Generation output not found for job: " + jobId);
        }
        return outputs.get(0);
    }

    public GenerationOutput getOutput(String id) {
        GenerationOutput output = outputStore.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Generation output not found: " + id));
        assertTenantAccessible(output.tenantId());
        return output;
    }

    private GenerationTemplate getTemplate(String templateId) {
        GenerationTemplate template = templateStore.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Generation template not found: " + templateId));
        assertTenantAccessible(template.tenantId());
        return template;
    }

    private Map<String, Object> buildOutline(String topic, List<Map<String, Object>> sections) {
        List<Map<String, Object>> outlineSections = new ArrayList<>();
        for (Map<String, Object> section : sections) {
            outlineSections.add(Map.of(
                    "key", section.get("key"),
                    "title", section.get("title"),
                    "summary", "围绕" + topic + "撰写" + section.get("title")
            ));
        }
        return Map.of("topic", topic, "sections", outlineSections);
    }

    private List<Map<String, Object>> collectCitations(List<Map<String, Object>> sectionOutputs) {
        List<Map<String, Object>> citations = new ArrayList<>();
        for (Map<String, Object> sectionOutput : sectionOutputs) {
            Object sectionCitations = sectionOutput.get("citations");
            if (sectionCitations instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> map) {
                        Map<String, Object> citation = new LinkedHashMap<>();
                        map.forEach((key, value) -> citation.put(String.valueOf(key), value));
                        citations.add(citation);
                    }
                }
            }
        }
        return citations;
    }

    private String mergeMarkdown(List<Map<String, Object>> sectionOutputs) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, Object> sectionOutput : sectionOutputs) {
            builder.append(sectionOutput.get("contentMarkdown")).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String resolveThemeLibraryId(Map<String, Object> externalCorpus) {
        if (externalCorpus == null || externalCorpus.isEmpty()) {
            return "theme_001";
        }
        return stringValue(externalCorpus.get("id"), "theme_001");
    }

    private boolean isDocxOutput(String outputType) {
        return "DOCX".equalsIgnoreCase(outputType);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readSections(Map<String, Object> schema) {
        Object sections = schema.get("sections");
        if (sections instanceof List<?> items) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> section = new LinkedHashMap<>();
                    map.forEach((key, value) -> section.put(String.valueOf(key), value));
                    result.add(section);
                }
            }
            return result;
        }
        return List.of();
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read JSON.", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write JSON.", exception);
        }
    }

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
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
