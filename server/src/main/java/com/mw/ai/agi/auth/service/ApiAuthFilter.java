package com.mw.ai.agi.auth.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 7)
@ConditionalOnBean(RequestIdentitySupport.class)
public class ApiAuthFilter extends OncePerRequestFilter {
    private static final String API_PREFIX = "/api/";
    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/v3/api-docs",
            "/swagger-ui"
    );

    private final RequestIdentitySupport identitySupport;

    public ApiAuthFilter(RequestIdentitySupport identitySupport) {
        this.identitySupport = identitySupport;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(API_PREFIX)) {
            return true;
        }
        if (path.startsWith("/api/open/")) {
            return true;
        }
        if (path.startsWith("/api/chat/")) {
            return true;
        }
        if (path.startsWith("/api/demo/")) {
            return true;
        }
        if (path.equals("/api/auth/login") || path.equals("/api/auth/refresh")) {
            return true;
        }
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (identitySupport.resolve(request).isEmpty()) {
                throw new AuthException("API_AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "Bearer token is required.");
            }
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
