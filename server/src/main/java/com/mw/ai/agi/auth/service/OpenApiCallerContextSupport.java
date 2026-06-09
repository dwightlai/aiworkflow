package com.mw.ai.agi.auth.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

public final class OpenApiCallerContextSupport {
    private OpenApiCallerContextSupport() {
    }

    public static ExternalCallerContext fromHeaders(HttpServletRequest request) {
        if (request == null) {
            return new ExternalCallerContext(null, List.of(), List.of(), null);
        }
        return new ExternalCallerContext(
                blankToNull(request.getHeader(OpenApiAuthHeaders.UNIT_ID)),
                splitHeader(request.getHeader(OpenApiAuthHeaders.DEPARTMENT_IDS)),
                splitHeader(request.getHeader(OpenApiAuthHeaders.ROLE_IDS)),
                blankToNull(request.getHeader(OpenApiAuthHeaders.USER_ID))
        );
    }

    public static ExternalCallerContext merge(
            ExternalCallerContext base,
            String userId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds
    ) {
        return new ExternalCallerContext(
                firstNonBlank(unitId, base == null ? null : base.unitId()),
                firstList(departmentIds, base == null ? List.of() : base.departmentIds()),
                firstList(roleIds, base == null ? List.of() : base.roleIds()),
                firstNonBlank(userId, base == null ? null : base.userId())
        );
    }

    private static List<String> splitHeader(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String normalized = blankToNull(preferred);
        return normalized != null ? normalized : blankToNull(fallback);
    }

    private static List<String> firstList(List<String> preferred, List<String> fallback) {
        if (preferred != null && !preferred.isEmpty()) {
            return preferred;
        }
        return fallback == null ? List.of() : fallback;
    }
}
