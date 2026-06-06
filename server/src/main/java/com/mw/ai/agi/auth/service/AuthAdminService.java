package com.mw.ai.agi.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretMapper;
import com.mw.ai.agi.auth.persistence.OrganizationEntity;
import com.mw.ai.agi.auth.persistence.OrganizationMapper;
import com.mw.ai.agi.auth.persistence.RoleEntity;
import com.mw.ai.agi.auth.persistence.RoleMapper;
import com.mw.ai.agi.auth.persistence.TenantEntity;
import com.mw.ai.agi.auth.persistence.TenantMapper;
import com.mw.ai.agi.auth.persistence.UserEntity;
import com.mw.ai.agi.auth.persistence.UserMapper;
import com.mw.ai.agi.auth.persistence.UserOrganizationEntity;
import com.mw.ai.agi.auth.persistence.UserOrganizationMapper;
import com.mw.ai.agi.auth.persistence.UserRoleEntity;
import com.mw.ai.agi.auth.persistence.UserRoleMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthAdminService {
    private final TenantMapper tenantMapper;
    private final OrganizationMapper organizationMapper;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserOrganizationMapper userOrganizationMapper;
    private final UserRoleMapper userRoleMapper;
    private final IntegrationAppMapper integrationAppMapper;
    private final IntegrationAppSecretMapper integrationAppSecretMapper;
    private final IntegrationAppScopeMapper integrationAppScopeMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecretHasher secretHasher;
    private final String defaultTenantId;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthAdminService(
            TenantMapper tenantMapper,
            OrganizationMapper organizationMapper,
            RoleMapper roleMapper,
            UserMapper userMapper,
            UserOrganizationMapper userOrganizationMapper,
            UserRoleMapper userRoleMapper,
            IntegrationAppMapper integrationAppMapper,
            IntegrationAppSecretMapper integrationAppSecretMapper,
            IntegrationAppScopeMapper integrationAppScopeMapper,
            PasswordEncoder passwordEncoder,
            SecretHasher secretHasher,
            @Value("${agi.auth.default-tenant-id:tenant_default}") String defaultTenantId
    ) {
        this.tenantMapper = tenantMapper;
        this.organizationMapper = organizationMapper;
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userOrganizationMapper = userOrganizationMapper;
        this.userRoleMapper = userRoleMapper;
        this.integrationAppMapper = integrationAppMapper;
        this.integrationAppSecretMapper = integrationAppSecretMapper;
        this.integrationAppScopeMapper = integrationAppScopeMapper;
        this.passwordEncoder = passwordEncoder;
        this.secretHasher = secretHasher;
        this.defaultTenantId = defaultTenantId;
    }

    public List<TenantEntity> listTenants() {
        return tenantMapper.selectList(new LambdaQueryWrapper<TenantEntity>().orderByAsc(TenantEntity::getCode));
    }

    public List<OrganizationEntity> listOrganizations() {
        return organizationMapper.selectList(new LambdaQueryWrapper<OrganizationEntity>()
                .ne(OrganizationEntity::getStatus, "DELETED")
                .orderByAsc(OrganizationEntity::getPath)
                .orderByAsc(OrganizationEntity::getSortOrder)
                .orderByAsc(OrganizationEntity::getCode));
    }

    public List<RoleEntity> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
                .ne(RoleEntity::getStatus, "DELETED")
                .orderByAsc(RoleEntity::getCode));
    }

    public List<AuthUserPrincipal> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                        .ne(UserEntity::getStatus, "DELETED")
                        .orderByAsc(UserEntity::getSortOrder)
                        .orderByAsc(UserEntity::getUsername))
                .stream()
                .map(this::toPrincipal)
                .toList();
    }

    public List<IntegrationAppEntity> listIntegrationApps() {
        return integrationAppMapper.selectList(new LambdaQueryWrapper<IntegrationAppEntity>()
                .ne(IntegrationAppEntity::getStatus, "DELETED")
                .orderByAsc(IntegrationAppEntity::getCode));
    }

    public OrganizationEntity createOrganization(String parentId, String code, String externalOrgId, String name, String orgType, Integer sortOrder) {
        Instant now = Instant.now();
        OrganizationEntity parent = findParentOrganization(parentId);
        OrganizationEntity entity = new OrganizationEntity();
        entity.setId("org_" + normalize(code));
        entity.setTenantId(defaultTenantId);
        entity.setCode(code);
        entity.setExternalOrgId(externalOrgId);
        entity.setName(name);
        entity.setOrgType(normalizeOrgType(orgType));
        entity.setParentId(parentId == null || parentId.isBlank() ? null : parentId);
        entity.setPath((parent == null ? "" : parent.getPath()) + "/" + entity.getId());
        entity.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        entity.setSortOrder(sortOrder == null ? 0 : sortOrder);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        organizationMapper.insert(entity);
        return entity;
    }

    public OrganizationEntity updateOrganization(
            String organizationId,
            String parentId,
            String externalOrgId,
            String name,
            String orgType,
            Integer sortOrder,
            String status
    ) {
        OrganizationEntity organization = Optional.ofNullable(organizationMapper.selectById(organizationId))
                .orElseThrow(() -> new AuthException("ORGANIZATION_NOT_FOUND", HttpStatus.NOT_FOUND, "Organization does not exist."));
        OrganizationEntity parent = findParentOrganization(parentId);
        String normalizedParentId = parentId == null || parentId.isBlank() ? null : parentId;
        organizationMapper.update(null, new LambdaUpdateWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getId, organization.getId())
                .set(OrganizationEntity::getParentId, normalizedParentId)
                .set(OrganizationEntity::getExternalOrgId, externalOrgId)
                .set(OrganizationEntity::getName, name)
                .set(OrganizationEntity::getOrgType, normalizeOrgType(orgType == null || orgType.isBlank() ? organization.getOrgType() : orgType))
                .set(OrganizationEntity::getPath, (parent == null ? "" : parent.getPath()) + "/" + organization.getId())
                .set(OrganizationEntity::getLevel, parent == null ? 1 : parent.getLevel() + 1)
                .set(OrganizationEntity::getSortOrder, sortOrder == null ? organization.getSortOrder() : sortOrder)
                .set(OrganizationEntity::getStatus, normalizeActiveStatus(status, "INVALID_ORGANIZATION_STATUS"))
                .set(OrganizationEntity::getUpdatedAt, Instant.now()));
        return organizationMapper.selectById(organizationId);
    }

    public OrganizationEntity deleteOrganization(String organizationId) {
        OrganizationEntity organization = Optional.ofNullable(organizationMapper.selectById(organizationId))
                .orElseThrow(() -> new AuthException("ORGANIZATION_NOT_FOUND", HttpStatus.NOT_FOUND, "Organization does not exist."));
        long childCount = organizationMapper.selectCount(new LambdaQueryWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getParentId, organization.getId())
                .ne(OrganizationEntity::getStatus, "DELETED"));
        if (childCount > 0) {
            throw new AuthException("ORGANIZATION_IN_USE", HttpStatus.BAD_REQUEST, "Organization has child organizations.");
        }
        List<UserOrganizationEntity> memberships = userOrganizationMapper.selectList(new LambdaQueryWrapper<UserOrganizationEntity>()
                .eq(UserOrganizationEntity::getOrganizationId, organization.getId()));
        boolean hasActiveUser = memberships.stream()
                .map(UserOrganizationEntity::getUserId)
                .distinct()
                .map(userMapper::selectById)
                .anyMatch(user -> user != null && !"DELETED".equals(user.getStatus()));
        if (hasActiveUser) {
            throw new AuthException("ORGANIZATION_IN_USE", HttpStatus.BAD_REQUEST, "Organization has users.");
        }
        organizationMapper.update(null, new LambdaUpdateWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getId, organization.getId())
                .set(OrganizationEntity::getStatus, "DELETED")
                .set(OrganizationEntity::getUpdatedAt, Instant.now()));
        return organizationMapper.selectById(organizationId);
    }

    public RoleEntity createRole(String organizationId, String code, String name, String roleType, String externalRoleId) {
        Instant now = Instant.now();
        RoleEntity entity = new RoleEntity();
        entity.setId("role_" + normalize(code));
        entity.setTenantId(defaultTenantId);
        entity.setOrganizationId(organizationId);
        entity.setExternalRoleId(externalRoleId);
        entity.setCode(code);
        entity.setName(name);
        entity.setRoleType(roleType == null || roleType.isBlank() ? "BUSINESS" : roleType);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        roleMapper.insert(entity);
        return entity;
    }

    public RoleEntity updateRole(String roleId, String organizationId, String code, String name, String roleType, String externalRoleId, String status) {
        RoleEntity role = Optional.ofNullable(roleMapper.selectById(roleId))
                .orElseThrow(() -> new AuthException("ROLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Role does not exist."));
        String normalizedCode = code == null || code.isBlank() ? role.getCode() : code.trim();
        if (!normalizedCode.equals(role.getCode())) {
            findRoleByCode(normalizedCode)
                    .filter(existing -> !existing.getId().equals(role.getId()))
                    .ifPresent(existing -> {
                        throw new AuthException("ROLE_CODE_EXISTS", HttpStatus.BAD_REQUEST, "Role code already exists.");
                    });
        }
        roleMapper.update(null, new LambdaUpdateWrapper<RoleEntity>()
                .eq(RoleEntity::getId, role.getId())
                .set(RoleEntity::getOrganizationId, organizationId)
                .set(RoleEntity::getCode, normalizedCode)
                .set(RoleEntity::getName, name)
                .set(RoleEntity::getRoleType, roleType == null || roleType.isBlank() ? role.getRoleType() : roleType)
                .set(RoleEntity::getExternalRoleId, externalRoleId)
                .set(RoleEntity::getStatus, normalizeActiveStatus(status, "INVALID_ROLE_STATUS"))
                .set(RoleEntity::getUpdatedAt, Instant.now()));
        return roleMapper.selectById(roleId);
    }

    public RoleEntity deleteRole(String roleId) {
        RoleEntity role = Optional.ofNullable(roleMapper.selectById(roleId))
                .orElseThrow(() -> new AuthException("ROLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Role does not exist."));
        roleMapper.update(null, new LambdaUpdateWrapper<RoleEntity>()
                .eq(RoleEntity::getId, role.getId())
                .set(RoleEntity::getStatus, "DELETED")
                .set(RoleEntity::getUpdatedAt, Instant.now()));
        return roleMapper.selectById(roleId);
    }

    public AuthUserPrincipal createLocalUser(
            String username,
            String password,
            String displayName,
            String mobile,
            String email,
            Integer sortOrder,
            List<String> organizationIds,
            List<String> roleCodes
    ) {
        Instant now = Instant.now();
        UserEntity entity = new UserEntity();
        entity.setId("user_" + normalize(username));
        entity.setTenantId(defaultTenantId);
        entity.setUsername(username);
        entity.setPasswordHash(passwordEncoder.encode(password));
        entity.setDisplayName(displayName);
        entity.setMobile(mobile);
        entity.setEmail(email);
        entity.setUserType("LOCAL");
        entity.setStatus("ACTIVE");
        entity.setSortOrder(sortOrder == null ? 0 : sortOrder);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userMapper.insert(entity);

        List<String> normalizedOrganizationIds = organizationIds == null ? List.of() : organizationIds;
        for (int index = 0; index < normalizedOrganizationIds.size(); index++) {
            UserOrganizationEntity relation = new UserOrganizationEntity();
            relation.setId(entity.getId() + "_org_" + normalize(normalizedOrganizationIds.get(index)));
            relation.setTenantId(defaultTenantId);
            relation.setUserId(entity.getId());
            relation.setOrganizationId(normalizedOrganizationIds.get(index));
            relation.setPrimaryOrganization(index == 0);
            relation.setCreatedAt(now);
            userOrganizationMapper.insert(relation);
        }

        for (String roleCode : roleCodes == null ? List.<String>of() : roleCodes) {
            RoleEntity role = findRoleByCode(roleCode)
                    .orElseThrow(() -> new AuthException("ROLE_NOT_FOUND", HttpStatus.BAD_REQUEST, "Role does not exist."));
            UserRoleEntity relation = new UserRoleEntity();
            relation.setId(entity.getId() + "_role_" + normalize(role.getCode()));
            relation.setTenantId(defaultTenantId);
            relation.setUserId(entity.getId());
            relation.setRoleId(role.getId());
            relation.setCreatedAt(now);
            userRoleMapper.insert(relation);
        }

        return toPrincipal(entity);
    }

    public UserEntity updateUserStatus(String userId, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "DISABLED", "LOCKED").contains(normalizedStatus)) {
            throw new AuthException("INVALID_USER_STATUS", HttpStatus.BAD_REQUEST, "User status must be ACTIVE, DISABLED, or LOCKED.");
        }
        UserEntity user = Optional.ofNullable(userMapper.selectById(userId))
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User does not exist."));
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, user.getId())
                .set(UserEntity::getStatus, normalizedStatus)
                .set(UserEntity::getUpdatedAt, Instant.now()));
        return userMapper.selectById(userId);
    }

    public UserEntity deleteUser(String userId) {
        UserEntity user = Optional.ofNullable(userMapper.selectById(userId))
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User does not exist."));
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, user.getId())
                .set(UserEntity::getStatus, "DELETED")
                .set(UserEntity::getUpdatedAt, Instant.now()));
        return userMapper.selectById(userId);
    }

    public AuthUserPrincipal updateLocalUser(
            String userId,
            String password,
            String displayName,
            String mobile,
            String email,
            Integer sortOrder,
            List<String> organizationIds,
            List<String> roleCodes
    ) {
        UserEntity user = Optional.ofNullable(userMapper.selectById(userId))
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User does not exist."));
        LambdaUpdateWrapper<UserEntity> updateWrapper = new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, user.getId())
                .set(UserEntity::getDisplayName, displayName)
                .set(UserEntity::getMobile, mobile)
                .set(UserEntity::getEmail, email)
                .set(UserEntity::getSortOrder, sortOrder == null ? 0 : sortOrder)
                .set(UserEntity::getUpdatedAt, Instant.now());
        if (password != null && !password.isBlank()) {
            if (!"LOCAL".equals(user.getUserType())) {
                throw new AuthException("USER_PASSWORD_UNSUPPORTED", HttpStatus.BAD_REQUEST, "Only local users can update password.");
            }
            updateWrapper.set(UserEntity::getPasswordHash, passwordEncoder.encode(password));
        }
        userMapper.update(null, updateWrapper);

        Instant now = Instant.now();
        userOrganizationMapper.delete(new LambdaQueryWrapper<UserOrganizationEntity>()
                .eq(UserOrganizationEntity::getUserId, user.getId()));
        List<String> normalizedOrganizationIds = organizationIds == null ? List.of() : organizationIds;
        for (int index = 0; index < normalizedOrganizationIds.size(); index++) {
            UserOrganizationEntity relation = new UserOrganizationEntity();
            relation.setId(user.getId() + "_org_" + normalize(normalizedOrganizationIds.get(index)));
            relation.setTenantId(user.getTenantId());
            relation.setUserId(user.getId());
            relation.setOrganizationId(normalizedOrganizationIds.get(index));
            relation.setPrimaryOrganization(index == 0);
            relation.setCreatedAt(now);
            userOrganizationMapper.insert(relation);
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, user.getId()));
        for (String roleCode : roleCodes == null ? List.<String>of() : roleCodes) {
            RoleEntity role = findRoleByCode(roleCode)
                    .orElseThrow(() -> new AuthException("ROLE_NOT_FOUND", HttpStatus.BAD_REQUEST, "Role does not exist."));
            UserRoleEntity relation = new UserRoleEntity();
            relation.setId(user.getId() + "_role_" + normalize(role.getCode()));
            relation.setTenantId(user.getTenantId());
            relation.setUserId(user.getId());
            relation.setRoleId(role.getId());
            relation.setCreatedAt(now);
            userRoleMapper.insert(relation);
        }
        return toPrincipal(userMapper.selectById(userId));
    }

    public List<AuthUserPrincipal> updateUserSortOrders(String organizationId, List<UserSortOrderUpdate> items) {
        if (organizationId == null || organizationId.isBlank()) {
            throw new AuthException("ORGANIZATION_REQUIRED", HttpStatus.BAD_REQUEST, "Organization is required.");
        }
        List<UserOrganizationEntity> memberships = userOrganizationMapper.selectList(new LambdaQueryWrapper<UserOrganizationEntity>()
                .eq(UserOrganizationEntity::getOrganizationId, organizationId));
        Set<String> memberUserIds = memberships.stream()
                .map(UserOrganizationEntity::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        Instant now = Instant.now();
        for (UserSortOrderUpdate item : items == null ? List.<UserSortOrderUpdate>of() : items) {
            if (item.userId() == null || item.userId().isBlank()) {
                throw new AuthException("USER_REQUIRED", HttpStatus.BAD_REQUEST, "User is required.");
            }
            if (!memberUserIds.contains(item.userId())) {
                throw new AuthException("USER_ORGANIZATION_NOT_FOUND", HttpStatus.BAD_REQUEST, "User does not belong to the selected organization.");
            }
            userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                    .eq(UserEntity::getId, item.userId())
                    .set(UserEntity::getSortOrder, item.sortOrder() == null ? 0 : item.sortOrder())
                    .set(UserEntity::getUpdatedAt, now));
        }
        if (memberUserIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                        .in(UserEntity::getId, memberUserIds)
                        .ne(UserEntity::getStatus, "DELETED"))
                .stream()
                .sorted(Comparator
                        .comparing((UserEntity user) -> user.getSortOrder() == null ? 0 : user.getSortOrder())
                        .thenComparing(UserEntity::getUsername))
                .map(this::toPrincipal)
                .toList();
    }

    public AuthUserPrincipal resetLocalUserPassword(String userId, String password) {
        if (password == null || password.isBlank()) {
            throw new AuthException("INVALID_PASSWORD", HttpStatus.BAD_REQUEST, "Password cannot be blank.");
        }
        UserEntity user = Optional.ofNullable(userMapper.selectById(userId))
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User does not exist."));
        if (!"LOCAL".equals(user.getUserType())) {
            throw new AuthException("USER_PASSWORD_UNSUPPORTED", HttpStatus.BAD_REQUEST, "Only local users can reset password.");
        }
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, user.getId())
                .set(UserEntity::getPasswordHash, passwordEncoder.encode(password))
                .set(UserEntity::getUpdatedAt, Instant.now()));
        return toPrincipal(userMapper.selectById(userId));
    }

    public IntegrationAppEntity createIntegrationApp(String code, String name, String appType, String authType) {
        Instant now = Instant.now();
        IntegrationAppEntity entity = new IntegrationAppEntity();
        entity.setId("app_" + normalize(code));
        entity.setTenantId(defaultTenantId);
        entity.setCode(code);
        entity.setName(name);
        entity.setAppType(appType == null || appType.isBlank() ? "OTHER" : appType);
        entity.setAuthType(authType == null || authType.isBlank() ? "API_KEY" : authType);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        integrationAppMapper.insert(entity);
        return entity;
    }

    public GeneratedApiKey createIntegrationAppSecret(String appId) {
        IntegrationAppEntity app = Optional.ofNullable(integrationAppMapper.selectById(appId))
                .orElseThrow(() -> new AuthException("APP_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app does not exist."));
        String apiKey = generateApiKey();
        Instant now = Instant.now();
        IntegrationAppSecretEntity entity = new IntegrationAppSecretEntity();
        entity.setId("secret_" + normalize(app.getCode()) + "_" + Long.toString(now.toEpochMilli(), 36));
        entity.setTenantId(app.getTenantId());
        entity.setAppId(appId);
        entity.setSecretHash(secretHasher.hash(apiKey));
        entity.setSecretPrefix(apiKey.substring(0, Math.min(12, apiKey.length())));
        entity.setEnabled(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        integrationAppSecretMapper.insert(entity);
        return new GeneratedApiKey(entity.getId(), entity.getSecretPrefix(), apiKey);
    }

    public IntegrationAppScopeEntity createIntegrationAppScope(String appId, String scopeType, String scopeId, String permission) {
        IntegrationAppEntity app = Optional.ofNullable(integrationAppMapper.selectById(appId))
                .orElseThrow(() -> new AuthException("APP_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app does not exist."));
        Instant now = Instant.now();
        IntegrationAppScopeEntity entity = new IntegrationAppScopeEntity();
        entity.setId("scope_" + normalize(app.getCode()) + "_" + normalize(scopeType) + "_" + normalize(scopeId));
        entity.setTenantId(app.getTenantId());
        entity.setAppId(appId);
        entity.setScopeType(scopeType);
        entity.setScopeId(scopeId);
        entity.setPermission(permission == null || permission.isBlank() ? "USE" : permission);
        entity.setEnabled(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        integrationAppScopeMapper.insert(entity);
        return entity;
    }

    public IntegrationAppEntity updateIntegrationAppStatus(String appId, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "DISABLED").contains(normalizedStatus)) {
            throw new AuthException("INVALID_APP_STATUS", HttpStatus.BAD_REQUEST, "Integration app status must be ACTIVE or DISABLED.");
        }
        IntegrationAppEntity app = Optional.ofNullable(integrationAppMapper.selectById(appId))
                .orElseThrow(() -> new AuthException("APP_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app does not exist."));
        integrationAppMapper.update(null, new LambdaUpdateWrapper<IntegrationAppEntity>()
                .eq(IntegrationAppEntity::getId, app.getId())
                .set(IntegrationAppEntity::getStatus, normalizedStatus)
                .set(IntegrationAppEntity::getUpdatedAt, Instant.now()));
        return integrationAppMapper.selectById(appId);
    }

    public IntegrationAppEntity deleteIntegrationApp(String appId) {
        IntegrationAppEntity app = Optional.ofNullable(integrationAppMapper.selectById(appId))
                .orElseThrow(() -> new AuthException("APP_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app does not exist."));
        integrationAppMapper.update(null, new LambdaUpdateWrapper<IntegrationAppEntity>()
                .eq(IntegrationAppEntity::getId, app.getId())
                .set(IntegrationAppEntity::getStatus, "DELETED")
                .set(IntegrationAppEntity::getUpdatedAt, Instant.now()));
        return integrationAppMapper.selectById(appId);
    }

    private AuthUserPrincipal toPrincipal(UserEntity user) {
        List<String> organizationIds = userOrganizationMapper.selectList(new LambdaQueryWrapper<UserOrganizationEntity>()
                        .eq(UserOrganizationEntity::getUserId, user.getId()))
                .stream()
                .sorted(Comparator
                        .comparing((UserOrganizationEntity entity) -> Boolean.TRUE.equals(entity.getPrimaryOrganization())).reversed()
                        .thenComparing(UserOrganizationEntity::getOrganizationId))
                .map(UserOrganizationEntity::getOrganizationId)
                .toList();
        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());
        return new AuthUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getTenantId(),
                user.getDisplayName(),
                user.getUserType(),
                user.getSortOrder() == null ? 0 : user.getSortOrder(),
                organizationIds,
                organizationIds.isEmpty() ? null : organizationIds.get(0),
                organizationIds,
                organizationIds.isEmpty() ? null : organizationIds.get(0),
                List.of(),
                roleCodes
        );
    }

    private Optional<RoleEntity> findRoleByCode(String roleCode) {
        return Optional.ofNullable(roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, defaultTenantId)
                .eq(RoleEntity::getCode, roleCode)));
    }

    private String generateApiKey() {
        byte[] random = new byte[24];
        secureRandom.nextBytes(random);
        return "agi_" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String normalize(String value) {
        return value == null ? "default" : value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private OrganizationEntity findParentOrganization(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        return Optional.ofNullable(organizationMapper.selectById(parentId))
                .orElseThrow(() -> new AuthException("PARENT_ORGANIZATION_NOT_FOUND", HttpStatus.BAD_REQUEST, "Parent organization does not exist."));
    }

    private String normalizeOrgType(String orgType) {
        String normalizedOrgType = orgType == null || orgType.isBlank() ? "DEPARTMENT" : orgType.trim().toUpperCase();
        if (!List.of("UNIT", "DEPARTMENT", "GROUP", "OTHER").contains(normalizedOrgType)) {
            throw new AuthException("INVALID_ORGANIZATION_TYPE", HttpStatus.BAD_REQUEST, "Organization type must be UNIT, DEPARTMENT, GROUP, or OTHER.");
        }
        return normalizedOrgType;
    }

    private String normalizeActiveStatus(String status, String errorCode) {
        String normalizedStatus = status == null ? "ACTIVE" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "DISABLED").contains(normalizedStatus)) {
            throw new AuthException(errorCode, HttpStatus.BAD_REQUEST, "Status must be ACTIVE or DISABLED.");
        }
        return normalizedStatus;
    }

    public record GeneratedApiKey(String id, String secretPrefix, String apiKey) {
    }

    public record UserSortOrderUpdate(String userId, Integer sortOrder) {
    }
}
