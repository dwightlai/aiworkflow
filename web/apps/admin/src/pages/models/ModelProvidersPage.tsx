import {
  CheckCircleOutlined,
  CloseOutlined,
  CloudServerOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  FullscreenOutlined,
  PlusOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Avatar,
  Button,
  Card,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useState } from 'react';
import {
  createModelProvider,
  deleteModelProvider,
  listModelProviders,
  updateModelProvider,
  type CreateModelProviderRequest,
  type ModelProvider
} from '../../api/models';

type ModelFormValues = CreateModelProviderRequest;

const initialValues: ModelFormValues = {
  name: 'deepseek-chat',
  modelType: 'DeepSeek',
  modelUsage: 'CHAT',
  description: null,
  visionSupport: false,
  pricePerMillionTokens: null,
  baseUrl: 'https://api.deepseek.com/v1',
  model: 'deepseek-chat',
  apiKeyRef: '',
  enabled: true
};

const modelTypes = [
  { value: 'DeepSeek', label: 'DeepSeek', mark: 'DS', color: '#3554d1' },
  { value: 'OpenAI', label: 'OpenAI', mark: 'OA', color: '#111827' },
  { value: 'Anthropic', label: 'Anthropic', mark: 'AI', color: '#171717' },
  { value: 'Zhipu', label: 'Zhipu', mark: 'ZP', color: '#5b7cfa' },
  { value: 'Moonshot', label: 'Moonshot', mark: 'MS', color: '#2f2f35' },
  { value: 'Qwen', label: 'Qwen', mark: 'QW', color: '#3344a5' },
  { value: 'MiniMax', label: 'MiniMax', mark: 'MM', color: '#ff4d7a' },
  { value: 'VolcEngine', label: 'VolcEngine', mark: 'VE', color: '#1677ff' },
  { value: 'SiliconFlow', label: 'SiliconFlow', mark: 'SF', color: '#6f49ff' },
  { value: 'Ollama', label: 'Ollama', mark: 'OL', color: '#0f172a' },
  { value: 'Custom', label: '自定义', mark: '自', color: '#0ea5e9' }
];

const modelUsages = [
  { value: 'CHAT', label: '对话', color: 'blue', description: '工作流 LLM 节点调用' },
  { value: 'EMBEDDING', label: '嵌入', color: 'purple', description: '知识库向量化检索' },
  { value: 'RERANK', label: '重排', color: 'gold', description: '检索结果二次排序' },
  { value: 'MULTIMODAL', label: '多模态', color: 'cyan', description: '图文理解或视觉任务' }
];

export function ModelProvidersPage() {
  const [form] = Form.useForm<ModelFormValues>();
  const queryClient = useQueryClient();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<ModelProvider | null>(null);
  const watchedModelType = Form.useWatch('modelType', form) ?? initialValues.modelType;

  const providersQuery = useQuery({
    queryKey: ['model-providers'],
    queryFn: listModelProviders
  });
  const providers = providersQuery.data?.items ?? [];
  const enabledCount = providers.filter((provider) => provider.enabled).length;
  const visionCount = providers.filter((provider) => provider.visionSupport).length;
  const embeddingCount = providers.filter((provider) => provider.modelUsage === 'EMBEDDING').length;

  const saveMutation = useMutation({
    mutationFn: (values: ModelFormValues) => {
      if (editingProvider) {
        return updateModelProvider(editingProvider.id, values);
      }
      return createModelProvider(values);
    },
    onSuccess: async () => {
      message.success(editingProvider ? '模型配置已修改' : '模型配置已保存');
      setDrawerOpen(false);
      setEditingProvider(null);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['model-providers'] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (provider: ModelProvider) => deleteModelProvider(provider.id),
    onSuccess: async () => {
      message.success('模型配置已删除');
      await queryClient.invalidateQueries({ queryKey: ['model-providers'] });
    }
  });

  function openCreateDrawer() {
    setEditingProvider(null);
    form.setFieldsValue(initialValues);
    setDrawerOpen(true);
  }

  function openEditDrawer(provider: ModelProvider) {
    setEditingProvider(provider);
    form.setFieldsValue({
      name: provider.name,
      modelType: provider.modelType,
      modelUsage: provider.modelUsage ?? 'CHAT',
      description: provider.description,
      visionSupport: provider.visionSupport,
      pricePerMillionTokens: provider.pricePerMillionTokens,
      baseUrl: provider.baseUrl,
      model: provider.model,
      apiKeyRef: provider.apiKeyRef,
      enabled: provider.enabled
    });
    setDrawerOpen(true);
  }

  const columns: ColumnsType<ModelProvider> = [
    {
      title: '模型名称',
      dataIndex: 'name',
      render: (_, provider) => (
        <Space size={10}>
          <ProviderAvatar type={provider.modelType} />
          <Space direction="vertical" size={2}>
            <Typography.Text strong>{provider.name}</Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>{provider.model}</Typography.Text>
          </Space>
        </Space>
      )
    },
    {
      title: '模型类型',
      dataIndex: 'modelType',
      width: 140,
      render: (value: string) => <ProviderTypeLabel value={value} />
    },
    {
      title: '用途',
      dataIndex: 'modelUsage',
      width: 110,
      render: (value: string) => <UsageTag value={value} />
    },
    {
      title: '能力',
      dataIndex: 'visionSupport',
      width: 130,
      render: (visionSupport: boolean) => (
        <Space size={6}>
          <Tag color="blue">文本</Tag>
          {visionSupport ? <Tag color="purple">视觉</Tag> : null}
        </Space>
      )
    },
    {
      title: '计费价格',
      dataIndex: 'pricePerMillionTokens',
      width: 130,
      render: (value: number | null) => value == null ? <Typography.Text type="secondary">未设置</Typography.Text> : `${value} / 百万Token`
    },
    {
      title: 'Base URL',
      dataIndex: 'baseUrl',
      ellipsis: true
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 92,
      render: (enabled: boolean) => <Tag color={enabled ? 'green' : 'default'}>{enabled ? '启用' : '停用'}</Tag>
    },
    {
      title: '操作',
      width: 168,
      render: (_, provider) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEditDrawer(provider)}>
            编辑
          </Button>
          <Button
            danger
            size="small"
            icon={<DeleteOutlined />}
            aria-label="删除模型"
            loading={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate(provider)}
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>大模型配置</Typography.Title>
          <Typography.Text type="secondary">统一维护工作流 LLM 节点、知识库嵌入检索可调用的模型、地址、密钥与价格。</Typography.Text>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>新增模型</Button>
      </div>

      <div style={metricRowStyle}>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="已配置模型" value={providers.length} prefix={<RobotOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="启用中" value={enabledCount} prefix={<CheckCircleOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="嵌入模型" value={embeddingCount} prefix={<SearchOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="视觉模型" value={visionCount} prefix={<EyeOutlined />} />
        </Card>
      </div>

      {providersQuery.isError ? (
        <Alert
          type="error"
          showIcon
          message="模型配置加载失败"
          description={(providersQuery.error as Error).message}
          style={{ marginBottom: 12 }}
        />
      ) : null}

      <Card
        variant="borderless"
        title={<Space><CloudServerOutlined />模型清单</Space>}
        extra={<Tag color="geekblue">工作流可调用</Tag>}
      >
        <Table
          rowKey="id"
          loading={providersQuery.isLoading}
          columns={columns}
          dataSource={providers}
          pagination={{ pageSize: 8, showSizeChanger: false }}
          size="middle"
        />
      </Card>

      <Drawer
        title={(
          <Space>
            <Typography.Text strong>{editingProvider ? '编辑' : '新增模型'}</Typography.Text>
            <Tooltip title="全屏"><FullscreenOutlined style={{ color: '#98a2b3' }} /></Tooltip>
          </Space>
        )}
        open={drawerOpen}
        width={500}
        onClose={() => setDrawerOpen(false)}
        closeIcon={<CloseOutlined />}
        styles={{ body: { padding: 0 }, footer: { padding: '10px 16px' } }}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button type="primary" icon={<CheckCircleOutlined />} loading={saveMutation.isPending} onClick={() => form.submit()}>
              {editingProvider ? '修改' : '保存'}
            </Button>
          </Space>
        )}
      >
        <div style={drawerBodyStyle}>
          <div style={logoPreviewStyle}>
            <ProviderAvatar type={watchedModelType} size={128} />
          </div>
          <Form form={form} layout="horizontal" labelCol={{ flex: '96px' }} wrapperCol={{ flex: 1 }} initialValues={initialValues} onFinish={(values) => saveMutation.mutate(normalizeValues(values))}>
            <Form.Item name="name" label="模型名称" rules={[{ required: true, message: '请输入模型名称' }]}>
              <Input placeholder="deepseek-chat" />
            </Form.Item>
            <Form.Item name="modelType" label="模型类型" rules={[{ required: true, message: '请选择模型类型' }]}>
              <Select
                aria-label="模型类型"
                options={modelTypes.map((item) => ({
                  value: item.value,
                  label: <ProviderOption option={item} />
                }))}
              />
            </Form.Item>
            <Form.Item name="modelUsage" label="模型用途" rules={[{ required: true, message: '请选择模型用途' }]}>
              <Select
                aria-label="模型用途"
                options={modelUsages.map((item) => ({
                  value: item.value,
                  label: (
                    <Space size={8}>
                      <Tag color={item.color} style={{ marginInlineEnd: 0 }}>{item.label}</Tag>
                      <Typography.Text type="secondary">{item.description}</Typography.Text>
                    </Space>
                  )
                }))}
              />
            </Form.Item>
            <Form.Item name="description" label="模型描述">
              <Input placeholder="请输入 模型描述" />
            </Form.Item>
            <Form.Item name="visionSupport" label={<InfoLabel label="视觉支持" />} valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="pricePerMillionTokens" label={<InfoLabel label="计费价格" />}>
              <InputNumber min={0} precision={4} style={{ width: '100%' }} placeholder="0.0000" />
            </Form.Item>
            <Form.Item name="baseUrl" label="Base URL" rules={[{ required: true, message: '请输入 Base URL' }]}>
              <Input placeholder="https://api.deepseek.com/v1" />
            </Form.Item>
            <Form.Item name="apiKeyRef" label="API Key" rules={[{ required: true, message: '请输入 API Key' }]}>
              <Input.Password placeholder="请输入 API Key" iconRender={(visible) => visible ? <EyeOutlined /> : <EyeInvisibleOutlined />} />
            </Form.Item>
            <Form.Item name="model" label="调用模型" rules={[{ required: true, message: '请输入调用模型标识' }]}>
              <Input placeholder="deepseek-chat" />
            </Form.Item>
            <Form.Item name="enabled" label="启用状态" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
          </Form>
        </div>
      </Drawer>
    </section>
  );
}

function normalizeValues(values: ModelFormValues): ModelFormValues {
  return {
    ...values,
    modelUsage: values.modelUsage || 'CHAT',
    description: values.description || null,
    pricePerMillionTokens: values.pricePerMillionTokens ?? null
  };
}

function UsageTag({ value }: { value: string }) {
  const option = modelUsages.find((item) => item.value === value) ?? modelUsages[0];
  return <Tag color={option.color}>{option.label}</Tag>;
}

function ProviderTypeLabel({ value }: { value: string }) {
  const option = modelTypes.find((item) => item.value === value) ?? modelTypes[modelTypes.length - 1];
  return (
    <Space size={8}>
      <ProviderAvatar type={option.value} size={22} />
      <Typography.Text>{option.label}</Typography.Text>
    </Space>
  );
}

function ProviderOption({ option }: { option: typeof modelTypes[number] }) {
  return (
    <Space size={8}>
      <ProviderAvatar type={option.value} size={20} />
      <span>{option.label}</span>
    </Space>
  );
}

function ProviderAvatar({ type, size = 28 }: { type: string; size?: number }) {
  const option = modelTypes.find((item) => item.value === type) ?? modelTypes[modelTypes.length - 1];
  return (
    <Avatar
      size={size}
      style={{ background: option.color, color: '#fff', fontSize: Math.max(10, Math.round(size * 0.28)), fontWeight: 700 }}
    >
      {option.mark}
    </Avatar>
  );
}

function InfoLabel({ label }: { label: string }) {
  return (
    <Space size={4}>
      <SafetyCertificateOutlined style={{ color: '#6b7280' }} />
      <span>{label}</span>
    </Space>
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

const metricRowStyle: React.CSSProperties = {
  display: 'grid',
  gap: 12,
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
  marginBottom: 16
};

const metricCardStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3'
};

const drawerBodyStyle: React.CSSProperties = {
  padding: '0 28px 24px'
};

const logoPreviewStyle: React.CSSProperties = {
  alignItems: 'center',
  borderBottom: '1px solid #edf1f7',
  display: 'flex',
  height: 174,
  justifyContent: 'center',
  margin: '0 -28px 24px'
};
