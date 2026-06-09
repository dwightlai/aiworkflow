package com.mw.ai.agi.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class RequestIdentitySupport {
    private final JwtTokenService jwtTokenService;

    public RequestIdentitySupport(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public Optional<RequestIdentity> resolve(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            JwtTokenService.JwtClaims claims = jwtTokenService.verify(authorization.substring("Bearer ".length()), "ACCESS");
            return Optional.of(new RequestIdentity(
                    claims.subject(),
                    claims.tenantId(),
                    claims.activeUnitId(),
                    claims.unitIds(),
                    claims.departmentIds()
            ));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public Map<String, Object> grantContext(HttpServletRequest request) {
        return resolve(request).map(RequestIdentity::toGrantContext).orElse(Map.of());
    }

    public Map<String, Object> mergeGrantContext(HttpServletRequest request, Map<String, Object> input) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (input != null) {
            merged.putAll(input);
        }
        resolve(request).ifPresent(identity -> merged.putAll(identity.toGrantContext()));
        return merged;
    }

    public String resolveTenantId(HttpServletRequest request) {
        return resolveAuthContext(request)
                .map(holder -> TenantContext.normalize(holder.tenantId()))
                .orElse(TenantContext.requireTenantId());
    }

    public java.util.Optional<AuthRequestContext.Holder> resolveAuthContext(HttpServletRequest request) {
        if (request == null) {
            return java.util.Optional.empty();
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return java.util.Optional.empty();
        }
        try {
            JwtTokenService.JwtClaims claims = jwtTokenService.verify(authorization.substring("Bearer ".length()), "ACCESS");
            return java.util.Optional.of(new AuthRequestContext.Holder(
                    claims.subject(),
                    claims.tenantId(),
                    claims.roleIds()
            ));
        } catch (RuntimeException ignored) {
            return java.util.Optional.empty();
        }
    }

    public String resolveOwnerUnitId(String requestedOwnerUnitId, HttpServletRequest request) {
        if (requestedOwnerUnitId != null && !requestedOwnerUnitId.isBlank()) {
            return requestedOwnerUnitId;
        }
        return resolve(request)
                .map(RequestIdentity::activeUnitId)
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
    }
}
