package com.mw.ai.agi.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mw.ai.agi.auth.persistence.AuthAuditLogEntity;
import com.mw.ai.agi.auth.persistence.AuthAuditLogMapper;
import com.mw.ai.agi.auth.persistence.LoginSessionEntity;
import com.mw.ai.agi.auth.persistence.LoginSessionMapper;
import com.mw.ai.agi.auth.persistence.RoleMapper;
import com.mw.ai.agi.auth.persistence.UserEntity;
import com.mw.ai.agi.auth.persistence.UserMapper;
import com.mw.ai.agi.auth.persistence.UserOrganizationEntity;
import com.mw.ai.agi.auth.persistence.UserOrganizationMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final ObjectProvider<UserMapper> userMapperProvider;
    private final ObjectProvider<UserOrganizationMapper> userOrganizationMapperProvider;
    private final ObjectProvider<RoleMapper> roleMapperProvider;
    private final ObjectProvider<LoginSessionMapper> loginSessionMapperProvider;
    private final ObjectProvider<AuthAuditLogMapper> authAuditLogMapperProvider;
    private final PasswordEncoder passwordEncoder;
    private final SecretHasher secretHasher;
    private final JwtTokenService jwtTokenService;
    private final String defaultTenantId;
    private final long refreshTokenSeconds;

    public AuthService(
            ObjectProvider<UserMapper> userMapperProvider,
            ObjectProvider<UserOrganizationMapper> userOrganizationMapperProvider,
            ObjectProvider<RoleMapper> roleMapperProvider,
            ObjectProvider<LoginSessionMapper> loginSessionMapperProvider,
            ObjectProvider<AuthAuditLogMapper> authAuditLogMapperProvider,
            PasswordEncoder passwordEncoder,
            SecretHasher secretHasher,
            JwtTokenService jwtTokenService,
            @Value("${agi.auth.default-tenant-id:tenant_default}") String defaultTenantId,
            @Value("${agi.auth.refresh-token-seconds:1209600}") long refreshTokenSeconds
    ) {
        this.userMapperProvider = userMapperProvider;
        this.userOrganizationMapperProvider = userOrganizationMapperProvider;
        this.roleMapperProvider = roleMapperProvider;
        this.loginSessionMapperProvider = loginSessionMapperProvider;
        this.authAuditLogMapperProvider = authAuditLogMapperProvider;
        this.passwordEncoder = passwordEncoder;
        this.secretHasher = secretHasher;
        this.jwtTokenService = jwtTokenService;
        this.defaultTenantId = defaultTenantId;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public AuthTokenResponse login(String username, String password, RequestAuditContext auditContext) {
        Optional<UserEntity> user = findUserByUsername(defaultTenantId, username);
        if (user.isEmpty() || user.get().getPasswordHash() == null
                || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            auditLogin(null, null, auditContext, "FAILED", "AUTH_LOGIN_FAILED");
            throw new AuthException("AUTH_LOGIN_FAILED", HttpStatus.UNAUTHORIZED, "Username or password is incorrect.");
        }
        if ("LOCKED".equals(user.get().getStatus())) {
            auditLogin(user.get().getTenantId(), user.get().getId(), auditContext, "FAILED", "AUTH_USER_LOCKED");
            throw new AuthException("AUTH_USER_LOCKED", HttpStatus.FORBIDDEN, "User is locked.");
        }
        if (!"ACTIVE".equals(user.get().getStatus())) {
            auditLogin(user.get().getTenantId(), user.get().getId(), auditContext, "FAILED", "AUTH_USER_DISABLED");
            throw new AuthException("AUTH_USER_DISABLED", HttpStatus.FORBIDDEN, "User is disabled.");
        }

        AuthUserPrincipal principal = loadPrincipal(user.get());
        String sessionId = "session_" + UUID.randomUUID().toString().replace("-", "");
        String accessToken = jwtTokenService.issueAccessToken(principal);
        String refreshToken = jwtTokenService.issueRefreshToken(principal, sessionId);
        Instant now = Instant.now();
        LoginSessionEntity session = new LoginSessionEntity();
        session.setId(sessionId);
        session.setTenantId(principal.tenantId());
        session.setUserId(principal.id());
        session.setRefreshTokenHash(secretHasher.hash(refreshToken));
        session.setUserAgent(auditContext.userAgent());
        session.setClientIp(auditContext.clientIp());
        session.setExpiresAt(now.plusSeconds(refreshTokenSeconds));
        session.setCreatedAt(now);
        loginSessionMapper().insert(session);

        userMapper().update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, principal.id())
                .set(UserEntity::getLastLoginAt, now)
                .set(UserEntity::getUpdatedAt, now));
        auditLogin(principal.tenantId(), principal.id(), auditContext, "SUCCESS", null);
        return new AuthTokenResponse(accessToken, refreshToken, jwtTokenService.accessTokenSeconds(), principal);
    }

    public AuthTokenResponse refresh(String refreshToken, RequestAuditContext auditContext) {
        JwtTokenService.JwtClaims claims = jwtTokenService.verify(refreshToken, "REFRESH");
        LoginSessionEntity session = findActiveSession(claims.sessionId())
                .orElseThrow(() -> new AuthException("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Refresh session is invalid."));
        if (!secretHasher.matches(refreshToken, session.getRefreshTokenHash())) {
            throw new AuthException("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Refresh token is invalid.");
        }
        UserEntity user = findUserById(claims.subject())
                .orElseThrow(() -> new AuthException("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "User does not exist."));
        AuthUserPrincipal principal = loadPrincipal(user);
        String accessToken = jwtTokenService.issueAccessToken(principal);
        audit("TOKEN_REFRESH", principal.tenantId(), principal.id(), null, principal.activeUnitId(),
                principal.departmentIds(), principal.roleIds(), auditContext, "SUCCESS", null);
        return new AuthTokenResponse(accessToken, refreshToken, jwtTokenService.accessTokenSeconds(), principal);
    }

    public void logout(String refreshToken, RequestAuditContext auditContext) {
        JwtTokenService.JwtClaims claims = jwtTokenService.verify(refreshToken, "REFRESH");
        loginSessionMapper().update(null, new LambdaUpdateWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getId, claims.sessionId())
                .set(LoginSessionEntity::getRevokedAt, Instant.now()));
        audit("LOGOUT", claims.tenantId(), claims.subject(), null, null, List.of(), List.of(), auditContext, "SUCCESS", null);
    }

    public AuthUserPrincipal currentUser(String accessToken) {
        JwtTokenService.JwtClaims claims = jwtTokenService.verify(accessToken, "ACCESS");
        return new AuthUserPrincipal(
                claims.subject(),
                claims.username(),
                claims.tenantId(),
                claims.username(),
                claims.userType(),
                0,
                claims.organizationIds(),
                claims.activeOrganizationId(),
                claims.unitIds(),
                claims.activeUnitId(),
                claims.departmentIds(),
                claims.roleIds()
        );
    }

    private AuthUserPrincipal loadPrincipal(UserEntity user) {
        List<String> organizationIds = userOrganizationMapper().selectList(new LambdaQueryWrapper<UserOrganizationEntity>()
                        .eq(UserOrganizationEntity::getUserId, user.getId()))
                .stream()
                .sorted(Comparator
                        .comparing((UserOrganizationEntity entity) -> Boolean.TRUE.equals(entity.getPrimaryOrganization())).reversed()
                        .thenComparing(UserOrganizationEntity::getOrganizationId))
                .map(UserOrganizationEntity::getOrganizationId)
                .toList();
        List<String> roleIds = roleMapper().selectRoleCodesByUserId(user.getId());
        String activeOrganizationId = organizationIds.isEmpty() ? null : organizationIds.get(0);
        return new AuthUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getTenantId(),
                user.getDisplayName(),
                user.getUserType(),
                user.getSortOrder() == null ? 0 : user.getSortOrder(),
                organizationIds,
                activeOrganizationId,
                organizationIds,
                activeOrganizationId,
                List.of(),
                roleIds
        );
    }

    private Optional<UserEntity> findUserByUsername(String tenantId, String username) {
        return Optional.ofNullable(userMapper().selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getTenantId, tenantId)
                .eq(UserEntity::getUsername, username)));
    }

    private Optional<UserEntity> findUserById(String id) {
        return Optional.ofNullable(userMapper().selectById(id));
    }

    private Optional<LoginSessionEntity> findActiveSession(String sessionId) {
        return Optional.ofNullable(loginSessionMapper().selectOne(new LambdaQueryWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getId, sessionId)
                .isNull(LoginSessionEntity::getRevokedAt)
                .gt(LoginSessionEntity::getExpiresAt, Instant.now())));
    }

    private void auditLogin(String tenantId, String userId, RequestAuditContext auditContext, String result, String errorCode) {
        audit("LOGIN_" + ("SUCCESS".equals(result) ? "SUCCESS" : "FAILED"), tenantId, userId, null, null,
                List.of(), List.of(), auditContext, result, errorCode);
    }

    void audit(
            String eventType,
            String tenantId,
            String userId,
            String appId,
            String unitId,
            List<String> departmentIds,
            List<String> roleIds,
            RequestAuditContext auditContext,
            String result,
            String errorCode
    ) {
        AuthAuditLogEntity auditLog = new AuthAuditLogEntity();
        auditLog.setId("audit_" + UUID.randomUUID().toString().replace("-", ""));
        auditLog.setTenantId(tenantId == null ? defaultTenantId : tenantId);
        auditLog.setEventType(eventType);
        auditLog.setUserId(userId);
        auditLog.setAppId(appId);
        auditLog.setUnitId(unitId);
        auditLog.setDepartmentIds(String.join(",", departmentIds == null ? List.of() : departmentIds));
        auditLog.setRoleIds(String.join(",", roleIds == null ? List.of() : roleIds));
        auditLog.setClientIp(auditContext.clientIp());
        auditLog.setUserAgent(auditContext.userAgent());
        auditLog.setResult(result);
        auditLog.setErrorCode(errorCode);
        auditLog.setOccurredAt(Instant.now());
        authAuditLogMapper().insert(auditLog);
    }

    private UserMapper userMapper() {
        return required(userMapperProvider, UserMapper.class);
    }

    private UserOrganizationMapper userOrganizationMapper() {
        return required(userOrganizationMapperProvider, UserOrganizationMapper.class);
    }

    private RoleMapper roleMapper() {
        return required(roleMapperProvider, RoleMapper.class);
    }

    private LoginSessionMapper loginSessionMapper() {
        return required(loginSessionMapperProvider, LoginSessionMapper.class);
    }

    private AuthAuditLogMapper authAuditLogMapper() {
        return required(authAuditLogMapperProvider, AuthAuditLogMapper.class);
    }

    private <T> T required(ObjectProvider<T> provider, Class<T> type) {
        T mapper = provider.getIfAvailable();
        if (mapper == null) {
            throw new AuthException("AUTH_STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    type.getSimpleName() + " is unavailable.");
        }
        return mapper;
    }
}
