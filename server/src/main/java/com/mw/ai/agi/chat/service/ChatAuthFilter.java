package com.mw.ai.agi.chat.service;

import com.mw.ai.agi.auth.service.AuthException;
import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 6)
@ConditionalOnBean(RequestIdentitySupport.class)
public class ChatAuthFilter extends OncePerRequestFilter {
    private static final String CHAT_PREFIX = "/api/chat/";

    private final RequestIdentitySupport identitySupport;
    private final AgentAuditService agentAuditService;

    public ChatAuthFilter(RequestIdentitySupport identitySupport, AgentAuditService agentAuditService) {
        this.identitySupport = identitySupport;
        this.agentAuditService = agentAuditService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(CHAT_PREFIX)) {
            return true;
        }
        return path.startsWith("/api/chat/embed-sessions/exchange");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (identitySupport.resolve(request).isEmpty()) {
                throw new AuthException("CHAT_AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "Bearer token is required.");
            }
            filterChain.doFilter(request, response);
        } catch (AuthException exception) {
            agentAuditService.log(null, null, null, null, null, null, "CHAT_AUTH_DENIED",
                    request.getRequestURI(), null, "FAILED", exception.getMessage(), null);
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
