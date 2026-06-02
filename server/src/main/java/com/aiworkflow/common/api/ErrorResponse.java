package com.aiworkflow.common.api;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        String requestId,
        Map<String, Object> details
) {
}
