package com.mw.ai.agi.auth.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class SecretHasher {
    public String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public boolean matches(String rawSecret, String storedHash) {
        if (rawSecret == null || storedHash == null) {
            return false;
        }
        byte[] actual = hash(rawSecret).getBytes(StandardCharsets.UTF_8);
        byte[] expected = storedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }
}
