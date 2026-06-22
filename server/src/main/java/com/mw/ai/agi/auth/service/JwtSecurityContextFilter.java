package com.mw.ai.agi.auth.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 8)
@ConditionalOnBean(RequestIdentitySupport.class)
public class JwtSecurityContextFilter extends OncePerRequestFilter {
    private final RequestIdentitySupport identitySupport;

    public JwtSecurityContextFilter(RequestIdentitySupport identitySupport) {
        this.identitySupport = identitySupport;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            identitySupport.resolve(request).ifPresent(identity -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        identity.userId(),
                        null,
                        List.of()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
