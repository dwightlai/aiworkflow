import { requestJson, resolveIdentityTenantId } from './auth';

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export interface Tenant {
  id: string;
  code: string;
  name: string;
  status: string;
}

export interface Organization {
  id: string;
  tenantId: string;
  code: string;
  externalOrgId?: string | null;
  name: string;
  orgType: string;
  parentId?: string | null;
  path: string;
  level: number;
  sortOrder?: number;
  status: string;
}

export interface Role {
  id: string;
  tenantId: string;
  organizationId?: string | null;
  externalRoleId?: string | null;
  code: string;
  name: string;
  roleType: string;
  status: string;
}

export interface IdentityUser {
  id: string;
  username: string;
  tenantId?: string;
  displayName: string;
  userType: string;
  sortOrder?: number;
  status?: string;
  organizationIds: string[];
  activeOrganizationId?: string | null;
  unitIds: string[];
  activeUnitId?: string | null;
  departmentIds: string[];
  roleIds: string[];
}

export interface IntegrationApp {
  id: string;
  tenantId?: string;
  code: string;
  name: string;
  appType: string;
  authType: string;
  status: string;
}

export interface IntegrationAppScope {
  id: string;
  appId: string;
  scopeType: string;
  scopeId: string;
  permission: string;
  enabled: boolean;
}

export interface GeneratedApiKey {
  id: string;
  secretPrefix: string;
  apiKey: string;
}

export interface SaveUserRequest {
  username: string;
  password: string;
  displayName: string;
  mobile?: string | null;
  email?: string | null;
  sortOrder?: number;
  organizationIds: string[];
  roleCodes: string[];
}

export interface SaveOrganizationRequest {
  code: string;
  externalOrgId?: string | null;
  name: string;
  orgType: string;
  parentId?: string | null;
  sortOrder?: number;
}

export interface UpdateOrganizationRequest {
  externalOrgId?: string | null;
  name: string;
  orgType?: string;
  parentId?: string | null;
  sortOrder?: number;
  status?: string;
}

export interface SaveRoleRequest {
  organizationId?: string | null;
  code: string;
  name: string;
  roleType: string;
  externalRoleId?: string | null;
}

export interface UpdateRoleRequest {
  organizationId?: string | null;
  code?: string;
  name: string;
  roleType?: string;
  externalRoleId?: string | null;
  status?: string;
}

export interface UpdateUserRequest {
  username: string;
  password?: string;
  displayName: string;
  mobile?: string | null;
  email?: string | null;
  sortOrder?: number;
  organizationIds: string[];
  roleCodes: string[];
}

export interface UserSortOrderUpdate {
  userId: string;
  sortOrder: number;
}

export interface SaveIntegrationAppRequest {
  code: string;
  name: string;
  appType: string;
  authType: string;
}

export interface SaveIntegrationAppScopeRequest {
  scopeType: string;
  scopeId: string;
  permission: string;
}

export interface SaveTenantRequest {
  code: string;
  name: string;
}

export interface UpdateTenantRequest {
  name: string;
  status?: string;
}

export async function listTenants(): Promise<PageResponse<Tenant>> {
  return requestJson<PageResponse<Tenant>>('/api/auth/admin/tenants');
}

export async function createTenant(request: SaveTenantRequest): Promise<Tenant> {
  return requestJson<Tenant>('/api/auth/admin/tenants', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateTenant(tenantId: string, request: UpdateTenantRequest): Promise<Tenant> {
  return requestJson<Tenant>(`/api/auth/admin/tenants/${tenantId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteTenant(tenantId: string): Promise<Tenant> {
  return requestJson<Tenant>(`/api/auth/admin/tenants/${tenantId}`, {
    method: 'DELETE'
  });
}

function tenantAdminBase(tenantId?: string) {
  return `/api/auth/admin/tenants/${resolveIdentityTenantId(tenantId)}`;
}

export async function getTenant(tenantId: string): Promise<Tenant> {
  return requestJson<Tenant>(`/api/auth/admin/tenants/${tenantId}`);
}

export async function listOrganizations(tenantId?: string): Promise<PageResponse<Organization>> {
  return requestJson<PageResponse<Organization>>(`${tenantAdminBase(tenantId)}/organizations`);
}

export async function listRoles(tenantId?: string): Promise<PageResponse<Role>> {
  return requestJson<PageResponse<Role>>(`${tenantAdminBase(tenantId)}/roles`);
}

export async function listUsers(tenantId?: string): Promise<PageResponse<IdentityUser>> {
  return requestJson<PageResponse<IdentityUser>>(`${tenantAdminBase(tenantId)}/users`);
}

export async function createOrganization(request: SaveOrganizationRequest, tenantId?: string): Promise<Organization> {
  return requestJson<Organization>(`${tenantAdminBase(tenantId)}/organizations`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateOrganization(organizationId: string, request: UpdateOrganizationRequest, tenantId?: string): Promise<Organization> {
  return requestJson<Organization>(`${tenantAdminBase(tenantId)}/organizations/${organizationId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteOrganization(organizationId: string, tenantId?: string): Promise<Organization> {
  return requestJson<Organization>(`${tenantAdminBase(tenantId)}/organizations/${organizationId}`, {
    method: 'DELETE'
  });
}

export async function createRole(request: SaveRoleRequest, tenantId?: string): Promise<Role> {
  return requestJson<Role>(`${tenantAdminBase(tenantId)}/roles`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateRole(roleId: string, request: UpdateRoleRequest, tenantId?: string): Promise<Role> {
  return requestJson<Role>(`${tenantAdminBase(tenantId)}/roles/${roleId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteRole(roleId: string, tenantId?: string): Promise<Role> {
  return requestJson<Role>(`${tenantAdminBase(tenantId)}/roles/${roleId}`, {
    method: 'DELETE'
  });
}

export async function listIntegrationApps(tenantId?: string): Promise<PageResponse<IntegrationApp>> {
  return requestJson<PageResponse<IntegrationApp>>(`${tenantAdminBase(tenantId)}/integration-apps`);
}

export async function createUser(request: SaveUserRequest, tenantId?: string): Promise<IdentityUser> {
  return requestJson<IdentityUser>(`${tenantAdminBase(tenantId)}/users`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateUser(userId: string, request: UpdateUserRequest, tenantId?: string): Promise<IdentityUser> {
  return requestJson<IdentityUser>(`${tenantAdminBase(tenantId)}/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function updateUserSortOrders(organizationId: string, items: UserSortOrderUpdate[], tenantId?: string): Promise<PageResponse<IdentityUser>> {
  return requestJson<PageResponse<IdentityUser>>(`${tenantAdminBase(tenantId)}/users/sort-orders`, {
    method: 'PUT',
    body: JSON.stringify({ organizationId, items })
  });
}

export async function deleteUser(userId: string, tenantId?: string): Promise<IdentityUser> {
  return requestJson<IdentityUser>(`${tenantAdminBase(tenantId)}/users/${userId}`, {
    method: 'DELETE'
  });
}

export async function updateUserStatus(userId: string, status: string, tenantId?: string): Promise<IdentityUser> {
  return requestJson<IdentityUser>(`${tenantAdminBase(tenantId)}/users/${userId}/status`, {
    method: 'POST',
    body: JSON.stringify({ status })
  });
}

export async function resetUserPassword(userId: string, password: string, tenantId?: string): Promise<IdentityUser> {
  return requestJson<IdentityUser>(`${tenantAdminBase(tenantId)}/users/${userId}/reset-password`, {
    method: 'POST',
    body: JSON.stringify({ password })
  });
}

export async function createIntegrationApp(request: SaveIntegrationAppRequest, tenantId?: string): Promise<IntegrationApp> {
  return requestJson<IntegrationApp>(`${tenantAdminBase(tenantId)}/integration-apps`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateIntegrationAppStatus(appId: string, status: string, tenantId?: string): Promise<IntegrationApp> {
  return requestJson<IntegrationApp>(`${tenantAdminBase(tenantId)}/integration-apps/${appId}/status`, {
    method: 'POST',
    body: JSON.stringify({ status })
  });
}

export async function deleteIntegrationApp(appId: string, tenantId?: string): Promise<IntegrationApp> {
  return requestJson<IntegrationApp>(`${tenantAdminBase(tenantId)}/integration-apps/${appId}`, {
    method: 'DELETE'
  });
}

export async function createIntegrationAppSecret(appId: string, tenantId?: string): Promise<GeneratedApiKey> {
  return requestJson<GeneratedApiKey>(`${tenantAdminBase(tenantId)}/integration-apps/${appId}/secrets`, {
    method: 'POST',
    body: '{}'
  });
}

export async function createIntegrationAppScope(
  appId: string,
  request: SaveIntegrationAppScopeRequest,
  tenantId?: string
): Promise<IntegrationAppScope> {
  return requestJson<IntegrationAppScope>(`${tenantAdminBase(tenantId)}/integration-apps/${appId}/scopes`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}
