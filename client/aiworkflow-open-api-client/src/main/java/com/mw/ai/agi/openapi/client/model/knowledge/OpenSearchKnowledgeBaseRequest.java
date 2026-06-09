package com.mw.ai.agi.openapi.client.model.knowledge;

import java.util.List;

public record OpenSearchKnowledgeBaseRequest(
        String query,
        int topK,
        String userId,
        String unitId,
        List<String> departmentIds,
        List<String> roleIds
) {
}
