package com.mw.ai.agi.openapi.client.model.bot;

import java.util.List;
import java.util.Map;

public record OpenBotRunRequest(
        String message,
        String userId,
        String unitId,
        List<String> departmentIds,
        List<String> roleIds,
        Map<String, Object> input
) {
}
