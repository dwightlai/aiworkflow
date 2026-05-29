package com.aiworkflow.model.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
class ModelProviderControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsModelProviders() throws Exception {
        mockMvc.perform(post("/api/model-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"OpenAI Compatible","baseUrl":"https://api.example.com/v1","model":"gpt-4.1-mini","apiKeyRef":"dev-key","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("OpenAI Compatible"))
                .andExpect(jsonPath("$.data.model").value("gpt-4.1-mini"));

        mockMvc.perform(get("/api/model-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].baseUrl").value("https://api.example.com/v1"))
                .andExpect(jsonPath("$.data.items[0].model").value("gpt-4.1-mini"));
    }
}
