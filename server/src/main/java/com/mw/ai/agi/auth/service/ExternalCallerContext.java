package com.mw.ai.agi.auth.service;

import java.util.List;

public record ExternalCallerContext(
        String unitId,
        List<String> departmentIds,
        List<String> roleIds,
        String userId
) {
    public ExternalCallerContext {
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
