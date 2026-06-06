import {
  ApiOutlined,
  ApartmentOutlined,
  EditOutlined,
  DeleteOutlined,
  KeyOutlined,
  LockOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  UnlockOutlined,
  UserOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Drawer, Form, Input, InputNumber, Modal, Select, Space, Statistic, Table, Tabs, Tag, Tree, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useEffect, useState } from 'react';
import {
  createIntegrationApp,
  createIntegrationAppScope,
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
  updateIntegrationAppStatus,
  updateOrganization,
  updateRole,
  updateUser,
  updateUserSortOrders,
  updateUserStatus,
  type IdentityUser,
  type IntegrationApp,
  type Organization,
  type Role,
  type SaveIntegrationAppRequest,
  type SaveOrganizationRequest,
  type SaveRoleRequest,
  type SaveUserRequest,
  type UpdateUserRequest,
  type UserSortOrderUpdate,
  type UpdateOrganizationRequest,
  type UpdateRoleRequest
} from '../../api/identity';

interface IdentityOrganizationPageProps {
  defaultTab?: string;
}

type DrawerMode = 'create' | 'edit';
type OrgDrawerState = { mode: DrawerMode; record?: Organization };
type UserDrawerState = { mode: DrawerMode; record?: IdentityUser };
type RoleDrawerState = { mode: DrawerMode; record?: Role };

type UserFormValues = SaveUserRequest & { id?: string };
type NormalizedUserRequest = SaveUserRequest | UpdateUserRequest;
type BatchSortItem = UserSortOrderUpdate & { username: string; displayName: string };

const appInitialValues: SaveIntegrationAppRequest = {
  code: '',
  name: '',
  appType: 'BUSINESS_SYSTEM',
  authType: 'API_KEY'
};

export function IdentityOrganizationPage({ defaultTab = 'organizations' }: IdentityOrganizationPageProps) {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState(normalizeTab(defaultTab));
  const [orgForm] = Form.useForm<SaveOrganizationRequest & UpdateOrganizationRequest>();
  const [userForm] = Form.useForm<UserFormValues>();
  const [roleForm] = Form.useForm<SaveRoleRequest & UpdateRoleRequest>();
  const [appForm] = Form.useForm<SaveIntegrationAppRequest>();
  const [scopeForm] = Form.useForm<{ scopeType: string; scopeId: string; permission: string }>();
  const [passwordForm] = Form.useForm<{ newPassword: string }>();
  const [orgDrawer, setOrgDrawer] = useState<OrgDrawerState | null>(null);
  const [userDrawer, setUserDrawer] = useState<UserDrawerState | null>(null);
  const [roleDrawer, setRoleDrawer] = useState<RoleDrawerState | null>(null);
  const [appDrawerOpen, setAppDrawerOpen] = useState(false);
  const [scopeApp, setScopeApp] = useState<IntegrationApp | null>(null);
  const [passwordUser, setPasswordUser] = useState<IdentityUser | null>(null);
  const [generatedKey, setGeneratedKey] = useState<string | null>(null);
  const [selectedUserOrganizationId, setSelectedUserOrganizationId] = useState<string>('ALL');
  const [expandedUserOrganizationIds, setExpandedUserOrganizationIds] = useState<React.Key[]>(['ALL']);
  const [userOrganizationTreeTouched, setUserOrganizationTreeTouched] = useState(false);
  const [batchSortOpen, setBatchSortOpen] = useState(false);
  const [batchSortItems, setBatchSortItems] = useState<BatchSortItem[]>([]);

  const organizationsQuery = useQuery({ queryKey: ['identity', 'organizations'], queryFn: listOrganizations });
  const rolesQuery = useQuery({ queryKey: ['identity', 'roles'], queryFn: listRoles });
  const usersQuery = useQuery({ queryKey: ['identity', 'users'], queryFn: listUsers });
  const appsQuery = useQuery({ queryKey: ['identity', 'integration-apps'], queryFn: listIntegrationApps });

  const organizations = organizationsQuery.data?.items ?? [];
  const roles = rolesQuery.data?.items ?? [];
  const users = usersQuery.data?.items ?? [];
  const apps = appsQuery.data?.items ?? [];
  const organizationOptions = organizations.map((org) => ({ value: org.id, label: `${org.name} / ${org.code}` }));
  const roleOptions = roles.map((role) => ({ value: role.code, label: `${role.name} / ${role.code}` }));
  const visibleUsers = sortUsers(selectedUserOrganizationId === 'ALL'
    ? users
    : users.filter((user) => (user.organizationIds ?? user.unitIds ?? []).includes(selectedUserOrganizationId)));

  useEffect(() => {
    if (!userOrganizationTreeTouched && organizations.length > 0) {
      setExpandedUserOrganizationIds(['ALL', ...organizations.map((org) => org.id)]);
    }
  }, [organizations, userOrganizationTreeTouched]);

  const invalidateIdentity = async () => queryClient.invalidateQueries({ queryKey: ['identity'] });

  const saveOrgMutation = useMutation({
    mutationFn: (values: SaveOrganizationRequest & UpdateOrganizationRequest) => {
      const payload = normalizeOrganization(values);
      return orgDrawer?.mode === 'edit' && orgDrawer.record
        ? updateOrganization(orgDrawer.record.id, payload)
        : createOrganization(payload as SaveOrganizationRequest);
    },
    onSuccess: async () => {
      message.success(orgDrawer?.mode === 'edit' ? '组织已更新' : '组织已创建');
      setOrgDrawer(null);
      orgForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['identity', 'organizations'] });
    }
  });

  const deleteOrgMutation = useMutation({
    mutationFn: deleteOrganization,
    onSuccess: async () => {
      message.success('组织已删除');
      await queryClient.invalidateQueries({ queryKey: ['identity', 'organizations'] });
    }
  });

  const saveUserMutation = useMutation({
    mutationFn: (values: UserFormValues) => {
      const payload = normalizeUser(values);
      return userDrawer?.mode === 'edit' && userDrawer.record
        ? updateUser(userDrawer.record.id, payload as UpdateUserRequest)
        : createUser(payload as SaveUserRequest);
    },
    onSuccess: async () => {
      message.success(userDrawer?.mode === 'edit' ? '用户已更新' : '用户已创建');
      setUserDrawer(null);
      userForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['identity', 'users'] });
    }
  });

  const deleteUserMutation = useMutation({
    mutationFn: deleteUser,
    onSuccess: async () => {
      message.success('用户已删除');
      await queryClient.invalidateQueries({ queryKey: ['identity', 'users'] });
    }
  });

  const sortUsersMutation = useMutation({
    mutationFn: ({ organizationId, items }: { organizationId: string; items: UserSortOrderUpdate[] }) =>
      updateUserSortOrders(organizationId, items),
    onSuccess: async () => {
      message.success('用户排序已更新');
      setBatchSortOpen(false);
      await queryClient.invalidateQueries({ queryKey: ['identity', 'users'] });
    }
  });

  const saveRoleMutation = useMutation({
    mutationFn: (values: SaveRoleRequest & UpdateRoleRequest) => {
      const payload = normalizeRole(values);
      return roleDrawer?.mode === 'edit' && roleDrawer.record
        ? updateRole(roleDrawer.record.id, payload)
        : createRole(payload as SaveRoleRequest);
    },
    onSuccess: async () => {
      message.success(roleDrawer?.mode === 'edit' ? '角色已更新' : '角色已创建');
      setRoleDrawer(null);
      roleForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['identity', 'roles'] });
    }
  });

  const deleteRoleMutation = useMutation({
    mutationFn: deleteRole,
    onSuccess: async () => {
      message.success('角色已删除');
      await queryClient.invalidateQueries({ queryKey: ['identity', 'roles'] });
    }
  });

  const updateUserStatusMutation = useMutation({
    mutationFn: ({ userId, status }: { userId: string; status: string }) => updateUserStatus(userId, status),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['identity', 'users'] })
  });

  const resetPasswordMutation = useMutation({
    mutationFn: ({ userId, password }: { userId: string; password: string }) => resetUserPassword(userId, password),
    onSuccess: async () => {
      setPasswordUser(null);
      passwordForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['identity', 'users'] });
    }
  });

  const createAppMutation = useMutation({
    mutationFn: createIntegrationApp,
    onSuccess: async () => {
      setAppDrawerOpen(false);
      appForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['identity', 'integration-apps'] });
    }
  });

  const updateAppStatusMutation = useMutation({
    mutationFn: ({ appId, status }: { appId: string; status: string }) => updateIntegrationAppStatus(appId, status),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['identity', 'integration-apps'] })
  });

  const deleteAppMutation = useMutation({
    mutationFn: deleteIntegrationApp,
    onSuccess: async () => {
      message.success('第三方应用已删除');
      await queryClient.invalidateQueries({ queryKey: ['identity', 'integration-apps'] });
    }
  });

  const createSecretMutation = useMutation({
    mutationFn: createIntegrationAppSecret,
    onSuccess: (secret) => setGeneratedKey(secret.apiKey)
  });

  const createScopeMutation = useMutation({
    mutationFn: ({ appId, values }: { appId: string; values: { scopeType: string; scopeId: string; permission: string } }) =>
      createIntegrationAppScope(appId, values),
    onSuccess: () => {
      setScopeApp(null);
      scopeForm.resetFields();
    }
  });

  const openCreateOrg = () => {
    setOrgDrawer({ mode: 'create' });
    orgForm.setFieldsValue({ code: '', name: '', orgType: 'DEPARTMENT', parentId: null, externalOrgId: null, sortOrder: 0 });
  };

  const openEditOrg = (record: Organization) => {
    setOrgDrawer({ mode: 'edit', record });
    orgForm.setFieldsValue(record);
  };

  const openCreateUser = () => {
    setUserDrawer({ mode: 'create' });
    userForm.setFieldsValue({ username: '', password: '', displayName: '', mobile: null, email: null, sortOrder: 0, organizationIds: [], roleCodes: ['app_user'] });
  };

  const openEditUser = (record: IdentityUser) => {
    setUserDrawer({ mode: 'edit', record });
    userForm.setFieldsValue({ ...record, password: '', sortOrder: record.sortOrder ?? 0, roleCodes: record.roleIds, organizationIds: record.organizationIds ?? record.unitIds ?? [] });
  };

  const openBatchSort = () => {
    if (selectedUserOrganizationId === 'ALL') {
      message.warning('请先选择一个单位或部门');
      return;
    }
    setBatchSortItems(visibleUsers.map((user) => ({
      userId: user.id,
      username: user.username,
      displayName: user.displayName,
      sortOrder: user.sortOrder ?? 0
    })));
    setBatchSortOpen(true);
  };

  const openCreateRole = () => {
    setRoleDrawer({ mode: 'create' });
    roleForm.setFieldsValue({ code: '', name: '', roleType: 'BUSINESS', organizationId: null, externalRoleId: null });
  };

  const openEditRole = (record: Role) => {
    setRoleDrawer({ mode: 'edit', record });
    roleForm.setFieldsValue(record);
  };

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>组织用户</Typography.Title>
          <Typography.Text type="secondary">统一维护组织树、用户、角色和第三方接入应用，作为知识库和智能体授权的身份基础。</Typography.Text>
        </Space>
        <Button icon={<ReloadOutlined />} onClick={invalidateIdentity}>刷新</Button>
      </div>

      <div style={metricRowStyle}>
        <Metric title="组织" value={organizations.length} icon={<ApartmentOutlined />} />
        <Metric title="用户" value={users.length} icon={<UserOutlined />} />
        <Metric title="角色" value={roles.length} icon={<SafetyCertificateOutlined />} />
        <Metric title="第三方应用" value={apps.length} icon={<ApiOutlined />} />
      </div>

      {hasError([organizationsQuery, rolesQuery, usersQuery, appsQuery]) ? (
        <Alert type="error" showIcon message="组织用户数据加载失败" style={{ marginBottom: 12 }} />
      ) : null}

      <Card variant="borderless" style={{ border: '1px solid #e7ecf3' }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'organizations',
              label: '组织架构',
              children: (
                <>
                  <Toolbar><Button type="primary" icon={<PlusOutlined />} onClick={openCreateOrg}>新增组织</Button></Toolbar>
                  <OrganizationTable organizations={organizations} loading={organizationsQuery.isLoading} onEdit={openEditOrg} onToggleStatus={(org) => saveOrgMutation.mutate({ ...org, status: org.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' })} onDelete={(org) => deleteOrgMutation.mutate(org.id)} />
                </>
              )
            },
            {
              key: 'users',
              label: '用户',
              children: (
                <>
                  <UserManagementPanel organizations={organizations} expandedOrganizationIds={expandedUserOrganizationIds} selectedOrganizationId={selectedUserOrganizationId} users={visibleUsers} loading={usersQuery.isLoading} onExpandOrganizations={(keys) => { setUserOrganizationTreeTouched(true); setExpandedUserOrganizationIds(keys); }} onSelectOrganization={setSelectedUserOrganizationId} onCreate={openCreateUser} onBatchSort={openBatchSort} onEdit={openEditUser} onStatus={(user, status) => updateUserStatusMutation.mutate({ userId: user.id, status })} onResetPassword={setPasswordUser} onDelete={(user) => deleteUserMutation.mutate(user.id)} />
                </>
              )
            },
            {
              key: 'roles',
              label: '角色',
              children: (
                <>
                  <Toolbar><Button type="primary" icon={<PlusOutlined />} onClick={openCreateRole}>新增角色</Button></Toolbar>
                  <RoleTable roles={roles} loading={rolesQuery.isLoading} onEdit={openEditRole} onToggleStatus={(role) => saveRoleMutation.mutate({ ...role, status: role.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' })} onDelete={(role) => deleteRoleMutation.mutate(role.id)} />
                </>
              )
            },
            {
              key: 'apps',
              label: '第三方应用',
              children: (
                <>
                  <Toolbar><Button type="primary" icon={<PlusOutlined />} onClick={() => { appForm.setFieldsValue(appInitialValues); setAppDrawerOpen(true); }}>新增应用</Button></Toolbar>
                  <IntegrationAppTable apps={apps} loading={appsQuery.isLoading} onStatus={(app, status) => updateAppStatusMutation.mutate({ appId: app.id, status })} onSecret={(app) => createSecretMutation.mutate(app.id)} onScope={setScopeApp} onDelete={(app) => deleteAppMutation.mutate(app.id)} />
                </>
              )
            }
          ]}
        />
      </Card>

      <Drawer title={orgDrawer?.mode === 'edit' ? '编辑组织' : '新增组织'} open={Boolean(orgDrawer)} width={520} onClose={() => setOrgDrawer(null)} footer={<DrawerFooter onCancel={() => setOrgDrawer(null)} onSubmit={() => orgForm.submit()} loading={saveOrgMutation.isPending} />}>
        <Form form={orgForm} layout="vertical" onFinish={(values) => saveOrgMutation.mutate(values)}>
          {orgDrawer?.mode === 'create' ? <Form.Item name="code" label="组织编码" rules={[{ required: true, message: '请输入组织编码' }]}><Input /></Form.Item> : null}
          <Form.Item name="name" label="组织名称" rules={[{ required: true, message: '请输入组织名称' }]}><Input /></Form.Item>
          <Form.Item name="orgType" label="组织类型" rules={[{ required: true, message: '请选择组织类型' }]}><Select options={[{ value: 'UNIT', label: '单位' }, { value: 'DEPARTMENT', label: '部门' }, { value: 'GROUP', label: '集团/区域' }, { value: 'OTHER', label: '其他' }]} /></Form.Item>
          <Form.Item name="parentId" label="上级组织"><Select allowClear options={organizationOptions} /></Form.Item>
          <Form.Item name="externalOrgId" label="外部组织标识"><Input /></Form.Item>
          <Form.Item name="sortOrder" label="排序" getValueFromEvent={(event) => toSortOrder(event.target.value)}><Input type="number" min={0} /></Form.Item>
        </Form>
      </Drawer>

      <Drawer title={userDrawer?.mode === 'edit' ? '编辑用户' : '新增用户'} open={Boolean(userDrawer)} width={520} onClose={() => setUserDrawer(null)} footer={<DrawerFooter onCancel={() => setUserDrawer(null)} onSubmit={() => userForm.submit()} loading={saveUserMutation.isPending} />}>
        <Form form={userForm} layout="vertical" onFinish={(values) => saveUserMutation.mutate(values)}>
          {userDrawer?.mode === 'create' ? <Form.Item name="username" label="登录名" rules={[{ required: true, message: '请输入登录名' }]}><Input /></Form.Item> : null}
          <Form.Item name="displayName" label="显示名" rules={[{ required: true, message: '请输入显示名' }]}><Input /></Form.Item>
          {userDrawer?.mode === 'edit' ? <Form.Item name="password" label="新密码"><Input.Password placeholder="留空则不修改" /></Form.Item> : null}
          {userDrawer?.mode === 'create' ? <Form.Item name="password" label="初始密码" rules={[{ required: true, message: '请输入初始密码' }]}><Input.Password /></Form.Item> : null}
          <Form.Item name="mobile" label="手机号"><Input /></Form.Item>
          <Form.Item name="email" label="邮箱"><Input /></Form.Item>
          <Form.Item name="sortOrder" label="排序"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="organizationIds" label="所属组织"><Select mode="multiple" options={organizationOptions} /></Form.Item>
          <Form.Item name="roleCodes" label="角色"><Select mode="multiple" options={roleOptions} /></Form.Item>
        </Form>
      </Drawer>

      <Drawer title={roleDrawer?.mode === 'edit' ? '编辑角色' : '新增角色'} open={Boolean(roleDrawer)} width={500} onClose={() => setRoleDrawer(null)} footer={<DrawerFooter onCancel={() => setRoleDrawer(null)} onSubmit={() => roleForm.submit()} loading={saveRoleMutation.isPending} />}>
        <Form form={roleForm} layout="vertical" onFinish={(values) => saveRoleMutation.mutate(values)}>
          <Form.Item name="code" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]}><Input /></Form.Item>
          <Form.Item name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}><Input /></Form.Item>
          <Form.Item name="roleType" label="角色类型"><Select options={[{ value: 'PLATFORM', label: '平台角色' }, { value: 'BUSINESS', label: '业务角色' }, { value: 'INTEGRATION', label: '接入角色' }]} /></Form.Item>
          <Form.Item name="organizationId" label="所属组织"><Select allowClear options={organizationOptions} /></Form.Item>
          <Form.Item name="externalRoleId" label="外部角色标识"><Input /></Form.Item>
        </Form>
      </Drawer>

      <Drawer title="新增第三方应用" open={appDrawerOpen} width={500} onClose={() => setAppDrawerOpen(false)} footer={<DrawerFooter onCancel={() => setAppDrawerOpen(false)} onSubmit={() => appForm.submit()} loading={createAppMutation.isPending} />}>
        <Form form={appForm} layout="vertical" initialValues={appInitialValues} onFinish={(values) => createAppMutation.mutate(values)}>
          <Form.Item name="code" label="应用编码" rules={[{ required: true, message: '请输入应用编码' }]}><Input /></Form.Item>
          <Form.Item name="name" label="应用名称" rules={[{ required: true, message: '请输入应用名称' }]}><Input /></Form.Item>
          <Form.Item name="appType" label="应用类型"><Select options={[{ value: 'ARCHIVE_SYSTEM', label: '数字档案馆' }, { value: 'OA_SYSTEM', label: 'OA 系统' }, { value: 'BUSINESS_SYSTEM', label: '业务系统' }, { value: 'OTHER', label: '其他' }]} /></Form.Item>
          <Form.Item name="authType" label="认证方式"><Select options={[{ value: 'API_KEY', label: 'API Key' }]} /></Form.Item>
        </Form>
      </Drawer>

      <Modal title={`重置密码：${passwordUser?.username ?? ''}`} open={Boolean(passwordUser)} onCancel={() => setPasswordUser(null)} okText="确认重置" onOk={async () => {
        const values = await passwordForm.validateFields();
        if (passwordUser) resetPasswordMutation.mutate({ userId: passwordUser.id, password: values.newPassword });
      }}>
        <Form form={passwordForm} layout="vertical"><Form.Item name="newPassword" label="新密码" rules={[{ required: true, message: '请输入新密码' }]}><Input.Password /></Form.Item></Form>
      </Modal>

      <Modal
        title="批量排序"
        open={batchSortOpen}
        onCancel={() => setBatchSortOpen(false)}
        okText="保存排序"
        confirmLoading={sortUsersMutation.isPending}
        onOk={() => sortUsersMutation.mutate({
          organizationId: selectedUserOrganizationId,
          items: batchSortItems.map((item) => ({ userId: item.userId, sortOrder: item.sortOrder }))
        })}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          {batchSortItems.map((item, index) => (
            <div key={item.userId} style={sortRowStyle}>
              <Typography.Text>{item.displayName || item.username}</Typography.Text>
              <InputNumber
                aria-label={`排序-${item.username}`}
                min={0}
                value={item.sortOrder}
                onChange={(value) => setBatchSortItems((items) => items.map((current, itemIndex) =>
                  itemIndex === index ? { ...current, sortOrder: value ?? 0 } : current
                ))}
              />
            </div>
          ))}
          {batchSortItems.length === 0 ? <Typography.Text type="secondary">当前组织暂无用户</Typography.Text> : null}
        </Space>
      </Modal>

      <Modal title={`添加调用范围：${scopeApp?.name ?? ''}`} open={Boolean(scopeApp)} onCancel={() => setScopeApp(null)} okText="添加范围" onOk={() => scopeForm.submit()}>
        <Form form={scopeForm} layout="vertical" initialValues={{ scopeType: 'ORGANIZATION', scopeId: organizations[0]?.id ?? '', permission: 'USE' }} onFinish={(values) => scopeApp && createScopeMutation.mutate({ appId: scopeApp.id, values })}>
          <Form.Item name="scopeType" label="范围类型"><Select options={[{ value: 'TENANT', label: '租户' }, { value: 'ORGANIZATION', label: '组织' }, { value: 'BOT', label: '智能体' }, { value: 'KNOWLEDGE_BASE', label: '知识库' }, { value: 'TEMPLATE', label: '模板' }]} /></Form.Item>
          <Form.Item name="scopeId" label="范围标识" rules={[{ required: true, message: '请输入范围标识' }]}><Input /></Form.Item>
          <Form.Item name="permission" label="权限"><Select options={[{ value: 'USE', label: '使用' }, { value: 'MANAGE', label: '管理' }]} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="一次性 API Key" open={Boolean(generatedKey)} onCancel={() => setGeneratedKey(null)} footer={<Button type="primary" onClick={() => setGeneratedKey(null)}>我已保存</Button>}>
        <Alert type="warning" showIcon message="密钥只展示一次，请立即复制保存。" style={{ marginBottom: 12 }} />
        <Typography.Text code style={{ userSelect: 'all' }}>{generatedKey}</Typography.Text>
      </Modal>
    </section>
  );
}

function OrganizationTable({ organizations, loading, onEdit, onToggleStatus, onDelete }: { organizations: Organization[]; loading: boolean; onEdit: (org: Organization) => void; onToggleStatus: (org: Organization) => void; onDelete: (org: Organization) => void }) {
  const columns: ColumnsType<Organization> = [
    { title: '组织名称', dataIndex: 'name' },
    { title: '编码', dataIndex: 'code' },
    { title: '类型', dataIndex: 'orgType', render: tag },
    { title: '外部标识', dataIndex: 'externalOrgId', render: emptyText },
    { title: '状态', dataIndex: 'status', render: statusTag },
    { title: '操作', width: 250, render: (_, org) => <Space><Button size="small" icon={<EditOutlined />} onClick={() => onEdit(org)}>编辑</Button><Button size="small" onClick={() => onToggleStatus(org)}>{org.status === 'ACTIVE' ? '停用' : '启用'}</Button><ConfirmDelete onConfirm={() => onDelete(org)} /></Space> }
  ];
  return <Table rowKey="id" columns={columns} dataSource={buildOrganizationTree(organizations)} loading={loading} pagination={false} size="middle" />;
}

function UserManagementPanel({ organizations, expandedOrganizationIds, selectedOrganizationId, users, loading, onExpandOrganizations, onSelectOrganization, onCreate, onBatchSort, onEdit, onStatus, onResetPassword, onDelete }: { organizations: Organization[]; expandedOrganizationIds: React.Key[]; selectedOrganizationId: string; users: IdentityUser[]; loading: boolean; onExpandOrganizations: (keys: React.Key[]) => void; onSelectOrganization: (organizationId: string) => void; onCreate: () => void; onBatchSort: () => void; onEdit: (user: IdentityUser) => void; onStatus: (user: IdentityUser, status: string) => void; onResetPassword: (user: IdentityUser) => void; onDelete: (user: IdentityUser) => void }) {
  return (
    <div style={userManagementStyle}>
      <aside style={userTreeStyle}>
        <Typography.Text strong>单位部门</Typography.Text>
        <Tree
          blockNode
          expandedKeys={expandedOrganizationIds}
          selectedKeys={[selectedOrganizationId]}
          treeData={buildUserOrganizationTree(organizations)}
          onExpand={(keys) => onExpandOrganizations(keys)}
          onSelect={(keys) => onSelectOrganization(String(keys[0] ?? 'ALL'))}
          style={{ marginTop: 12 }}
        />
      </aside>
      <div style={{ minWidth: 0 }}>
        <Toolbar>
          <Space>
            <Button onClick={onBatchSort} disabled={selectedOrganizationId === 'ALL'}>批量排序</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>新增用户</Button>
          </Space>
        </Toolbar>
        <UserTable users={users} loading={loading} onEdit={onEdit} onStatus={onStatus} onResetPassword={onResetPassword} onDelete={onDelete} />
      </div>
    </div>
  );
}

function UserTable({ users, loading, onEdit, onStatus, onResetPassword, onDelete }: { users: IdentityUser[]; loading: boolean; onEdit: (user: IdentityUser) => void; onStatus: (user: IdentityUser, status: string) => void; onResetPassword: (user: IdentityUser) => void; onDelete: (user: IdentityUser) => void }) {
  const columns: ColumnsType<IdentityUser> = [
    { title: '用户', dataIndex: 'username', render: (_, user) => <Space direction="vertical" size={0}><Typography.Text strong>{user.displayName}</Typography.Text><Typography.Text type="secondary">{user.username}</Typography.Text></Space> },
    { title: '类型', dataIndex: 'userType', width: 100, render: tag },
    { title: '排序', dataIndex: 'sortOrder', width: 90, render: (value?: number) => value ?? 0 },
    { title: '组织', dataIndex: 'organizationIds', render: (values: string[]) => (values ?? []).join(', ') },
    { title: '角色', dataIndex: 'roleIds', render: (values: string[]) => (values ?? []).map((value) => <Tag key={value}>{value}</Tag>) },
    { title: '操作', width: 430, render: (_, user) => <Space wrap><Button size="small" icon={<EditOutlined />} onClick={() => onEdit(user)}>编辑</Button><Button size="small" icon={<UnlockOutlined />} onClick={() => onStatus(user, 'ACTIVE')}>启用</Button><Button size="small" onClick={() => onStatus(user, 'DISABLED')}>禁用</Button><Button size="small" icon={<LockOutlined />} onClick={() => onStatus(user, 'LOCKED')}>锁定</Button><Button size="small" icon={<KeyOutlined />} onClick={() => onResetPassword(user)}>重置密码</Button><ConfirmDelete onConfirm={() => onDelete(user)} /></Space> }
  ];
  return <Table rowKey="id" columns={columns} dataSource={users} loading={loading} pagination={false} size="middle" />;
}

function RoleTable({ roles, loading, onEdit, onToggleStatus, onDelete }: { roles: Role[]; loading: boolean; onEdit: (role: Role) => void; onToggleStatus: (role: Role) => void; onDelete: (role: Role) => void }) {
  const columns: ColumnsType<Role> = [
    { title: '角色名称', dataIndex: 'name' },
    { title: '编码', dataIndex: 'code' },
    { title: '类型', dataIndex: 'roleType', render: tag },
    { title: '所属组织', dataIndex: 'organizationId', render: emptyText },
    { title: '状态', dataIndex: 'status', render: statusTag },
    { title: '操作', width: 250, render: (_, role) => <Space><Button size="small" icon={<EditOutlined />} onClick={() => onEdit(role)}>编辑</Button><Button size="small" onClick={() => onToggleStatus(role)}>{role.status === 'ACTIVE' ? '停用' : '启用'}</Button><ConfirmDelete onConfirm={() => onDelete(role)} /></Space> }
  ];
  return <Table rowKey="id" columns={columns} dataSource={roles} loading={loading} pagination={false} size="middle" />;
}

function IntegrationAppTable({ apps, loading, onStatus, onSecret, onScope, onDelete }: { apps: IntegrationApp[]; loading: boolean; onStatus: (app: IntegrationApp, status: string) => void; onSecret: (app: IntegrationApp) => void; onScope: (app: IntegrationApp) => void; onDelete: (app: IntegrationApp) => void }) {
  const columns: ColumnsType<IntegrationApp> = [
    { title: '应用', dataIndex: 'name', render: (_, app) => <Space direction="vertical" size={0}><Typography.Text strong>{app.name}</Typography.Text><Typography.Text type="secondary">{app.code}</Typography.Text></Space> },
    { title: '类型', dataIndex: 'appType', render: tag },
    { title: '认证', dataIndex: 'authType', render: tag },
    { title: '状态', dataIndex: 'status', render: statusTag },
    { title: '操作', width: 400, render: (_, app) => <Space wrap><Button size="small" onClick={() => onStatus(app, app.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')}>{app.status === 'ACTIVE' ? '停用' : '启用'}</Button><Button size="small" icon={<KeyOutlined />} onClick={() => onSecret(app)}>生成 Key</Button><Button size="small" icon={<SafetyCertificateOutlined />} onClick={() => onScope(app)}>添加范围</Button><ConfirmDelete onConfirm={() => onDelete(app)} /></Space> }
  ];
  return <Table rowKey="id" columns={columns} dataSource={apps} loading={loading} pagination={false} size="middle" />;
}

function Metric({ title, value, icon }: { title: string; value: number; icon: React.ReactNode }) {
  return <Card variant="borderless" style={metricCardStyle}><Statistic title={title} value={value} prefix={icon} /></Card>;
}

function Toolbar({ children }: { children: React.ReactNode }) {
  return <div style={toolbarStyle}>{children}</div>;
}

function DrawerFooter({ onCancel, onSubmit, loading }: { onCancel: () => void; onSubmit: () => void; loading: boolean }) {
  return <Space style={{ display: 'flex', justifyContent: 'flex-end' }}><Button onClick={onCancel}>取消</Button><Button type="primary" loading={loading} onClick={onSubmit}>保存</Button></Space>;
}

function ConfirmDelete({ onConfirm }: { onConfirm: () => void }) {
  return <Button danger size="small" icon={<DeleteOutlined />} onClick={onConfirm}>删除</Button>;
}

function buildOrganizationTree(organizations: Organization[]): Organization[] {
  const byId = new Map<string, Organization & { children?: Organization[] }>();
  organizations.forEach((org) => byId.set(org.id, { ...org, children: [] }));
  const roots: Array<Organization & { children?: Organization[] }> = [];
  byId.forEach((org) => {
    if (org.parentId && byId.has(org.parentId)) byId.get(org.parentId)?.children?.push(org);
    else roots.push(org);
  });
  byId.forEach((org) => {
    if (org.children?.length === 0) delete org.children;
    else org.children?.sort(compareOrganizations);
  });
  return roots.sort(compareOrganizations);
}

function buildUserOrganizationTree(organizations: Organization[]) {
  return [
    {
      key: 'ALL',
      title: '全部组织',
      children: buildOrganizationTree(organizations).map(toTreeNode)
    }
  ];
}

function toTreeNode(org: Organization & { children?: Organization[] }): { key: string; title: string; children?: ReturnType<typeof toTreeNode>[] } {
  return {
    key: org.id,
    title: org.name,
    children: org.children?.map(toTreeNode)
  };
}

function compareOrganizations(left: Organization, right: Organization) {
  return (left.sortOrder ?? 0) - (right.sortOrder ?? 0)
    || left.code.localeCompare(right.code)
    || left.name.localeCompare(right.name);
}

function sortUsers(users: IdentityUser[]) {
  return [...users].sort((left, right) =>
    (left.sortOrder ?? 0) - (right.sortOrder ?? 0)
    || left.username.localeCompare(right.username)
  );
}

function normalizeOrganization(values: SaveOrganizationRequest & UpdateOrganizationRequest): SaveOrganizationRequest & UpdateOrganizationRequest {
  return { ...values, parentId: emptyToNull(values.parentId), externalOrgId: emptyToNull(values.externalOrgId), orgType: values.orgType ?? 'DEPARTMENT', sortOrder: values.sortOrder ?? 0 };
}

function normalizeUser(values: UserFormValues): NormalizedUserRequest {
  return {
    username: values.username,
    password: values.password && values.password.trim().length > 0 ? values.password : undefined,
    displayName: values.displayName,
    mobile: emptyToNull(values.mobile),
    email: emptyToNull(values.email),
    sortOrder: toSortOrder(values.sortOrder),
    organizationIds: values.organizationIds ?? [],
    roleCodes: values.roleCodes ?? []
  };
}

function normalizeRole(values: SaveRoleRequest & UpdateRoleRequest): SaveRoleRequest & UpdateRoleRequest {
  return { ...values, organizationId: emptyToNull(values.organizationId), externalRoleId: emptyToNull(values.externalRoleId), roleType: values.roleType ?? 'BUSINESS' };
}

function normalizeTab(tab: string) {
  if (tab === 'units' || tab === 'departments') return 'organizations';
  return tab;
}

function statusTag(value: string) {
  const color = value === 'ACTIVE' ? 'green' : value === 'LOCKED' ? 'gold' : 'default';
  return <Tag color={color}>{value}</Tag>;
}

function tag(value: string) {
  return <Tag>{value}</Tag>;
}

function emptyText(value?: string | null) {
  return value ? value : <Typography.Text type="secondary">-</Typography.Text>;
}

function emptyToNull(value?: string | null) {
  return value && value.trim().length > 0 ? value : null;
}

function toSortOrder(value?: number | string | null) {
  if (value === null || value === undefined || value === '') return 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function hasError(queries: Array<{ isError: boolean }>) {
  return queries.some((query) => query.isError);
}

const pageStyle: React.CSSProperties = { background: '#f5f7fb', minHeight: '100%', padding: 24 };
const headerStyle: React.CSSProperties = { alignItems: 'center', background: '#fff', border: '1px solid #e7ecf3', borderRadius: 8, display: 'flex', justifyContent: 'space-between', marginBottom: 16, padding: '18px 20px' };
const metricRowStyle: React.CSSProperties = { display: 'grid', gap: 12, gridTemplateColumns: 'repeat(4, minmax(0, 1fr))', marginBottom: 16 };
const metricCardStyle: React.CSSProperties = { border: '1px solid #e7ecf3' };
const toolbarStyle: React.CSSProperties = { display: 'flex', justifyContent: 'flex-end', marginBottom: 12 };
const userManagementStyle: React.CSSProperties = { display: 'grid', gap: 16, gridTemplateColumns: '260px minmax(0, 1fr)' };
const userTreeStyle: React.CSSProperties = { border: '1px solid #e7ecf3', borderRadius: 8, minHeight: 420, padding: 12 };
const sortRowStyle: React.CSSProperties = { alignItems: 'center', display: 'flex', justifyContent: 'space-between', gap: 12 };
