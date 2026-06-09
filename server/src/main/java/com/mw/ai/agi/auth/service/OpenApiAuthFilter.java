package com.mw.ai.agi.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnBean(IntegrationAppAuthenticator.class)
public class OpenApiAuthFilter extends OncePerRequestFilter {
    private static final String OPEN_API_PREFIX = "/api/open/";

    private final IntegrationAppAuthenticator authenticator;

    public OpenApiAuthFilter(IntegrationAppAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(OPEN_API_PREFIX)) {
            return true;
        }
        return path.startsWith("/api/open/identity/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String appCode = request.getHeader(OpenApiAuthHeaders.APP_CODE);
            String apiKey = request.getHeader(OpenApiAuthHeaders.API_KEY);
            if (appCode == null || appCode.isBlank() || apiKey == null || apiKey.isBlank()) {
                throw new AuthException(
                        "OPEN_API_AUTH_REQUIRED",
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "X-AGI-App-Code and X-AGI-Api-Key are required."
                );
            }
            RuntimeIdentityContext identity = authenticator.authenticateApiKey(
                    appCode,
                    apiKey,
                    OpenApiCallerContextSupport.fromHeaders(request),
                    new RequestAuditContext(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT))
            );
            OpenApiRequestContext.set(request, identity);
            filterChain.doFilter(request, response);
        } catch (AuthException exception) {
            response.setStatus(exception.getStatus().value());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                    + escapeJson(exception.getCode())
                    + "\",\"message\":\""
                    + escapeJson(exception.getMessage())
                    + "\"}}");
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
