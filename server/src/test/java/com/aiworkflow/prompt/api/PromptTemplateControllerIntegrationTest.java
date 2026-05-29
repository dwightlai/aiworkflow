package com.aiworkflow.prompt.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
class PromptTemplateControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsPromptTemplates() throws Exception {
        mockMvc.perform(post("/api/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Greeting","template":"Hello {{name}}","description":"Greeting prompt"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Greeting"));

        mockMvc.perform(get("/api/prompts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].template").value("Hello {{name}}"));
    }

    @Test
    void updatesPromptTemplates() throws Exception {
        String response = mockMvc.perform(post("/api/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Classifier","template":"Classify {{text}}","description":"Classifier prompt"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String promptId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response)
                .path("data")
                .path("id")
                .asText();

        mockMvc.perform(put("/api/prompts/{id}", promptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Classifier v2","template":"Classify carefully: {{text}}","description":"Updated prompt"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Classifier v2"))
                .andExpect(jsonPath("$.data.template").value("Classify carefully: {{text}}"));
    }
}
