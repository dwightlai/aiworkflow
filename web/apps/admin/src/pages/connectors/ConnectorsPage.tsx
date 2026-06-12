import { ApiOutlined, DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SettingOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Drawer,
  Form,
  Input,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useState } from 'react';
import {
  createConnector,
  createConnectorOperation,
  deleteConnector,
  deleteConnectorOperation,
  listConnectorOperations,
  listConnectors,
  testConnectorOperation,
  updateConnector,
  updateConnectorOperation,
  type Connector,
  type ConnectorOperation,
  type SaveConnectorOperationRequest,
  type SaveConnectorRequest
} from '../../api/connectors';

const connectorInitial: SaveConnectorRequest = {
  name: '',
  code: '',
  type: 'GENERIC',
  accessType: 'HTTP',
  baseUrl: null,
  authMode: 'NONE',
  authConfig: null,
  enabled: true,
  description: null
};

const operationInitial: SaveConnectorOperationRequest = {
  name: '',
  code: '',
  method: 'GET',
  path: '',
  operationType: 'QUERY',
  riskLevel: 'LOW',
  needConfirm: false,
  confirmSummaryTemplate: null,
  requestTemplate: null,
  enabled: true,
  description: null
};

export function ConnectorsPage() {
  const queryClient = useQueryClient();
  const [connectorForm] = Form.useForm<SaveConnectorRequest>();
  const [operationForm] = Form.useForm<SaveConnectorOperationRequest>();
  const [connectorDrawerOpen, setConnectorDrawerOpen] = useState(false);
  const [editingConnector, setEditingConnector] = useState<Connector | null>(null);
  const [opsConnector, setOpsConnector] = useState<Connector | null>(null);
  const [operationDrawerOpen, setOperationDrawerOpen] = useState(false);
  const [editingOperation, setEditingOperation] = useState<ConnectorOperation | null>(null);
  const [testResult, setTestResult] = useState<string | null>(null);

  const connectorsQuery = useQuery({ queryKey: ['connectors'], queryFn: listConnectors });
  const connectors = connectorsQuery.data?.items ?? [];

  const operationsQuery = useQuery({
    queryKey: ['connector-operations', opsConnector?.id],
    queryFn: () => listConnectorOperations(opsConnector!.id),
    enabled: Boolean(opsConnector)
  });
  const operations = operationsQuery.data?.items ?? [];

  const saveConnectorMutation = useMutation({
    mutationFn: (values: SaveConnectorRequest) =>
      editingConnector ? updateConnector(editingConnector.id, values) : createConnector(values),
    onSuccess: async () => {
      message.success(editingConnector ? '连接器已更新' : '连接器已创建');
      setConnectorDrawerOpen(false);
      setEditingConnector(null);
      connectorForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['connectors'] });
    }
  });

  const deleteConnectorMutation = useMutation({
    mutationFn: (id: string) => deleteConnector(id),
    onSuccess: async () => {
      message.success('连接器已删除');
      await queryClient.invalidateQueries({ queryKey: ['connectors'] });
    }
  });

  const saveOperationMutation = useMutation({
    mutationFn: (values: SaveConnectorOperationRequest) => {
      if (!opsConnector) {
        throw new Error('未选择连接器');
      }
      return editingOperation
        ? updateConnectorOperation(opsConnector.id, editingOperation.id, values)
        : createConnectorOperation(opsConnector.id, values);
    },
    onSuccess: async () => {
      message.success(editingOperation ? '接口已更新' : '接口已创建');
      setOperationDrawerOpen(false);
      setEditingOperation(null);
      operationForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['connector-operations', opsConnector?.id] });
    }
  });

  const deleteOperationMutation = useMutation({
    mutationFn: (operationId: string) => {
      if (!opsConnector) {
        throw new Error('未选择连接器');
      }
      return deleteConnectorOperation(opsConnector.id, operationId);
    },
    onSuccess: async () => {
      message.success('接口已删除');
      await queryClient.invalidateQueries({ queryKey: ['connector-operations', opsConnector?.id] });
    }
  });

  const testMutation = useMutation({
    mutationFn: (operationId: string) => {
      if (!opsConnector) {
        throw new Error('未选择连接器');
      }
      return testConnectorOperation(opsConnector.id, operationId);
    },
    onSuccess: (result) => {
      setTestResult(JSON.stringify(result, null, 2));
    },
    onError: (error) => {
      setTestResult(error instanceof Error ? error.message : '测试失败');
    }
  });

  const connectorColumns: ColumnsType<Connector> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '编码', dataIndex: 'code', key: 'code' },
    { title: '类型', dataIndex: 'type', key: 'type', render: (v) => <Tag>{v}</Tag> },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean) => (enabled ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>)
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<SettingOutlined />} onClick={() => setOpsConnector(record)}>接口</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEditConnector(record)}>编辑</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => deleteConnectorMutation.mutate(record.id)} />
        </Space>
      )
    }
  ];

  const operationColumns: ColumnsType<ConnectorOperation> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '编码', dataIndex: 'code', key: 'code' },
    { title: '方法', dataIndex: 'method', key: 'method', width: 80 },
    { title: '路径', dataIndex: 'path', key: 'path', ellipsis: true },
    {
      title: '风险',
      dataIndex: 'riskLevel',
      key: 'riskLevel',
      render: (v) => <Tag color={v === 'HIGH' ? 'red' : 'default'}>{v}</Tag>
    },
    {
      title: '确认',
      dataIndex: 'needConfirm',
      key: 'needConfirm',
      render: (v) => (v ? '是' : '否')
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => testMutation.mutate(record.id)} loading={testMutation.isPending}>测试</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEditOperation(record)}>编辑</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => deleteOperationMutation.mutate(record.id)} />
        </Space>
      )
    }
  ];

  function openCreateConnector() {
    setEditingConnector(null);
    connectorForm.setFieldsValue(connectorInitial);
    setConnectorDrawerOpen(true);
  }

  function openEditConnector(connector: Connector) {
    setEditingConnector(connector);
    connectorForm.setFieldsValue({
      name: connector.name,
      code: connector.code,
      type: connector.type,
      accessType: connector.accessType,
      baseUrl: connector.baseUrl,
      authMode: connector.authMode ?? 'NONE',
      authConfig: connector.authConfig,
      enabled: connector.enabled,
      description: connector.description
    });
    setConnectorDrawerOpen(true);
  }

  function openCreateOperation() {
    setEditingOperation(null);
    operationForm.setFieldsValue(operationInitial);
    setOperationDrawerOpen(true);
  }

  function openEditOperation(operation: ConnectorOperation) {
    setEditingOperation(operation);
    operationForm.setFieldsValue({
      name: operation.name,
      code: operation.code,
      method: operation.method,
      path: operation.path,
      operationType: operation.operationType,
      riskLevel: operation.riskLevel,
      needConfirm: operation.needConfirm,
      confirmSummaryTemplate: operation.confirmSummaryTemplate,
      requestTemplate: operation.requestTemplate,
      enabled: operation.enabled,
      description: operation.description
    });
    setOperationDrawerOpen(true);
  }

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>连接器</Typography.Title>
          <Typography.Text type="secondary">管理外部系统 HTTP 连接器与接口 Operation</Typography.Text>
        </Space>
        <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['connectors'] })}>
          刷新
        </Button>
      </div>

      <Card variant="borderless" style={{ marginBottom: 16, border: '1px solid #e7ecf3' }}>
        <Statistic title="连接器数量" value={connectors.length} prefix={<ApiOutlined />} />
      </Card>

      {connectorsQuery.isError ? (
        <Alert type="error" showIcon message="连接器加载失败" style={{ marginBottom: 12 }} />
      ) : null}

      <Card variant="borderless" style={{ border: '1px solid #e7ecf3' }}>
        <div style={{ marginBottom: 12 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateConnector}>新增连接器</Button>
        </div>
        <Table rowKey="id" columns={connectorColumns} dataSource={connectors} loading={connectorsQuery.isLoading} />
      </Card>

      <Drawer
        title={editingConnector ? '编辑连接器' : '新增连接器'}
        open={connectorDrawerOpen}
        width={520}
        onClose={() => setConnectorDrawerOpen(false)}
        footer={
          <Space>
            <Button onClick={() => setConnectorDrawerOpen(false)}>取消</Button>
            <Button type="primary" loading={saveConnectorMutation.isPending} onClick={() => connectorForm.submit()}>
              保存
            </Button>
          </Space>
        }
      >
        <Form form={connectorForm} layout="vertical" onFinish={(v) => saveConnectorMutation.mutate(v)}>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="编码" rules={[{ required: true }]}>
            <Input disabled={Boolean(editingConnector)} />
          </Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true }]}>
            <Select options={['OA', 'ARCHIVE', 'WORKFLOW', 'KB', 'GENERIC'].map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Form.Item name="accessType" label="接入类型" rules={[{ required: true }]}>
            <Select options={[{ value: 'HTTP', label: 'HTTP' }]} />
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL">
            <Input placeholder="https://api.example.com" />
          </Form.Item>
          <Form.Item name="authMode" label="认证模式">
            <Select
              options={['NONE', 'USER_TOKEN', 'API_KEY', 'FIXED_HEADER'].map((v) => ({ value: v, label: v }))}
            />
          </Form.Item>
          <Form.Item name="authConfig" label="认证配置 JSON">
            <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} placeholder='{"headerName":"X-Api-Key"}' />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={opsConnector ? `${opsConnector.name} · 接口管理` : '接口管理'}
        open={Boolean(opsConnector)}
        width={900}
        onClose={() => {
          setOpsConnector(null);
          setTestResult(null);
        }}
      >
        <div style={{ marginBottom: 12 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateOperation}>新增接口</Button>
        </div>
        <Table
          rowKey="id"
          size="small"
          columns={operationColumns}
          dataSource={operations}
          loading={operationsQuery.isLoading}
        />
        {testResult ? (
          <Alert type="info" message="测试结果" description={<pre style={{ whiteSpace: 'pre-wrap' }}>{testResult}</pre>} style={{ marginTop: 12 }} />
        ) : null}
      </Drawer>

      <Drawer
        title={editingOperation ? '编辑接口' : '新增接口'}
        open={operationDrawerOpen}
        width={520}
        onClose={() => setOperationDrawerOpen(false)}
        footer={
          <Space>
            <Button onClick={() => setOperationDrawerOpen(false)}>取消</Button>
            <Button type="primary" loading={saveOperationMutation.isPending} onClick={() => operationForm.submit()}>
              保存
            </Button>
          </Space>
        }
      >
        <Form form={operationForm} layout="vertical" onFinish={(v) => saveOperationMutation.mutate(v)}>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="编码" rules={[{ required: true }]}>
            <Input disabled={Boolean(editingOperation)} />
          </Form.Item>
          <Form.Item name="method" label="方法">
            <Select options={['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Form.Item name="path" label="路径">
            <Input placeholder="/api/archive/search" />
          </Form.Item>
          <Form.Item name="operationType" label="类型" rules={[{ required: true }]}>
            <Select options={['QUERY', 'ACTION'].map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Form.Item name="riskLevel" label="风险等级" rules={[{ required: true }]}>
            <Select options={['LOW', 'HIGH'].map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Form.Item name="needConfirm" label="需要确认" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="confirmSummaryTemplate" label="确认摘要模板">
            <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
          <Form.Item name="requestTemplate" label="请求模板">
            <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Drawer>
    </section>
  );
}

const pageStyle: React.CSSProperties = {
  background: '#f5f7fb',
  minHeight: '100%',
  padding: 24
};

const headerStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  display: 'flex',
  justifyContent: 'space-between',
  marginBottom: 16,
  padding: '18px 20px'
};
