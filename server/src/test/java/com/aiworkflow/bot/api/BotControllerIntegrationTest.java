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

    @Test
    void chatsWithBotAcrossMultipleTurnsAndStoresHistory() throws Exception {
        String workflowId = createAndPublishWorkflow();
        String botId = createBot(workflowId);

        String firstResponse = mockMvc.perform(post("/api/bots/{botId}/chat", botId)
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
                .andExpect(jsonPath("$.data.session.botId").value(botId))
                .andExpect(jsonPath("$.data.reply.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.reply.content").value("Hello Ada"))
                .andExpect(jsonPath("$.data.messages.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionId = objectMapper.readTree(firstResponse).path("data").path("session").path("id").asText();

        mockMvc.perform(post("/api/bots/{botId}/chat", botId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "%s",
                                  "message": "Grace",
                                  "input": {}
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session.id").value(sessionId))
                .andExpect(jsonPath("$.data.reply.content").value("Hello Grace"))
                .andExpect(jsonPath("$.data.messages.length()").value(4))
                .andExpect(jsonPath("$.data.execution.input.history[0].role").value("USER"))
                .andExpect(jsonPath("$.data.execution.input.history[1].content").value("Hello Ada"));

        mockMvc.perform(get("/api/bots/{botId}/sessions", botId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].messageCount").value(4));

        mockMvc.perform(get("/api/bots/{botId}/sessions/{sessionId}/messages", botId, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.items[0].content").value("Ada"))
                .andExpect(jsonPath("$.data.items[3].content").value("Hello Grace"));
    }

    @Test
    void createsAndChatsWithDirectModelBotWithoutWorkflow() throws Exception {
        String modelProviderId = createModelProvider();
        String createResponse = mockMvc.perform(post("/api/bots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Direct Model Bot",
                                  "description": "No workflow required",
                                  "avatar": "robot",
                                  "workflowId": null,
                                  "modelProviderId": "%s",
                                  "knowledgeBaseId": null,
                                  "systemPrompt": "Be helpful.",
                                  "openingMessage": "Hello.",
                                  "status": "ENABLED"
                                }
                                """.formatted(modelProviderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Direct Model Bot"))
                .andExpect(jsonPath("$.data.modelProviderId").value(modelProviderId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String botId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        mockMvc.perform(post("/api/bots/{botId}/chat", botId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Hello",
                                  "input": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply.content").value("model response"))
                .andExpect(jsonPath("$.data.execution.workflowId").value("bot:" + botId))
                .andExpect(jsonPath("$.data.execution.output.answer").value("model response"))
                .andExpect(jsonPath("$.data.execution.output.mode").value("DIRECT_BOT"));
    }

    private String createBot(String workflowId) throws Exception {
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
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(createResponse).path("data").path("id").asText();
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

    private String createModelProvider() throws Exception {
        String createResponse = mockMvc.perform(post("/api/model-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Smoke Model",
                                  "modelType": "DeepSeek",
                                  "modelUsage": "CHAT",
                                  "description": null,
                                  "visionSupport": false,
                                  "pricePerMillionTokens": 1,
                                  "baseUrl": "https://api.example.com/v1",
                                  "model": "deepseek-chat",
                                  "apiKeyRef": "dev-key",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(createResponse).path("data").path("id").asText();
    }
}
