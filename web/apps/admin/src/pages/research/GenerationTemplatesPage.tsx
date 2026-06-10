import {
  ApartmentOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  FileTextOutlined,
  LinkOutlined,
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
  Select,
  Space,
  Statistic,
  Steps,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  createGenerationTemplate,
  deleteGenerationTemplate,
  listGenerationTemplates,
  parseTemplateSchema,
  parseWorkflowSnapshot,
  updateGenerationTemplate,
  type GenerationTemplate,
  type GenerationTemplateSchema,
  type SaveGenerationTemplateRequest,
  type WorkflowSnapshotNode
} from '../../api/generationTemplates';
import { getWorkflow, listWorkflows } from '../../api/workflows';
import { resolveWorkflowDisplay } from './workflowDisplay';

const defaultTemplateSchema: GenerationTemplateSchema = {
  title: '专题编研成果模板',
  variables: [
    { name: 'topic', label: '主题', type: 'string', required: true },
    { name: 'audience', label: '面向对象', type: 'string', required: false }
  ],
  sections: [
    {
      key: 'overview',
      title: '一、背景概述',
      instruction: '根据资料概括主题背景，不超过800字。',
      requiredSources: ['INTERNAL_KNOWLEDGE_BASE', 'EXTERNAL_CORPUS'],
      citationRequired: true
    },
    {
      key: 'timeline',
      title: '二、发展脉络',
      instruction: '按时间顺序梳理关键事件。',
      outputFormat: 'timeline',
      citationRequired: true
    },
    {
      key: 'conclusion',
      title: '三、总结建议',
      instruction: '结合资料形成总结，不得编造事实。',
      citationRequired: false
    }
  ]
};

const initialValues = {
  templateName: defaultTemplateSchema.title ?? '',
  code: '',
  description: null,
  category: 'RESEARCH',
  outputType: 'MARKDOWN',
  templateSchema: defaultTemplateSchema,
  workflowId: null,
  status: 'ENABLED',
  templateSchemaText: JSON.stringify(defaultTemplateSchema, null, 2)
};

type GenerationTemplateFormValues = SaveGenerationTemplateRequest & {
  templateName: string;
  templateSchemaText: string;
};

function resolveTemplateName(template: GenerationTemplate): string {
  const schema = parseTemplateSchema(template.templateSchema);
  return template.name?.trim() || schema.title?.trim() || '';
}

export function GenerationTemplatesPage() {
  const [form] = Form.useForm<GenerationTemplateFormValues>();
  const queryClient = useQueryClient();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<GenerationTemplate | null>(null);
  const templateSchemaText = Form.useWatch('templateSchemaText', form) ?? '';
  const workflowId = Form.useWatch('workflowId', form) ?? null;
  const parsedSchema = useMemo(() => parseJsonField<GenerationTemplateSchema>(templateSchemaText, defaultTemplateSchema), [templateSchemaText]);

  const templatesQuery = useQuery({
    queryKey: ['generation-templates'],
    queryFn: listGenerationTemplates
  });
  const workflowsQuery = useQuery({
    queryKey: ['workflows'],
    queryFn: listWorkflows
  });
  const selectedWorkflowQuery = useQuery({
    queryKey: ['workflow', workflowId],
    queryFn: () => getWorkflow(workflowId!),
    enabled: Boolean(workflowId)
  });

  const templates = templatesQuery.data?.items ?? [];
  const workflows = workflowsQuery.data?.items ?? [];
  const workflowNameById = useMemo(
    () => new Map(workflows.map((workflow) => [workflow.id, workflow.name])),
    [workflows]
  );
  const parsedWorkflow = useMemo(
    () => resolveWorkflowDisplay(
      workflowId,
      selectedWorkflowQuery.data?.name ?? null,
      selectedWorkflowQuery.data?.latestVersion?.definition ?? null,
      null,
      parseWorkflowSnapshot
    ),
    [workflowId, selectedWorkflowQuery.data]
  );
  const sectionCount = templates.reduce(
    (count, template) => count + (parseTemplateSchema(template.templateSchema).sections?.length ?? 0),
    0
  );

  const saveMutation = useMutation({
    mutationFn: (values: GenerationTemplateFormValues) => {
      const request = normalizeValues(values);
      if (editingTemplate) {
        return updateGenerationTemplate(editingTemplate.id, request);
      }
      return createGenerationTemplate(request);
    },
    onSuccess: async () => {
      message.success(editingTemplate ? '编研模板已修改' : '编研模板已保存');
      setDrawerOpen(false);
      setEditingTemplate(null);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['generation-templates'] });
    },
    onError: (error: Error) => {
      message.error(error.message || '保存失败');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (template: GenerationTemplate) => deleteGenerationTemplate(template.id),
    onSuccess: async () => {
      message.success('编研模板已删除');
      await queryClient.invalidateQueries({ queryKey: ['generation-templates'] });
    }
  });

  function openCreateDrawer() {
    setEditingTemplate(null);
    form.setFieldsValue(initialValues);
    setDrawerOpen(true);
  }

  function openEditDrawer(template: GenerationTemplate) {
    setEditingTemplate(template);
    const schema = parseTemplateSchema(template.templateSchema);
    form.setFieldsValue({
      templateName: resolveTemplateName(template),
      code: template.code,
      description: template.description,
      category: template.category,
      outputType: template.outputType,
      workflowId: template.workflowId ?? null,
      status: template.status,
      templateSchemaText: JSON.stringify(schema, null, 2)
    });
    setDrawerOpen(true);
  }

  const columns: ColumnsType<GenerationTemplate> = [
    {
      title: '模板名称',
      dataIndex: 'name',
      render: (_, template) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{resolveTemplateName(template) || '未命名模板'}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{template.description || '暂无描述'}</Typography.Text>
        </Space>
      )
    },
    {
      title: '编码',
      dataIndex: 'code',
      width: 160,
      render: (value: string) => <Typography.Text code>{value}</Typography.Text>
    },
    {
      title: '章节',
      width: 100,
      render: (_, template) => {
        const sections = parseTemplateSchema(template.templateSchema).sections ?? [];
        return <Tag color="blue">{sections.length} 节</Tag>;
      }
    },
    {
      title: '绑定工作流',
      width: 180,
      render: (_, template) => (
        template.workflowId
          ? <Tag color="geekblue">{workflowNameById.get(template.workflowId) ?? template.workflowId}</Tag>
          : <Tag>未绑定</Tag>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value: string) => <Tag color={value === 'ENABLED' ? 'success' : 'default'}>{value === 'ENABLED' ? '启用' : value}</Tag>
    },
    {
      title: '操作',
      width: 168,
      render: (_, template) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEditDrawer(template)}>编辑</Button>
          <Button
            danger
            size="small"
            icon={<DeleteOutlined />}
            loading={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate(template)}
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
          <Typography.Title level={3} style={{ margin: 0 }}>编研模板</Typography.Title>
          <Typography.Text type="secondary">维护章节结构与变量定义，编研工作流在工作流模块设计后在此绑定。</Typography.Text>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>新增模板</Button>
      </div>

      <div style={metricRowStyle}>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="模板总数" value={templates.length} prefix={<FileTextOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="章节总数" value={sectionCount} prefix={<ApartmentOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="输出格式" value="Markdown" prefix={<CheckCircleOutlined />} valueStyle={{ fontSize: 18 }} />
        </Card>
      </div>

      {(templatesQuery.isError || workflowsQuery.isError) ? (
        <Alert
          type="error"
          showIcon
          message="数据加载失败"
          description={String((templatesQuery.error as Error)?.message || (workflowsQuery.error as Error)?.message)}
          style={{ marginBottom: 12 }}
        />
      ) : null}

      <Card variant="borderless" title={<Space><FileTextOutlined />模板清单</Space>} extra={<Tag color="geekblue">智能编研可引用</Tag>}>
        <Table
          rowKey="id"
          loading={templatesQuery.isLoading}
          columns={columns}
          dataSource={templates}
          pagination={{ pageSize: 8, showSizeChanger: false }}
          size="middle"
        />
      </Card>

      <Drawer
        title={editingTemplate ? '编辑编研模板' : '新增编研模板'}
        open={drawerOpen}
        width={720}
        destroyOnClose
        onClose={() => {
          setDrawerOpen(false);
          setEditingTemplate(null);
          form.resetFields();
        }}
        styles={{ body: { paddingBottom: 0 }, footer: { padding: '10px 16px' } }}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button type="primary" icon={<CheckCircleOutlined />} loading={saveMutation.isPending} onClick={() => form.submit()}>
              {editingTemplate ? '修改' : '保存'}
            </Button>
          </Space>
        )}
      >
        <Form
          key={editingTemplate?.id ?? 'create'}
          form={form}
          layout="vertical"
          initialValues={initialValues}
          onFinish={(values) => saveMutation.mutate(values)}
        >
          <Form.Item name="templateName" label="模板名称" rules={[{ required: true, message: '请输入模板名称' }]}>
            <Input placeholder="专题编研成果模板" />
          </Form.Item>
          <Form.Item name="code" label="模板编码" rules={[{ required: true, message: '请输入模板编码' }]}>
            <Input placeholder="research_topic_v1" />
          </Form.Item>
          <Form.Item name="description" label="模板描述">
            <Input placeholder="请输入模板用途说明" />
          </Form.Item>
          <Space style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, width: '100%' }}>
            <Form.Item name="category" label="分类" rules={[{ required: true, message: '请选择分类' }]}>
              <Select options={[
                { value: 'RESEARCH', label: '编研' },
                { value: 'REPORT', label: '报告' },
                { value: 'GENERAL', label: '通用' }
              ]} />
            </Form.Item>
            <Form.Item name="outputType" label="输出类型" rules={[{ required: true, message: '请选择输出类型' }]}>
              <Select options={[
                { value: 'MARKDOWN', label: 'Markdown' },
                { value: 'DOCX', label: 'Word (DOCX)' },
                { value: 'HTML', label: 'HTML' },
                { value: 'JSON', label: 'JSON' }
              ]} />
            </Form.Item>
          </Space>
          <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
            <Select options={[
              { value: 'ENABLED', label: '启用' },
              { value: 'DISABLED', label: '停用' }
            ]} />
          </Form.Item>
          <Form.Item name="workflowId" label="绑定编研工作流" rules={[{ required: true, message: '请选择工作流' }]}>
            <Select
              placeholder="选择已在工作流模块设计好的编研流程"
              options={workflows.map((workflow) => ({
                value: workflow.id,
                label: `${workflow.name} (${workflow.status})`
              }))}
            />
          </Form.Item>
          {workflowId ? (
            <Button
              type="link"
              icon={<LinkOutlined />}
              href={`/workflows/${workflowId}`}
              target="_blank"
              style={{ paddingLeft: 0, marginBottom: 12 }}
            >
              打开工作流设计器
            </Button>
          ) : null}
          <Form.Item name="templateSchemaText" label="模板结构 JSON" rules={[{ required: true, message: '请输入模板结构' }]}>
            <Input.TextArea autoSize={{ minRows: 8, maxRows: 14 }} />
          </Form.Item>
          <Card size="small" title="章节结构预览" style={{ marginBottom: 12 }}>
            <Table
              size="small"
              rowKey="key"
              pagination={false}
              dataSource={parsedSchema.sections ?? []}
              columns={[
                { title: '章节键', dataIndex: 'key', width: 120 },
                { title: '标题', dataIndex: 'title' },
                { title: '生成说明', dataIndex: 'instruction', ellipsis: true }
              ]}
            />
          </Card>
          <Card size="small" title="工作流编排预览" loading={selectedWorkflowQuery.isLoading}>
            {(parsedWorkflow.nodes ?? []).length > 0 ? (
              <Steps
                size="small"
                direction="vertical"
                current={(parsedWorkflow.nodes ?? []).length}
                items={(parsedWorkflow.nodes ?? []).map((node: WorkflowSnapshotNode) => ({
                  title: node.name,
                  description: node.type ? `${node.type}${node.order ? ` · 步骤 ${node.order}` : ''}` : undefined
                }))}
              />
            ) : (
              <Typography.Text type="secondary">选择工作流后显示节点编排。</Typography.Text>
            )}
          </Card>
        </Form>
      </Drawer>
    </section>
  );
}

function normalizeValues(values: GenerationTemplateFormValues): SaveGenerationTemplateRequest {
  const schema = parseJsonField<GenerationTemplateSchema>(values.templateSchemaText, defaultTemplateSchema);
  const templateName = values.templateName?.trim() || schema.title?.trim() || '';
  return {
    name: templateName,
    code: values.code,
    description: values.description || null,
    category: values.category,
    outputType: values.outputType,
    workflowId: values.workflowId ?? null,
    status: values.status,
    templateSchema: values.templateSchemaText
  };
}

function parseJsonField<T>(value: string, fallback: T): T {
  if (!value.trim()) {
    return fallback;
  }
  try {
    return JSON.parse(value) as T;
  } catch {
    return fallback;
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

const metricCardStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3'
};
