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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping("/tenants")
    public ApiResponse<TenantEntity> createTenant(@Valid @RequestBody SaveTenantRequest request) {
        return ApiResponse.success(authAdminService.createTenant(request.code(), request.name()));
    }

    @PutMapping("/tenants/{tenantId}")
    public ApiResponse<TenantEntity> updateTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantRequest request
    ) {
        return ApiResponse.success(authAdminService.updateTenant(tenantId, request.name(), request.status()));
    }

    @DeleteMapping("/tenants/{tenantId}")
    public ApiResponse<TenantEntity> deleteTenant(@PathVariable String tenantId) {
        return ApiResponse.success(authAdminService.deleteTenant(tenantId));
    }

    @GetMapping("/organizations")
    public ApiResponse<PageResponse<OrganizationEntity>> listOrganizations(
            @RequestParam(required = false) String parentId,
            @RequestParam(defaultValue = "false") boolean childrenOnly,
            @RequestParam(defaultValue = "false") boolean countOnly
    ) {
        String tenantId = authAdminService.currentTenantId();
        if (countOnly) {
            long total = authAdminService.countOrganizationsForTenant(tenantId);
            return ApiResponse.success(new PageResponse<>(List.of(), total));
        }
        List<OrganizationEntity> organizations = childrenOnly
                ? authAdminService.listOrganizationChildrenForTenant(tenantId, parentId)
                : authAdminService.listOrganizations();
        return ApiResponse.success(new PageResponse<>(organizations, organizations.size()));
    }

    @PostMapping("/organizations")
    public ApiResponse<OrganizationEntity> createOrganization(@Valid @RequestBody SaveOrganizationRequest request) {
        return ApiResponse.success(authAdminService.createOrganization(
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
            @PathVariable String organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return ApiResponse.success(authAdminService.updateOrganization(
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
    public ApiResponse<OrganizationEntity> deleteOrganization(@PathVariable String organizationId) {
        return ApiResponse.success(authAdminService.deleteOrganization(organizationId));
    }

    @GetMapping("/roles")
    public ApiResponse<PageResponse<RoleEntity>> listRoles() {
        List<RoleEntity> roles = authAdminService.listRoles();
        return ApiResponse.success(new PageResponse<>(roles, roles.size()));
    }

    @PostMapping("/roles")
    public ApiResponse<RoleEntity> createRole(@Valid @RequestBody SaveRoleRequest request) {
        return ApiResponse.success(authAdminService.createRole(
                request.organizationId(),
                request.code(),
                request.name(),
                request.roleType(),
                request.externalRoleId()
        ));
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<RoleEntity> updateRole(@PathVariable String roleId, @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success(authAdminService.updateRole(
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
    public ApiResponse<RoleEntity> deleteRole(@PathVariable String roleId) {
        return ApiResponse.success(authAdminService.deleteRole(roleId));
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<AuthUserPrincipal>> listUsers(
            @RequestParam(required = false) String organizationId,
            @RequestParam(defaultValue = "false") boolean countOnly
    ) {
        String tenantId = authAdminService.currentTenantId();
        if (countOnly) {
            long total = authAdminService.countUsersForTenant(tenantId);
            return ApiResponse.success(new PageResponse<>(List.of(), total));
        }
        List<AuthUserPrincipal> users = authAdminService.listUsersForTenant(tenantId, organizationId);
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
                request.sortOrder(),
                request.organizationIds(),
                request.roleCodes()
        ));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<AuthUserPrincipal> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success(authAdminService.updateLocalUser(
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
    public ApiResponse<PageResponse<AuthUserPrincipal>> updateUserSortOrders(
            @Valid @RequestBody UpdateUserSortOrdersRequest request
    ) {
        List<AuthUserPrincipal> users = authAdminService.updateUserSortOrders(
                request.organizationId(),
                request.items() == null ? List.of() : request.items().stream()
                        .map(item -> new AuthAdminService.UserSortOrderUpdate(item.userId(), item.sortOrder()))
                        .toList()
        );
        return ApiResponse.success(new PageResponse<>(users, users.size()));
    }

    @PostMapping("/users/{userId}/status")
    public ApiResponse<UserEntity> updateUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ApiResponse.success(authAdminService.updateUserStatus(userId, request.status()));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<UserEntity> deleteUser(@PathVariable String userId) {
        return ApiResponse.success(authAdminService.deleteUser(userId));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<AuthUserPrincipal> resetUserPassword(
            @PathVariable String userId,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ApiResponse.success(authAdminService.resetLocalUserPassword(userId, request.password()));
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

    @DeleteMapping("/integration-apps/{appId}")
    public ApiResponse<IntegrationAppEntity> deleteIntegrationApp(@PathVariable String appId) {
        return ApiResponse.success(authAdminService.deleteIntegrationApp(appId));
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

    @GetMapping("/integration-apps/{appId}/scopes")
    public ApiResponse<PageResponse<IntegrationAppScopeEntity>> listIntegrationAppScopes(@PathVariable String appId) {
        List<IntegrationAppScopeEntity> scopes = authAdminService.listIntegrationAppScopes(appId);
        return ApiResponse.success(new PageResponse<>(scopes, scopes.size()));
    }

    @DeleteMapping("/integration-apps/{appId}/scopes/{scopeId}")
    public ApiResponse<Void> deleteIntegrationAppScope(@PathVariable String appId, @PathVariable String scopeId) {
        authAdminService.deleteIntegrationAppScope(appId, scopeId);
        return ApiResponse.success(null);
    }

    @GetMapping("/integration-apps/{appId}/secrets")
    public ApiResponse<PageResponse<AuthAdminService.IntegrationAppSecretView>> listIntegrationAppSecrets(@PathVariable String appId) {
        List<AuthAdminService.IntegrationAppSecretView> secrets = authAdminService.listIntegrationAppSecrets(appId);
        return ApiResponse.success(new PageResponse<>(secrets, secrets.size()));
    }

    @DeleteMapping("/integration-apps/{appId}/secrets/{secretId}")
    public ApiResponse<Void> deleteIntegrationAppSecret(@PathVariable String appId, @PathVariable String secretId) {
        authAdminService.deleteIntegrationAppSecret(appId, secretId);
        return ApiResponse.success(null);
    }

    public record SaveTenantRequest(@NotBlank String code, @NotBlank String name) {
    }

    public record UpdateTenantRequest(@NotBlank String name, String status) {
    }

    public record SaveOrganizationRequest(
            String parentId,
            @NotBlank String code,
            String externalOrgId,
            @NotBlank String name,
            String orgType,
            Integer sortOrder
    ) {
    }

    public record UpdateOrganizationRequest(
            String parentId,
            String externalOrgId,
            @NotBlank String name,
            String orgType,
            Integer sortOrder,
            String status
    ) {
    }

    public record SaveRoleRequest(
            String organizationId,
            @NotBlank String code,
            @NotBlank String name,
            String roleType,
            String externalRoleId
    ) {
    }

    public record UpdateRoleRequest(
            String organizationId,
            @NotBlank String code,
            @NotBlank String name,
            String roleType,
            String externalRoleId,
            String status
    ) {
    }

    public record SaveUserRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String displayName,
            String mobile,
            String email,
            Integer sortOrder,
            List<String> organizationIds,
            List<String> roleCodes
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank String username,
            String password,
            @NotBlank String displayName,
            String mobile,
            String email,
            Integer sortOrder,
            List<String> organizationIds,
            List<String> roleCodes
    ) {
    }

    public record UpdateUserSortOrdersRequest(
            @NotBlank String organizationId,
            List<UserSortOrderRequest> items
    ) {
    }

    public record UserSortOrderRequest(
            @NotBlank String userId,
            Integer sortOrder
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

    public record ResetPasswordRequest(@NotBlank String password) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
