package com.mw.ai.agi.common.audit;

import com.mw.ai.agi.auth.service.AuthRequestContext;

public final class OperatorContext {
    private OperatorContext() {
    }

    public static String currentUserId() {
        return AuthRequestContext.current()
                .map(AuthRequestContext.Holder::userId)
                .filter(userId -> userId != null && !userId.isBlank())
                .orElse("system");
    }
}
