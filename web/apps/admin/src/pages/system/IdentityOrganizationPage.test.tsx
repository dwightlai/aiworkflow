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
    items: [
      {
        id: 'user_admin',
        username: 'admin',
        displayName: '平台管理员',
        userType: 'LOCAL',
        sortOrder: 20,
        organizationIds: ['org_default_unit'],
        roleIds: ['platform_admin']
      },
      {
        id: 'user_dept',
        username: 'dept-user',
        displayName: '部门用户',
        userType: 'LOCAL',
        sortOrder: 5,
        organizationIds: ['org_default_dept'],
        roleIds: ['app_user']
      }
    ],
    total: 2
  })),
  listIntegrationApps: vi.fn(async () => ({
    items: [{ id: 'app_archive', code: 'archive-system', name: '档案系统', appType: 'ARCHIVE_SYSTEM', authType: 'API_KEY', status: 'ACTIVE' }],
    total: 1
  })),
  createOrganization: vi.fn(async () => ({ id: 'org_ops', code: 'ops', name: '运营组织', orgType: 'UNIT', status: 'ACTIVE' })),
  updateOrganization: vi.fn(async () => ({ id: 'org_default_unit', status: 'DISABLED' })),
  deleteOrganization: vi.fn(async () => ({ id: 'org_default_unit', status: 'DELETED' })),
  createUser: vi.fn(async () => ({ id: 'user_archive', username: 'archive-user' })),
  updateUser: vi.fn(async () => ({ id: 'user_admin', displayName: '平台管理员2' })),
  updateUserSortOrders: vi.fn(async () => ({ items: [{ id: 'user_admin', sortOrder: 1 }], total: 1 })),
  updateUserStatus: vi.fn(async () => ({ id: 'user_admin', status: 'LOCKED' })),
  deleteUser: vi.fn(async () => ({ id: 'user_admin', status: 'DELETED' })),
  resetUserPassword: vi.fn(async () => ({ id: 'user_admin', username: 'admin' })),
  createRole: vi.fn(async () => ({ id: 'role_ops', code: 'ops_role', name: '运营角色', status: 'ACTIVE' })),
  updateRole: vi.fn(async () => ({ id: 'role_app_user', status: 'DISABLED' })),
  deleteRole: vi.fn(async () => ({ id: 'role_app_user', status: 'DELETED' })),
  createIntegrationApp: vi.fn(async () => ({ id: 'app_oa', code: 'oa-system', name: 'OA 系统', appType: 'BUSINESS_SYSTEM', authType: 'API_KEY', status: 'ACTIVE' })),
  createIntegrationAppSecret: vi.fn(async () => ({ id: 'secret_1', secretPrefix: 'agi_demo', apiKey: 'agi_demo_full' })),
  createIntegrationAppScope: vi.fn(async () => ({ id: 'scope_1', scopeType: 'BOT', scopeId: 'bot_1', permission: 'USE', enabled: true, appId: 'app_archive' })),
  listIntegrationAppScopes: vi.fn(async () => ({ items: [{ id: 'scope_1', scopeType: 'BOT', scopeId: 'bot_1', permission: 'USE', enabled: true, appId: 'app_archive' }], total: 1 })),
  listIntegrationAppSecrets: vi.fn(async () => ({ items: [{ id: 'secret_1', secretPrefix: 'agi_demo', enabled: true }], total: 1 })),
  deleteIntegrationAppScope: vi.fn(async () => undefined),
  updateIntegrationAppStatus: vi.fn(async () => ({ id: 'app_archive', status: 'DISABLED' })),
  deleteIntegrationApp: vi.fn(async () => ({ id: 'app_archive', status: 'DELETED' }))
}));

vi.mock('../../api/bots', () => ({
  listBots: vi.fn(async () => ({ items: [{ id: 'bot_1', name: '客服助手', status: 'ENABLED' }], total: 1 }))
}));

vi.mock('../../api/knowledge', () => ({
  listKnowledgeBases: vi.fn(async () => ({ items: [{ id: 'kb_1', name: 'FAQ 库', status: 'ACTIVE' }], total: 1 }))
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

vi.mock('../../api/auth', () => ({
  resolveIdentityTenantId: (tenantId?: string) => tenantId ?? 'tenant_default',
  DEFAULT_TENANT_ID: 'tenant_default',
  isPlatformOperator: () => true
}));

const TENANT = 'tenant_default';

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
  });

  it('creates an organization and edits a user', async () => {
    const { container } = renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /新增组织/ }));
    await userEvent.type(await screen.findByLabelText('组织编码'), 'ops');
    await userEvent.type(screen.getByLabelText('组织名称'), '运营组织');
    await clickLatestSave();

    await waitFor(() => expect(identityApiMock.createOrganization).toHaveBeenCalledWith(expect.objectContaining({
      code: 'ops',
      name: '运营组织',
      orgType: 'DEPARTMENT'
    }), TENANT));

    await userEvent.click(screen.getByRole('tab', { name: '用户' }));
    await userEvent.click(getTreeNode(container, '默认单位'));
    const editButtons = await screen.findAllByRole('button', { name: /编辑/ });
    await userEvent.click(editButtons[0]);
    const displayName = await screen.findByLabelText('姓名');
    await userEvent.clear(displayName);
    await userEvent.type(displayName, '平台管理员2');
    expect(await screen.findByLabelText('排序')).toBeInTheDocument();
    await clickLatestSave();

    await waitFor(() => expect(identityApiMock.updateUser).toHaveBeenCalledWith('user_admin', expect.objectContaining({
      displayName: '平台管理员2',
      sortOrder: 20,
      organizationIds: ['org_default_unit']
    }), TENANT));
  }, 16000);

  it('filters users by the selected organization without including child organizations and deletes users', async () => {
    renderPage('users');

    expect(await screen.findByText('平台管理员')).toBeInTheDocument();
    expect(screen.getByText('部门用户')).toBeInTheDocument();
    const allRows = screen.getAllByRole('row').map((row) => row.textContent ?? '');
    expect(allRows[1]).toContain('部门用户');
    expect(allRows[2]).toContain('平台管理员');

    await userEvent.click(screen.getByText('默认单位'));
    expect(screen.getByText('平台管理员')).toBeInTheDocument();
    expect(screen.queryByText('部门用户')).not.toBeInTheDocument();

    await userEvent.click(screen.getByText('默认部门'));
    expect(screen.queryByText('平台管理员')).not.toBeInTheDocument();
    expect(screen.getByText('部门用户')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /删除/ }));

    await waitFor(() => expect(identityApiMock.deleteUser).toHaveBeenCalledWith('user_dept', TENANT));
  }, 16000);

  it('edits user password in the user drawer', async () => {
    renderPage('users');

    await screen.findByText('平台管理员');
    await userEvent.click(await screen.findByText('默认单位'));
    const editButtons = await screen.findAllByRole('button', { name: /编辑/ });
    await userEvent.click(editButtons[0]);
    await userEvent.type(await screen.findByLabelText('新密码'), 'editpass123');
    await clickLatestSave();

    await waitFor(() => expect(identityApiMock.updateUser).toHaveBeenCalledWith('user_admin', expect.objectContaining({
      password: 'editpass123'
    }), TENANT));
  }, 16000);

  it('batch updates user sort orders for the selected organization', async () => {
    renderPage('users');

    await userEvent.click(await screen.findByText('默认单位'));
    await userEvent.click(await screen.findByRole('button', { name: /批量排序/ }));
    const sortInputs = await screen.findAllByLabelText(/排序-/);
    await userEvent.clear(sortInputs[0]);
    await userEvent.type(sortInputs[0], '1');
    await userEvent.click(screen.getByRole('button', { name: /保存排序/ }));

    await waitFor(() => expect(identityApiMock.updateUserSortOrders).toHaveBeenCalledWith('org_default_unit', expect.arrayContaining([
      expect.objectContaining({ userId: 'user_admin', sortOrder: 1 })
    ]), TENANT));
  }, 16000);

  it('allows role code to be edited', async () => {
    renderPage('roles');

    await userEvent.click(await screen.findByRole('button', { name: /编辑/ }));
    const code = await screen.findByLabelText('角色编码');
    await userEvent.clear(code);
    await userEvent.type(code, 'app_user_new');
    await clickLatestSave();

    await waitFor(() => expect(identityApiMock.updateRole).toHaveBeenCalledWith('role_app_user', expect.objectContaining({
      code: 'app_user_new'
    }), TENANT));
  }, 16000);

  it('allows the organization tree to collapse and expand', async () => {
    const { container } = renderPage('users');

    expect(await screen.findByText('默认部门')).toBeInTheDocument();
    const switchers = container.querySelectorAll('.ant-tree-switcher');
    await userEvent.click(switchers[1]);

    await waitFor(() => expect(screen.queryByText('默认部门')).not.toBeInTheDocument());
  });

});

function renderPage(defaultTab = 'organizations') {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <IdentityOrganizationPage defaultTab={defaultTab} />
    </QueryClientProvider>
  );
}

async function clickLatestSave() {
  const saveButtons = screen.getAllByRole('button', { name: /保\s*存/ });
  await userEvent.click(saveButtons[saveButtons.length - 1]);
}

function getTreeNode(container: HTMLElement, title: string) {
  const node = Array.from(container.querySelectorAll('.ant-tree-title')).find((element) => element.textContent === title);
  if (!node) throw new Error(`Tree node not found: ${title}`);
  return node as HTMLElement;
}
