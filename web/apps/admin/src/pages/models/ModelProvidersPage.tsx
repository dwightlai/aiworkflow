import { ApiOutlined, CheckCircleOutlined, CloudServerOutlined, PlusOutlined, RobotOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Form, Input, Row, Space, Statistic, Switch, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import {
  createModelProvider,
  listModelProviders,
  type CreateModelProviderRequest,
  type ModelProvider
} from '../../api/models';

const initialValues: CreateModelProviderRequest = {
  name: '',
  baseUrl: '',
  model: '',
  apiKeyRef: '',
  enabled: true
};

export function ModelProvidersPage() {
  const [form] = Form.useForm<CreateModelProviderRequest>();
  const queryClient = useQueryClient();
  const providersQuery = useQuery({
    queryKey: ['model-providers'],
    queryFn: listModelProviders
  });
  const providers = providersQuery.data?.items ?? [];
  const enabledCount = providers.filter((provider) => provider.enabled).length;

  const createMutation = useMutation({
    mutationFn: createModelProvider,
    onSuccess: async () => {
      message.success('模型配置已保存');
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['model-providers'] });
    }
  });

  const columns: ColumnsType<ModelProvider> = [
    {
      title: '模型名称',
      dataIndex: 'name',
      render: (_, provider) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{provider.name}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{provider.id}</Typography.Text>
        </Space>
      )
    },
    {
      title: '模型标识',
      dataIndex: 'model',
      render: (value: string) => <Tag color="blue">{value}</Tag>
    },
    {
      title: 'Base URL',
      dataIndex: 'baseUrl',
      ellipsis: true
    },
    {
      title: 'API Key 引用',
      dataIndex: 'apiKeyRef'
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 92,
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'green' : 'default'}>{enabled ? '启用' : '停用'}</Tag>
      )
    }
  ];

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>大模型配置</Typography.Title>
          <Typography.Text type="secondary">保存可被工作流 LLM 节点调用的模型供应商、Base URL、模型标识和密钥引用。</Typography.Text>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card title={<Space><PlusOutlined />新增模型</Space>} variant="borderless" styles={{ body: { paddingBottom: 12 } }}>
            <Form
              form={form}
              layout="vertical"
              initialValues={initialValues}
              onFinish={(values) => createMutation.mutate(values)}
            >
              <Form.Item name="name" label="配置名称" rules={[{ required: true, message: '请输入配置名称' }]}>
                <Input placeholder="OpenAI Compatible" />
              </Form.Item>
              <Form.Item name="baseUrl" label="Base URL" rules={[{ required: true, message: '请输入 Base URL' }]}>
                <Input placeholder="https://api.example.com/v1" />
              </Form.Item>
              <Form.Item name="model" label="模型标识" rules={[{ required: true, message: '请输入模型标识' }]}>
                <Input placeholder="gpt-4.1-mini / deepseek-v3" />
              </Form.Item>
              <Form.Item name="apiKeyRef" label="API Key 引用" rules={[{ required: true, message: '请输入密钥引用' }]}>
                <Input placeholder="dev-openai-key" />
              </Form.Item>
              <Form.Item name="enabled" label="启用状态" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="停用" />
              </Form.Item>
              <Button block type="primary" htmlType="submit" icon={<PlusOutlined />} loading={createMutation.isPending}>
                保存模型配置
              </Button>
            </Form>
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <div style={metricRowStyle}>
            <Card variant="borderless" style={metricCardStyle}>
              <Statistic title="已配置模型" value={providers.length} prefix={<RobotOutlined />} />
            </Card>
            <Card variant="borderless" style={metricCardStyle}>
              <Statistic title="启用中" value={enabledCount} prefix={<CheckCircleOutlined />} />
            </Card>
            <Card variant="borderless" style={metricCardStyle}>
              <Statistic title="工作流调用方式" value="LLM 节点选择" prefix={<ApiOutlined />} valueStyle={{ fontSize: 18 }} />
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
        </Col>
      </Row>
    </section>
  );
}

const pageStyle: React.CSSProperties = {
  background: '#f5f7fb',
  minHeight: '100%',
  padding: 24
};

const headerStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  marginBottom: 16,
  padding: '18px 20px'
};

const metricRowStyle: React.CSSProperties = {
  display: 'grid',
  gap: 12,
  gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
  marginBottom: 16
};

const metricCardStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3'
};
