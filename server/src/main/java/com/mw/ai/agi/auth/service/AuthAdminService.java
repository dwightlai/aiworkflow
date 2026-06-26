package com.mw.ai.agi.auth.service;

import com.mw.ai.agi.common.asset.AssetReferenceSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mw.ai.agi.common.audit.OperatorContext;
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
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
    private final TenantAdminGuard tenantAdminGuard;
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
            TenantAdminGuard tenantAdminGuard,
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
        this.tenantAdminGuard = tenantAdminGuard;
        this.defaultTenantId = defaultTenantId;
    }

    public List<TenantEntity> listTenants() {
        tenantAdminGuard.assertPlatformOperator();
        return tenantMapper.selectList(new LambdaQueryWrapper<TenantEntity>().orderByAsc(TenantEntity::getCode));
    }

    public TenantEntity createTenant(String code, String name) {
        tenantAdminGuard.assertPlatformOperator();
        if (code == null || code.isBlank()) {
            throw new AuthException("TENANT_CODE_REQUIRED", HttpStatus.BAD_REQUEST, "Tenant code is required.");
        }
        if (name == null || name.isBlank()) {
            throw new AuthException("TENANT_NAME_REQUIRED", HttpStatus.BAD_REQUEST, "Tenant name is required.");
        }
        String normalizedCode = code.trim();
        findTenantByCode(normalizedCode).ifPresent(existing -> {
            throw new AuthException("TENANT_CODE_EXISTS", HttpStatus.BAD_REQUEST, "Tenant code already exists.");
        });
        Instant now = Instant.now();
        TenantEntity entity = new TenantEntity();
        entity.setId("tenant_" + normalize(normalizedCode));
        entity.setCode(normalizedCode);
        entity.setName(name.trim());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        tenantMapper.insert(entity);
        initializeTenantResources(entity.getId(), entity.getName());
        return entity;
    }

    public TenantEntity getTenant(String tenantId) {
        ensureTenantAccess(tenantId);
        return findTenantOrThrow(tenantId);
    }

    public TenantEntity updateTenant(String tenantId, String name, String status) {
        tenantAdminGuard.assertPlatformOperator();
        ensureTenantAccess(tenantId);
        TenantEntity tenant = findTenantOrThrow(tenantId);
        if (name == null || name.isBlank()) {
            throw new AuthException("TENANT_NAME_REQUIRED", HttpStatus.BAD_REQUEST, "Tenant name is required.");
        }
        String normalizedStatus = normalizeActiveStatus(status, "INVALID_TENANT_STATUS");
        if (defaultTenantId.equals(tenant.getId()) && "DISABLED".equals(normalizedStatus)) {
            throw new AuthException("DEFAULT_TENANT_PROTECTED", HttpStatus.BAD_REQUEST, "Default tenant cannot be disabled.");
        }
        tenantMapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, tenant.getId())
                .set(TenantEntity::getName, name.trim())
                .set(TenantEntity::getStatus, normalizedStatus)
                .set(TenantEntity::getUpdatedAt, Instant.now()));
        return tenantMapper.selectById(tenantId);
    }

    public TenantEntity deleteTenant(String tenantId) {
        tenantAdminGuard.assertPlatformOperator();
        ensureTenantAccess(tenantId);
        TenantEntity tenant = findTenantOrThrow(tenantId);
        if (defaultTenantId.equals(tenant.getId())) {
            throw new AuthException("DEFAULT_TENANT_PROTECTED", HttpStatus.BAD_REQUEST, "Default tenant cannot be deleted.");
        }
        Instant now = Instant.now();
        organizationMapper.update(null, new LambdaUpdateWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .set(OrganizationEntity::getStatus, "DELETED")
                .set(OrganizationEntity::getUpdatedAt, now));
        roleMapper.update(null, new LambdaUpdateWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, tenantId)
                .set(RoleEntity::getStatus, "DELETED")
                .set(RoleEntity::getUpdatedAt, now));
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getTenantId, tenantId)
                .set(UserEntity::getStatus, "DELETED")
                .set(UserEntity::getUpdatedAt, now));
        integrationAppMapper.update(null, new LambdaUpdateWrapper<IntegrationAppEntity>()
                .eq(IntegrationAppEntity::getTenantId, tenantId)
                .set(IntegrationAppEntity::getStatus, "DELETED")
                .set(IntegrationAppEntity::getUpdatedAt, now));
        tenantMapper.deleteById(tenant.getId());
        return tenant;
    }

    public List<OrganizationEntity> listOrganizations() {
        return listOrganizationsForTenant(tenantAdminGuard.currentTenantId());
    }

    public String currentTenantId() {
        return tenantAdminGuard.currentTenantId();
    }

    public List<OrganizationEntity> listOrganizationsForTenant(String tenantId) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        return organizationMapper.selectList(new LambdaQueryWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .ne(OrganizationEntity::getStatus, "DELETED")
                .orderByAsc(OrganizationEntity::getPath)
                .orderByAsc(OrganizationEntity::getSortOrder)
                .orderByAsc(OrganizationEntity::getCode));
    }

    public long countOrganizationsForTenant(String tenantId) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        return organizationMapper.selectCount(new LambdaQueryWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .ne(OrganizationEntity::getStatus, "DELETED"));
    }

    public List<OrganizationEntity> listOrganizationChildrenForTenant(String tenantId, String parentId) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        LambdaQueryWrapper<OrganizationEntity> wrapper = new LambdaQueryWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .ne(OrganizationEntity::getStatus, "DELETED");
        if (parentId == null || parentId.isBlank()) {
            wrapper.and(query -> query.isNull(OrganizationEntity::getParentId)
                    .or()
                    .eq(OrganizationEntity::getParentId, ""));
        } else {
            wrapper.eq(OrganizationEntity::getParentId, parentId);
        }
        List<OrganizationEntity> organizations = organizationMapper.selectList(wrapper
                .orderByAsc(OrganizationEntity::getSortOrder)
                .orderByAsc(OrganizationEntity::getCode)
                .orderByAsc(OrganizationEntity::getName));
        for (OrganizationEntity organization : organizations) {
            organization.setHasChildren(hasOrganizationChildrenForTenant(tenantId, organization.getId()));
        }
        return organizations;
    }

    private boolean hasOrganizationChildrenForTenant(String tenantId, String organizationId) {
        return organizationMapper.selectCount(new LambdaQueryWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .eq(OrganizationEntity::getParentId, organizationId)
                .ne(OrganizationEntity::getStatus, "DELETED")) > 0;
    }

    public List<RoleEntity> listRoles() {
        return listRolesForTenant(tenantAdminGuard.currentTenantId());
    }

    public List<RoleEntity> listRolesForTenant(String tenantId) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, tenantId)
                .ne(RoleEntity::getStatus, "DELETED")
                .orderByAsc(RoleEntity::getCode));
    }

    public List<AuthUserPrincipal> listUsers() {
        return listUsersForTenant(tenantAdminGuard.currentTenantId());
    }

    public List<AuthUserPrincipal> listUsersForTenant(String tenantId) {
        return listUsersForTenant(tenantId, null);
    }

    public long countUsersForTenant(String tenantId) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        return userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getTenantId, tenantId)
                .ne(UserEntity::getStatus, "DELETED"));
    }

    public List<AuthUserPrincipal> listUsersForTenant(String tenantId, String organizationId) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        if (organizationId == null || organizationId.isBlank()) {
            return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                            .eq(UserEntity::getTenantId, tenantId)
                            .ne(UserEntity::getStatus, "DELETED")
                            .orderByAsc(UserEntity::getSortOrder)
                            .orderByAsc(UserEntity::getUsername))
                    .stream()
                    .map(this::toPrincipal)
                    .toList();
        }
        requireOrganizationForTenant(tenantId, organizationId);
        List<String> userIds = userOrganizationMapper.selectList(new LambdaQueryWrapper<UserOrganizationEntity>()
                        .eq(UserOrganizationEntity::getOrganizationId, organizationId))
                .stream()
                .map(UserOrganizationEntity::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                        .in(UserEntity::getId, userIds)
                        .eq(UserEntity::getTenantId, tenantId)
                        .ne(UserEntity::getStatus, "DELETED")
                        .orderByAsc(UserEntity::getSortOrder)
                        .orderByAsc(UserEntity::getUsername))
                .stream()
                .map(this::toPrincipal)
                .toList();
    }

    public List<IntegrationAppEntity> listIntegrationApps() {
        return listIntegrationAppsForTenant(tenantAdminGuard.currentTenantId());
    }

    public List<IntegrationAppEntity> listIntegrationAppsForTenant(String tenantId) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        return integrationAppMapper.selectList(new LambdaQueryWrapper<IntegrationAppEntity>()
                .eq(IntegrationAppEntity::getTenantId, tenantId)
                .ne(IntegrationAppEntity::getStatus, "DELETED")
                .orderByAsc(IntegrationAppEntity::getCode));
    }

    public OrganizationEntity createOrganization(String parentId, String code, String externalOrgId, String name, String orgType, Integer sortOrder) {
        return createOrganizationForTenant(tenantAdminGuard.currentTenantId(), parentId, code, externalOrgId, name, orgType, sortOrder);
    }

    public OrganizationEntity createOrganizationForTenant(String tenantId, String parentId, String code, String externalOrgId, String name, String orgType, Integer sortOrder) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        Instant now = Instant.now();
        OrganizationEntity parent = findParentOrganizationForTenant(tenantId, parentId);
        OrganizationEntity entity = new OrganizationEntity();
        entity.setId(organizationId(tenantId, code));
        entity.setTenantId(tenantId);
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
        return updateOrganizationForTenant(tenantAdminGuard.currentTenantId(), organizationId, parentId, externalOrgId, name, orgType, sortOrder, status);
    }

    public OrganizationEntity updateOrganizationForTenant(
            String tenantId,
            String organizationId,
            String parentId,
            String externalOrgId,
            String name,
            String orgType,
            Integer sortOrder,
            String status
    ) {
        ensureTenantAccess(tenantId);
        OrganizationEntity organization = requireOrganizationForTenant(tenantId, organizationId);
        OrganizationEntity parent = findParentOrganizationForTenant(tenantId, parentId);
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
        return deleteOrganizationForTenant(tenantAdminGuard.currentTenantId(), organizationId);
    }

    public OrganizationEntity deleteOrganizationForTenant(String tenantId, String organizationId) {
        ensureTenantAccess(tenantId);
        OrganizationEntity organization = requireOrganizationForTenant(tenantId, organizationId);
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
        return createRoleForTenant(tenantAdminGuard.currentTenantId(), organizationId, code, name, roleType, externalRoleId);
    }

    public RoleEntity createRoleForTenant(String tenantId, String organizationId, String code, String name, String roleType, String externalRoleId) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        findRoleByCode(tenantId, code).ifPresent(existing -> {
            throw new AuthException("ROLE_CODE_EXISTS", HttpStatus.BAD_REQUEST, "Role code already exists.");
        });
        Instant now = Instant.now();
        RoleEntity entity = new RoleEntity();
        entity.setId(roleId(tenantId, code));
        entity.setTenantId(tenantId);
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
        return updateRoleForTenant(tenantAdminGuard.currentTenantId(), roleId, organizationId, code, name, roleType, externalRoleId, status);
    }

    public RoleEntity updateRoleForTenant(String tenantId, String roleId, String organizationId, String code, String name, String roleType, String externalRoleId, String status) {
        ensureTenantAccess(tenantId);
        RoleEntity role = requireRoleForTenant(tenantId, roleId);
        String normalizedCode = code == null || code.isBlank() ? role.getCode() : code.trim();
        if (!normalizedCode.equals(role.getCode())) {
            findRoleByCode(tenantId, normalizedCode)
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
        return deleteRoleForTenant(tenantAdminGuard.currentTenantId(), roleId);
    }

    public RoleEntity deleteRoleForTenant(String tenantId, String roleId) {
        ensureTenantAccess(tenantId);
        RoleEntity role = requireRoleForTenant(tenantId, roleId);
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
        return createLocalUserForTenant(tenantAdminGuard.currentTenantId(), username, password, displayName, mobile, email, sortOrder, organizationIds, roleCodes);
    }

    public AuthUserPrincipal createLocalUserForTenant(
            String tenantId,
            String username,
            String password,
            String displayName,
            String mobile,
            String email,
            Integer sortOrder,
            List<String> organizationIds,
            List<String> roleCodes
    ) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getTenantId, tenantId)
                        .eq(UserEntity::getUsername, username)))
                .ifPresent(existing -> {
                    throw new AuthException("USERNAME_EXISTS", HttpStatus.BAD_REQUEST, "Username already exists.");
                });
        Instant now = Instant.now();
        UserEntity entity = new UserEntity();
        entity.setId(userId(tenantId, username));
        entity.setTenantId(tenantId);
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
            requireOrganizationForTenant(tenantId, normalizedOrganizationIds.get(index));
            UserOrganizationEntity relation = new UserOrganizationEntity();
            relation.setId(entity.getId() + "_org_" + normalize(normalizedOrganizationIds.get(index)));
            relation.setTenantId(tenantId);
            relation.setUserId(entity.getId());
            relation.setOrganizationId(normalizedOrganizationIds.get(index));
            relation.setPrimaryOrganization(index == 0);
            relation.setCreatedAt(now);
            userOrganizationMapper.insert(relation);
        }

        for (String roleCode : roleCodes == null ? List.<String>of() : roleCodes) {
            RoleEntity role = findRoleByCode(tenantId, roleCode)
                    .orElseThrow(() -> new AuthException("ROLE_NOT_FOUND", HttpStatus.BAD_REQUEST, "Role does not exist."));
            UserRoleEntity relation = new UserRoleEntity();
            relation.setId(entity.getId() + "_role_" + normalize(role.getCode()));
            relation.setTenantId(tenantId);
            relation.setUserId(entity.getId());
            relation.setRoleId(role.getId());
            relation.setCreatedAt(now);
            userRoleMapper.insert(relation);
        }

        return toPrincipal(entity);
    }

    public UserEntity updateUserStatus(String userId, String status) {
        return updateUserStatusForTenant(tenantAdminGuard.currentTenantId(), userId, status);
    }

    public UserEntity updateUserStatusForTenant(String tenantId, String userId, String status) {
        ensureTenantAccess(tenantId);
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "DISABLED", "LOCKED").contains(normalizedStatus)) {
            throw new AuthException("INVALID_USER_STATUS", HttpStatus.BAD_REQUEST, "User status must be ACTIVE, DISABLED, or LOCKED.");
        }
        UserEntity user = requireUserForTenant(tenantId, userId);
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, user.getId())
                .set(UserEntity::getStatus, normalizedStatus)
                .set(UserEntity::getUpdatedAt, Instant.now()));
        return userMapper.selectById(userId);
    }

    public UserEntity deleteUser(String userId) {
        return deleteUserForTenant(tenantAdminGuard.currentTenantId(), userId);
    }

    public UserEntity deleteUserForTenant(String tenantId, String userId) {
        ensureTenantAccess(tenantId);
        UserEntity user = requireUserForTenant(tenantId, userId);
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, user.getId())
                .set(UserEntity::getStatus, "DELETED")
                .set(UserEntity::getUpdatedAt, Instant.now()));
        return userMapper.selectById(userId);
    }

    public AuthUserPrincipal updateLocalUser(
            String userId,
            String username,
            String password,
            String displayName,
            String mobile,
            String email,
            Integer sortOrder,
            List<String> organizationIds,
            List<String> roleCodes
    ) {
        return updateLocalUserForTenant(tenantAdminGuard.currentTenantId(), userId, username, password, displayName, mobile, email, sortOrder, organizationIds, roleCodes);
    }

    public AuthUserPrincipal updateLocalUserForTenant(
            String tenantId,
            String userId,
            String username,
            String password,
            String displayName,
            String mobile,
            String email,
            Integer sortOrder,
            List<String> organizationIds,
            List<String> roleCodes
    ) {
        ensureTenantAccess(tenantId);
        UserEntity user = requireUserForTenant(tenantId, userId);
        if (!"LOCAL".equals(user.getUserType())) {
            throw new AuthException("USER_UPDATE_UNSUPPORTED", HttpStatus.BAD_REQUEST, "Only local users can be updated.");
        }
        LambdaUpdateWrapper<UserEntity> updateWrapper = new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, user.getId())
                .set(UserEntity::getDisplayName, displayName)
                .set(UserEntity::getMobile, mobile)
                .set(UserEntity::getEmail, email)
                .set(UserEntity::getSortOrder, sortOrder == null ? 0 : sortOrder)
                .set(UserEntity::getUpdatedAt, Instant.now());
        if (username != null && !username.isBlank() && !username.equals(user.getUsername())) {
            Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                            .eq(UserEntity::getTenantId, user.getTenantId())
                            .eq(UserEntity::getUsername, username)))
                    .ifPresent(existing -> {
                        throw new AuthException("USERNAME_EXISTS", HttpStatus.BAD_REQUEST, "Username already exists.");
                    });
            updateWrapper.set(UserEntity::getUsername, username.trim());
        }
        if (password != null && !password.isBlank()) {
            updateWrapper.set(UserEntity::getPasswordHash, passwordEncoder.encode(password));
        }
        userMapper.update(null, updateWrapper);

        Instant now = Instant.now();
        userOrganizationMapper.delete(new LambdaQueryWrapper<UserOrganizationEntity>()
                .eq(UserOrganizationEntity::getUserId, user.getId()));
        List<String> normalizedOrganizationIds = organizationIds == null ? List.of() : organizationIds;
        for (int index = 0; index < normalizedOrganizationIds.size(); index++) {
            requireOrganizationForTenant(tenantId, normalizedOrganizationIds.get(index));
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
            RoleEntity role = findRoleByCode(tenantId, roleCode)
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
        return updateUserSortOrdersForTenant(tenantAdminGuard.currentTenantId(), organizationId, items);
    }

    public List<AuthUserPrincipal> updateUserSortOrdersForTenant(String tenantId, String organizationId, List<UserSortOrderUpdate> items) {
        ensureTenantAccess(tenantId);
        if (organizationId == null || organizationId.isBlank()) {
            throw new AuthException("ORGANIZATION_REQUIRED", HttpStatus.BAD_REQUEST, "Organization is required.");
        }
        requireOrganizationForTenant(tenantId, organizationId);
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
        return resetLocalUserPasswordForTenant(tenantAdminGuard.currentTenantId(), userId, password);
    }

    public AuthUserPrincipal resetLocalUserPasswordForTenant(String tenantId, String userId, String password) {
        ensureTenantAccess(tenantId);
        if (password == null || password.isBlank()) {
            throw new AuthException("INVALID_PASSWORD", HttpStatus.BAD_REQUEST, "Password cannot be blank.");
        }
        UserEntity user = requireUserForTenant(tenantId, userId);
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
        return createIntegrationAppForTenant(tenantAdminGuard.currentTenantId(), code, name, appType, authType);
    }

    public IntegrationAppEntity createIntegrationAppForTenant(String tenantId, String code, String name, String appType, String authType) {
        ensureTenantAccess(tenantId);
        findTenantOrThrow(tenantId);
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        IntegrationAppEntity entity = new IntegrationAppEntity();
        entity.setId(integrationAppId(tenantId, code));
        entity.setTenantId(tenantId);
        entity.setCode(code);
        entity.setName(name);
        entity.setAppType(appType == null || appType.isBlank() ? "OTHER" : appType);
        entity.setAuthType(authType == null || authType.isBlank() ? "API_KEY" : authType);
        entity.setStatus("ACTIVE");
        entity.setCreatedBy(operator);
        entity.setUpdatedBy(operator);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        integrationAppMapper.insert(entity);
        return entity;
    }

    public GeneratedApiKey createIntegrationAppSecret(String appId) {
        return createIntegrationAppSecretForTenant(tenantAdminGuard.currentTenantId(), appId);
    }

    public GeneratedApiKey createIntegrationAppSecretForTenant(String tenantId, String appId) {
        ensureTenantAccess(tenantId);
        IntegrationAppEntity app = requireIntegrationAppForTenant(tenantId, appId);
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
        return createIntegrationAppScopeForTenant(tenantAdminGuard.currentTenantId(), appId, scopeType, scopeId, permission);
    }

    public IntegrationAppScopeEntity createIntegrationAppScopeForTenant(String tenantId, String appId, String scopeType, String scopeId, String permission) {
        ensureTenantAccess(tenantId);
        IntegrationAppEntity app = requireIntegrationAppForTenant(tenantId, appId);
        String normalizedScopeId = AssetReferenceSupport.requireAssetId(scopeId);
        IntegrationAppScopeEntity existing = integrationAppScopeMapper.selectOne(new LambdaQueryWrapper<IntegrationAppScopeEntity>()
                .eq(IntegrationAppScopeEntity::getAppId, appId)
                .eq(IntegrationAppScopeEntity::getScopeType, scopeType)
                .eq(IntegrationAppScopeEntity::getScopeId, normalizedScopeId)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Instant now = Instant.now();
        IntegrationAppScopeEntity entity = new IntegrationAppScopeEntity();
        entity.setId(buildScopeRecordId(app.getCode(), scopeType, normalizedScopeId));
        entity.setTenantId(app.getTenantId());
        entity.setAppId(appId);
        entity.setScopeType(scopeType);
        entity.setScopeId(normalizedScopeId);
        entity.setPermission(permission == null || permission.isBlank() ? "USE" : permission);
        entity.setEnabled(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        integrationAppScopeMapper.insert(entity);
        return entity;
    }

    public List<IntegrationAppScopeEntity> listIntegrationAppScopes(String appId) {
        return listIntegrationAppScopesForTenant(tenantAdminGuard.currentTenantId(), appId);
    }

    public List<IntegrationAppScopeEntity> listIntegrationAppScopesForTenant(String tenantId, String appId) {
        ensureTenantAccess(tenantId);
        requireIntegrationAppForTenant(tenantId, appId);
        return integrationAppScopeMapper.selectList(new LambdaQueryWrapper<IntegrationAppScopeEntity>()
                .eq(IntegrationAppScopeEntity::getAppId, appId)
                .eq(IntegrationAppScopeEntity::getEnabled, true)
                .orderByAsc(IntegrationAppScopeEntity::getScopeType)
                .orderByAsc(IntegrationAppScopeEntity::getScopeId));
    }

    public void deleteIntegrationAppScope(String appId, String scopeId) {
        deleteIntegrationAppScopeForTenant(tenantAdminGuard.currentTenantId(), appId, scopeId);
    }

    public void deleteIntegrationAppScopeForTenant(String tenantId, String appId, String scopeId) {
        ensureTenantAccess(tenantId);
        requireIntegrationAppForTenant(tenantId, appId);
        IntegrationAppScopeEntity scope = Optional.ofNullable(integrationAppScopeMapper.selectById(scopeId))
                .orElseThrow(() -> new AuthException("APP_SCOPE_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app scope does not exist."));
        if (!appId.equals(scope.getAppId())) {
            throw new AuthException("APP_SCOPE_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app scope does not exist.");
        }
        integrationAppScopeMapper.deleteById(scopeId);
    }

    public List<IntegrationAppSecretView> listIntegrationAppSecrets(String appId) {
        return listIntegrationAppSecretsForTenant(tenantAdminGuard.currentTenantId(), appId);
    }

    public List<IntegrationAppSecretView> listIntegrationAppSecretsForTenant(String tenantId, String appId) {
        ensureTenantAccess(tenantId);
        requireIntegrationAppForTenant(tenantId, appId);
        return integrationAppSecretMapper.selectList(new LambdaQueryWrapper<IntegrationAppSecretEntity>()
                        .eq(IntegrationAppSecretEntity::getTenantId, tenantId)
                        .eq(IntegrationAppSecretEntity::getAppId, appId)
                        .orderByDesc(IntegrationAppSecretEntity::getCreatedAt))
                .stream()
                .map(secret -> new IntegrationAppSecretView(
                        secret.getId(),
                        secret.getSecretPrefix(),
                        Boolean.TRUE.equals(secret.getEnabled()),
                        secret.getExpiresAt(),
                        secret.getCreatedAt()
                ))
                .toList();
    }

    public void deleteIntegrationAppSecret(String appId, String secretId) {
        deleteIntegrationAppSecretForTenant(tenantAdminGuard.currentTenantId(), appId, secretId);
    }

    public void deleteIntegrationAppSecretForTenant(String tenantId, String appId, String secretId) {
        ensureTenantAccess(tenantId);
        requireIntegrationAppForTenant(tenantId, appId);
        IntegrationAppSecretEntity secret = Optional.ofNullable(integrationAppSecretMapper.selectById(secretId))
                .orElseThrow(() -> new AuthException("APP_SECRET_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app secret does not exist."));
        if (!tenantId.equals(secret.getTenantId()) || !appId.equals(secret.getAppId())) {
            throw new AuthException("APP_SECRET_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app secret does not exist.");
        }
        integrationAppSecretMapper.deleteById(secretId);
    }

    public IntegrationAppEntity updateIntegrationAppStatus(String appId, String status) {
        return updateIntegrationAppStatusForTenant(tenantAdminGuard.currentTenantId(), appId, status);
    }

    public IntegrationAppEntity updateIntegrationAppStatusForTenant(String tenantId, String appId, String status) {
        ensureTenantAccess(tenantId);
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "DISABLED").contains(normalizedStatus)) {
            throw new AuthException("INVALID_APP_STATUS", HttpStatus.BAD_REQUEST, "Integration app status must be ACTIVE or DISABLED.");
        }
        IntegrationAppEntity app = requireIntegrationAppForTenant(tenantId, appId);
        integrationAppMapper.update(null, new LambdaUpdateWrapper<IntegrationAppEntity>()
                .eq(IntegrationAppEntity::getId, app.getId())
                .set(IntegrationAppEntity::getStatus, normalizedStatus)
                .set(IntegrationAppEntity::getUpdatedBy, OperatorContext.currentUserId())
                .set(IntegrationAppEntity::getUpdatedAt, Instant.now()));
        return integrationAppMapper.selectById(appId);
    }

    public IntegrationAppEntity deleteIntegrationApp(String appId) {
        return deleteIntegrationAppForTenant(tenantAdminGuard.currentTenantId(), appId);
    }

    public IntegrationAppEntity deleteIntegrationAppForTenant(String tenantId, String appId) {
        ensureTenantAccess(tenantId);
        IntegrationAppEntity app = requireIntegrationAppForTenant(tenantId, appId);
        integrationAppMapper.update(null, new LambdaUpdateWrapper<IntegrationAppEntity>()
                .eq(IntegrationAppEntity::getId, app.getId())
                .set(IntegrationAppEntity::getStatus, "DELETED")
                .set(IntegrationAppEntity::getUpdatedBy, OperatorContext.currentUserId())
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

    private Optional<RoleEntity> findRoleByCode(String tenantId, String roleCode) {
        return Optional.ofNullable(roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, tenantId)
                .eq(RoleEntity::getCode, roleCode)));
    }

    private void initializeTenantResources(String tenantId, String tenantName) {
        if (defaultTenantId.equals(tenantId)) {
            return;
        }
        Instant now = Instant.now();
        String unitOrgId = organizationId(tenantId, "default_unit");
        OrganizationEntity unit = new OrganizationEntity();
        unit.setId(unitOrgId);
        unit.setTenantId(tenantId);
        unit.setCode("default_unit");
        unit.setName(tenantName + "默认单位");
        unit.setOrgType("UNIT");
        unit.setParentId(null);
        unit.setPath("/" + unitOrgId);
        unit.setLevel(1);
        unit.setSortOrder(0);
        unit.setStatus("ACTIVE");
        unit.setCreatedAt(now);
        unit.setUpdatedAt(now);
        organizationMapper.insert(unit);

        String deptOrgId = organizationId(tenantId, "default_dept");
        OrganizationEntity department = new OrganizationEntity();
        department.setId(deptOrgId);
        department.setTenantId(tenantId);
        department.setCode("default_dept");
        department.setName("默认部门");
        department.setOrgType("DEPARTMENT");
        department.setParentId(unitOrgId);
        department.setPath(unit.getPath() + "/" + deptOrgId);
        department.setLevel(2);
        department.setSortOrder(0);
        department.setStatus("ACTIVE");
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        organizationMapper.insert(department);

        insertPlatformRole(tenantId, "platform_admin", "Platform Administrator", now);
        insertPlatformRole(tenantId, "unit_admin", "Unit Administrator", now);
        insertPlatformRole(tenantId, "asset_manager", "Asset Manager", now);
        insertPlatformRole(tenantId, "app_user", "Application User", now);
        insertPlatformRole(tenantId, "integration_admin", "Integration Administrator", now);
        insertPlatformRole(tenantId, "auditor", "Auditor", now);
    }

    private void insertPlatformRole(String tenantId, String code, String name, Instant now) {
        RoleEntity role = new RoleEntity();
        role.setId(roleId(tenantId, code));
        role.setTenantId(tenantId);
        role.setOrganizationId(null);
        role.setExternalRoleId(null);
        role.setCode(code);
        role.setName(name);
        role.setRoleType("PLATFORM");
        role.setStatus("ACTIVE");
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        roleMapper.insert(role);
    }

    private OrganizationEntity requireOrganizationForTenant(String tenantId, String organizationId) {
        OrganizationEntity organization = Optional.ofNullable(organizationMapper.selectById(organizationId))
                .orElseThrow(() -> new AuthException("ORGANIZATION_NOT_FOUND", HttpStatus.NOT_FOUND, "Organization does not exist."));
        if (!tenantId.equals(organization.getTenantId())) {
            throw new AuthException("TENANT_MISMATCH", HttpStatus.BAD_REQUEST, "Organization does not belong to tenant.");
        }
        return organization;
    }

    private RoleEntity requireRoleForTenant(String tenantId, String roleId) {
        RoleEntity role = Optional.ofNullable(roleMapper.selectById(roleId))
                .orElseThrow(() -> new AuthException("ROLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Role does not exist."));
        if (!tenantId.equals(role.getTenantId())) {
            throw new AuthException("TENANT_MISMATCH", HttpStatus.BAD_REQUEST, "Role does not belong to tenant.");
        }
        return role;
    }

    private UserEntity requireUserForTenant(String tenantId, String userId) {
        UserEntity user = Optional.ofNullable(userMapper.selectById(userId))
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User does not exist."));
        if (!tenantId.equals(user.getTenantId())) {
            throw new AuthException("TENANT_MISMATCH", HttpStatus.BAD_REQUEST, "User does not belong to tenant.");
        }
        return user;
    }

    private IntegrationAppEntity requireIntegrationAppForTenant(String tenantId, String appId) {
        IntegrationAppEntity app = Optional.ofNullable(integrationAppMapper.selectById(appId))
                .orElseThrow(() -> new AuthException("APP_NOT_FOUND", HttpStatus.NOT_FOUND, "Integration app does not exist."));
        if (!tenantId.equals(app.getTenantId())) {
            throw new AuthException("TENANT_MISMATCH", HttpStatus.BAD_REQUEST, "Integration app does not belong to tenant.");
        }
        return app;
    }

    private OrganizationEntity findParentOrganizationForTenant(String tenantId, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        return requireOrganizationForTenant(tenantId, parentId);
    }

    private String organizationId(String tenantId, String code) {
        if (defaultTenantId.equals(tenantId)) {
            return "org_" + normalize(code);
        }
        return tenantId + "_org_" + normalize(code);
    }

    private String roleId(String tenantId, String code) {
        if (defaultTenantId.equals(tenantId)) {
            return "role_" + normalize(code);
        }
        return tenantId + "_role_" + normalize(code);
    }

    private String userId(String tenantId, String username) {
        if (defaultTenantId.equals(tenantId)) {
            return "user_" + normalize(username);
        }
        return tenantId + "_user_" + normalize(username);
    }

    private String integrationAppId(String tenantId, String code) {
        if (defaultTenantId.equals(tenantId)) {
            return "app_" + normalize(code);
        }
        return tenantId + "_app_" + normalize(code);
    }

    private void ensureTenantAccess(String tenantId) {
        tenantAdminGuard.assertCanAccessTenant(tenantId);
    }

    private TenantEntity findTenantOrThrow(String tenantId) {
        return Optional.ofNullable(tenantMapper.selectById(tenantId))
                .orElseThrow(() -> new AuthException("TENANT_NOT_FOUND", HttpStatus.NOT_FOUND, "Tenant does not exist."));
    }

    private Optional<TenantEntity> findTenantByCode(String code) {
        return Optional.ofNullable(tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getCode, code)));
    }

    private boolean hasTenantDependencies(String tenantId) {
        if (organizationMapper.selectCount(new LambdaQueryWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .ne(OrganizationEntity::getStatus, "DELETED")) > 0) {
            return true;
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getTenantId, tenantId)
                .ne(UserEntity::getStatus, "DELETED")) > 0) {
            return true;
        }
        if (roleMapper.selectCount(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, tenantId)
                .ne(RoleEntity::getStatus, "DELETED")) > 0) {
            return true;
        }
        return integrationAppMapper.selectCount(new LambdaQueryWrapper<IntegrationAppEntity>()
                .eq(IntegrationAppEntity::getTenantId, tenantId)
                .ne(IntegrationAppEntity::getStatus, "DELETED")) > 0;
    }

    private String generateApiKey() {
        byte[] random = new byte[24];
        secureRandom.nextBytes(random);
        return "agi_" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String normalize(String value) {
        return value == null ? "default" : value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private String buildScopeRecordId(String appCode, String scopeType, String scopeId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String material = normalize(appCode) + "\0" + normalize(scopeType) + "\0" + scopeId;
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return "scope_" + hex;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private OrganizationEntity findParentOrganization(String parentId) {
        return findParentOrganizationForTenant(defaultTenantId, parentId);
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

    public record IntegrationAppSecretView(
            String id,
            String secretPrefix,
            boolean enabled,
            java.time.Instant expiresAt,
            java.time.Instant createdAt
    ) {
    }

    public record GeneratedApiKey(String id, String secretPrefix, String apiKey) {
    }

    public record UserSortOrderUpdate(String userId, Integer sortOrder) {
    }
}
