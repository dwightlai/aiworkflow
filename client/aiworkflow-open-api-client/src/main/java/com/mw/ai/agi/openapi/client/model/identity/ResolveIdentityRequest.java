package com.mw.ai.agi.openapi.client.model.identity;

import java.util.List;

public record ResolveIdentityRequest(
        String unitId,
        List<String> departmentIds,
        List<String> roleIds,
        String userId
) {
}
