package com.mw.ai.agi.generation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.generation.domain.GenerationTemplate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationTemplateRuntimeMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toRuntimeView_exposesSectionsVariablesAndOutline() {
        String schema = """
                {
                  "title": "测试模板",
                  "variables": [
                    {"name": "topic", "label": "主题", "type": "string", "required": true}
                  ],
                  "sections": [
                    {"key": "s1", "title": "第一章", "instruction": "写概述", "citationRequired": true}
                  ]
                }
                """;
        GenerationTemplate template = new GenerationTemplate(
                "tpl_1",
                "tenant_default",
                "测试模板",
                "test_v1",
                null,
                "RESEARCH",
                null,
                "MARKDOWN",
                schema,
                "wf_1",
                null,
                "ENABLED",
                1,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Map<String, Object> view = GenerationTemplateRuntimeMapper.toRuntimeView(template, objectMapper);

        assertThat(view.get("id")).isEqualTo("tpl_1");
        assertThat(view.get("sectionCount")).isEqualTo(1);
        assertThat(view.get("outlineSections")).isEqualTo("- 第一章");
        assertThat((List<?>) view.get("variables")).hasSize(1);
        assertThat((List<?>) view.get("sections")).hasSize(1);
    }
}
