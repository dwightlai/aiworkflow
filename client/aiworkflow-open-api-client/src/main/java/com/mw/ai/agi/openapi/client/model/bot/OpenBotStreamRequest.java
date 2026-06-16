package com.mw.ai.agi.openapi.client.model.bot;

import java.util.Map;

public record OpenBotStreamRequest(
        String message,
        String userId,
        String unitId,
        java.util.List<String> departmentIds,
        java.util.List<String> roleIds,
        Map<String, Object> input
) {
}
