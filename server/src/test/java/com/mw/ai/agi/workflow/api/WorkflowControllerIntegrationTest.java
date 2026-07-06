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
    void atomicallySavesMetadataAndDefinitionBeforePublishing() throws Exception {
        String workflowId = createWorkflow();

        mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Knowledge federation",
                                  "description": "Searches two knowledge bases",
                                  "definition": {
                                    "nodes": [
                                      { "id": "start", "type": "START", "name": "Start", "config": {} },
                                      {
                                        "id": "knowledge",
                                        "type": "KNOWLEDGE_RETRIEVAL",
                                        "name": "Knowledge",
                                        "config": {
                                          "knowledgeBaseIds": ["kb-standards", "kb-policies"],
                                          "queryText": "{{message}}",
                                          "topK": 5
                                        }
                                      },
                                      { "id": "end", "type": "END", "name": "End", "config": {} }
                                    ],
                                    "edges": [
                                      { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "knowledge", "condition": null },
                                      { "id": "edge-2", "sourceNodeId": "knowledge", "targetNodeId": "end", "condition": null }
                                    ],
                                    "variables": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Knowledge federation"))
                .andExpect(jsonPath("$.data.description").value("Searches two knowledge bases"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.latestVersion.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.latestVersion.definition.nodes[1].config.knowledgeBaseIds[0]")
                        .value("kb-standards"))
                .andExpect(jsonPath("$.data.latestVersion.definition.nodes[1].config.knowledgeBaseIds[1]")
                        .value("kb-policies"));
    }

    @Test
    void rejectsInvalidAtomicPublishBeforeChangingWorkflow() throws Exception {
        String workflowId = createWorkflow();

        mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Must not be saved",
                                  "description": "Invalid snapshot",
                                  "definition": {
                                    "nodes": [
                                      { "id": "start", "type": "START", "name": "Start", "config": {} },
                                      { "id": "end", "type": "END", "name": "End", "config": {} }
                                    ],
                                    "edges": [
                                      { "id": "broken", "sourceNodeId": "start", "targetNodeId": "missing", "condition": null }
                                    ],
                                    "variables": []
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_WORKFLOW_DAG"));

        mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Support triage"))
                .andExpect(jsonPath("$.data.description").isEmpty())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.currentVersionId").isEmpty())
                .andExpect(jsonPath("$.data.latestVersion.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.latestVersion.definition.edges[0].targetNodeId").value("end"));
    }

    @Test
    void rejectsAtomicPublishingForArchivedWorkflow() throws Exception {
        String workflowId = createWorkflow();
        mockMvc.perform(post("/api/workflows/{workflowId}/archive", workflowId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Archived",
                                  "description": "Must not be saved",
                                  "definition": {
                                    "nodes": [
                                      { "id": "start", "type": "START", "name": "Start", "config": {} },
                                      { "id": "end", "type": "END", "name": "End", "config": {} }
                                    ],
                                    "edges": [
                                      { "id": "new-edge", "sourceNodeId": "start", "targetNodeId": "end", "condition": null }
                                    ],
                                    "variables": []
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Support triage"))
                .andExpect(jsonPath("$.data.description").isEmpty())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.latestVersion.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.latestVersion.definition.edges[0].id").value("edge-1"));
    }

    @Test
    void restoresArchivedPublishedWorkflow() throws Exception {
        String workflowId = createWorkflow();
        mockMvc.perform(post("/api/workflows/{workflowId}/publish", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/workflows/{workflowId}/archive", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(post("/api/workflows/{workflowId}/restore", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.currentVersionId").isNotEmpty());
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
