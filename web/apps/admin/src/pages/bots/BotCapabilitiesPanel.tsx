import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Select, Space, Switch, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  createBotCapability,
  deleteBotCapability,
  listBotCapabilities,
  type BotCapability,
  type SaveBotCapabilityRequest
} from '../../api/bots';

export function BotCapabilitiesPanel({
  botId,
  workflowOptions
}: {
  botId: string;
  workflowOptions: { value: string; label: string }[];
}) {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<SaveBotCapabilityRequest>();
  const capabilitiesQuery = useQuery({
    queryKey: ['bot-capabilities', botId],
    queryFn: () => listBotCapabilities(botId)
  });

  const createMutation = useMutation({
    mutationFn: (values: SaveBotCapabilityRequest) => createBotCapability(botId, values),
    onSuccess: async () => {
      message.success('已添加工作流能力');
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['bot-capabilities', botId] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (capabilityId: string) => deleteBotCapability(botId, capabilityId),
    onSuccess: async () => {
      message.success('已删除');
      await queryClient.invalidateQueries({ queryKey: ['bot-capabilities', botId] });
    }
  });

  const columns: ColumnsType<BotCapability> = [
    { title: '编码', dataIndex: 'capabilityCode', width: 120 },
    { title: '工作流', dataIndex: 'capabilityId', ellipsis: true },
    { title: '路由关键词', dataIndex: 'routingKeywords', ellipsis: true },
    {
      title: '主工作流',
      dataIndex: 'primaryCapability',
      width: 100,
      render: (value) => (value ? '是' : '否')
    },
    {
      title: '操作',
      width: 80,
      render: (_, row) => (
        <Button size="small" danger icon={<DeleteOutlined />} onClick={() => deleteMutation.mutate(row.id)} />
      )
    }
  ];

  return (
    <div style={{ marginTop: 16 }}>
      <Typography.Title level={5}>多工作流路由（P1b）</Typography.Title>
      <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
        配置多个候选工作流，系统按用户问题中的关键词自动选择；未命中时使用主工作流。
      </Typography.Paragraph>
      <Table
        size="small"
        rowKey="id"
        loading={capabilitiesQuery.isLoading}
        columns={columns}
        dataSource={capabilitiesQuery.data?.items ?? []}
        pagination={false}
        style={{ marginBottom: 12 }}
      />
      <Form
        form={form}
        layout="inline"
        initialValues={{ capabilityType: 'WORKFLOW', enabled: true, primaryCapability: false }}
        onFinish={(values) => createMutation.mutate(values)}
      >
        <Form.Item name="capabilityId" rules={[{ required: true, message: '选择工作流' }]}>
          <Select placeholder="工作流" style={{ width: 200 }} options={workflowOptions} />
        </Form.Item>
        <Form.Item name="capabilityCode">
          <Input placeholder="意图编码" style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="routingKeywords">
          <Input placeholder="关键词，逗号分隔" style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="primaryCapability" valuePropName="checked">
          <Switch checkedChildren="主" unCheckedChildren="备" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" icon={<PlusOutlined />} htmlType="submit" loading={createMutation.isPending}>
            添加
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
}
