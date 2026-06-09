package com.mw.ai.agi.openapi.client.feign;

import com.mw.ai.agi.openapi.client.model.ApiResponse;
import com.mw.ai.agi.openapi.client.model.PageResponse;
import com.mw.ai.agi.openapi.client.model.knowledge.KnowledgeSearchResult;
import com.mw.ai.agi.openapi.client.model.knowledge.OpenKnowledgeBaseView;
import com.mw.ai.agi.openapi.client.model.knowledge.OpenSearchKnowledgeBaseRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        contextId = "agiOpenKnowledgeBaseClient",
        name = "${agi.openapi.service-name:aiworkflow-server}",
        url = "${agi.openapi.base-url:}"
)
public interface AgiOpenKnowledgeBaseClient {

    @GetMapping("/api/open/knowledge-bases")
    ApiResponse<PageResponse<OpenKnowledgeBaseView>> listKnowledgeBases();

    @GetMapping("/api/open/knowledge-bases/{id}")
    ApiResponse<OpenKnowledgeBaseView> getKnowledgeBase(@PathVariable("id") String id);

    @PostMapping("/api/open/knowledge-bases/{id}/search")
    ApiResponse<List<KnowledgeSearchResult>> searchKnowledgeBase(
            @PathVariable("id") String id,
            @RequestBody OpenSearchKnowledgeBaseRequest request
    );
}
