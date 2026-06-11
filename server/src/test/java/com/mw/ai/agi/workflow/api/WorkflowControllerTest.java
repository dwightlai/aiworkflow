package com.mw.ai.agi.workflow.api;

import com.mw.ai.agi.workflow.domain.Workflow;
import com.mw.ai.agi.workflow.domain.WorkflowStatus;
import com.mw.ai.agi.workflow.service.WorkflowApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkflowControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowApplicationService workflowService;

    @Test
    void listWorkflowsReturnsEmptyPageEnvelope() throws Exception {
        when(workflowService.listWorkflows()).thenReturn(List.of());

        mockMvc.perform(get("/api/workflows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void updatesWorkflowMetadata() throws Exception {
        Instant now = Instant.parse("2026-06-06T01:00:00Z");
        Workflow workflow = new Workflow(
                "workflow-1",
                "tenant-default",
                null,
                "售后处理流程",
                "售后自动化",
                WorkflowStatus.DRAFT,
                null,
                "system",
                "system",
                now,
                now
        );
        when(workflowService.updateWorkflowMetadata("workflow-1", "售后处理流程", "售后自动化"))
                .thenReturn(workflow);
        when(workflowService.listVersions("workflow-1")).thenReturn(List.of());

        mockMvc.perform(put("/api/workflows/{workflowId}/metadata", "workflow-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "售后处理流程",
                                  "description": "售后自动化"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("售后处理流程"))
                .andExpect(jsonPath("$.data.description").value("售后自动化"));
    }
}
