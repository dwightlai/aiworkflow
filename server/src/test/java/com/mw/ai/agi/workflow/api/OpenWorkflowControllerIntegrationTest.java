package com.mw.ai.agi.workflow.api;

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
        "spring.datasource.url=jdbc:h2:mem:open-workflow-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false",
        "agi.auth.admin-password=admin123"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpenWorkflowControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAndRunsScopedWorkflowForIntegrationApp() throws Exception {
        String workflowId = createPublishedWorkflow();
        String apiKey = createIntegrationAppWithWorkflowScope("biz-workflow-a", workflowId);

        mockMvc.perform(get("/api/open/workflows")
                        .header("X-AGI-App-Code", "biz-workflow-a")
                        .header("X-AGI-Api-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(workflowId));

        mockMvc.perform(post("/api/open/workflows/" + workflowId + "/runs")
                        .header("X-AGI-App-Code", "biz-workflow-a")
                        .header("X-AGI-Api-Key", apiKey)
                        .header("X-AGI-User-Id", "biz-user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"input":{"message":"hello"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }

    @Test
    void rejectsWorkflowOutsideAppScope() throws Exception {
        String allowedWorkflowId = createPublishedWorkflow();
        String deniedWorkflowId = createPublishedWorkflow();
        String apiKey = createIntegrationAppWithWorkflowScope("biz-workflow-b", allowedWorkflowId);

        mockMvc.perform(post("/api/open/workflows/" + deniedWorkflowId + "/runs")
                        .header("X-AGI-App-Code", "biz-workflow-b")
                        .header("X-AGI-Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("APP_ASSET_SCOPE_DENIED"));
    }

    private String createPublishedWorkflow() throws Exception {
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

    private String createIntegrationAppWithWorkflowScope(String appCode, String workflowId) throws Exception {
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
                                {"scopeType":"WORKFLOW","scopeId":"%s","permission":"USE"}
                                """.formatted(workflowId)))
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
