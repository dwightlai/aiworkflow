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
  name: string;
  roleType?: string;
  externalRoleId?: string | null;
  status?: string;
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

export async function listTenants(): Promise<PageResponse<Tenant>> {
  return requestJson<PageResponse<Tenant>>('/api/auth/admin/tenants');
}

export async function listOrganizations(): Promise<PageResponse<Organization>> {
  return requestJson<PageResponse<Organization>>('/api/auth/admin/organizations');
}

export async function listRoles(): Promise<PageResponse<Role>> {
  return requestJson<PageResponse<Role>>('/api/auth/admin/roles');
}

export async function listUsers(): Promise<PageResponse<IdentityUser>> {
  return requestJson<PageResponse<IdentityUser>>('/api/auth/admin/users');
}

export async function createOrganization(request: SaveOrganizationRequest): Promise<Organization> {
  return requestJson<Organization>('/api/auth/admin/organizations', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateOrganization(organizationId: string, request: UpdateOrganizationRequest): Promise<Organization> {
  return requestJson<Organization>(`/api/auth/admin/organizations/${organizationId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function createRole(request: SaveRoleRequest): Promise<Role> {
  return requestJson<Role>('/api/auth/admin/roles', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateRole(roleId: string, request: UpdateRoleRequest): Promise<Role> {
  return requestJson<Role>(`/api/auth/admin/roles/${roleId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function listIntegrationApps(): Promise<PageResponse<IntegrationApp>> {
  return requestJson<PageResponse<IntegrationApp>>('/api/auth/admin/integration-apps');
}

export async function createUser(request: SaveUserRequest): Promise<IdentityUser> {
  return requestJson<IdentityUser>('/api/auth/admin/users', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateUser(userId: string, request: Omit<SaveUserRequest, 'username' | 'password'>): Promise<IdentityUser> {
  return requestJson<IdentityUser>(`/api/auth/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function updateUserStatus(userId: string, status: string): Promise<IdentityUser> {
  return requestJson<IdentityUser>(`/api/auth/admin/users/${userId}/status`, {
    method: 'POST',
    body: JSON.stringify({ status })
  });
}

export async function resetUserPassword(userId: string, password: string): Promise<IdentityUser> {
  return requestJson<IdentityUser>(`/api/auth/admin/users/${userId}/reset-password`, {
    method: 'POST',
    body: JSON.stringify({ password })
  });
}

export async function createIntegrationApp(request: SaveIntegrationAppRequest): Promise<IntegrationApp> {
  return requestJson<IntegrationApp>('/api/auth/admin/integration-apps', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateIntegrationAppStatus(appId: string, status: string): Promise<IntegrationApp> {
  return requestJson<IntegrationApp>(`/api/auth/admin/integration-apps/${appId}/status`, {
    method: 'POST',
    body: JSON.stringify({ status })
  });
}

export async function createIntegrationAppSecret(appId: string): Promise<GeneratedApiKey> {
  return requestJson<GeneratedApiKey>(`/api/auth/admin/integration-apps/${appId}/secrets`, {
    method: 'POST',
    body: '{}'
  });
}

export async function createIntegrationAppScope(
  appId: string,
  request: SaveIntegrationAppScopeRequest
): Promise<IntegrationAppScope> {
  return requestJson<IntegrationAppScope>(`/api/auth/admin/integration-apps/${appId}/scopes`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = init ? await fetch(url, withJsonHeaders(init)) : await fetch(url);
  const envelope = await response.json() as ApiEnvelope<T>;
  if (!response.ok || !envelope.success) {
    throw new Error(envelope.error?.message ?? `Request failed: ${response.status}`);
  }
  return envelope.data;
}

function withJsonHeaders(init?: RequestInit): RequestInit | undefined {
  if (!init) {
    return undefined;
  }
  return {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init.headers
    }
  };
}
