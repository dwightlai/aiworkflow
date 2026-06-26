package com.mw.ai.agi.auth.identity;

public enum IdentityMode {
    LOCAL,
    REMOTE;

    public static IdentityMode from(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        return REMOTE.name().equalsIgnoreCase(value.trim()) ? REMOTE : LOCAL;
    }
}
