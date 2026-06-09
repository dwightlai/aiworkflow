import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SettingOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Drawer, Form, Input, Modal, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { getAuthSession, isPlatformOperator } from '../../api/auth';
import {
  createTenant,
  deleteTenant,
  listTenants,
  updateTenant,
  type SaveTenantRequest,
  type Tenant,
  type UpdateTenantRequest
} from '../../api/identity';

const DEFAULT_TENANT_ID = 'tenant_default';

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

type TenantFormValues = SaveTenantRequest & UpdateTenantRequest;

export function TenantsPage() {
  const session = getAuthSession();
  const platformOperator = isPlatformOperator(session?.user);
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TenantFormValues>();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingTenant, setEditingTenant] = useState<Tenant | null>(null);

  const tenantsQuery = useQuery({
    queryKey: ['identity', 'tenants'],
    queryFn: listTenants,
    enabled: platformOperator
  });
  const tenants = tenantsQuery.data?.items ?? [];

  const saveMutation = useMutation({
    mutationFn: (values: TenantFormValues) => {
      if (editingTenant) {
        return updateTenant(editingTenant.id, { name: values.name, status: values.status });
      }
      return createTenant({ code: values.code, name: values.name });
    },
    onSuccess: async () => {
      message.success(editingTenant ? '租户已更新' : '租户已创建');
      setDrawerOpen(false);
      setEditingTenant(null);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['identity', 'tenants'] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (tenant: Tenant) => deleteTenant(tenant.id),
    onSuccess: async () => {
      message.success('租户已删除');
      await queryClient.invalidateQueries({ queryKey: ['identity', 'tenants'] });
    }
  });

  const openCreate = () => {
    setEditingTenant(null);
    form.resetFields();
    setDrawerOpen(true);
  };

  const openEdit = (tenant: Tenant) => {
    setEditingTenant(tenant);
    form.setFieldsValue({ code: tenant.code, name: tenant.name, status: tenant.status });
    setDrawerOpen(true);
  };

  const confirmDelete = (tenant: Tenant) => {
    Modal.confirm({
      title: '删除租户',
      content: `确定删除租户「${tenant.name}」吗？`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(tenant)
    });
  };

  const columns: ColumnsType<Tenant> = [
    {
      title: '编码',
      dataIndex: 'code',
      width: 160,
      render: (code: string, record) => (
        <Space>
          <span>{code}</span>
          {record.id === DEFAULT_TENANT_ID ? <Tag color="blue">系统默认</Tag> : null}
        </Space>
      )
    },
    { title: '名称', dataIndex: 'name' },
    { title: 'ID', dataIndex: 'id', width: 220, ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'success' : 'default'}>{status === 'ACTIVE' ? '启用' : '禁用'}</Tag>
      )
    },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<SettingOutlined />} onClick={() => navigateTo(`/system/tenants/${record.id}`)}>
            管理
          </Button>
          <Button type="link" icon={<EditOutlined />} onClick={() => openEdit(record)}>
            编辑
          </Button>
          {record.id !== DEFAULT_TENANT_ID ? (
            <Button type="link" danger icon={<DeleteOutlined />} onClick={() => confirmDelete(record)}>
              删除
            </Button>
          ) : null}
        </Space>
      )
    }
  ];

  if (!platformOperator) {
    return <Card title="租户管理">无权限访问</Card>;
  }

  return (
    <Card
      title="租户管理"
      extra={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => tenantsQuery.refetch()}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建租户
          </Button>
        </Space>
      }
    >
      <Table
        rowKey="id"
        loading={tenantsQuery.isLoading}
        columns={columns}
        dataSource={tenants}
        pagination={false}
      />

      <Drawer
        title={editingTenant ? '编辑租户' : '新建租户'}
        width={480}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setEditingTenant(null);
          form.resetFields();
        }}
        destroyOnClose
        footer={
          <Space style={{ float: 'right' }}>
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button type="primary" loading={saveMutation.isPending} onClick={() => form.submit()}>
              保存
            </Button>
          </Space>
        }
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => saveMutation.mutate(values)}
          initialValues={{ status: 'ACTIVE' }}
        >
          <Form.Item
            name="code"
            label="编码"
            rules={[{ required: true, message: '请输入编码' }]}
          >
            <Input disabled={Boolean(editingTenant)} placeholder="例如 archive" />
          </Form.Item>
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input placeholder="租户名称" />
          </Form.Item>
          {editingTenant ? (
            <Form.Item name="status" label="状态">
              <Select
                disabled={editingTenant.id === DEFAULT_TENANT_ID}
                options={[
                  { value: 'ACTIVE', label: '启用' },
                  { value: 'DISABLED', label: '禁用' }
                ]}
              />
            </Form.Item>
          ) : null}
        </Form>
      </Drawer>
    </Card>
  );
}
