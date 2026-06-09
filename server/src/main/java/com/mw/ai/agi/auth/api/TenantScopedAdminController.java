package com.mw.ai.agi.auth.api;

import com.mw.ai.agi.auth.persistence.IntegrationAppEntity;
import com.mw.ai.agi.auth.persistence.IntegrationAppScopeEntity;
import com.mw.ai.agi.auth.persistence.OrganizationEntity;
import com.mw.ai.agi.auth.persistence.RoleEntity;
import com.mw.ai.agi.auth.persistence.TenantEntity;
import com.mw.ai.agi.auth.persistence.UserEntity;
import com.mw.ai.agi.auth.service.AuthAdminService;
import com.mw.ai.agi.auth.service.AuthUserPrincipal;
import com.mw.ai.agi.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin/tenants/{tenantId}")
public class TenantScopedAdminController {
    private final AuthAdminService authAdminService;

    public TenantScopedAdminController(AuthAdminService authAdminService) {
        this.authAdminService = authAdminService;
    }

    @GetMapping
    public ApiResponse<TenantEntity> getTenant(@PathVariable String tenantId) {
        return ApiResponse.success(authAdminService.getTenant(tenantId));
    }

    @GetMapping("/organizations")
    public ApiResponse<AuthAdminController.PageResponse<OrganizationEntity>> listOrganizations(@PathVariable String tenantId) {
        List<OrganizationEntity> organizations = authAdminService.listOrganizationsForTenant(tenantId);
        return ApiResponse.success(new AuthAdminController.PageResponse<>(organizations, organizations.size()));
    }

    @PostMapping("/organizations")
    public ApiResponse<OrganizationEntity> createOrganization(
            @PathVariable String tenantId,
            @Valid @RequestBody AuthAdminController.SaveOrganizationRequest request
    ) {
        return ApiResponse.success(authAdminService.createOrganizationForTenant(
                tenantId,
                request.parentId(),
                request.code(),
                request.externalOrgId(),
                request.name(),
                request.orgType(),
                request.sortOrder()
        ));
    }

    @PutMapping("/organizations/{organizationId}")
    public ApiResponse<OrganizationEntity> updateOrganization(
            @PathVariable String tenantId,
            @PathVariable String organizationId,
            @Valid @RequestBody AuthAdminController.UpdateOrganizationRequest request
    ) {
        return ApiResponse.success(authAdminService.updateOrganizationForTenant(
                tenantId,
                organizationId,
                request.parentId(),
                request.externalOrgId(),
                request.name(),
                request.orgType(),
                request.sortOrder(),
                request.status()
        ));
    }

    @DeleteMapping("/organizations/{organizationId}")
    public ApiResponse<OrganizationEntity> deleteOrganization(
            @PathVariable String tenantId,
            @PathVariable String organizationId
    ) {
        return ApiResponse.success(authAdminService.deleteOrganizationForTenant(tenantId, organizationId));
    }

    @GetMapping("/roles")
    public ApiResponse<AuthAdminController.PageResponse<RoleEntity>> listRoles(@PathVariable String tenantId) {
        List<RoleEntity> roles = authAdminService.listRolesForTenant(tenantId);
        return ApiResponse.success(new AuthAdminController.PageResponse<>(roles, roles.size()));
    }

    @PostMapping("/roles")
    public ApiResponse<RoleEntity> createRole(
            @PathVariable String tenantId,
            @Valid @RequestBody AuthAdminController.SaveRoleRequest request
    ) {
        return ApiResponse.success(authAdminService.createRoleForTenant(
                tenantId,
                request.organizationId(),
                request.code(),
                request.name(),
                request.roleType(),
                request.externalRoleId()
        ));
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<RoleEntity> updateRole(
            @PathVariable String tenantId,
            @PathVariable String roleId,
            @Valid @RequestBody AuthAdminController.UpdateRoleRequest request
    ) {
        return ApiResponse.success(authAdminService.updateRoleForTenant(
                tenantId,
                roleId,
                request.organizationId(),
                request.code(),
                request.name(),
                request.roleType(),
                request.externalRoleId(),
                request.status()
        ));
    }

    @DeleteMapping("/roles/{roleId}")
    public ApiResponse<RoleEntity> deleteRole(@PathVariable String tenantId, @PathVariable String roleId) {
        return ApiResponse.success(authAdminService.deleteRoleForTenant(tenantId, roleId));
    }

    @GetMapping("/users")
    public ApiResponse<AuthAdminController.PageResponse<AuthUserPrincipal>> listUsers(@PathVariable String tenantId) {
        List<AuthUserPrincipal> users = authAdminService.listUsersForTenant(tenantId);
        return ApiResponse.success(new AuthAdminController.PageResponse<>(users, users.size()));
    }

    @PostMapping("/users")
    public ApiResponse<AuthUserPrincipal> createUser(
            @PathVariable String tenantId,
            @Valid @RequestBody AuthAdminController.SaveUserRequest request
    ) {
        return ApiResponse.success(authAdminService.createLocalUserForTenant(
                tenantId,
                request.username(),
                request.password(),
                request.displayName(),
                request.mobile(),
                request.email(),
                request.sortOrder(),
                request.organizationIds(),
                request.roleCodes()
        ));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<AuthUserPrincipal> updateUser(
            @PathVariable String tenantId,
            @PathVariable String userId,
            @Valid @RequestBody AuthAdminController.UpdateUserRequest request
    ) {
        return ApiResponse.success(authAdminService.updateLocalUserForTenant(
                tenantId,
                userId,
                request.username(),
                request.password(),
                request.displayName(),
                request.mobile(),
                request.email(),
                request.sortOrder(),
                request.organizationIds(),
                request.roleCodes()
        ));
    }

    @PutMapping("/users/sort-orders")
    public ApiResponse<AuthAdminController.PageResponse<AuthUserPrincipal>> updateUserSortOrders(
            @PathVariable String tenantId,
            @Valid @RequestBody AuthAdminController.UpdateUserSortOrdersRequest request
    ) {
        List<AuthUserPrincipal> users = authAdminService.updateUserSortOrdersForTenant(
                tenantId,
                request.organizationId(),
                request.items() == null ? List.of() : request.items().stream()
                        .map(item -> new AuthAdminService.UserSortOrderUpdate(item.userId(), item.sortOrder()))
                        .toList()
        );
        return ApiResponse.success(new AuthAdminController.PageResponse<>(users, users.size()));
    }

    @PostMapping("/users/{userId}/status")
    public ApiResponse<UserEntity> updateUserStatus(
            @PathVariable String tenantId,
            @PathVariable String userId,
            @Valid @RequestBody AuthAdminController.UpdateStatusRequest request
    ) {
        return ApiResponse.success(authAdminService.updateUserStatusForTenant(tenantId, userId, request.status()));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<UserEntity> deleteUser(@PathVariable String tenantId, @PathVariable String userId) {
        return ApiResponse.success(authAdminService.deleteUserForTenant(tenantId, userId));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<AuthUserPrincipal> resetUserPassword(
            @PathVariable String tenantId,
            @PathVariable String userId,
            @Valid @RequestBody AuthAdminController.ResetPasswordRequest request
    ) {
        return ApiResponse.success(authAdminService.resetLocalUserPasswordForTenant(tenantId, userId, request.password()));
    }

    @GetMapping("/integration-apps")
    public ApiResponse<AuthAdminController.PageResponse<IntegrationAppEntity>> listIntegrationApps(@PathVariable String tenantId) {
        List<IntegrationAppEntity> apps = authAdminService.listIntegrationAppsForTenant(tenantId);
        return ApiResponse.success(new AuthAdminController.PageResponse<>(apps, apps.size()));
    }

    @PostMapping("/integration-apps")
    public ApiResponse<IntegrationAppEntity> createIntegrationApp(
            @PathVariable String tenantId,
            @Valid @RequestBody AuthAdminController.SaveIntegrationAppRequest request
    ) {
        return ApiResponse.success(authAdminService.createIntegrationAppForTenant(
                tenantId,
                request.code(),
                request.name(),
                request.appType(),
                request.authType()
        ));
    }

    @PostMapping("/integration-apps/{appId}/status")
    public ApiResponse<IntegrationAppEntity> updateIntegrationAppStatus(
            @PathVariable String tenantId,
            @PathVariable String appId,
            @Valid @RequestBody AuthAdminController.UpdateStatusRequest request
    ) {
        return ApiResponse.success(authAdminService.updateIntegrationAppStatusForTenant(tenantId, appId, request.status()));
    }

    @DeleteMapping("/integration-apps/{appId}")
    public ApiResponse<IntegrationAppEntity> deleteIntegrationApp(@PathVariable String tenantId, @PathVariable String appId) {
        return ApiResponse.success(authAdminService.deleteIntegrationAppForTenant(tenantId, appId));
    }

    @PostMapping("/integration-apps/{appId}/secrets")
    public ApiResponse<AuthAdminService.GeneratedApiKey> createIntegrationAppSecret(
            @PathVariable String tenantId,
            @PathVariable String appId,
            @RequestBody(required = false) AuthAdminController.SaveIntegrationAppSecretRequest request
    ) {
        return ApiResponse.success(authAdminService.createIntegrationAppSecretForTenant(tenantId, appId));
    }

    @PostMapping("/integration-apps/{appId}/scopes")
    public ApiResponse<IntegrationAppScopeEntity> createIntegrationAppScope(
            @PathVariable String tenantId,
            @PathVariable String appId,
            @Valid @RequestBody AuthAdminController.SaveIntegrationAppScopeRequest request
    ) {
        return ApiResponse.success(authAdminService.createIntegrationAppScopeForTenant(
                tenantId,
                appId,
                request.scopeType(),
                request.scopeId(),
                request.permission()
        ));
    }
}
