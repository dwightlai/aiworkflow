import {
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  MessageOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  RobotOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Avatar, Button, Card, Drawer, Empty, Form, Input, Select, Space, Statistic, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useMemo, useState } from 'react';
import { createBot, deleteBot, listBots, runBot, updateBot, type Bot, type BotRunResult, type SaveBotRequest } from '../../api/bots';
import { listKnowledgeBases } from '../../api/knowledge';
import { listModelProviders } from '../../api/models';
import { listWorkflows } from '../../api/workflows';

const initialBotValues: SaveBotRequest = {
  name: '',
  description: null,
  avatar: 'robot',
  workflowId: '',
  modelProviderId: null,
  knowledgeBaseId: null,
  systemPrompt: '',
  openingMessage: '你好，我是你的智能助手。',
  status: 'ENABLED'
};

export function BotsPage() {
  const [botForm] = Form.useForm<SaveBotRequest>();
  const [runForm] = Form.useForm<{ message: string; input: string }>();
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingBot, setEditingBot] = useState<Bot | null>(null);
  const [runningBot, setRunningBot] = useState<Bot | null>(null);
  const [runResult, setRunResult] = useState<BotRunResult | null>(null);
  const [runError, setRunError] = useState<string | null>(null);

  const botsQuery = useQuery({ queryKey: ['bots'], queryFn: listBots });
  const workflowsQuery = useQuery({ queryKey: ['workflows'], queryFn: listWorkflows });
  const modelsQuery = useQuery({ queryKey: ['model-providers'], queryFn: listModelProviders });
  const knowledgeQuery = useQuery({ queryKey: ['knowledge-bases'], queryFn: listKnowledgeBases });

  const bots = botsQuery.data?.items ?? [];
  const workflows = workflowsQuery.data?.items ?? [];
  const models = modelsQuery.data?.items ?? [];
  const knowledgeBases = knowledgeQuery.data?.items ?? [];

  const workflowNameById = useMemo(() => new Map(workflows.map((workflow) => [workflow.id, workflow.name])), [workflows]);
  const modelNameById = useMemo(() => new Map(models.map((model) => [model.id, `${model.name} / ${model.model}`])), [models]);
  const knowledgeNameById = useMemo(() => new Map(knowledgeBases.map((base) => [base.id, base.name])), [knowledgeBases]);
  const publishedWorkflows = workflows.filter((workflow) => workflow.status === 'PUBLISHED');
  const enabledCount = bots.filter((bot) => bot.status === 'ENABLED').length;
  const conversationCount = bots.reduce((sum, bot) => sum + bot.conversationCount, 0);
  const visibleBots = bots.filter((bot) => {
    const text = `${bot.name} ${bot.description ?? ''}`.toLowerCase();
    return !keyword.trim() || text.includes(keyword.trim().toLowerCase());
  });

  const saveMutation = useMutation({
    mutationFn: (values: SaveBotRequest) => {
      const request: SaveBotRequest = {
        ...initialBotValues,
        ...values,
        description: values.description || null,
        modelProviderId: values.modelProviderId || null,
        knowledgeBaseId: values.knowledgeBaseId || null
      };
      return editingBot ? updateBot(editingBot.id, request) : createBot(request);
    },
    onSuccess: async () => {
      message.success(editingBot ? '智能体已修改' : '智能体已保存');
      setDrawerOpen(false);
      setEditingBot(null);
      botForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['bots'] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (bot: Bot) => deleteBot(bot.id),
    onSuccess: async () => {
      message.success('智能体已删除');
      await queryClient.invalidateQueries({ queryKey: ['bots'] });
    }
  });

  const runMutation = useMutation({
    mutationFn: (values: { message: string; input: string }) => {
      if (!runningBot) {
        throw new Error('请选择智能体');
      }
      const parsed = parseJsonObject(values.input || '{}');
      if (!parsed.ok) {
        throw new Error(parsed.message);
      }
      return runBot(runningBot.id, { message: values.message, input: parsed.value });
    },
    onSuccess: async (result) => {
      setRunResult(result);
      setRunError(null);
      await queryClient.invalidateQueries({ queryKey: ['bots'] });
    },
    onError: (error) => {
      setRunError((error as Error).message);
    }
  });

  const columns: ColumnsType<Bot> = [
    {
      title: '智能体',
      dataIndex: 'name',
      render: (_, bot) => (
        <Space size={12}>
          <Avatar icon={<RobotOutlined />} style={{ background: '#e8f1ff', color: '#1677ff' }} />
          <Space direction="vertical" size={2}>
            <Typography.Text strong>{bot.name}</Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>{bot.description || '暂无描述'}</Typography.Text>
          </Space>
        </Space>
      )
    },
    {
      title: '绑定工作流',
      width: 180,
      render: (_, bot) => <Tag color="blue">{workflowNameById.get(bot.workflowId) ?? bot.workflowId}</Tag>
    },
    {
      title: '模型',
      width: 190,
      render: (_, bot) => <Tag color={bot.modelProviderId ? 'geekblue' : 'default'}>{bot.modelProviderId ? modelNameById.get(bot.modelProviderId) ?? bot.modelProviderId : '跟随工作流'}</Tag>
    },
    {
      title: '知识库',
      width: 160,
      render: (_, bot) => <Tag color={bot.knowledgeBaseId ? 'purple' : 'default'}>{bot.knowledgeBaseId ? knowledgeNameById.get(bot.knowledgeBaseId) ?? bot.knowledgeBaseId : '未绑定'}</Tag>
    },
    {
      title: '会话',
      width: 90,
      dataIndex: 'conversationCount'
    },
    {
      title: '状态',
      width: 100,
      render: (_, bot) => <Tag color={bot.status === 'ENABLED' ? 'green' : 'default'}>{bot.status === 'ENABLED' ? '启用' : '停用'}</Tag>
    },
    {
      title: '操作',
      width: 260,
      render: (_, bot) => (
        <Space size={6} wrap>
          <Button size="small" icon={<PlayCircleOutlined />} aria-label="运行智能体" onClick={() => openRunDrawer(bot)}>运行</Button>
          <Button size="small" icon={<EditOutlined />} aria-label="编辑智能体" onClick={() => openEditDrawer(bot)}>编辑</Button>
          <Button size="small" danger icon={<DeleteOutlined />} aria-label="删除智能体" onClick={() => deleteMutation.mutate(bot)}>删除</Button>
        </Space>
      )
    }
  ];

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>智能体 Bots</Typography.Title>
          <Typography.Text type="secondary">把已发布工作流包装成可配置、可测试、可投放的对话式智能体。</Typography.Text>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>新增智能体</Button>
      </div>

      <div style={metricRowStyle}>
        <Card variant="borderless"><Statistic title="智能体总数" value={bots.length} prefix={<RobotOutlined />} /></Card>
        <Card variant="borderless"><Statistic title="已启用" value={enabledCount} prefix={<CheckCircleOutlined />} /></Card>
        <Card variant="borderless"><Statistic title="累计会话" value={conversationCount} prefix={<MessageOutlined />} /></Card>
      </div>

      <Card variant="borderless" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Typography.Text strong>搜索：</Typography.Text>
          <Input allowClear prefix={<SearchOutlined />} placeholder="请输入智能体名称" style={{ width: 320 }} value={keyword} onChange={(event) => setKeyword(event.target.value)} />
          <Button icon={<SearchOutlined />} type="primary">搜索</Button>
          <Button onClick={() => setKeyword('')}>重置</Button>
        </Space>
      </Card>

      {botsQuery.isError ? <Alert type="error" showIcon message="智能体加载失败" description={(botsQuery.error as Error).message} style={{ marginBottom: 16 }} /> : null}

      <Card variant="borderless" title={<Space><RobotOutlined />智能体清单</Space>} extra={<Tag color="geekblue">工作流可投放</Tag>}>
        <Table rowKey="id" loading={botsQuery.isLoading} columns={columns} dataSource={visibleBots} pagination={{ pageSize: 8, showSizeChanger: false }} />
        {!botsQuery.isLoading && bots.length === 0 ? <Empty description="暂无智能体，先新增一个绑定已发布工作流" /> : null}
      </Card>

      <Drawer
        title={editingBot ? '编辑智能体' : '新增智能体'}
        open={drawerOpen}
        width={640}
        onClose={closeBotDrawer}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={closeBotDrawer}>取消</Button>
            <Button type="primary" icon={<CheckCircleOutlined />} loading={saveMutation.isPending} onClick={() => botForm.submit()}>
              {editingBot ? '修改' : '保存'}
            </Button>
          </Space>
        )}
      >
        <Form form={botForm} layout="vertical" initialValues={initialBotValues} onFinish={(values) => saveMutation.mutate(values)}>
          <Form.Item name="name" label="智能体名称" rules={[{ required: true, message: '请输入智能体名称' }]}>
            <Input placeholder="客服助手" />
          </Form.Item>
          <Form.Item name="description" label="智能体描述">
            <Input placeholder="适用场景、服务范围或内部说明" />
          </Form.Item>
          <Form.Item name="workflowId" label="绑定工作流" rules={[{ required: true, message: '请选择已发布工作流' }]}>
            <Select
              aria-label="绑定工作流"
              loading={workflowsQuery.isLoading}
              placeholder="选择已发布工作流"
              options={publishedWorkflows.map((workflow) => ({ value: workflow.id, label: workflow.name }))}
            />
          </Form.Item>
          <Form.Item name="modelProviderId" label="默认模型">
            <Select
              allowClear
              placeholder="不选择则跟随工作流节点配置"
              options={models.filter((model) => model.enabled && model.modelUsage === 'CHAT').map((model) => ({ value: model.id, label: `${model.name} / ${model.model}` }))}
            />
          </Form.Item>
          <Form.Item name="knowledgeBaseId" label="默认知识库">
            <Select
              allowClear
              placeholder="可绑定一个常用知识库"
              options={knowledgeBases.map((base) => ({ value: base.id, label: base.name }))}
            />
          </Form.Item>
          <Form.Item name="openingMessage" label="开场白">
            <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
          <Form.Item name="systemPrompt" label="系统提示词">
            <Input.TextArea autoSize={{ minRows: 4, maxRows: 8 }} placeholder="约束智能体语气、范围和回答策略" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: 'ENABLED', label: '启用' }, { value: 'DISABLED', label: '停用' }]} />
          </Form.Item>
          <Form.Item name="avatar" hidden>
            <Input />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={runningBot ? `运行智能体 - ${runningBot.name}` : '运行智能体'}
        open={Boolean(runningBot)}
        width={640}
        onClose={() => {
          setRunningBot(null);
          setRunResult(null);
          setRunError(null);
        }}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setRunningBot(null)}>关闭</Button>
            <Button type="primary" icon={<PlayCircleOutlined />} loading={runMutation.isPending} onClick={() => runForm.submit()}>发送测试</Button>
          </Space>
        )}
      >
        <Form form={runForm} layout="vertical" initialValues={{ message: '', input: '{}' }} onFinish={(values) => runMutation.mutate(values)}>
          <Form.Item name="message" label="测试消息" rules={[{ required: true, message: '请输入测试消息' }]}>
            <Input.TextArea autoSize={{ minRows: 3, maxRows: 6 }} placeholder="输入一条用户消息" />
          </Form.Item>
          <Form.Item name="input" label="附加变量 JSON">
            <Input.TextArea style={{ fontFamily: 'Consolas, monospace' }} autoSize={{ minRows: 5, maxRows: 10 }} />
          </Form.Item>
        </Form>
        {runError ? <Alert type="error" showIcon message={runError} style={{ marginBottom: 12 }} /> : null}
        {runResult ? (
          <Alert
            type="success"
            showIcon
            message={`执行状态：${runResult.execution.status}`}
            description={<pre style={resultStyle}>{JSON.stringify(runResult.execution.output, null, 2)}</pre>}
          />
        ) : null}
      </Drawer>
    </section>
  );

  function openCreateDrawer() {
    setEditingBot(null);
    botForm.setFieldsValue(initialBotValues);
    setDrawerOpen(true);
  }

  function openEditDrawer(bot: Bot) {
    setEditingBot(bot);
    botForm.setFieldsValue({
      name: bot.name,
      description: bot.description || null,
      avatar: bot.avatar || 'robot',
      workflowId: bot.workflowId,
      modelProviderId: bot.modelProviderId || null,
      knowledgeBaseId: bot.knowledgeBaseId || null,
      systemPrompt: bot.systemPrompt || '',
      openingMessage: bot.openingMessage || '',
      status: bot.status
    });
    setDrawerOpen(true);
  }

  function closeBotDrawer() {
    setDrawerOpen(false);
    setEditingBot(null);
  }

  function openRunDrawer(bot: Bot) {
    setRunningBot(bot);
    setRunResult(null);
    setRunError(null);
    runForm.setFieldsValue({ message: '', input: '{}' });
  }
}

function parseJsonObject(value: string): { ok: true; value: Record<string, unknown> } | { ok: false; message: string } {
  try {
    const parsed = JSON.parse(value || '{}') as unknown;
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return { ok: false, message: '附加变量必须是 JSON 对象' };
    }
    return { ok: true, value: parsed as Record<string, unknown> };
  } catch {
    return { ok: false, message: '附加变量不是合法 JSON' };
  }
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
  gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
  marginBottom: 16
};

const resultStyle: React.CSSProperties = {
  background: '#0f172a',
  borderRadius: 6,
  color: '#dbeafe',
  margin: '8px 0 0',
  padding: 12,
  whiteSpace: 'pre-wrap'
};
