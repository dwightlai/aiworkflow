package com.mw.ai.agi.auth.api;

import com.mw.ai.agi.auth.service.AuthService;
import com.mw.ai.agi.auth.service.AuthTokenResponse;
import com.mw.ai.agi.auth.service.AuthUserPrincipal;
import com.mw.ai.agi.auth.service.RequestAuditContext;
import com.mw.ai.agi.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(authService.login(
                request.username(),
                request.password(),
                request.tenantCode(),
                auditContext(servletRequest)
        ));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(authService.refresh(request.refreshToken(), auditContext(servletRequest)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.logout(request.refreshToken(), auditContext(servletRequest));
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserPrincipal> me(HttpServletRequest request) {
        return ApiResponse.success(authService.currentUser(bearerToken(request)));
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new com.mw.ai.agi.auth.service.AuthException(
                    "AUTH_TOKEN_INVALID",
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Bearer token is required."
            );
        }
        return authorization.substring("Bearer ".length());
    }

    private RequestAuditContext auditContext(HttpServletRequest request) {
        return new RequestAuditContext(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password,
            String tenantCode
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }
}
