// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { IdentityOrganizationPage } from './IdentityOrganizationPage';

const identityApiMock = vi.hoisted(() => ({
  listOrganizations: vi.fn(async () => ({
    items: [
      { id: 'org_default_unit', code: 'default_unit', name: '默认单位', orgType: 'UNIT', parentId: null, path: '/org_default_unit', level: 1, sortOrder: 0, status: 'ACTIVE' },
      { id: 'org_default_dept', code: 'default_dept', name: '默认部门', orgType: 'DEPARTMENT', parentId: 'org_default_unit', path: '/org_default_unit/org_default_dept', level: 2, sortOrder: 0, status: 'ACTIVE' }
    ],
    total: 2
  })),
  listRoles: vi.fn(async () => ({
    items: [{ id: 'role_app_user', code: 'app_user', name: '普通用户', roleType: 'PLATFORM', status: 'ACTIVE' }],
    total: 1
  })),
  listUsers: vi.fn(async () => ({
    items: [{
      id: 'user_admin',
      username: 'admin',
      displayName: '平台管理员',
      userType: 'LOCAL',
      organizationIds: ['org_default_unit'],
      roleIds: ['platform_admin']
    }],
    total: 1
  })),
  listIntegrationApps: vi.fn(async () => ({
    items: [{ id: 'app_archive', code: 'archive-system', name: '档案系统', appType: 'ARCHIVE_SYSTEM', authType: 'API_KEY', status: 'ACTIVE' }],
    total: 1
  })),
  createOrganization: vi.fn(async () => ({ id: 'org_ops', code: 'ops', name: '运营组织', orgType: 'UNIT', status: 'ACTIVE' })),
  updateOrganization: vi.fn(async () => ({ id: 'org_default_unit', status: 'DISABLED' })),
  createUser: vi.fn(async () => ({ id: 'user_archive', username: 'archive-user' })),
  updateUser: vi.fn(async () => ({ id: 'user_admin', displayName: '平台管理员2' })),
  updateUserStatus: vi.fn(async () => ({ id: 'user_admin', status: 'LOCKED' })),
  resetUserPassword: vi.fn(async () => ({ id: 'user_admin', username: 'admin' })),
  createRole: vi.fn(async () => ({ id: 'role_ops', code: 'ops_role', name: '运营角色', status: 'ACTIVE' })),
  updateRole: vi.fn(async () => ({ id: 'role_app_user', status: 'DISABLED' })),
  createIntegrationApp: vi.fn(async () => ({ id: 'app_oa', code: 'oa-system' })),
  createIntegrationAppSecret: vi.fn(async () => ({ id: 'secret_1', secretPrefix: 'agi_demo', apiKey: 'agi_demo_full' })),
  createIntegrationAppScope: vi.fn(async () => ({ id: 'scope_1', scopeType: 'ORGANIZATION', scopeId: 'org_default_unit' })),
  updateIntegrationAppStatus: vi.fn(async () => ({ id: 'app_archive', status: 'DISABLED' }))
}));

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
});

Object.defineProperty(window, 'getComputedStyle', {
  value: vi.fn(() => ({ getPropertyValue: vi.fn(() => ''), overflowX: 'auto', overflowY: 'auto' }))
});

vi.mock('../../api/identity', () => identityApiMock);

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('IdentityOrganizationPage', () => {
  it('shows organization user tabs with organization tree data', async () => {
    renderPage();

    expect(await screen.findByText('组织用户')).toBeInTheDocument();
    expect(await screen.findByText('默认单位')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '组织架构' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '用户' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '角色' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '第三方应用' })).toBeInTheDocument();
  });

  it('creates an organization and edits a user', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /新增组织/ }));
    await userEvent.type(await screen.findByLabelText('组织编码'), 'ops');
    await userEvent.type(screen.getByLabelText('组织名称'), '运营组织');
    await userEvent.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => expect(identityApiMock.createOrganization).toHaveBeenCalledWith(expect.objectContaining({
      code: 'ops',
      name: '运营组织',
      orgType: 'DEPARTMENT'
    })));

    await userEvent.click(screen.getByRole('tab', { name: '用户' }));
    await userEvent.click(await screen.findByRole('button', { name: /编辑/ }));
    const displayName = await screen.findByLabelText('显示名');
    await userEvent.clear(displayName);
    await userEvent.type(displayName, '平台管理员2');
    await userEvent.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => expect(identityApiMock.updateUser).toHaveBeenCalledWith('user_admin', expect.objectContaining({
      displayName: '平台管理员2',
      organizationIds: ['org_default_unit']
    })));
  }, 16000);

  it('generates an integration app api key and displays the one-time key', async () => {
    renderPage('apps');

    await userEvent.click(await screen.findByRole('button', { name: /生成 Key/ }));

    expect(await screen.findByText('agi_demo_full')).toBeInTheDocument();
    expect(identityApiMock.createIntegrationAppSecret).toHaveBeenCalledWith('app_archive', expect.anything());
  });
});

function renderPage(defaultTab = 'organizations') {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <IdentityOrganizationPage defaultTab={defaultTab} />
    </QueryClientProvider>
  );
}
