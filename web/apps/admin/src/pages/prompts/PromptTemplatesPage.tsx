import {
  CheckCircleOutlined,
  CodeOutlined,
  EditOutlined,
  FileTextOutlined,
  PlusOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Drawer,
  Form,
  Input,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  createPromptTemplate,
  listPromptTemplates,
  updatePromptTemplate,
  type PromptTemplate,
  type SavePromptTemplateRequest
} from '../../api/prompts';

const initialValues: SavePromptTemplateRequest = {
  name: '',
  template: '请根据以下输入生成回复：\n{{input}}',
  description: null
};

export function PromptTemplatesPage() {
  const [form] = Form.useForm<SavePromptTemplateRequest>();
  const queryClient = useQueryClient();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingPrompt, setEditingPrompt] = useState<PromptTemplate | null>(null);
  const watchedTemplate = Form.useWatch('template', form) ?? '';
  const watchedVariables = useMemo(() => extractVariables(watchedTemplate), [watchedTemplate]);

  const promptsQuery = useQuery({
    queryKey: ['prompt-templates'],
    queryFn: listPromptTemplates
  });
  const prompts = promptsQuery.data?.items ?? [];
  const variableCount = new Set(prompts.flatMap((prompt) => extractVariables(prompt.template))).size;

  const saveMutation = useMutation({
    mutationFn: (values: SavePromptTemplateRequest) => {
      const request = normalizeValues(values);
      if (editingPrompt) {
        return updatePromptTemplate(editingPrompt.id, request);
      }
      return createPromptTemplate(request);
    },
    onSuccess: async () => {
      message.success(editingPrompt ? 'Prompt 模板已修改' : 'Prompt 模板已保存');
      setDrawerOpen(false);
      setEditingPrompt(null);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['prompt-templates'] });
    }
  });

  function openCreateDrawer() {
    setEditingPrompt(null);
    form.setFieldsValue(initialValues);
    setDrawerOpen(true);
  }

  function openEditDrawer(prompt: PromptTemplate) {
    setEditingPrompt(prompt);
    form.setFieldsValue({
      name: prompt.name,
      template: prompt.template,
      description: prompt.description
    });
    setDrawerOpen(true);
  }

  const columns: ColumnsType<PromptTemplate> = [
    {
      title: '模板名称',
      dataIndex: 'name',
      render: (_, prompt) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{prompt.name}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{prompt.description || '暂无描述'}</Typography.Text>
        </Space>
      )
    },
    {
      title: '变量',
      width: 220,
      render: (_, prompt) => {
        const variables = extractVariables(prompt.template);
        return variables.length === 0 ? (
          <Typography.Text type="secondary">无变量</Typography.Text>
        ) : (
          <Space size={6} wrap>
            {variables.slice(0, 4).map((variable) => <Tag key={variable} color="blue">{variable}</Tag>)}
            {variables.length > 4 ? <Tag>+{variables.length - 4}</Tag> : null}
          </Space>
        );
      }
    },
    {
      title: '模板预览',
      dataIndex: 'template',
      ellipsis: true,
      render: (value: string) => <Typography.Text code>{value.split('\n')[0]}</Typography.Text>
    },
    {
      title: '操作',
      width: 96,
      render: (_, prompt) => (
        <Button size="small" icon={<EditOutlined />} onClick={() => openEditDrawer(prompt)}>
          编辑
        </Button>
      )
    }
  ];

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>Prompt 模板</Typography.Title>
          <Typography.Text type="secondary">集中管理可被工作流 PROMPT 节点引用的提示词模板和变量。</Typography.Text>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>新增 Prompt</Button>
      </div>

      <div style={metricRowStyle}>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="模板总数" value={prompts.length} prefix={<FileTextOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="变量总数" value={variableCount} prefix={<CodeOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="工作流调用方式" value="PROMPT 节点选择" prefix={<CheckCircleOutlined />} valueStyle={{ fontSize: 18 }} />
        </Card>
      </div>

      {promptsQuery.isError ? (
        <Alert
          type="error"
          showIcon
          message="Prompt 模板加载失败"
          description={(promptsQuery.error as Error).message}
          style={{ marginBottom: 12 }}
        />
      ) : null}

      <Card
        variant="borderless"
        title={<Space><FileTextOutlined />Prompt 清单</Space>}
        extra={<Tag color="geekblue">工作流可引用</Tag>}
      >
        <Table
          rowKey="id"
          loading={promptsQuery.isLoading}
          columns={columns}
          dataSource={prompts}
          pagination={{ pageSize: 8, showSizeChanger: false }}
          size="middle"
        />
      </Card>

      <Drawer
        title={editingPrompt ? '编辑 Prompt' : '新增 Prompt'}
        open={drawerOpen}
        width={560}
        onClose={() => setDrawerOpen(false)}
        styles={{ body: { paddingBottom: 0 }, footer: { padding: '10px 16px' } }}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button type="primary" icon={<CheckCircleOutlined />} loading={saveMutation.isPending} onClick={() => form.submit()}>
              {editingPrompt ? '修改' : '保存'}
            </Button>
          </Space>
        )}
      >
        <Form form={form} layout="vertical" initialValues={initialValues} onFinish={(values) => saveMutation.mutate(values)}>
          <Form.Item name="name" label="模板名称" rules={[{ required: true, message: '请输入模板名称' }]}>
            <Input placeholder="客服意图识别 Prompt" />
          </Form.Item>
          <Form.Item name="description" label="模板描述">
            <Input placeholder="请输入模板用途说明" />
          </Form.Item>
          <Form.Item name="template" label="Prompt 内容" rules={[{ required: true, message: '请输入 Prompt 内容' }]}>
            <Input.TextArea autoSize={{ minRows: 12, maxRows: 18 }} placeholder="使用 {{变量名}} 引用上下文变量" />
          </Form.Item>
          <Form.Item label="识别变量">
            {watchedVariables.length === 0 ? (
              <Typography.Text type="secondary">当前模板未识别到变量</Typography.Text>
            ) : (
              <Space size={6} wrap>
                {watchedVariables.map((variable) => <Tag key={variable} color="blue">{variable}</Tag>)}
              </Space>
            )}
          </Form.Item>
        </Form>
      </Drawer>
    </section>
  );
}

export function extractVariables(template: string): string[] {
  return Array.from(new Set(Array.from(template.matchAll(/\{\{\s*([A-Za-z0-9_.-]+)\s*}}/g)).map((match) => match[1])));
}

function normalizeValues(values: SavePromptTemplateRequest): SavePromptTemplateRequest {
  return {
    ...values,
    description: values.description || null
  };
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

const metricCardStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3'
};
