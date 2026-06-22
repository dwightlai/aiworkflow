import { DeleteOutlined, EditOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Drawer, Form, Input, Select, Space, Switch, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import {
  createBotCapability,
  deleteBotCapability,
  listBotCapabilities,
  previewBotWorkflowRoute,
  updateBotCapability,
  type BotCapability,
  type SaveBotCapabilityRequest,
  type WorkflowRoutePreview
} from '../../api/bots';

const MATCH_LABELS: Record<string, string> = {
  default: '默认工作流',
  single: '唯一候选',
  primary: '主工作流兜底',
  empty: '空消息'
};

function matchLabel(reason: string) {
  if (MATCH_LABELS[reason]) {
    return MATCH_LABELS[reason];
  }
  if (reason.startsWith('keyword:')) {
    return `关键词「${reason.slice(8)}」`;
  }
  if (reason.startsWith('code:')) {
    return `意图编码「${reason.slice(5)}」`;
  }
  return reason;
}

export function BotCapabilitiesPanel({
  botId,
  workflowOptions
}: {
  botId: string;
  workflowOptions: { value: string; label: string }[];
}) {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<SaveBotCapabilityRequest>();
  const [editForm] = Form.useForm<SaveBotCapabilityRequest>();
  const [editing, setEditing] = useState<BotCapability | null>(null);
  const [previewMessage, setPreviewMessage] = useState('请帮我生成报告');
  const [previewResult, setPreviewResult] = useState<WorkflowRoutePreview | null>(null);

  const workflowNameMap = useMemo(
    () => Object.fromEntries(workflowOptions.map((item) => [item.value, item.label])),
    [workflowOptions]
  );

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

  const updateMutation = useMutation({
    mutationFn: ({ id, values }: { id: string; values: SaveBotCapabilityRequest }) =>
      updateBotCapability(botId, id, values),
    onSuccess: async () => {
      message.success('已更新');
      setEditing(null);
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

  const previewMutation = useMutation({
    mutationFn: (text: string) => previewBotWorkflowRoute(botId, text),
    onSuccess: (result) => setPreviewResult(result)
  });

  const columns: ColumnsType<BotCapability> = [
    { title: '编码', dataIndex: 'capabilityCode', width: 120 },
    {
      title: '工作流',
      dataIndex: 'capabilityId',
      ellipsis: true,
      render: (value: string) => workflowNameMap[value] ?? value
    },
    { title: '路由关键词', dataIndex: 'routingKeywords', ellipsis: true },
    {
      title: '主工作流',
      dataIndex: 'primaryCapability',
      width: 100,
      render: (value) => (value ? <Tag color="blue">主</Tag> : '否')
    },
    {
      title: '操作',
      width: 120,
      render: (_, row) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(row)} />
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => deleteMutation.mutate(row.id)} />
        </Space>
      )
    }
  ];

  function openEdit(row: BotCapability) {
    setEditing(row);
    editForm.setFieldsValue({
      capabilityType: row.capabilityType,
      capabilityId: row.capabilityId,
      capabilityCode: row.capabilityCode,
      routingKeywords: row.routingKeywords,
      primaryCapability: row.primaryCapability,
      enabled: row.enabled
    });
  }

  return (
    <div style={{ marginTop: 16 }}>
      <Typography.Title level={5}>多工作流路由</Typography.Title>
      <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
        配置多个候选工作流，按用户问题中的关键词或意图编码自动选择；未命中时使用主工作流。
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

      <div style={{ marginTop: 16, padding: 12, background: '#fafcff', borderRadius: 8, border: '1px solid #e7ecf3' }}>
        <Typography.Text strong>路由试算</Typography.Text>
        <Space style={{ marginTop: 8, width: '100%' }} wrap>
          <Input
            value={previewMessage}
            onChange={(e) => setPreviewMessage(e.target.value)}
            placeholder="输入用户问题"
            style={{ width: 360 }}
          />
          <Button
            icon={<ThunderboltOutlined />}
            loading={previewMutation.isPending}
            onClick={() => previewMutation.mutate(previewMessage)}
          >
            试算
          </Button>
        </Space>
        {previewResult ? (
          <Typography.Paragraph style={{ marginTop: 8, marginBottom: 0 }}>
            命中：<Tag color="processing">{previewResult.workflowName || previewResult.workflowId || '未绑定'}</Tag>
            <Typography.Text type="secondary">（{matchLabel(previewResult.matchReason)}）</Typography.Text>
          </Typography.Paragraph>
        ) : null}
      </div>

      <Drawer
        title="编辑工作流能力"
        open={Boolean(editing)}
        width={480}
        onClose={() => setEditing(null)}
        footer={
          <Button type="primary" loading={updateMutation.isPending} onClick={() => editForm.submit()}>
            保存
          </Button>
        }
      >
        <Form
          form={editForm}
          layout="vertical"
          onFinish={(values) => editing && updateMutation.mutate({ id: editing.id, values })}
        >
          <Form.Item name="capabilityId" label="工作流" rules={[{ required: true }]}>
            <Select options={workflowOptions} />
          </Form.Item>
          <Form.Item name="capabilityCode" label="意图编码">
            <Input />
          </Form.Item>
          <Form.Item name="routingKeywords" label="路由关键词">
            <Input placeholder="逗号分隔" />
          </Form.Item>
          <Form.Item name="primaryCapability" label="主工作流" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
}
