import {
  BookOutlined,
  CheckCircleOutlined,
  DatabaseOutlined,
  DownloadOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  RocketOutlined
} from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Form,
  Input,
  Radio,
  Space,
  Steps,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  listGenerationTemplates,
  parseTemplateSchema,
  parseWorkflowSnapshot,
  type GenerationTemplate,
  type WorkflowSnapshotNode
} from '../../api/generationTemplates';
import { listKnowledgeBases } from '../../api/knowledge';
import { getWorkflow } from '../../api/workflows';
import {
  createResearchJob,
  getResearchJobOutput,
  getResearchOutputDocxUrl,
  listThemeLibraries,
  type GenerationOutput,
  type ResearchJob
} from '../../api/research';
import { resolveWorkflowDisplay } from './workflowDisplay';

interface WizardFormValues {
  topic: string;
  audience: string;
  themeLibraryId: string;
  knowledgeBaseIds: string[];
  templateId: string;
}

export function ResearchCompilationPage() {
  const [form] = Form.useForm<WizardFormValues>();
  const [currentStep, setCurrentStep] = useState(0);
  const [jobResult, setJobResult] = useState<{ job: ResearchJob; output: GenerationOutput } | null>(null);
  const themeLibraryId = Form.useWatch('themeLibraryId', form);
  const knowledgeBaseIds = Form.useWatch('knowledgeBaseIds', form) ?? [];
  const templateId = Form.useWatch('templateId', form);

  const themeLibrariesQuery = useQuery({
    queryKey: ['research-theme-libraries'],
    queryFn: listThemeLibraries
  });
  const knowledgeBasesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const templatesQuery = useQuery({
    queryKey: ['generation-templates'],
    queryFn: listGenerationTemplates
  });

  const themeLibraries = themeLibrariesQuery.data?.items ?? [];
  const knowledgeBases = knowledgeBasesQuery.data?.items ?? [];
  const templates = templatesQuery.data?.items ?? [];
  const selectedTemplate = useMemo(
    () => templates.find((template) => template.id === templateId) ?? null,
    [templateId, templates]
  );
  const boundWorkflowQuery = useQuery({
    queryKey: ['workflow', selectedTemplate?.workflowId],
    queryFn: () => getWorkflow(selectedTemplate!.workflowId!),
    enabled: Boolean(selectedTemplate?.workflowId)
  });
  const selectedWorkflow = useMemo(
    () => resolveWorkflowDisplay(
      selectedTemplate?.workflowId,
      boundWorkflowQuery.data?.name ?? null,
      boundWorkflowQuery.data?.latestVersion?.definition ?? null,
      selectedTemplate?.workflowSnapshot,
      parseWorkflowSnapshot
    ),
    [selectedTemplate, boundWorkflowQuery.data]
  );
  const selectedSchema = useMemo(
    () => parseTemplateSchema(selectedTemplate?.templateSchema),
    [selectedTemplate]
  );

  const submitMutation = useMutation({
    mutationFn: async (values: WizardFormValues) => {
      const job = await createResearchJob({
        templateId: values.templateId,
        themeLibraryId: values.themeLibraryId,
        knowledgeBaseIds: values.knowledgeBaseIds,
        variables: {
          topic: values.topic,
          audience: values.audience
        }
      });
      const output = await getResearchJobOutput(job.id);
      return { job, output };
    },
    onSuccess: (result) => {
      setJobResult(result);
      setCurrentStep(4);
      message.success('编研任务已完成');
    },
    onError: (error: Error) => {
      message.error(error.message || '编研任务提交失败');
    }
  });

  function nextStep() {
    const fields = stepFields[currentStep];
    form.validateFields(fields).then(() => setCurrentStep((step) => Math.min(step + 1, 4))).catch(() => undefined);
  }

  function prevStep() {
    setCurrentStep((step) => Math.max(step - 1, 0));
  }

  const stepItems = [
    { title: '编研主题', description: '主题与受众' },
    { title: '主题库', description: '外部资料源' },
    { title: '知识库', description: '内部检索范围' },
    { title: '编研模板', description: '章节与工作流' },
    { title: '生成成果', description: '大纲与正文' }
  ];

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>智能编研</Typography.Title>
          <Typography.Text type="secondary">按向导选择资料源、知识库和编研模板，提交后查看大纲、分节正文与最终 Markdown 成果。</Typography.Text>
        </Space>
      </div>

      <Card variant="borderless" style={{ marginBottom: 16 }}>
        <Steps current={currentStep} items={stepItems} />
      </Card>

      {(themeLibrariesQuery.isError || knowledgeBasesQuery.isError || templatesQuery.isError) ? (
        <Alert
          type="error"
          showIcon
          message="向导数据加载失败"
          description={String(
            (themeLibrariesQuery.error as Error)?.message
            || (knowledgeBasesQuery.error as Error)?.message
            || (templatesQuery.error as Error)?.message
          )}
          style={{ marginBottom: 12 }}
        />
      ) : null}

      <div style={contentGridStyle}>
        <Card variant="borderless" title={stepItems[currentStep]?.title}>
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              topic: '',
              audience: '档案管理人员',
              themeLibraryId: 'theme_001',
              knowledgeBaseIds: [],
              templateId: 'template_research_001'
            }}
          >
            {currentStep === 0 ? (
              <>
                <Form.Item name="topic" label="编研主题" preserve rules={[{ required: true, message: '请输入编研主题' }]}>
                  <Input placeholder="例如：某专题档案编研" />
                </Form.Item>
                <Form.Item name="audience" label="面向对象">
                  <Input placeholder="例如：领导参阅、公众发布" />
                </Form.Item>
              </>
            ) : null}

            {currentStep === 1 ? (
              <Form.Item name="themeLibraryId" label="主题库" rules={[{ required: true, message: '请选择主题库' }]}>
                <Radio.Group style={{ width: '100%' }}>
                  <Space direction="vertical" style={{ width: '100%' }}>
                    {themeLibraries.map((library) => (
                      <Radio key={library.id} value={library.id} style={optionCardStyle(themeLibraryId === library.id)}>
                        <Space direction="vertical" size={0}>
                          <Typography.Text strong>{library.name}</Typography.Text>
                          <Typography.Text type="secondary">{library.description || '模拟数字档案馆主题库'}</Typography.Text>
                          {library.itemCount !== undefined ? <Tag>{library.itemCount} 条资料</Tag> : null}
                        </Space>
                      </Radio>
                    ))}
                  </Space>
                </Radio.Group>
              </Form.Item>
            ) : null}

            {currentStep === 2 ? (
              <Form.Item name="knowledgeBaseIds" label="知识库" rules={[{ required: true, message: '请至少选择一个知识库' }]}>
                <Checkbox.Group style={{ width: '100%' }}>
                  <Space direction="vertical" style={{ width: '100%' }}>
                    {knowledgeBases.map((base) => (
                      <Checkbox key={base.id} value={base.id} style={optionCardStyle(knowledgeBaseIds.includes(base.id))}>
                        <Space direction="vertical" size={0}>
                          <Typography.Text strong>{base.name}</Typography.Text>
                          <Typography.Text type="secondary">{base.description || '暂无描述'}</Typography.Text>
                          <Space size={6}>
                            <Tag icon={<FileTextOutlined />}>{base.documentCount} 文档</Tag>
                            <Tag icon={<DatabaseOutlined />}>{base.chunkCount} 片段</Tag>
                          </Space>
                        </Space>
                      </Checkbox>
                    ))}
                  </Space>
                </Checkbox.Group>
              </Form.Item>
            ) : null}

            {currentStep === 3 ? (
              <>
                <Form.Item name="templateId" label="编研模板" rules={[{ required: true, message: '请选择编研模板' }]}>
                  <Radio.Group style={{ width: '100%' }}>
                    <Space direction="vertical" style={{ width: '100%' }}>
                      {templates.map((template) => (
                        <Radio key={template.id} value={template.id} style={optionCardStyle(templateId === template.id)}>
                          <TemplateOption template={template} />
                        </Radio>
                      ))}
                    </Space>
                  </Radio.Group>
                </Form.Item>
                {selectedTemplate ? (
                  <Card size="small" title="模板章节预览" style={{ marginTop: 12 }}>
                    <Table
                      size="small"
                      rowKey="key"
                      pagination={false}
                      dataSource={selectedSchema.sections ?? []}
                      columns={[
                        { title: '章节', dataIndex: 'title' },
                        { title: '说明', dataIndex: 'instruction', ellipsis: true }
                      ]}
                    />
                  </Card>
                ) : null}
              </>
            ) : null}

            {currentStep === 4 ? (
              jobResult ? (
                <Space direction="vertical" size={16} style={{ width: '100%' }}>
                  <Alert type="success" showIcon message={`任务 ${jobResult.job.id} 已完成`} />
                  <Card size="small" title="编研大纲">
                    <Steps
                      direction="vertical"
                      size="small"
                      current={(jobResult.job.outline ?? []).length}
                      items={(jobResult.job.outline ?? []).map((section) => ({ title: section.title }))}
                    />
                  </Card>
                  <Card size="small" title="分节输出">
                    <Space direction="vertical" style={{ width: '100%' }} size={12}>
                      {(jobResult.job.sectionOutputs ?? []).map((section) => (
                        <Card key={section.key} size="small" type="inner" title={section.title}>
                          <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>
                            {section.contentMarkdown}
                          </Typography.Paragraph>
                          {(section.citations ?? []).length > 0 ? (
                            <Space wrap>
                              {(section.citations ?? []).map((citation, index) => (
                                <Tag key={`${section.key}-${index}`} color="blue">{citation.title}</Tag>
                              ))}
                            </Space>
                          ) : null}
                        </Card>
                      ))}
                    </Space>
                  </Card>
                  <Card
                    size="small"
                    title="最终成果"
                    extra={jobResult.output.hasDocx ? (
                      <Button
                        type="link"
                        icon={<DownloadOutlined />}
                        href={getResearchOutputDocxUrl(jobResult.output.id)}
                        target="_blank"
                      >
                        下载 DOCX
                      </Button>
                    ) : null}
                  >
                    <Typography.Paragraph copyable style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                      {jobResult.output.contentMarkdown}
                    </Typography.Paragraph>
                  </Card>
                </Space>
              ) : (
                <Alert type="info" showIcon message="尚未提交编研任务" />
              )
            ) : null}
          </Form>

          <div style={footerStyle}>
            {currentStep > 0 && currentStep < 4 ? <Button onClick={prevStep}>上一步</Button> : null}
            {currentStep < 3 ? (
              <Button type="primary" onClick={nextStep}>下一步</Button>
            ) : null}
            {currentStep === 3 ? (
              <Button
                type="primary"
                icon={<RocketOutlined />}
                loading={submitMutation.isPending}
                onClick={() => {
                  form.validateFields(submitFieldNames)
                    .then((values) => submitMutation.mutate(values))
                    .catch(() => undefined);
                }}
              >
                提交编研任务
              </Button>
            ) : null}
            {currentStep === 4 ? (
              <Button
                onClick={() => {
                  setJobResult(null);
                  setCurrentStep(0);
                  form.resetFields();
                }}
              >
                重新开始
              </Button>
            ) : null}
          </div>
        </Card>

        <Card variant="borderless" title={<Space><FileSearchOutlined />工作流编排</Space>} loading={boundWorkflowQuery.isLoading}>
          {selectedTemplate ? (
            <>
              <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
                当前模板：{selectedTemplate.name}
                {selectedTemplate.workflowId ? ` · 工作流 ${boundWorkflowQuery.data?.name ?? selectedTemplate.workflowId}` : ''}
              </Typography.Text>
              <Steps
                direction="vertical"
                size="small"
                current={(selectedWorkflow.nodes ?? []).length}
                items={(selectedWorkflow.nodes ?? []).map((node: WorkflowSnapshotNode) => ({
                  title: node.name,
                  description: node.type ? `${node.type}${node.order ? ` · 步骤 ${node.order}` : ''}` : undefined
                }))}
              />
            </>
          ) : (
            <Typography.Text type="secondary">选择编研模板后显示工作流节点。</Typography.Text>
          )}
        </Card>
      </div>
    </section>
  );
}

function TemplateOption({ template }: { template: GenerationTemplate }) {
  const schema = parseTemplateSchema(template.templateSchema);
  const workflow = parseWorkflowSnapshot(template.workflowSnapshot);
  return (
    <Space direction="vertical" size={0}>
      <Typography.Text strong>{template.name}</Typography.Text>
      <Typography.Text type="secondary">{template.description || template.code}</Typography.Text>
      <Space size={6}>
        <Tag icon={<BookOutlined />}>{schema.sections?.length ?? 0} 章节</Tag>
        {template.workflowId ? <Tag icon={<CheckCircleOutlined />}>已绑定工作流</Tag> : null}
        {!template.workflowId && (workflow.nodes?.length ?? 0) > 0 ? (
          <Tag icon={<CheckCircleOutlined />}>{workflow.nodes?.length ?? 0} 节点</Tag>
        ) : null}
      </Space>
    </Space>
  );
}

const submitFieldNames: Array<keyof WizardFormValues> = ['topic', 'audience', 'themeLibraryId', 'knowledgeBaseIds', 'templateId'];

const stepFields: Array<Array<keyof WizardFormValues>> = [
  ['topic', 'audience'],
  ['themeLibraryId'],
  ['knowledgeBaseIds'],
  ['templateId']
];

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

const contentGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(0, 2fr) minmax(280px, 1fr)'
};

const footerStyle: React.CSSProperties = {
  display: 'flex',
  gap: 8,
  justifyContent: 'flex-end',
  marginTop: 16
};

function optionCardStyle(active: boolean): React.CSSProperties {
  return {
    width: '100%',
    border: `1px solid ${active ? '#1677ff' : '#e7ecf3'}`,
    borderRadius: 8,
    padding: '10px 12px',
    background: active ? '#f0f7ff' : '#fff'
  };
}
