package com.mw.ai.agi.auth.identity;

import com.mw.ai.agi.auth.service.AuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 4)
public class RemoteIdentityAdminFilter extends OncePerRequestFilter {
    private static final Set<String> BLOCKED_PREFIXES = Set.of(
            "/api/auth/admin/tenants",
            "/api/auth/admin/organizations",
            "/api/auth/admin/users",
            "/api/auth/admin/roles"
    );

    private final IdentityProperties identityProperties;

    public RemoteIdentityAdminFilter(IdentityProperties identityProperties) {
        this.identityProperties = identityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!identityProperties.isRemote() || !identityProperties.getRemote().isDisableLocalOrgAdmin()) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod()) || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return BLOCKED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\"IDENTITY_MODE_REMOTE\","
                + "\"message\":\"Organization and user administration is disabled in remote identity mode.\"}}");
    }
}
