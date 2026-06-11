package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.generation.api.ResearchApiMapper;
import com.mw.ai.agi.generation.domain.GenerationJob;
import com.mw.ai.agi.generation.domain.GenerationOutput;
import com.mw.ai.agi.generation.domain.GenerationTemplate;
import com.mw.ai.agi.workflow.engine.WorkflowExecution;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionRequest;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionResult;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionService;
import com.mw.ai.agi.workflow.engine.WorkflowExecutionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResearchGenerationServiceTest {
    private InMemoryGenerationTemplateStore templateStore;
    private ResearchGenerationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TenantContext.set("tenant_default");
        objectMapper = new ObjectMapper();
        templateStore = new InMemoryGenerationTemplateStore();
        service = new ResearchGenerationService(
                templateStore,
                new InMemoryGenerationJobStore(),
                new InMemoryGenerationOutputStore(),
                objectMapper,
                null,
                stubWorkflowExecutionService(),
                new ResearchDocxExporter(),
                new ResearchOutputFileStorage("./target/test-outputs")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void runsResearchPipelineAndPersistsOutput() {
        GenerationJob job = service.run(
                null,
                "template_research_001",
                "unit_default",
                "user_admin",
                List.of("kb_standard"),
                Map.of("id", "theme_001", "type", "ARCHIVE_THEME_LIBRARY"),
                Map.of("topic", "电子文件归档", "audience", "档案员")
        );

        assertThat(job.status()).isEqualTo("COMPLETED");
        assertThat(job.outlineJson()).contains("背景概述");

        ResearchApiMapper.ResearchJobView view = ResearchApiMapper.toJobView(job, objectMapper);
        assertThat(view.outline()).hasSize(3);
        assertThat(view.sectionOutputs()).hasSize(3);
        assertThat(view.sectionOutputs().get(0)).containsKey("key");

        GenerationOutput output = service.getFirstOutput(job.id());
        assertThat(output.contentMarkdown()).contains("背景概述");
    }

    @Test
    void exportsDocxWhenTemplateOutputTypeIsDocx() {
        GenerationTemplate template = templateStore.findById("template_research_001").orElseThrow();
        templateStore.save(new GenerationTemplate(
                template.id(),
                template.tenantId(),
                template.name(),
                template.code(),
                template.description(),
                template.category(),
                template.ownerUnitId(),
                "DOCX",
                template.templateSchema(),
                template.workflowId(),
                template.workflowSnapshot(),
                template.status(),
                template.version(),
                template.createdBy(),
                template.updatedBy(),
                template.createdAt(),
                Instant.now()
        ));

        GenerationJob job = service.run(
                null,
                "template_research_001",
                "unit_default",
                "user_admin",
                List.of("kb_standard"),
                Map.of("id", "theme_001", "type", "ARCHIVE_THEME_LIBRARY"),
                Map.of("topic", "档案数字化", "audience", "档案员")
        );

        GenerationOutput output = service.getFirstOutput(job.id());
        assertThat(output.outputType()).isEqualToIgnoringCase("DOCX");
        assertThat(output.contentDocxPath()).isNotBlank();
        ResearchApiMapper.GenerationOutputView view = ResearchApiMapper.toOutputView(output, objectMapper);
        assertThat(view.hasDocx()).isTrue();
    }

    private WorkflowExecutionService stubWorkflowExecutionService() {
        WorkflowExecutionService workflowExecutionService = mock(WorkflowExecutionService.class);
        when(workflowExecutionService.runWorkflow(ArgumentMatchers.any())).thenAnswer(invocation -> {
            WorkflowExecutionRequest request = invocation.getArgument(0);
            String templateId = String.valueOf(request.input().get("templateId"));
            GenerationTemplate template = templateStore.findById(templateId).orElseThrow();
            List<Map<String, Object>> sections = readSections(template.templateSchema());
            List<Map<String, Object>> sectionOutputs = new ArrayList<>();
            for (Map<String, Object> section : sections) {
                sectionOutputs.add(Map.of(
                        "sectionMarkdown",
                        "## " + section.get("title") + "\n\n" + section.get("instruction")
                ));
            }
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("topic", request.input().get("topic"));
            output.put("outlineText", "# 编研大纲");
            output.put("templateSections", sections);
            output.put("sectionOutputs", sectionOutputs);
            output.put("outputType", template.outputType());
            output.put("corpusItems", List.of());
            WorkflowExecution execution = new WorkflowExecution(
                    "exec_test",
                    request.workflowId(),
                    "ver_test",
                    WorkflowExecutionStatus.SUCCEEDED,
                    request.input(),
                    output,
                    null,
                    Instant.now(),
                    Instant.now()
            );
            return new WorkflowExecutionResult(execution, List.of());
        });
        return workflowExecutionService;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readSections(String schemaJson) {
        try {
            Map<String, Object> schema = objectMapper.readValue(schemaJson, new TypeReference<>() {
            });
            Object sections = schema.get("sections");
            if (!(sections instanceof List<?> items)) {
                return List.of();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> section = new LinkedHashMap<>();
                    map.forEach((key, value) -> section.put(String.valueOf(key), value));
                    result.add(section);
                }
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
