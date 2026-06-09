package com.mw.ai.agi.openapi.client.model.identity;

import java.util.List;

public record RuntimeIdentityContext(
        String tenantId,
        String appId,
        String userId,
        List<String> unitIds,
        String activeUnitId,
        List<String> departmentIds,
        List<String> roleIds,
        String authType,
        String source
) {
}
