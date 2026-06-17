import {
  ApartmentOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  LinkOutlined,
  PlusOutlined,
  UploadOutlined
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
  Table,
  Tag,
  Typography,
  Upload,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  createGenerationTemplate,
  deleteGenerationTemplate,
  getGenerationTemplateRuntime,
  listGenerationTemplates,
  parseTemplateSchema,
  parseTemplateSchemaText,
  parseWorkflowSnapshot,
  runtimeViewToSchema,
  updateGenerationTemplate,
  uploadGenerationTemplateDocxMaster,
  type GenerationTemplate,
  type GenerationTemplateSchema,
  type SaveGenerationTemplateRequest
} from '../../api/generationTemplates';
import { getWorkflow, listWorkflows } from '../../api/workflows';
import { GenerationTemplatePreviewPanel } from './GenerationTemplatePreviewPanel';
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

const defaultDocxConfig = JSON.stringify({
  showCover: true,
  showToc: true,
  tocDepth: 2,
  showPageNumber: true,
  showReferenceSection: true,
  lineSpacingPt: 28
}, null, 2);

const defaultLayoutConfig = JSON.stringify({
  defaultTab: 'docx',
  enableHtmlPreview: true
}, null, 2);

const defaultTopicCollectionDocxConfig = JSON.stringify({
  masterFile: '',
  templateType: 'archive_topic_collection'
}, null, 2);

const initialValues = {
  templateName: defaultTemplateSchema.title ?? '',
  code: '',
  description: null,
  category: 'RESEARCH',
  outputType: 'DOCX',
  templateCategory: 'report',
  docxConfigText: defaultDocxConfig,
  layoutConfigText: defaultLayoutConfig,
  linkedHtmlTemplateId: null,
  templateSchema: defaultTemplateSchema,
  workflowId: null,
  status: 'ENABLED',
  templateSchemaText: JSON.stringify(defaultTemplateSchema, null, 2)
};

type GenerationTemplateFormValues = SaveGenerationTemplateRequest & {
  templateName: string;
  templateSchemaText: string;
  docxConfigText: string;
  layoutConfigText: string;
};

function resolveTemplateName(template: GenerationTemplate): string {
  const schema = parseTemplateSchema(template.templateSchema);
  return template.name?.trim() || schema.title?.trim() || '';
}

export function GenerationTemplatesPage() {
  const [form] = Form.useForm<GenerationTemplateFormValues>();
  const queryClient = useQueryClient();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewTemplate, setPreviewTemplate] = useState<GenerationTemplate | null>(null);
  const [editingTemplate, setEditingTemplate] = useState<GenerationTemplate | null>(null);
  const templateSchemaText = Form.useWatch('templateSchemaText', form) ?? '';
  const outputType = Form.useWatch('outputType', form) ?? 'MARKDOWN';
  const templateName = Form.useWatch('templateName', form) ?? '';
  const workflowId = Form.useWatch('workflowId', form) ?? null;
  const templateCategory = Form.useWatch('templateCategory', form) ?? 'report';
  const docxConfigText = Form.useWatch('docxConfigText', form) ?? '';
  const masterFileLabel = useMemo(() => parseMasterFileLabel(docxConfigText), [docxConfigText]);
  const parsedSchemaResult = useMemo(() => parseTemplateSchemaText(templateSchemaText), [templateSchemaText]);
  const parsedSchema = parsedSchemaResult.schema ?? defaultTemplateSchema;

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
  const previewRuntimeQuery = useQuery({
    queryKey: ['generation-template-runtime', previewTemplate?.id],
    queryFn: () => getGenerationTemplateRuntime(previewTemplate!.id),
    enabled: Boolean(previewTemplate?.id && previewOpen)
  });
  const previewWorkflowQuery = useQuery({
    queryKey: ['workflow', previewTemplate?.workflowId],
    queryFn: () => getWorkflow(previewTemplate!.workflowId!),
    enabled: Boolean(previewTemplate?.workflowId && previewOpen)
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
  const previewWorkflow = useMemo(
    () => resolveWorkflowDisplay(
      previewTemplate?.workflowId,
      previewWorkflowQuery.data?.name ?? null,
      previewWorkflowQuery.data?.latestVersion?.definition ?? null,
      previewTemplate?.workflowSnapshot,
      parseWorkflowSnapshot
    ),
    [previewTemplate, previewWorkflowQuery.data]
  );
  const previewSchema = useMemo(() => {
    if (previewRuntimeQuery.data) {
      return runtimeViewToSchema(previewRuntimeQuery.data);
    }
    if (previewTemplate) {
      return parseTemplateSchema(previewTemplate.templateSchema);
    }
    return { sections: [], variables: [] };
  }, [previewRuntimeQuery.data, previewTemplate]);
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

  const uploadDocxMasterMutation = useMutation({
    mutationFn: (file: File) => uploadGenerationTemplateDocxMaster(editingTemplate!.id, file),
    onSuccess: (result) => {
      form.setFieldValue('docxConfigText', JSON.stringify(JSON.parse(result.docxConfig), null, 2));
      message.success(`DOCX 母版已上传：${result.fileName}`);
      void queryClient.invalidateQueries({ queryKey: ['generation-templates'] });
    },
    onError: (error: Error) => message.error(error.message || 'DOCX 母版上传失败')
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
      templateCategory: template.templateCategory ?? 'report',
      docxConfigText: stringifyConfig(template.docxConfig, defaultDocxConfig),
      layoutConfigText: stringifyConfig(template.layoutConfig, defaultLayoutConfig),
      linkedHtmlTemplateId: template.linkedHtmlTemplateId ?? null,
      workflowId: template.workflowId ?? null,
      status: template.status,
      templateSchemaText: JSON.stringify(schema, null, 2)
    });
    setDrawerOpen(true);
  }

  function openPreviewDrawer(template: GenerationTemplate) {
    setPreviewTemplate(template);
    setPreviewOpen(true);
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
      title: '版式',
      width: 110,
      render: (_, template) => (
        <Tag color="purple">{formatTemplateCategory(template.templateCategory)}</Tag>
      )
    },
    {
      title: '输出',
      width: 90,
      dataIndex: 'outputType',
      render: (value: string) => <Tag color={value === 'DOCX' ? 'blue' : 'default'}>{value}</Tag>
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
      width: 220,
      render: (_, template) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />} onClick={() => openPreviewDrawer(template)}>预览</Button>
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
          <Statistic title="默认输出" value="DOCX" prefix={<CheckCircleOutlined />} valueStyle={{ fontSize: 18 }} />
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
        title={previewTemplate ? `模板预览 · ${resolveTemplateName(previewTemplate)}` : '模板预览'}
        open={previewOpen}
        width={760}
        destroyOnClose
        onClose={() => {
          setPreviewOpen(false);
          setPreviewTemplate(null);
        }}
      >
        {previewRuntimeQuery.isError ? (
          <Alert
            type="error"
            showIcon
            message="运行时预览加载失败"
            description={String((previewRuntimeQuery.error as Error)?.message)}
            style={{ marginBottom: 12 }}
          />
        ) : null}
        {previewRuntimeQuery.isLoading ? (
          <Typography.Text type="secondary">加载运行时预览...</Typography.Text>
        ) : null}
        <GenerationTemplatePreviewPanel
          schema={previewSchema}
          outputType={previewRuntimeQuery.data?.outputType ?? previewTemplate?.outputType}
          templateName={previewRuntimeQuery.data?.name ?? (previewTemplate ? resolveTemplateName(previewTemplate) : undefined)}
          workflowNodes={previewWorkflow.nodes ?? []}
          workflowLoading={previewWorkflowQuery.isLoading}
          workflowName={previewWorkflow.name ?? previewWorkflowQuery.data?.name ?? null}
        />
      </Drawer>

      <Drawer
        title={editingTemplate ? '编辑编研模板' : '新增编研模板'}
        open={drawerOpen}
        width={760}
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
            <Button
              type="primary"
              icon={<CheckCircleOutlined />}
              loading={saveMutation.isPending}
              disabled={Boolean(parsedSchemaResult.error)}
              onClick={() => form.submit()}
            >
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
                { value: 'DOCX', label: 'Word (DOCX)' },
                { value: 'MARKDOWN', label: 'Markdown' },
                { value: 'HTML', label: 'HTML' }
              ]} />
            </Form.Item>
            <Form.Item name="templateCategory" label="成果版式" rules={[{ required: true, message: '请选择成果版式' }]}>
              <Select
                options={[
                  { value: 'report', label: '普通报告' },
                  { value: 'gallery', label: '图文展陈' },
                  { value: 'timeline', label: '时间轴专题' },
                  { value: 'topic_collection', label: '专题汇编 (DOCX母版)' }
                ]}
                onChange={(value) => {
                  if (value === 'topic_collection') {
                    const current = form.getFieldValue('docxConfigText');
                    if (!current?.trim() || current.includes('"showCover"')) {
                      form.setFieldValue('docxConfigText', defaultTopicCollectionDocxConfig);
                    }
                  }
                }}
              />
            </Form.Item>
          </Space>
          {templateCategory === 'topic_collection' ? (
            <Form.Item label="DOCX 母版文件">
              {editingTemplate ? (
                <Upload
                  accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  maxCount={1}
                  showUploadList={false}
                  beforeUpload={(file) => {
                    uploadDocxMasterMutation.mutate(file);
                    return false;
                  }}
                >
                  <Button icon={<UploadOutlined />} loading={uploadDocxMasterMutation.isPending}>
                    上传 DOCX 母版
                  </Button>
                </Upload>
              ) : (
                <Typography.Text type="secondary">请先保存模板，再上传 DOCX 母版。</Typography.Text>
              )}
              <Typography.Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                当前母版：{masterFileLabel}
              </Typography.Text>
            </Form.Item>
          ) : null}
          <Form.Item
            name="docxConfigText"
            label={templateCategory === 'topic_collection' ? 'DOCX 版式配置 JSON（上传后自动写入 masterFile）' : 'DOCX 版式配置 JSON'}
          >
            <Input.TextArea autoSize={{ minRows: 4, maxRows: 8 }} />
          </Form.Item>
          <Form.Item name="layoutConfigText" label="前端展示配置 JSON">
            <Input.TextArea autoSize={{ minRows: 3, maxRows: 6 }} />
          </Form.Item>
          <Form.Item name="linkedHtmlTemplateId" label="关联 HTML 预览模板 ID">
            <Input placeholder="layout_report_html" />
          </Form.Item>
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
          {parsedSchemaResult.error ? (
            <Alert type="error" showIcon message="模板 JSON 无效" description={parsedSchemaResult.error} style={{ marginBottom: 12 }} />
          ) : null}
          <GenerationTemplatePreviewPanel
            schema={parsedSchema}
            outputType={outputType}
            templateName={templateName}
            workflowNodes={parsedWorkflow.nodes ?? []}
            workflowLoading={selectedWorkflowQuery.isLoading}
            workflowName={parsedWorkflow.name ?? selectedWorkflowQuery.data?.name ?? null}
          />
        </Form>
      </Drawer>
    </section>
  );
}

function normalizeValues(values: GenerationTemplateFormValues): SaveGenerationTemplateRequest {
  const parsed = parseTemplateSchemaText(values.templateSchemaText);
  if (!parsed.schema) {
    throw new Error(parsed.error ?? '模板结构无效');
  }
  const schema = parsed.schema;
  const templateName = values.templateName?.trim() || schema.title?.trim() || '';
  return {
    name: templateName,
    code: values.code,
    description: values.description || null,
    category: values.category,
    outputType: values.outputType,
    templateCategory: values.templateCategory ?? 'report',
    docxConfig: values.docxConfigText?.trim() || null,
    layoutConfig: values.layoutConfigText?.trim() || null,
    linkedHtmlTemplateId: values.linkedHtmlTemplateId || null,
    workflowId: values.workflowId ?? null,
    status: values.status,
    templateSchema: values.templateSchemaText
  };
}

function stringifyConfig(
  value: string | Record<string, unknown> | null | undefined,
  fallback: string
): string {
  if (!value) return fallback;
  if (typeof value === 'string') return value;
  return JSON.stringify(value, null, 2);
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

function formatTemplateCategory(category?: string | null): string {
  if (category === 'gallery') return '图文展陈';
  if (category === 'timeline') return '时间轴专题';
  if (category === 'topic_collection' || category === 'archive_topic_collection') return '专题汇编';
  return category ?? '普通报告';
}

function parseMasterFileLabel(docxConfigText?: string | null): string {
  if (!docxConfigText?.trim()) {
    return '未上传';
  }
  try {
    const config = JSON.parse(docxConfigText) as { masterFile?: string };
    if (!config.masterFile?.trim()) {
      return '未上传';
    }
    const parts = config.masterFile.replace(/\\/g, '/').split('/');
    return parts[parts.length - 1] || config.masterFile;
  } catch {
    return '未上传';
  }
}
