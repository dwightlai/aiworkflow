package com.mw.ai.agi.common.api;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        String requestId,
        Map<String, Object> details
) {
}
