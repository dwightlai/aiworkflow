package com.mw.ai.agi.auth.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnBean(RequestIdentitySupport.class)
public class TenantContextFilter extends OncePerRequestFilter {
    private final RequestIdentitySupport identitySupport;

    public TenantContextFilter(RequestIdentitySupport identitySupport) {
        this.identitySupport = identitySupport;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            OpenApiRequestContext.get(request).ifPresentOrElse(identity -> {
                TenantContext.set(TenantContext.normalize(identity.tenantId()));
                AuthRequestContext.set(new AuthRequestContext.Holder(
                        identity.userId(),
                        identity.tenantId(),
                        identity.roleIds()
                ));
            }, () -> identitySupport.resolveAuthContext(request).ifPresentOrElse(holder -> {
                TenantContext.set(TenantContext.normalize(holder.tenantId()));
                AuthRequestContext.set(holder);
            }, () -> TenantContext.set(identitySupport.resolveTenantId(request))));
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            AuthRequestContext.clear();
        }
    }
}
