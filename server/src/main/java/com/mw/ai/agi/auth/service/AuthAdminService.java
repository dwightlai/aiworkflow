package com.mw.ai.agi.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mw.ai.agi.auth.persistence.DepartmentEntity;
import com.mw.ai.agi.auth.persistence.DepartmentMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeMapper;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppSecretMapper;
import com.mw.ai.agi.auth.persistence.RoleEntity;
import com.mw.ai.agi.auth.persistence.RoleMapper;
import com.mw.ai.agi.auth.persistence.TenantEntity;
import com.mw.ai.agi.auth.persistence.TenantMapper;
import com.mw.ai.agi.auth.persistence.UnitEntity;
import com.mw.ai.agi.auth.persistence.UnitMapper;
import com.mw.ai.agi.auth.persistence.UserDepartmentEntity;
import com.mw.ai.agi.auth.persistence.UserDepartmentMapper;
import com.mw.ai.agi.auth.persistence.UserEntity;
import com.mw.ai.agi.auth.persistence.UserMapper;
import com.mw.ai.agi.auth.persistence.UserRoleEntity;
import com.mw.ai.agi.auth.persistence.UserRoleMapper;
import com.mw.ai.agi.auth.persistence.UserUnitEntity;
import com.mw.ai.agi.auth.persistence.UserUnitMapper;
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

@Service
public class AuthAdminService {
    private final TenantMapper tenantMapper;
    private final UnitMapper unitMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserUnitMapper userUnitMapper;
    private final UserDepartmentMapper userDepartmentMapper;
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
            UnitMapper unitMapper,
            DepartmentMapper departmentMapper,
            RoleMapper roleMapper,
            UserMapper userMapper,
            UserUnitMapper userUnitMapper,
            UserDepartmentMapper userDepartmentMapper,
            UserRoleMapper userRoleMapper,
            IntegrationAppMapper integrationAppMapper,
            IntegrationAppSecretMapper integrationAppSecretMapper,
            IntegrationAppScopeMapper integrationAppScopeMapper,
            PasswordEncoder passwordEncoder,
            SecretHasher secretHasher,
            @Value("${agi.auth.default-tenant-id:tenant_default}") String defaultTenantId
    ) {
        this.tenantMapper = tenantMapper;
        this.unitMapper = unitMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userUnitMapper = userUnitMapper;
        this.userDepartmentMapper = userDepartmentMapper;
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

    public List<UnitEntity> listUnits() {
        return unitMapper.selectList(new LambdaQueryWrapper<UnitEntity>().orderByAsc(UnitEntity::getCode));
    }

    public List<DepartmentEntity> listDepartments() {
        return departmentMapper.selectList(new LambdaQueryWrapper<DepartmentEntity>()
                .orderByAsc(DepartmentEntity::getUnitId)
                .orderByAsc(DepartmentEntity::getSortOrder)
                .orderByAsc(DepartmentEntity::getCode));
    }

    public List<RoleEntity> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>().orderByAsc(RoleEntity::getCode));
    }

    public List<AuthUserPrincipal> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>().orderByAsc(UserEntity::getUsername))
                .stream()
                .map(this::toPrincipal)
                .toList();
    }

    public List<IntegrationAppEntity> listIntegrationApps() {
        return integrationAppMapper.selectList(new LambdaQueryWrapper<IntegrationAppEntity>().orderByAsc(IntegrationAppEntity::getCode));
    }

    public UnitEntity createUnit(String code, String externalUnitId, String name, String unitType) {
        Instant now = Instant.now();
        UnitEntity entity = new UnitEntity();
        entity.setId("unit_" + normalize(code));
        entity.setTenantId(defaultTenantId);
        entity.setCode(code);
        entity.setExternalUnitId(externalUnitId);
        entity.setName(name);
        entity.setUnitType(unitType == null || unitType.isBlank() ? "BUSINESS_ORG" : unitType);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        unitMapper.insert(entity);
        return entity;
    }

    public DepartmentEntity createDepartment(String unitId, String code, String externalDepartmentId, String parentId, String name, Integer sortOrder) {
        Instant now = Instant.now();
        DepartmentEntity entity = new DepartmentEntity();
        entity.setId("dept_" + normalize(code));
        entity.setTenantId(defaultTenantId);
        entity.setUnitId(unitId);
        entity.setCode(code);
        entity.setExternalDepartmentId(externalDepartmentId);
        entity.setParentId(parentId);
        entity.setName(name);
        entity.setSortOrder(sortOrder == null ? 0 : sortOrder);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        departmentMapper.insert(entity);
        return entity;
    }

    public RoleEntity createRole(String unitId, String code, String name, String roleType, String externalRoleId) {
        Instant now = Instant.now();
        RoleEntity entity = new RoleEntity();
        entity.setId("role_" + normalize(code));
        entity.setTenantId(defaultTenantId);
        entity.setUnitId(unitId);
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

    public AuthUserPrincipal createLocalUser(
            String username,
            String password,
            String displayName,
            String mobile,
            String email,
            List<String> unitIds,
            List<String> departmentIds,
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
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userMapper.insert(entity);

        List<String> normalizedUnitIds = unitIds == null ? List.of() : unitIds;
        for (int index = 0; index < normalizedUnitIds.size(); index++) {
            UserUnitEntity relation = new UserUnitEntity();
            relation.setId(entity.getId() + "_unit_" + normalize(normalizedUnitIds.get(index)));
            relation.setTenantId(defaultTenantId);
            relation.setUserId(entity.getId());
            relation.setUnitId(normalizedUnitIds.get(index));
            relation.setPrimaryUnit(index == 0);
            relation.setCreatedAt(now);
            userUnitMapper.insert(relation);
        }

        List<String> normalizedDepartmentIds = departmentIds == null ? List.of() : departmentIds;
        for (int index = 0; index < normalizedDepartmentIds.size(); index++) {
            UserDepartmentEntity relation = new UserDepartmentEntity();
            relation.setId(entity.getId() + "_dept_" + normalize(normalizedDepartmentIds.get(index)));
            relation.setTenantId(defaultTenantId);
            relation.setUserId(entity.getId());
            relation.setDepartmentId(normalizedDepartmentIds.get(index));
            relation.setPrimaryDepartment(index == 0);
            relation.setCreatedAt(now);
            userDepartmentMapper.insert(relation);
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

    private AuthUserPrincipal toPrincipal(UserEntity user) {
        List<String> unitIds = userUnitMapper.selectList(new LambdaQueryWrapper<UserUnitEntity>()
                        .eq(UserUnitEntity::getUserId, user.getId()))
                .stream()
                .sorted(Comparator
                        .comparing((UserUnitEntity entity) -> Boolean.TRUE.equals(entity.getPrimaryUnit())).reversed()
                        .thenComparing(UserUnitEntity::getUnitId))
                .map(UserUnitEntity::getUnitId)
                .toList();
        List<String> departmentIds = userDepartmentMapper.selectList(new LambdaQueryWrapper<UserDepartmentEntity>()
                        .eq(UserDepartmentEntity::getUserId, user.getId()))
                .stream()
                .sorted(Comparator
                        .comparing((UserDepartmentEntity entity) -> Boolean.TRUE.equals(entity.getPrimaryDepartment())).reversed()
                        .thenComparing(UserDepartmentEntity::getDepartmentId))
                .map(UserDepartmentEntity::getDepartmentId)
                .toList();
        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());
        return new AuthUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getTenantId(),
                user.getDisplayName(),
                user.getUserType(),
                unitIds,
                unitIds.isEmpty() ? null : unitIds.get(0),
                departmentIds,
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

    public record GeneratedApiKey(String id, String secretPrefix, String apiKey) {
    }
}
