package com.mw.ai.agi.openapi.client.model.workflow;

import java.util.List;
import java.util.Map;

public record OpenRunWorkflowRequest(
        Map<String, Object> input,
        String userId,
        String unitId,
        List<String> departmentIds,
        List<String> roleIds
) {
}
