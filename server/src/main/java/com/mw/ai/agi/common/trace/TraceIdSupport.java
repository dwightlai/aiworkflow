package com.mw.ai.agi.common.trace;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;
import java.util.regex.Pattern;

public final class TraceIdSupport {
    private static final Pattern VALID = Pattern.compile("^[a-zA-Z0-9._-]{8,64}$");

    private TraceIdSupport() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }
        return resolve(request.getHeader("X-Trace-Id"));
    }

    public static String resolve(String incoming) {
        if (incoming != null) {
            String trimmed = incoming.trim();
            if (!trimmed.isBlank() && VALID.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString();
    }
}
