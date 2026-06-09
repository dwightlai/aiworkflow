import { describe, expect, it, vi } from 'vitest';
import {
  createIntegrationAppSecret,
  createOrganization,
  createRole,
  createUser,
  deleteIntegrationApp,
  deleteOrganization,
  deleteRole,
  deleteUser,
  listIntegrationAppScopes,
  listIntegrationAppSecrets,
  listIntegrationApps,
  listOrganizations,
  listRoles,
  listUsers,
  resetUserPassword,
  updateUserSortOrders,
  updateUser,
  updateOrganization,
  updateRole,
  updateUserStatus
} from './identity';

const TENANT_BASE = '/api/auth/admin/tenants/tenant_default';

describe('identity api', () => {
  it('loads identity organization resources from auth admin endpoints', async () => {
    const fetchMock = mockFetch({
      [`${TENANT_BASE}/organizations`]: { items: [{ id: 'org_default_unit', code: 'default_unit' }], total: 1 },
      [`${TENANT_BASE}/roles`]: { items: [{ id: 'role_app_user', code: 'app_user' }], total: 1 },
      [`${TENANT_BASE}/users`]: { items: [{ id: 'user_admin', username: 'admin' }], total: 1 },
      [`${TENANT_BASE}/integration-apps`]: { items: [{ id: 'app_archive', code: 'archive' }], total: 1 },
      [`${TENANT_BASE}/integration-apps/app_archive/scopes`]: { items: [], total: 0 },
      [`${TENANT_BASE}/integration-apps/app_archive/secrets`]: { items: [], total: 0 }
    });

    await expect(listOrganizations()).resolves.toEqual({ items: [{ id: 'org_default_unit', code: 'default_unit' }], total: 1 });
    await expect(listRoles()).resolves.toEqual({ items: [{ id: 'role_app_user', code: 'app_user' }], total: 1 });
    await expect(listUsers()).resolves.toEqual({ items: [{ id: 'user_admin', username: 'admin' }], total: 1 });
    await expect(listIntegrationApps()).resolves.toEqual({ items: [{ id: 'app_archive', code: 'archive' }], total: 1 });
    await expect(listIntegrationAppScopes('app_archive')).resolves.toEqual({ items: [], total: 0 });
    await expect(listIntegrationAppSecrets('app_archive')).resolves.toEqual({ items: [], total: 0 });

    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/organizations`);
    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/integration-apps`);
  });

  it('posts user lifecycle and integration app actions', async () => {
    const fetchMock = mockFetch({
      [`${TENANT_BASE}/users`]: { id: 'user_new', username: 'new-user' },
      [`${TENANT_BASE}/users/user_new`]: { id: 'user_new', username: 'new-user', displayName: 'New User Updated' },
      [`${TENANT_BASE}/users/user_new_delete`]: { id: 'user_new_delete', status: 'DELETED' },
      [`${TENANT_BASE}/users/user_new/status`]: { id: 'user_new', status: 'LOCKED' },
      [`${TENANT_BASE}/users/sort-orders`]: { items: [{ id: 'user_new', sortOrder: 1 }], total: 1 },
      [`${TENANT_BASE}/users/user_new/reset-password`]: { id: 'user_new', username: 'new-user' },
      [`${TENANT_BASE}/integration-apps/app_archive/secrets`]: {
        id: 'secret_1',
        secretPrefix: 'agi_',
        apiKey: 'agi_test_key'
      }
    });

    await expect(createUser({
      username: 'new-user',
      password: 'pass123456',
      displayName: 'New User',
      organizationIds: ['org_default_unit'],
      roleCodes: ['app_user']
    })).resolves.toEqual({ id: 'user_new', username: 'new-user' });

    await expect(updateUser('user_new', {
      username: 'new-user',
      displayName: 'New User Updated',
      organizationIds: ['org_default_unit'],
      roleCodes: ['app_user']
    })).resolves.toEqual({ id: 'user_new', username: 'new-user', displayName: 'New User Updated' });

    await expect(deleteUser('user_new_delete')).resolves.toEqual({ id: 'user_new_delete', status: 'DELETED' });
    await expect(updateUserStatus('user_new', 'LOCKED')).resolves.toEqual({ id: 'user_new', status: 'LOCKED' });
    await expect(updateUserSortOrders('org_default_unit', [{ userId: 'user_new', sortOrder: 1 }]))
      .resolves.toEqual({ items: [{ id: 'user_new', sortOrder: 1 }], total: 1 });
    await expect(resetUserPassword('user_new', 'newpass123456')).resolves.toEqual({ id: 'user_new', username: 'new-user' });
    await expect(createIntegrationAppSecret('app_archive')).resolves.toEqual({
      id: 'secret_1',
      secretPrefix: 'agi_',
      apiKey: 'agi_test_key'
    });

    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/users`, expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/users/user_new`, expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/users/user_new_delete`, expect.objectContaining({ method: 'DELETE' }));
    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/users/user_new/status`, expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/users/sort-orders`, expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/users/user_new/reset-password`, expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/integration-apps/app_archive/secrets`, expect.objectContaining({ method: 'POST' }));
  });

  it('posts organization and role lifecycle actions', async () => {
    const fetchMock = mockFetch({
      [`${TENANT_BASE}/organizations`]: { id: 'org_ops', code: 'ops' },
      [`${TENANT_BASE}/organizations/org_ops`]: { id: 'org_ops', name: 'Ops Updated', status: 'DISABLED' },
      [`${TENANT_BASE}/roles`]: { id: 'role_ops', code: 'ops_user' },
      [`${TENANT_BASE}/roles/role_ops`]: { id: 'role_ops', name: 'Ops User Updated', status: 'DISABLED' },
      [`${TENANT_BASE}/integration-apps/app_ops`]: { id: 'app_ops', status: 'DELETED' }
    });

    await expect(createOrganization({
      code: 'ops',
      name: 'Ops',
      orgType: 'UNIT'
    })).resolves.toEqual({ id: 'org_ops', code: 'ops' });

    await expect(updateOrganization('org_ops', {
      name: 'Ops Updated',
      status: 'DISABLED'
    })).resolves.toEqual({ id: 'org_ops', name: 'Ops Updated', status: 'DISABLED' });

    await expect(deleteOrganization('org_ops')).resolves.toEqual({ id: 'org_ops', name: 'Ops Updated', status: 'DISABLED' });

    await expect(createRole({
      code: 'ops_user',
      name: 'Ops User',
      roleType: 'BUSINESS'
    })).resolves.toEqual({ id: 'role_ops', code: 'ops_user' });

    await expect(updateRole('role_ops', {
      name: 'Ops User Updated',
      status: 'DISABLED'
    })).resolves.toEqual({ id: 'role_ops', name: 'Ops User Updated', status: 'DISABLED' });

    await expect(deleteRole('role_ops')).resolves.toEqual({ id: 'role_ops', name: 'Ops User Updated', status: 'DISABLED' });
    await expect(deleteIntegrationApp('app_ops')).resolves.toEqual({ id: 'app_ops', status: 'DELETED' });

    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/organizations/org_ops`, expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenCalledWith(`${TENANT_BASE}/organizations/org_ops`, expect.objectContaining({ method: 'DELETE' }));
  });
});

function mockFetch(routes: Record<string, unknown>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString();
    const path = url.replace(/^https?:\/\/[^/]+/, '');
    const payload = routes[path];
    if (payload === undefined) {
      throw new Error(`Unexpected fetch: ${path}`);
    }
    return {
      ok: true,
      status: 200,
      json: async () => ({ success: true, data: payload, error: null })
    } as Response;
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}
