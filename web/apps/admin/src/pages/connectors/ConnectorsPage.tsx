import { ApiOutlined, DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SettingOutlined, UploadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Drawer,
  Form,
  Input,
  Modal,
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
  exportConnector,
  importConnector,
  listConnectorOperations,
  listConnectorStats,
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
  const [testModalOpen, setTestModalOpen] = useState(false);
  const [testingOperation, setTestingOperation] = useState<ConnectorOperation | null>(null);
  const [testPayloadJson, setTestPayloadJson] = useState('{}');

  const connectorsQuery = useQuery({ queryKey: ['connectors'], queryFn: listConnectors });
  const statsQuery = useQuery({ queryKey: ['connector-stats'], queryFn: listConnectorStats });
  const connectors = connectorsQuery.data?.items ?? [];
  const stats = statsQuery.data?.items ?? [];

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
    mutationFn: ({ operationId, payload }: { operationId: string; payload?: Record<string, unknown> }) => {
      if (!opsConnector) {
        throw new Error('未选择连接器');
      }
      return testConnectorOperation(opsConnector.id, operationId, payload);
    },
    onSuccess: (result) => {
      const lines = [
        `success: ${result.success}`,
        result.statusCode != null ? `statusCode: ${result.statusCode}` : null,
        result.durationMs != null ? `durationMs: ${result.durationMs}` : null,
        result.traceId ? `traceId: ${result.traceId}` : null,
        result.requestUrl ? `requestUrl: ${result.requestUrl}` : null,
        result.body ? `body:\n${result.body}` : null,
        result.errorMessage ? `error: ${result.errorMessage}` : null
      ].filter(Boolean);
      setTestResult(lines.join('\n'));
      setTestModalOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['connector-stats'] });
    },
    onError: (error) => {
      setTestResult(error instanceof Error ? error.message : '测试失败');
    }
  });

  function openTestOperation(operation: ConnectorOperation) {
    setTestingOperation(operation);
    setTestPayloadJson(operation.requestTemplate?.trim() ? operation.requestTemplate : '{}');
    setTestModalOpen(true);
  }

  function runTestOperation() {
    if (!testingOperation) {
      return;
    }
    let payload: Record<string, unknown> | undefined;
    const trimmed = testPayloadJson.trim();
    if (trimmed) {
      try {
        payload = JSON.parse(trimmed) as Record<string, unknown>;
      } catch {
        message.error('请求参数 JSON 格式错误');
        return;
      }
    }
    testMutation.mutate({ operationId: testingOperation.id, payload });
  }

  async function handleExportConnector(connector: Connector) {
    const bundle = await exportConnector(connector.id);
    const blob = new Blob([JSON.stringify(bundle, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${connector.code}-connector.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  async function handleImportConnector(file: File) {
    const text = await file.text();
    const bundle = JSON.parse(text);
    await importConnector(bundle, true);
    message.success('连接器已导入');
    await queryClient.invalidateQueries({ queryKey: ['connectors'] });
  }

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
          <Button size="small" icon={<DownloadOutlined />} onClick={() => void handleExportConnector(record)}>导出</Button>
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
          <Button size="small" onClick={() => openTestOperation(record)} loading={testMutation.isPending && testingOperation?.id === record.id}>
            测试
          </Button>
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
        <Space size={48} wrap>
          <Statistic title="连接器数量" value={connectors.length} prefix={<ApiOutlined />} />
          <Statistic title="调用记录" value={stats.reduce((sum, item) => sum + item.totalCalls, 0)} />
          <Statistic title="成功调用" value={stats.reduce((sum, item) => sum + item.successCalls, 0)} />
        </Space>
      </Card>

      {connectorsQuery.isError ? (
        <Alert type="error" showIcon message="连接器加载失败" style={{ marginBottom: 12 }} />
      ) : null}

      <Card variant="borderless" style={{ border: '1px solid #e7ecf3' }}>
        <div style={{ marginBottom: 12, display: 'flex', gap: 8 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateConnector}>新增连接器</Button>
          <Button icon={<UploadOutlined />} onClick={() => document.getElementById('connector-import-input')?.click()}>
            导入 JSON
          </Button>
          <input
            id="connector-import-input"
            type="file"
            accept="application/json"
            hidden
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) {
                void handleImportConnector(file);
              }
              e.currentTarget.value = '';
            }}
          />
        </div>
        <Table rowKey="id" columns={connectorColumns} dataSource={connectors} loading={connectorsQuery.isLoading} />
      </Card>

      {stats.length > 0 ? (
        <Card variant="borderless" title="调用统计" style={{ marginTop: 16, border: '1px solid #e7ecf3' }}>
          <Table
            size="small"
            rowKey={(row) => `${row.connectorCode}:${row.operationCode}`}
            pagination={false}
            dataSource={stats.slice(0, 10)}
            columns={[
              { title: '连接器', dataIndex: 'connectorCode' },
              { title: '接口', dataIndex: 'operationCode' },
              { title: '总次数', dataIndex: 'totalCalls', width: 90 },
              { title: '成功', dataIndex: 'successCalls', width: 80 },
              { title: '失败', dataIndex: 'failedCalls', width: 80 }
            ]}
          />
        </Card>
      ) : null}

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

      <Modal
        title={testingOperation ? `测试 · ${testingOperation.name}` : '测试接口'}
        open={testModalOpen}
        onCancel={() => setTestModalOpen(false)}
        onOk={runTestOperation}
        confirmLoading={testMutation.isPending}
        okText="执行测试"
      >
        <Typography.Text type="secondary">请求参数 JSON（留空则使用接口模板渲染）</Typography.Text>
        <Input.TextArea
          value={testPayloadJson}
          onChange={(e) => setTestPayloadJson(e.target.value)}
          autoSize={{ minRows: 6, maxRows: 16 }}
          style={{ marginTop: 8, fontFamily: 'monospace' }}
        />
      </Modal>

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
