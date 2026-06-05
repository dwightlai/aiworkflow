package com.mw.ai.agi.auth.service;

public record RequestAuditContext(
        String clientIp,
        String userAgent
) {
}
