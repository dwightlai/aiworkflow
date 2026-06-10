package com.mw.ai.agi.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ResearchWorkflowDefinitionBuilderTest {
    @Test
    void buildsRunnableResearchWorkflowDefinition() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = ResearchWorkflowDefinitionBuilder.buildDefinitionJson(objectMapper);
        System.out.println(json);
        org.assertj.core.api.Assertions.assertThat(json).contains("HTTP 读取专题编研成果模板");
        org.assertj.core.api.Assertions.assertThat(json).contains("/api/generation-templates/{{templateId}}/runtime");
        org.assertj.core.api.Assertions.assertThat(ResearchWorkflowDefinitionBuilder.build().nodes()).hasSize(8);
    }
}
