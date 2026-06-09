package com.mw.ai.agi.bot.api;

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
        "spring.datasource.url=jdbc:h2:mem:open-bot-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false",
        "agi.auth.admin-password=admin123"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpenBotControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAndRunsScopedBotForIntegrationApp() throws Exception {
        String workflowId = createWorkflow();
        String botId = createBot(workflowId);
        String apiKey = createIntegrationAppWithBotScope("biz-system-a", botId);

        mockMvc.perform(get("/api/open/bots")
                        .header("X-AGI-App-Code", "biz-system-a")
                        .header("X-AGI-Api-Key", apiKey)
                        .header("X-AGI-User-Id", "biz-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(botId));

        mockMvc.perform(get("/api/open/bots/" + botId)
                        .header("X-AGI-App-Code", "biz-system-a")
                        .header("X-AGI-Api-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(botId));

        mockMvc.perform(post("/api/open/bots/" + botId + "/run")
                        .header("X-AGI-App-Code", "biz-system-a")
                        .header("X-AGI-Api-Key", apiKey)
                        .header("X-AGI-User-Id", "biz-user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hello","input":{"channel":"biz"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bot.id").value(botId));
    }

    @Test
    void rejectsBotOutsideAppScope() throws Exception {
        String workflowId = createWorkflow();
        String allowedBotId = createBot(workflowId);
        String deniedBotId = createBot(workflowId);
        String apiKey = createIntegrationAppWithBotScope("biz-system-b", allowedBotId);

        mockMvc.perform(post("/api/open/bots/" + deniedBotId + "/run")
                        .header("X-AGI-App-Code", "biz-system-b")
                        .header("X-AGI-Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hello"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("APP_ASSET_SCOPE_DENIED"));
    }

    private String createWorkflow() throws Exception {
        String createResponse = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Open API Workflow",
                                  "description": null,
                                  "definition": {
                                    "nodes": [
                                      { "id": "start", "type": "START", "name": "Start", "config": {} },
                                      {
                                        "id": "transform",
                                        "type": "TEXT_TRANSFORM",
                                        "name": "Transform",
                                        "config": {
                                          "outputKey": "message",
                                          "template": "Hello {{message}}"
                                        }
                                      },
                                      {
                                        "id": "end",
                                        "type": "END",
                                        "name": "End",
                                        "config": {
                                          "outputKeys": ["message"]
                                        }
                                      }
                                    ],
                                    "edges": [
                                      { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "transform", "condition": null },
                                      { "id": "edge-2", "sourceNodeId": "transform", "targetNodeId": "end", "condition": null }
                                    ],
                                    "variables": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String workflowId = extract(createResponse, "id");
        mockMvc.perform(post("/api/workflows/" + workflowId + "/publish"))
                .andExpect(status().isOk());
        return workflowId;
    }

    private String createBot(String workflowId) throws Exception {
        String createResponse = mockMvc.perform(post("/api/bots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Open API Bot",
                                  "workflowId":"%s",
                                  "status":"ENABLED"
                                }
                                """.formatted(workflowId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extract(createResponse, "id");
    }

    private String createIntegrationAppWithBotScope(String appCode, String botId) throws Exception {
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
                                {"scopeType":"BOT","scopeId":"%s","permission":"USE"}
                                """.formatted(botId)))
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
