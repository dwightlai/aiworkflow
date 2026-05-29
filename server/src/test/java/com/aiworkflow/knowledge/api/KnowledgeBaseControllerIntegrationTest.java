package com.aiworkflow.knowledge.api;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class KnowledgeBaseControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsKnowledgeBaseAndIngestsDocuments() throws Exception {
        String response = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"产品手册","description":"客服知识库"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("产品手册"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String knowledgeBaseId = new ObjectMapper().readTree(response).path("data").path("id").asText();

        mockMvc.perform(post("/api/knowledge-bases/{id}/documents", knowledgeBaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"退换货政策.txt","content":"七天无理由退货。质量问题支持免费换货。"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chunkCount").value(1));

        mockMvc.perform(get("/api/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].documentCount").value(1))
                .andExpect(jsonPath("$.data.items[0].chunkCount").value(1));
    }

    @Test
    void searchesKnowledgeBaseChunks() throws Exception {
        String response = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"售后知识库","description":null}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String knowledgeBaseId = new ObjectMapper().readTree(response).path("data").path("id").asText();

        mockMvc.perform(post("/api/knowledge-bases/{id}/documents", knowledgeBaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"policy.txt","content":"发票可以在订单完成后七日内申请。会员积分不可兑换现金。"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/knowledge-bases/{id}/search", knowledgeBaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"发票申请","topK":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].documentName").value("policy.txt"))
                .andExpect(jsonPath("$.data[0].content").value("发票可以在订单完成后七日内申请。会员积分不可兑换现金。"));
    }
}
