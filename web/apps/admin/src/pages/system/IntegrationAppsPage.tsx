import { ApiOutlined, DeleteOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Drawer, Form, Input, Select, Space, Statistic, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useState } from 'react';
import { resolveIdentityTenantId } from '../../api/auth';
import {
  createIntegrationApp,
  deleteIntegrationApp,
  listIntegrationApps,
  updateIntegrationAppStatus,
  type IntegrationApp,
  type SaveIntegrationAppRequest
} from '../../api/identity';
import { IntegrationAppConfigDrawer } from '../../components/IntegrationAppConfigDrawer';

interface IntegrationAppsPageProps {
  tenantId?: string;
}

const appInitialValues: SaveIntegrationAppRequest = {
  code: '',
  name: '',
  appType: 'BUSINESS_SYSTEM',
  authType: 'API_KEY'
};

export function IntegrationAppsPage({ tenantId }: IntegrationAppsPageProps) {
  const queryClient = useQueryClient();
  const effectiveTenantId = resolveIdentityTenantId(tenantId);
  const scopeKey = effectiveTenantId;
  const workspaceMode = Boolean(tenantId);
  const [appForm] = Form.useForm<SaveIntegrationAppRequest>();
  const [appDrawerOpen, setAppDrawerOpen] = useState(false);
  const [configApp, setConfigApp] = useState<IntegrationApp | null>(null);

  const appsQuery = useQuery({
    queryKey: ['identity', scopeKey, 'integration-apps'],
    queryFn: () => listIntegrationApps(effectiveTenantId)
  });
  const apps = appsQuery.data?.items ?? [];

  const createAppMutation = useMutation({
    mutationFn: (request: SaveIntegrationAppRequest) => createIntegrationApp(request, effectiveTenantId),
    onSuccess: async (app) => {
      message.success('应用已创建，请继续配置 API Key 和白名单');
      setAppDrawerOpen(false);
      appForm.resetFields();
      setConfigApp(app);
      await queryClient.invalidateQueries({ queryKey: ['identity', scopeKey, 'integration-apps'] });
    }
  });

  const updateAppStatusMutation = useMutation({
    mutationFn: ({ appId, status }: { appId: string; status: string }) => updateIntegrationAppStatus(appId, status, effectiveTenantId),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['identity', scopeKey, 'integration-apps'] })
  });

  const deleteAppMutation = useMutation({
    mutationFn: (appId: string) => deleteIntegrationApp(appId, effectiveTenantId),
    onSuccess: async () => {
      message.success('第三方应用已删除');
      await queryClient.invalidateQueries({ queryKey: ['identity', scopeKey, 'integration-apps'] });
    }
  });

  const openCreateApp = () => {
    appForm.setFieldsValue(appInitialValues);
    setAppDrawerOpen(true);
  };

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>第三方应用</Typography.Title>
          <Typography.Text type="secondary">
            {workspaceMode ? '管理当前租户的开放 API 接入应用、密钥与资产白名单。' : '管理本租户的开放 API 接入应用、密钥与资产白名单。'}
          </Typography.Text>
        </Space>
        <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['identity', scopeKey, 'integration-apps'] })}>
          刷新
        </Button>
      </div>

      <Card variant="borderless" style={metricCardStyle}>
        <Statistic title="应用数量" value={apps.length} prefix={<ApiOutlined />} />
      </Card>

      {appsQuery.isError ? (
        <Alert type="error" showIcon message="第三方应用加载失败" style={{ marginBottom: 12 }} />
      ) : null}

      <Card variant="borderless" style={{ border: '1px solid #e7ecf3' }}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="接入流程：新增应用 → 点击「配置 API Key 与白名单」→ 生成密钥并选择可调用的智能体/知识库"
        />
        <div style={toolbarStyle}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateApp}>新增应用</Button>
        </div>
        <IntegrationAppTable
          apps={apps}
          loading={appsQuery.isLoading}
          onStatus={(app, status) => updateAppStatusMutation.mutate({ appId: app.id, status })}
          onConfigure={setConfigApp}
          onDelete={(app) => deleteAppMutation.mutate(app.id)}
        />
      </Card>

      <Drawer
        title="新增第三方应用"
        open={appDrawerOpen}
        width={500}
        onClose={() => setAppDrawerOpen(false)}
        footer={<DrawerFooter onCancel={() => setAppDrawerOpen(false)} onSubmit={() => appForm.submit()} loading={createAppMutation.isPending} />}
      >
        <Alert type="info" showIcon message="保存后将自动打开「应用配置」，可在其中生成 API Key 和设置智能体/知识库白名单。" style={{ marginBottom: 16 }} />
        <Form form={appForm} layout="vertical" initialValues={appInitialValues} onFinish={(values) => createAppMutation.mutate(values)}>
          <Form.Item name="code" label="应用编码" rules={[{ required: true, message: '请输入应用编码' }]}><Input /></Form.Item>
          <Form.Item name="name" label="应用名称" rules={[{ required: true, message: '请输入应用名称' }]}><Input /></Form.Item>
          <Form.Item name="appType" label="应用类型"><Select options={[{ value: 'ARCHIVE_SYSTEM', label: '数字档案馆' }, { value: 'OA_SYSTEM', label: 'OA 系统' }, { value: 'BUSINESS_SYSTEM', label: '业务系统' }, { value: 'OTHER', label: '其他' }]} /></Form.Item>
          <Form.Item name="authType" label="认证方式"><Select options={[{ value: 'API_KEY', label: 'API Key' }]} /></Form.Item>
        </Form>
      </Drawer>

      <IntegrationAppConfigDrawer
        open={Boolean(configApp)}
        app={configApp}
        tenantId={effectiveTenantId}
        onClose={() => setConfigApp(null)}
      />
    </section>
  );
}

function IntegrationAppTable({ apps, loading, onStatus, onConfigure, onDelete }: { apps: IntegrationApp[]; loading: boolean; onStatus: (app: IntegrationApp, status: string) => void; onConfigure: (app: IntegrationApp) => void; onDelete: (app: IntegrationApp) => void }) {
  const columns: ColumnsType<IntegrationApp> = [
    {
      title: '应用',
      dataIndex: 'name',
      fixed: 'left',
      width: 220,
      render: (_, app) => (
        <Space direction="vertical" size={0}>
          <Button type="link" style={{ padding: 0, height: 'auto' }} onClick={() => onConfigure(app)}>
            <Typography.Text strong>{app.name}</Typography.Text>
          </Button>
          <Typography.Text type="secondary">{app.code}</Typography.Text>
        </Space>
      )
    },
    { title: '类型', dataIndex: 'appType', width: 120, render: (value: string) => <Tag>{value}</Tag> },
    { title: '认证', dataIndex: 'authType', width: 100, render: (value: string) => <Tag>{value}</Tag> },
    { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
    {
      title: '操作',
      fixed: 'right',
      width: 320,
      render: (_, app) => (
        <Space wrap>
          <Button size="small" type="primary" onClick={() => onConfigure(app)}>配置 API Key 与白名单</Button>
          <Button size="small" onClick={() => onStatus(app, app.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')}>{app.status === 'ACTIVE' ? '停用' : '启用'}</Button>
          <Button danger size="small" icon={<DeleteOutlined />} onClick={() => onDelete(app)}>删除</Button>
        </Space>
      )
    }
  ];
  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={apps}
      loading={loading}
      pagination={false}
      size="middle"
      scroll={{ x: 960 }}
      locale={{
        emptyText: (
          <Space direction="vertical" size={8} style={{ padding: '24px 0' }}>
            <Typography.Text type="secondary">暂无第三方应用</Typography.Text>
            <Typography.Text type="secondary">请先点击「新增应用」，保存后会自动进入配置页。</Typography.Text>
          </Space>
        )
      }}
    />
  );
}

function DrawerFooter({ onCancel, onSubmit, loading }: { onCancel: () => void; onSubmit: () => void; loading: boolean }) {
  return <Space style={{ display: 'flex', justifyContent: 'flex-end' }}><Button onClick={onCancel}>取消</Button><Button type="primary" loading={loading} onClick={onSubmit}>保存</Button></Space>;
}

function statusTag(value: string) {
  const color = value === 'ACTIVE' ? 'green' : value === 'LOCKED' ? 'gold' : 'default';
  return <Tag color={color}>{value}</Tag>;
}

const pageStyle: React.CSSProperties = { background: '#f5f7fb', minHeight: '100%', padding: 24 };
const headerStyle: React.CSSProperties = { alignItems: 'center', background: '#fff', border: '1px solid #e7ecf3', borderRadius: 8, display: 'flex', justifyContent: 'space-between', marginBottom: 16, padding: '18px 20px' };
const metricCardStyle: React.CSSProperties = { border: '1px solid #e7ecf3', marginBottom: 16, maxWidth: 240 };
const toolbarStyle: React.CSSProperties = { display: 'flex', justifyContent: 'flex-end', marginBottom: 12 };
