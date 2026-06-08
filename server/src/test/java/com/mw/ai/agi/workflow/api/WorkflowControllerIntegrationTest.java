package com.mw.ai.agi.workflow.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
class WorkflowControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndFetchesWorkflow() throws Exception {
        String createBody = """
                {
                  "name": "Support triage",
                  "description": "Routes incoming messages",
                  "definition": {
                    "nodes": [
                      { "id": "start", "type": "START", "name": "Start", "config": {} },
                      { "id": "transform", "type": "TEXT_TRANSFORM", "name": "Transform", "config": {} },
                      { "id": "end", "type": "END", "name": "End", "config": {} }
                    ],
                    "edges": [
                      { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "transform", "condition": null },
                      { "id": "edge-2", "sourceNodeId": "transform", "targetNodeId": "end", "condition": null }
                    ],
                    "variables": []
                  }
                }
                """;

        String response = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Support triage"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.latestVersion.version").value(1))
                .andExpect(jsonPath("$.data.latestVersion.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        String workflowId = body.path("data").path("id").asText();

        assertThat(workflowId).isNotBlank();

        mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(workflowId))
                .andExpect(jsonPath("$.data.latestVersion.definition.nodes[0].id").value("start"));
    }

    @Test
    void rejectsInvalidDagOnUpdate() throws Exception {
        String workflowId = createWorkflow();
        String invalidBody = """
                {
                  "definition": {
                    "nodes": [
                      { "id": "start", "type": "START", "name": "Start", "config": {} },
                      { "id": "end", "type": "END", "name": "End", "config": {} }
                    ],
                    "edges": [
                      { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "missing", "condition": null }
                    ],
                    "variables": []
                  }
                }
                """;

        mockMvc.perform(put("/api/workflows/{workflowId}/draft", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_WORKFLOW_DAG"))
                .andExpect(jsonPath("$.error.message").value("Workflow edge references missing node."));
    }

    @Test
    void updatesWorkflowMetadata() throws Exception {
        String workflowId = createWorkflow();

        mockMvc.perform(put("/api/workflows/{workflowId}/metadata", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "客服意图识别",
                                  "description": "识别用户咨询意图"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("客服意图识别"))
                .andExpect(jsonPath("$.data.description").value("识别用户咨询意图"))
                .andExpect(jsonPath("$.data.latestVersion.version").value(1));
    }

    @Test
    void publishesWorkflowDraft() throws Exception {
        String workflowId = createWorkflow();

        mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.currentVersionId").isNotEmpty())
                .andExpect(jsonPath("$.data.latestVersion.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.latestVersion.publishedBy").value("system"));
    }

    @Test
    void archivesWorkflowWithoutDeletingVersions() throws Exception {
        String workflowId = createWorkflow();

        mockMvc.perform(post("/api/workflows/{workflowId}/archive", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.latestVersion.version").value(1));

        mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.latestVersion.status").value("DRAFT"));
    }

    @Test
    void softDeletesWorkflowAndHidesItFromList() throws Exception {
        String workflowId = createWorkflow();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/workflows/{workflowId}", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/workflows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id=='" + workflowId + "')]").isEmpty());
    }

    private String createWorkflow() throws Exception {
        String response = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Support triage",
                                  "description": null,
                                  "definition": {
                                    "nodes": [
                                      { "id": "start", "type": "START", "name": "Start", "config": {} },
                                      { "id": "end", "type": "END", "name": "End", "config": {} }
                                    ],
                                    "edges": [
                                      { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "end", "condition": null }
                                    ],
                                    "variables": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
