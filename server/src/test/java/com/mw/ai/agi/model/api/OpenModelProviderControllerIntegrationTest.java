package com.mw.ai.agi.model.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:open-model-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false",
        "agi.auth.admin-password=admin123"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpenModelProviderControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsScopedModelProviderForIntegrationApp() throws Exception {
        String modelId = createModelProvider();
        String apiKey = createIntegrationAppWithModelScope("biz-model-a", modelId);

        mockMvc.perform(get("/api/open/model-providers")
                        .header("X-AGI-App-Code", "biz-model-a")
                        .header("X-AGI-Api-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(modelId))
                .andExpect(jsonPath("$.data.items[0].model").value("deepseek-chat"));

        mockMvc.perform(get("/api/open/model-providers/" + modelId)
                        .header("X-AGI-App-Code", "biz-model-a")
                        .header("X-AGI-Api-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(modelId));
    }

    @Test
    void rejectsModelOutsideAppScope() throws Exception {
        String allowedModelId = createModelProvider();
        String deniedModelId = createModelProvider();
        String apiKey = createIntegrationAppWithModelScope("biz-model-b", allowedModelId);

        mockMvc.perform(get("/api/open/model-providers/" + deniedModelId)
                        .header("X-AGI-App-Code", "biz-model-b")
                        .header("X-AGI-Api-Key", apiKey))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("APP_ASSET_SCOPE_DENIED"));
    }

    private String createModelProvider() throws Exception {
        String createResponse = mockMvc.perform(post("/api/model-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Open API Model",
                                  "modelType":"OpenAI",
                                  "modelUsage":"CHAT",
                                  "description":null,
                                  "visionSupport":false,
                                  "pricePerMillionTokens":1,
                                  "baseUrl":"https://api.example.com/v1",
                                  "model":"deepseek-chat",
                                  "apiKeyRef":"secret",
                                  "enabled":true
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extract(createResponse, "id");
    }

    private String createIntegrationAppWithModelScope(String appCode, String modelId) throws Exception {
        mockMvc.perform(post("/api/auth/admin/integration-apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Biz System","appType":"BUSINESS_SYSTEM","authType":"API_KEY"}
                                """.formatted(appCode)))
                .andExpect(status().isOk());
        String appId = "app_" + appCode.replace('-', '_');
        String secretResponse = mockMvc.perform(post("/api/auth/admin/integration-apps/" + appId + "/secrets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKey", containsString("agi_")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String apiKey = extract(secretResponse, "apiKey");
        mockMvc.perform(post("/api/auth/admin/integration-apps/" + appId + "/scopes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"MODEL_PROVIDER","scopeId":"%s","permission":"USE"}
                                """.formatted(modelId)))
                .andExpect(status().isOk());
        return apiKey;
    }

    private String extract(String json, String name) {
        String marker = "\"" + name + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
