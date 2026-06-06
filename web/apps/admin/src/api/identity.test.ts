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

describe('identity api', () => {
  it('loads identity organization resources from auth admin endpoints', async () => {
    const fetchMock = mockFetch({
      '/api/auth/admin/organizations': { items: [{ id: 'org_default_unit', code: 'default_unit' }], total: 1 },
      '/api/auth/admin/roles': { items: [{ id: 'role_app_user', code: 'app_user' }], total: 1 },
      '/api/auth/admin/users': { items: [{ id: 'user_admin', username: 'admin' }], total: 1 },
      '/api/auth/admin/integration-apps': { items: [{ id: 'app_archive', code: 'archive' }], total: 1 }
    });

    await expect(listOrganizations()).resolves.toEqual({ items: [{ id: 'org_default_unit', code: 'default_unit' }], total: 1 });
    await expect(listRoles()).resolves.toEqual({ items: [{ id: 'role_app_user', code: 'app_user' }], total: 1 });
    await expect(listUsers()).resolves.toEqual({ items: [{ id: 'user_admin', username: 'admin' }], total: 1 });
    await expect(listIntegrationApps()).resolves.toEqual({ items: [{ id: 'app_archive', code: 'archive' }], total: 1 });

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/organizations');
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/integration-apps');
  });

  it('posts user lifecycle and integration app actions', async () => {
    const fetchMock = mockFetch({
      '/api/auth/admin/users': { id: 'user_new', username: 'new-user' },
      '/api/auth/admin/users/user_new': { id: 'user_new', username: 'new-user', displayName: 'New User Updated' },
      '/api/auth/admin/users/user_new_delete': { id: 'user_new_delete', status: 'DELETED' },
      '/api/auth/admin/users/user_new/status': { id: 'user_new', status: 'LOCKED' },
      '/api/auth/admin/users/sort-orders': { items: [{ id: 'user_new', sortOrder: 1 }], total: 1 },
      '/api/auth/admin/users/user_new/reset-password': { id: 'user_new', username: 'new-user' },
      '/api/auth/admin/integration-apps/app_archive/secrets': {
        id: 'secret_1',
        secretPrefix: 'agi_xxx',
        apiKey: 'agi_xxx_full'
      }
    });

    await createUser({
      username: 'new-user',
      password: 'pass123456',
      displayName: 'New User',
      sortOrder: 7,
      organizationIds: ['org_default_unit'],
      roleCodes: ['app_user']
    });
    await updateUserStatus('user_new', 'LOCKED');
    await updateUser('user_new', {
      displayName: 'New User Updated',
      mobile: null,
      email: null,
      password: 'editpass123',
      sortOrder: 3,
      organizationIds: ['org_default_unit'],
      roleCodes: ['app_user']
    });
    await updateUserSortOrders('org_default_unit', [
      { userId: 'user_new', sortOrder: 1 },
      { userId: 'user_admin', sortOrder: 2 }
    ]);
    await resetUserPassword('user_new', 'newpass123');
    await deleteUser('user_new_delete');
    await createIntegrationAppSecret('app_archive');

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/users', expect.objectContaining({
      method: 'POST',
      body: expect.stringContaining('"sortOrder":7')
    }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/users/user_new/status', expect.objectContaining({
      method: 'POST',
      body: '{"status":"LOCKED"}'
    }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/users/user_new', expect.objectContaining({
      method: 'PUT',
      body: expect.stringContaining('"password":"editpass123"')
    }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/users/sort-orders', expect.objectContaining({
      method: 'PUT',
      body: expect.stringContaining('"organizationId":"org_default_unit"')
    }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/users/user_new/reset-password', expect.objectContaining({
      method: 'POST',
      body: '{"password":"newpass123"}'
    }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/users/user_new_delete', expect.objectContaining({
      method: 'DELETE'
    }));
  });

  it('posts organization resource create update and delete actions', async () => {
    const fetchMock = mockFetch({
      '/api/auth/admin/organizations': { id: 'org_ops', code: 'ops' },
      '/api/auth/admin/organizations/org_ops': { id: 'org_ops', name: 'Ops Updated', status: 'DISABLED' },
      '/api/auth/admin/roles': { id: 'role_ops', code: 'ops_role' },
      '/api/auth/admin/roles/role_ops': { id: 'role_ops', name: 'Ops Role Updated', status: 'DISABLED' },
      '/api/auth/admin/integration-apps/app_ops': { id: 'app_ops', status: 'DELETED' }
    });

    await createOrganization({ code: 'ops', name: 'Ops', orgType: 'UNIT', externalOrgId: 'U-1' });
    await updateOrganization('org_ops', { name: 'Ops Updated', orgType: 'UNIT', externalOrgId: 'U-2', parentId: null, status: 'DISABLED' });
    await deleteOrganization('org_ops');
    await createRole({ organizationId: 'org_ops', code: 'ops_role', name: 'Ops Role', roleType: 'BUSINESS', externalRoleId: 'R-1' });
    await updateRole('role_ops', { organizationId: 'org_ops', code: 'ops_role_new', name: 'Ops Role Updated', roleType: 'BUSINESS', externalRoleId: 'R-2', status: 'DISABLED' });
    await deleteRole('role_ops');
    await deleteIntegrationApp('app_ops');

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/organizations/org_ops', expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/organizations/org_ops', expect.objectContaining({ method: 'DELETE' }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/roles/role_ops', expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/roles/role_ops', expect.objectContaining({ body: expect.stringContaining('"code":"ops_role_new"') }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/roles/role_ops', expect.objectContaining({ method: 'DELETE' }));
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/admin/integration-apps/app_ops', expect.objectContaining({ method: 'DELETE' }));
  });
});

function mockFetch(routes: Record<string, unknown>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    const data = routes[url];
    if (!data) {
      return new Response(JSON.stringify({ success: false, data: null, error: { message: 'missing mock' } }), {
        status: 404
      });
    }
    return new Response(JSON.stringify({ success: true, data, error: null }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}
