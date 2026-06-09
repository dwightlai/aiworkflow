package com.mw.ai.agi.openapi.client;

import java.util.List;

public record AgiOpenApiRequestContext(
        String userId,
        String unitId,
        List<String> departmentIds,
        List<String> roleIds
) {
    public static AgiOpenApiRequestContext of(String userId, String unitId, List<String> departmentIds, List<String> roleIds) {
        return new AgiOpenApiRequestContext(userId, unitId, departmentIds, roleIds);
    }
}
