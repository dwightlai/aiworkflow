package com.mw.ai.agi.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class MessageDigestSupport {
    private MessageDigestSupport() {
    }

    static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
