package com.aiworkflow.knowledge.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    @Test
    void managesVectorStoreConfigsAndKnowledgeBaseRetrievalSettings() throws Exception {
        mockMvc.perform(post("/api/vector-store-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"local-memory","storeType":"MEMORY","endpoint":"","indexName":"aiworkflow_kb","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("local-memory"))
                .andExpect(jsonPath("$.data.storeType").value("MEMORY"));

        mockMvc.perform(get("/api/vector-store-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].indexName").value("aiworkflow_kb"));

        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"support-kb","description":"support docs","embeddingModelId":"model_embed","vectorStoreConfigId":"vector_1","splitterType":"MARKDOWN_HEADING","chunkSize":160,"chunkOverlap":20,"retrievalMode":"HYBRID","topK":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.embeddingModelId").value("model_embed"))
                .andExpect(jsonPath("$.data.vectorStoreConfigId").value("vector_1"))
                .andExpect(jsonPath("$.data.splitterType").value("MARKDOWN_HEADING"))
                .andExpect(jsonPath("$.data.retrievalMode").value("HYBRID"))
                .andExpect(jsonPath("$.data.topK").value(5));
    }

    @Test
    void previewsAndListsDocumentChunksBeforeIngestion() throws Exception {
        mockMvc.perform(post("/api/knowledge-bases/chunks/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"# Refund\\nRefund requests are handled within seven days.\\n\\n# Invoice\\nInvoices can be downloaded after payment.","splitterType":"MARKDOWN_HEADING","chunkSize":80,"chunkOverlap":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("# Refund\nRefund requests are handled within seven days."))
                .andExpect(jsonPath("$.data[1].content").value("# Invoice\nInvoices can be downloaded after payment."))
                .andExpect(jsonPath("$.data[0].tokenEstimate").isNumber());

        String response = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"chunk-kb","description":null}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String knowledgeBaseId = new ObjectMapper().readTree(response).path("data").path("id").asText();
        String documentResponse = mockMvc.perform(post("/api/knowledge-bases/{id}/documents", knowledgeBaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"kb.md","content":"# Refund\\nRefund requests are handled within seven days.","splitterType":"MARKDOWN_HEADING","chunkSize":80,"chunkOverlap":10}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String documentId = new ObjectMapper().readTree(documentResponse).path("data").path("id").asText();

        mockMvc.perform(get("/api/knowledge-bases/{id}/documents/{documentId}/chunks", knowledgeBaseId, documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].documentId").value(documentId))
                .andExpect(jsonPath("$.data.items[0].enabled").value(true))
                .andExpect(jsonPath("$.data.items[0].tokenEstimate").isNumber());
    }

    @Test
    void updatesChunksAndDeletesDocuments() throws Exception {
        String response = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"editable-kb","description":null}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String knowledgeBaseId = new ObjectMapper().readTree(response).path("data").path("id").asText();

        String documentResponse = mockMvc.perform(post("/api/knowledge-bases/{id}/documents", knowledgeBaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"faq.txt","content":"Refund requests are handled within seven days."}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentId = new ObjectMapper().readTree(documentResponse).path("data").path("id").asText();

        String chunksResponse = mockMvc.perform(get("/api/knowledge-bases/{id}/documents/{documentId}/chunks", knowledgeBaseId, documentId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String chunkId = new ObjectMapper().readTree(chunksResponse).path("data").path("items").path(0).path("id").asText();

        mockMvc.perform(put("/api/knowledge-bases/{id}/chunks/{chunkId}", knowledgeBaseId, chunkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Refund requests are handled within five days.","enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Refund requests are handled within five days."))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(post("/api/knowledge-bases/{id}/search", knowledgeBaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"Refund","topK":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(delete("/api/knowledge-bases/{id}/documents/{documentId}", knowledgeBaseId, documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/knowledge-bases/{id}/documents", knowledgeBaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].documentCount").value(0))
                .andExpect(jsonPath("$.data.items[0].chunkCount").value(0));
    }

    @Test
    void updatesVectorStoreConfigs() throws Exception {
        String response = mockMvc.perform(post("/api/vector-store-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Elastic dev","storeType":"ELASTICSEARCH","endpoint":"http://localhost:9200","indexName":"kb_dev","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String vectorStoreId = new ObjectMapper().readTree(response).path("data").path("id").asText();

        mockMvc.perform(put("/api/vector-store-configs/{id}", vectorStoreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Elastic prod","storeType":"ELASTICSEARCH","endpoint":"https://es.example.com","indexName":"kb_prod","enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Elastic prod"))
                .andExpect(jsonPath("$.data.endpoint").value("https://es.example.com"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void updatesAndDeletesKnowledgeBases() throws Exception {
        String response = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ops-kb","description":"draft docs","splitterType":"SIMPLE_TEXT","chunkSize":300,"chunkOverlap":20,"retrievalMode":"KEYWORD","topK":3}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String knowledgeBaseId = new ObjectMapper().readTree(response).path("data").path("id").asText();

        mockMvc.perform(post("/api/knowledge-bases/{id}/documents", knowledgeBaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"runbook.txt","content":"Restart service after checking health probes."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/knowledge-bases/{id}", knowledgeBaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ops-kb-prod","description":"production docs","embeddingModelId":"embed-prod","vectorStoreConfigId":"vector-prod","splitterType":"MARKDOWN_HEADING","chunkSize":180,"chunkOverlap":30,"retrievalMode":"HYBRID","topK":6}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("ops-kb-prod"))
                .andExpect(jsonPath("$.data.description").value("production docs"))
                .andExpect(jsonPath("$.data.embeddingModelId").value("embed-prod"))
                .andExpect(jsonPath("$.data.vectorStoreConfigId").value("vector-prod"))
                .andExpect(jsonPath("$.data.splitterType").value("MARKDOWN_HEADING"))
                .andExpect(jsonPath("$.data.retrievalMode").value("HYBRID"))
                .andExpect(jsonPath("$.data.topK").value(6))
                .andExpect(jsonPath("$.data.documentCount").value(1))
                .andExpect(jsonPath("$.data.chunkCount").value(1));

        mockMvc.perform(delete("/api/knowledge-bases/{id}", knowledgeBaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
