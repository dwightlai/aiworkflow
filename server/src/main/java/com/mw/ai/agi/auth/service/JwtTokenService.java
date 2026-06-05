package com.mw.ai.agi.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtTokenService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${agi.auth.jwt-secret:${AGI_JWT_SECRET:change-me-agi-development-secret}}") String secret,
            @Value("${agi.auth.access-token-seconds:3600}") long accessTokenSeconds,
            @Value("${agi.auth.refresh-token-seconds:1209600}") long refreshTokenSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public long accessTokenSeconds() {
        return accessTokenSeconds;
    }

    public String issueAccessToken(AuthUserPrincipal user) {
        Instant now = Instant.now();
        Map<String, Object> claims = baseClaims(now, now.plusSeconds(accessTokenSeconds), "ACCESS");
        claims.put("sub", user.id());
        claims.put("username", user.username());
        claims.put("tenantId", user.tenantId());
        claims.put("unitIds", user.unitIds());
        claims.put("activeUnitId", user.activeUnitId());
        claims.put("departmentIds", user.departmentIds());
        claims.put("roleIds", user.roleIds());
        claims.put("userType", user.userType());
        return sign(claims);
    }

    public String issueRefreshToken(AuthUserPrincipal user, String sessionId) {
        Instant now = Instant.now();
        Map<String, Object> claims = baseClaims(now, now.plusSeconds(refreshTokenSeconds), "REFRESH");
        claims.put("sub", user.id());
        claims.put("tenantId", user.tenantId());
        claims.put("sid", sessionId);
        return sign(claims);
    }

    public JwtClaims verify(String token, String expectedType) {
        Map<String, Object> claims = verifyRaw(token);
        String tokenType = string(claims.get("tokenType"));
        if (!expectedType.equals(tokenType)) {
            throw new AuthException("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Token type is invalid.");
        }
        long expiresAt = number(claims.get("exp"));
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new AuthException("AUTH_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Token has expired.");
        }
        return new JwtClaims(
                string(claims.get("sub")),
                string(claims.get("sid")),
                string(claims.get("username")),
                string(claims.get("tenantId")),
                string(claims.get("activeUnitId")),
                list(claims.get("unitIds")),
                list(claims.get("departmentIds")),
                list(claims.get("roleIds")),
                string(claims.get("userType")),
                tokenType
        );
    }

    private Map<String, Object> baseClaims(Instant issuedAt, Instant expiresAt, String tokenType) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("tokenType", tokenType);
        return claims;
    }

    private String sign(Map<String, Object> claims) {
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(claims);
            String signatureInput = header + "." + payload;
            return signatureInput + "." + base64Url(hmac(signatureInput));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT.", exception);
        }
    }

    private Map<String, Object> verifyRaw(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw invalid();
            }
            String signatureInput = parts[0] + "." + parts[1];
            String expected = base64Url(hmac(signatureInput));
            if (!MessageDigestSupport.constantTimeEquals(expected, parts[2])) {
                throw invalid();
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private byte[] hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeJson(Object value) throws Exception {
        return base64Url(objectMapper.writeValueAsBytes(value));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AuthException invalid() {
        return new AuthException("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Token is invalid.");
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }

    private List<String> list(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public record JwtClaims(
            String subject,
            String sessionId,
            String username,
            String tenantId,
            String activeUnitId,
            List<String> unitIds,
            List<String> departmentIds,
            List<String> roleIds,
            String userType,
            String tokenType
    ) {
    }
}
