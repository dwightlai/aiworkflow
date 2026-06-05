package com.mw.ai.agi.auth.api;

import com.mw.ai.agi.auth.persistence.DepartmentEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeEntity;
import com.mw.ai.agi.auth.persistence.RoleEntity;
import com.mw.ai.agi.auth.persistence.TenantEntity;
import com.mw.ai.agi.auth.persistence.UnitEntity;
import com.mw.ai.agi.auth.service.AuthAdminService;
import com.mw.ai.agi.auth.service.AuthUserPrincipal;
import com.mw.ai.agi.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin")
public class AuthAdminController {
    private final AuthAdminService authAdminService;

    public AuthAdminController(AuthAdminService authAdminService) {
        this.authAdminService = authAdminService;
    }

    @GetMapping("/tenants")
    public ApiResponse<PageResponse<TenantEntity>> listTenants() {
        List<TenantEntity> tenants = authAdminService.listTenants();
        return ApiResponse.success(new PageResponse<>(tenants, tenants.size()));
    }

    @GetMapping("/units")
    public ApiResponse<PageResponse<UnitEntity>> listUnits() {
        List<UnitEntity> units = authAdminService.listUnits();
        return ApiResponse.success(new PageResponse<>(units, units.size()));
    }

    @PostMapping("/units")
    public ApiResponse<UnitEntity> createUnit(@Valid @RequestBody SaveUnitRequest request) {
        return ApiResponse.success(authAdminService.createUnit(
                request.code(),
                request.externalUnitId(),
                request.name(),
                request.unitType()
        ));
    }

    @GetMapping("/departments")
    public ApiResponse<PageResponse<DepartmentEntity>> listDepartments() {
        List<DepartmentEntity> departments = authAdminService.listDepartments();
        return ApiResponse.success(new PageResponse<>(departments, departments.size()));
    }

    @PostMapping("/departments")
    public ApiResponse<DepartmentEntity> createDepartment(@Valid @RequestBody SaveDepartmentRequest request) {
        return ApiResponse.success(authAdminService.createDepartment(
                request.unitId(),
                request.code(),
                request.externalDepartmentId(),
                request.parentId(),
                request.name(),
                request.sortOrder()
        ));
    }

    @GetMapping("/roles")
    public ApiResponse<PageResponse<RoleEntity>> listRoles() {
        List<RoleEntity> roles = authAdminService.listRoles();
        return ApiResponse.success(new PageResponse<>(roles, roles.size()));
    }

    @PostMapping("/roles")
    public ApiResponse<RoleEntity> createRole(@Valid @RequestBody SaveRoleRequest request) {
        return ApiResponse.success(authAdminService.createRole(
                request.unitId(),
                request.code(),
                request.name(),
                request.roleType(),
                request.externalRoleId()
        ));
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<AuthUserPrincipal>> listUsers() {
        List<AuthUserPrincipal> users = authAdminService.listUsers();
        return ApiResponse.success(new PageResponse<>(users, users.size()));
    }

    @PostMapping("/users")
    public ApiResponse<AuthUserPrincipal> createUser(@Valid @RequestBody SaveUserRequest request) {
        return ApiResponse.success(authAdminService.createLocalUser(
                request.username(),
                request.password(),
                request.displayName(),
                request.mobile(),
                request.email(),
                request.unitIds(),
                request.departmentIds(),
                request.roleCodes()
        ));
    }

    @GetMapping("/integration-apps")
    public ApiResponse<PageResponse<IntegrationAppEntity>> listIntegrationApps() {
        List<IntegrationAppEntity> apps = authAdminService.listIntegrationApps();
        return ApiResponse.success(new PageResponse<>(apps, apps.size()));
    }

    @PostMapping("/integration-apps")
    public ApiResponse<IntegrationAppEntity> createIntegrationApp(@Valid @RequestBody SaveIntegrationAppRequest request) {
        return ApiResponse.success(authAdminService.createIntegrationApp(
                request.code(),
                request.name(),
                request.appType(),
                request.authType()
        ));
    }

    @PostMapping("/integration-apps/{appId}/status")
    public ApiResponse<IntegrationAppEntity> updateIntegrationAppStatus(
            @PathVariable String appId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ApiResponse.success(authAdminService.updateIntegrationAppStatus(appId, request.status()));
    }

    @PostMapping("/integration-apps/{appId}/secrets")
    public ApiResponse<AuthAdminService.GeneratedApiKey> createIntegrationAppSecret(
            @PathVariable String appId,
            @RequestBody(required = false) SaveIntegrationAppSecretRequest request
    ) {
        return ApiResponse.success(authAdminService.createIntegrationAppSecret(appId));
    }

    @PostMapping("/integration-apps/{appId}/scopes")
    public ApiResponse<IntegrationAppScopeEntity> createIntegrationAppScope(
            @PathVariable String appId,
            @Valid @RequestBody SaveIntegrationAppScopeRequest request
    ) {
        return ApiResponse.success(authAdminService.createIntegrationAppScope(
                appId,
                request.scopeType(),
                request.scopeId(),
                request.permission()
        ));
    }

    public record SaveUnitRequest(@NotBlank String code, String externalUnitId, @NotBlank String name, String unitType) {
    }

    public record SaveDepartmentRequest(
            @NotBlank String unitId,
            @NotBlank String code,
            String externalDepartmentId,
            String parentId,
            @NotBlank String name,
            Integer sortOrder
    ) {
    }

    public record SaveRoleRequest(
            String unitId,
            @NotBlank String code,
            @NotBlank String name,
            String roleType,
            String externalRoleId
    ) {
    }

    public record SaveUserRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String displayName,
            String mobile,
            String email,
            List<String> unitIds,
            List<String> departmentIds,
            List<String> roleCodes
    ) {
    }

    public record SaveIntegrationAppRequest(
            @NotBlank String code,
            @NotBlank String name,
            String appType,
            String authType
    ) {
    }

    public record SaveIntegrationAppSecretRequest(String secretName) {
    }

    public record SaveIntegrationAppScopeRequest(@NotBlank String scopeType, @NotBlank String scopeId, String permission) {
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
