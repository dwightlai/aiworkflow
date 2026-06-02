package com.aiworkflow.workflow.api;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
class WorkflowRunControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void runsPublishedWorkflow() throws Exception {
        String workflowId = createAndPublishWorkflow();

        String response = mockMvc.perform(post("/api/workflows/{workflowId}/runs", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input": {
                                    "name": "Ada"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.output.message").value("Hello Ada"))
                .andExpect(jsonPath("$.data.nodeExecutions[0].nodeId").value("start"))
                .andExpect(jsonPath("$.data.nodeExecutions[1].nodeId").value("transform"))
                .andExpect(jsonPath("$.data.nodeExecutions[2].nodeId").value("end"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(response).path("data").path("id").asText()).isNotBlank();
    }

    @Test
    void getsWorkflowExecutionDetail() throws Exception {
        String workflowId = createAndPublishWorkflow();
        String runResponse = mockMvc.perform(post("/api/workflows/{workflowId}/runs", workflowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input": {
                                    "name": "Grace"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode runBody = objectMapper.readTree(runResponse);
        String executionId = runBody.path("data").path("id").asText();

        mockMvc.perform(get("/api/workflow-runs/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(executionId))
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.output.message").value("Hello Grace"))
                .andExpect(jsonPath("$.data.nodeExecutions.length()").value(3));
    }

    @Test
    void returnsNotFoundForMissingExecution() throws Exception {
        mockMvc.perform(get("/api/workflow-runs/{executionId}", "missing-execution"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WORKFLOW_RUN_NOT_FOUND"));
    }

    private String createAndPublishWorkflow() throws Exception {
        String createResponse = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Greeting workflow",
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
                                          "template": "Hello {{name}}"
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
