package com.aiworkflow.bot.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BotControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void managesBotsAndRunsBoundWorkflow() throws Exception {
        String workflowId = createAndPublishWorkflow();

        String createResponse = mockMvc.perform(post("/api/bots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Support Bot",
                                  "description": "Answers support questions",
                                  "avatar": "robot",
                                  "workflowId": "%s",
                                  "modelProviderId": "model_chat",
                                  "knowledgeBaseId": "kb_support",
                                  "systemPrompt": "Use the support handbook.",
                                  "openingMessage": "Hi, how can I help?",
                                  "status": "ENABLED"
                                }
                                """.formatted(workflowId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Support Bot"))
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.conversationCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String botId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        mockMvc.perform(get("/api/bots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(botId))
                .andExpect(jsonPath("$.data.items[0].openingMessage").value("Hi, how can I help?"));

        mockMvc.perform(post("/api/bots/{botId}/run", botId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Ada",
                                  "input": {
                                    "channel": "web"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bot.id").value(botId))
                .andExpect(jsonPath("$.data.bot.conversationCount").value(1))
                .andExpect(jsonPath("$.data.execution.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.execution.output.message").value("Hello Ada"))
                .andExpect(jsonPath("$.data.execution.input.botName").value("Support Bot"))
                .andExpect(jsonPath("$.data.execution.input.knowledgeBaseId").value("kb_support"));

        mockMvc.perform(put("/api/bots/{botId}", botId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Support Bot Pro",
                                  "description": "Production bot",
                                  "avatar": "assistant",
                                  "workflowId": "%s",
                                  "modelProviderId": "model_chat",
                                  "knowledgeBaseId": null,
                                  "systemPrompt": "Be concise.",
                                  "openingMessage": "Ask me anything.",
                                  "status": "DISABLED"
                                }
                                """.formatted(workflowId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Support Bot Pro"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.conversationCount").value(1));

        mockMvc.perform(delete("/api/bots/{botId}", botId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/bots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    private String createAndPublishWorkflow() throws Exception {
        String createResponse = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bot greeting workflow",
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
        String workflowId = objectMapper.readTree(createResponse).path("data").path("id").asText();
        mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId))
                .andExpect(status().isOk());
        return workflowId;
    }
}
