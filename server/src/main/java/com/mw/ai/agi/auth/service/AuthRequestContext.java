package com.mw.ai.agi.auth.service;

import java.util.List;

public final class AuthRequestContext {
    private static final ThreadLocal<Holder> CURRENT = new ThreadLocal<>();

    private AuthRequestContext() {
    }

    public static void set(Holder holder) {
        CURRENT.set(holder);
    }

    public static Holder require() {
        Holder holder = CURRENT.get();
        if (holder == null) {
            throw new AuthException("AUTH_REQUIRED", org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return holder;
    }

    public static java.util.Optional<Holder> current() {
        return java.util.Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Holder(String userId, String tenantId, List<String> roleIds) {
        public Holder {
            roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
        }
    }
}
