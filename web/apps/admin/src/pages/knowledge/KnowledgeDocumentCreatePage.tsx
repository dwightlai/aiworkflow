import {
  ArrowLeftOutlined,
  DatabaseOutlined,
  FileAddOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  PlusOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, InputNumber, List, Radio, Space, Tag, Typography, message } from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  addManualKnowledgeDataset,
  listKnowledgeBases,
  previewUploadedTableKnowledgeDocumentFile,
  previewUploadedTextKnowledgeDocumentFile,
  uploadTableKnowledgeDocumentFile,
  uploadTextKnowledgeDocumentFile,
  type KnowledgeChunkPreview,
  type KnowledgeSplitOptions,
  type ManualDatasetEntryRequest,
  type UploadedDocumentPreview
} from '../../api/knowledge';
import { navigateTo } from '../../navigation';

interface KnowledgeDocumentCreatePageProps {
  knowledgeBaseId: string;
}

interface ManualDatasetFormValues {
  entries: ManualDatasetEntryRequest[];
}

type DataMode = 'manual' | 'text' | 'table';

const textDocumentTypes = '.txt,.md,.markdown,.doc,.docx,.pdf,.html,.htm';
const tableDocumentTypes = '.xls,.xlsx';

export function KnowledgeDocumentCreatePage({ knowledgeBaseId }: KnowledgeDocumentCreatePageProps) {
  const queryClient = useQueryClient();
  const [manualDatasetForm] = Form.useForm<ManualDatasetFormValues>();
  const [textDocumentForm] = Form.useForm<KnowledgeSplitOptions>();
  const [tableDocumentForm] = Form.useForm<KnowledgeSplitOptions>();
  const textSplitterType = Form.useWatch('splitterType', textDocumentForm);
  const mode = parseCreateMode();
  const [textUploadFile, setTextUploadFile] = useState<File | null>(null);
  const [textUploadPreview, setTextUploadPreview] = useState<UploadedDocumentPreview | null>(null);
  const [tableUploadFile, setTableUploadFile] = useState<File | null>(null);
  const [tableUploadPreview, setTableUploadPreview] = useState<UploadedDocumentPreview | null>(null);
  const [chunkPreviews, setChunkPreviews] = useState<KnowledgeChunkPreview[]>([]);

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const knowledgeBase = useMemo(
    () => basesQuery.data?.items.find((base) => base.id === knowledgeBaseId) ?? null,
    [basesQuery.data?.items, knowledgeBaseId]
  );

  const manualDatasetMutation = useMutation({
    mutationFn: async (values: ManualDatasetFormValues) => {
      const entries = (values.entries ?? []).map((entry) => ({
        title: entry.title,
        content: entry.content,
        tags: entry.tags || null,
        category: entry.category || null,
        source: entry.source || null
      }));
      return addManualKnowledgeDataset(knowledgeBaseId, { entries });
    },
    onSuccess: async () => {
      message.success('手动数据集已入库');
      await refreshKnowledgeQueries(queryClient, knowledgeBaseId);
      navigateTo(`/knowledge/${knowledgeBaseId}/documents`);
    }
  });

  const textUploadPreviewMutation = useMutation({
    mutationFn: async () => {
      if (!textUploadFile) {
        throw new Error('请先选择文本文档');
      }
      return previewUploadedTextKnowledgeDocumentFile(textUploadFile, normalizedTextOptions(textDocumentForm.getFieldsValue()));
    },
    onSuccess: (preview) => {
      setTextUploadPreview(preview);
      setChunkPreviews(preview.chunks);
    }
  });

  const textUploadMutation = useMutation({
    mutationFn: async () => {
      if (!textUploadFile) {
        throw new Error('请先选择文本文档');
      }
      return uploadTextKnowledgeDocumentFile(knowledgeBaseId, textUploadFile, normalizedTextOptions(textDocumentForm.getFieldsValue()));
    },
    onSuccess: async () => {
      message.success('文本文档已解析并入库');
      await refreshKnowledgeQueries(queryClient, knowledgeBaseId);
      navigateTo(`/knowledge/${knowledgeBaseId}/documents`);
    }
  });

  const tableUploadPreviewMutation = useMutation({
    mutationFn: async () => {
      if (!tableUploadFile) {
        throw new Error('请先选择表格文档');
      }
      return previewUploadedTableKnowledgeDocumentFile(tableUploadFile, normalizedTableOptions(tableDocumentForm.getFieldsValue()));
    },
    onSuccess: (preview) => {
      setTableUploadPreview(preview);
      setChunkPreviews(preview.chunks);
    }
  });

  const tableUploadMutation = useMutation({
    mutationFn: async () => {
      if (!tableUploadFile) {
        throw new Error('请先选择表格文档');
      }
      return uploadTableKnowledgeDocumentFile(knowledgeBaseId, tableUploadFile, normalizedTableOptions(tableDocumentForm.getFieldsValue()));
    },
    onSuccess: async () => {
      message.success('表格文档已解析并入库');
      await refreshKnowledgeQueries(queryClient, knowledgeBaseId);
      navigateTo(`/knowledge/${knowledgeBaseId}/documents`);
    }
  });

  const previews = chunkPreviews;
  const modeLabel = getCreateModeLabel(mode);

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={8}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigateTo(`/knowledge/${knowledgeBaseId}/documents`)}
            style={{ width: 'fit-content' }}
          >
            返回文档列表
          </Button>
          <Typography.Title level={3} style={{ margin: 0 }}>
            新增文档 - {modeLabel}
          </Typography.Title>
          <Typography.Text type="secondary">
            {knowledgeBase?.name ?? '知识库'} 的{modeLabel}入库入口，当前页面只处理这一种新增方式。
          </Typography.Text>
        </Space>
      </div>

      {mode === 'manual' ? (
        <Card variant="borderless" title={<Space><FileAddOutlined />手动数据集</Space>} style={{ marginBottom: 16 }}>
          <Form
            form={manualDatasetForm}
            layout="vertical"
            initialValues={{ entries: [{ title: '', content: '', tags: '', category: '', source: '' }] }}
            onFinish={(values) => manualDatasetMutation.mutate(values)}
          >
            <Form.List name="entries">
              {(fields, { add, remove }) => (
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  {fields.map((field, index) => (
                    <Card
                      key={field.key}
                      size="small"
                      title={`知识条目 ${index + 1}`}
                      extra={fields.length > 1 ? <Button size="small" danger onClick={() => remove(field.name)}>删除</Button> : null}
                    >
                      <Form.Item name={[field.name, 'title']} label="标题" rules={[{ required: true, message: '请输入标题' }]}>
                        <Input placeholder="例如：退费规则" />
                      </Form.Item>
                      <Form.Item name={[field.name, 'content']} label="正文内容" rules={[{ required: true, message: '请输入正文内容' }]}>
                        <Input.TextArea autoSize={{ minRows: 5, maxRows: 10 }} placeholder="录入 FAQ、业务规则、标准说明等知识内容" />
                      </Form.Item>
                      <Space wrap>
                        <Form.Item name={[field.name, 'tags']} label="标签">
                          <Input style={{ width: 180 }} placeholder="多个标签用逗号分隔" />
                        </Form.Item>
                        <Form.Item name={[field.name, 'category']} label="分类">
                          <Input style={{ width: 180 }} placeholder="业务分类" />
                        </Form.Item>
                        <Form.Item name={[field.name, 'source']} label="来源说明">
                          <Input style={{ width: 220 }} placeholder="来源系统、文件或人工录入" />
                        </Form.Item>
                      </Space>
                    </Card>
                  ))}
                  <Button icon={<PlusOutlined />} onClick={() => add({ title: '', content: '', tags: '', category: '', source: '' })}>
                    新增一条
                  </Button>
                </Space>
              )}
            </Form.List>
            <Button
              type="primary"
              icon={<FileAddOutlined />}
              loading={manualDatasetMutation.isPending}
              onClick={() => manualDatasetForm.submit()}
              style={{ marginTop: 16 }}
            >
              保存并入库
            </Button>
          </Form>
        </Card>
      ) : null}

      {mode === 'text' ? (
        <Card variant="borderless" title={<Space><FileTextOutlined />文本文档</Space>} style={{ marginBottom: 16 }}>
          <div style={uploadWizardStyle}>
            <label htmlFor="knowledge-text-upload-file" style={uploadDropStyle}>
              <UploadOutlined style={{ color: '#1677ff', fontSize: 28 }} />
              <Typography.Text strong>选择文本文档</Typography.Text>
              <Typography.Text type="secondary">支持 TXT、MD、DOC、DOCX、PDF、HTML</Typography.Text>
              <input
                id="knowledge-text-upload-file"
                aria-label="选择文本文档"
                accept={textDocumentTypes}
                type="file"
                style={{ display: 'none' }}
                onChange={(event) => {
                  const nextFile = event.target.files?.[0] ?? null;
                  setTextUploadFile(nextFile);
                  setTextUploadPreview(null);
                  setChunkPreviews([]);
                }}
              />
            </label>
            <Card size="small" title="分段策略">
              <Form form={textDocumentForm} layout="vertical" initialValues={{ splitterType: 'FIXED_LENGTH', chunkSize: 200, separator: '' }}>
                <Form.Item name="splitterType">
                  <Radio.Group>
                    <Space direction="vertical">
                      <Radio value="FIXED_LENGTH">固定长度分段</Radio>
                      <Radio value="PARAGRAPH">段落分段</Radio>
                      <Radio value="SEMANTIC">语义分段</Radio>
                      <Radio value="SYMBOL">符号分段</Radio>
                    </Space>
                  </Radio.Group>
                </Form.Item>
                <Form.Item name="chunkSize" label="分段长度" rules={[{ required: true, message: '请输入分段长度' }]}>
                  <InputNumber min={1} controls style={{ width: 180 }} />
                </Form.Item>
                {textSplitterType === 'SYMBOL' ? (
                  <Form.Item name="separator" label="分段符">
                    <Input placeholder="请输入分段符号" />
                  </Form.Item>
                ) : null}
              </Form>
              {textUploadFile ? <Tag color="blue">{textUploadFile.name} / {(textUploadFile.size / 1024 / 1024).toFixed(2)} MB</Tag> : null}
              <Space style={{ marginTop: 16 }}>
                <Button icon={<FileSearchOutlined />} disabled={!textUploadFile} loading={textUploadPreviewMutation.isPending} onClick={() => textUploadPreviewMutation.mutate()}>
                  预览分段
                </Button>
                <Button type="primary" icon={<FileAddOutlined />} disabled={!textUploadFile || !textUploadPreview} loading={textUploadMutation.isPending} onClick={() => textUploadMutation.mutate()}>
                  确认上传
                </Button>
              </Space>
            </Card>
          </div>
        </Card>
      ) : null}

      {mode === 'table' ? (
        <Card variant="borderless" title={<Space><DatabaseOutlined />表格文档</Space>} style={{ marginBottom: 16 }}>
          <div style={uploadWizardStyle}>
            <label htmlFor="knowledge-table-upload-file" style={uploadDropStyle}>
              <UploadOutlined style={{ color: '#1677ff', fontSize: 28 }} />
              <Typography.Text strong>选择表格文档</Typography.Text>
              <Typography.Text type="secondary">支持 XLS、XLSX</Typography.Text>
              <input
                id="knowledge-table-upload-file"
                aria-label="选择表格文档"
                accept={tableDocumentTypes}
                type="file"
                style={{ display: 'none' }}
                onChange={(event) => {
                  const nextFile = event.target.files?.[0] ?? null;
                  setTableUploadFile(nextFile);
                  setTableUploadPreview(null);
                  setChunkPreviews([]);
                }}
              />
            </label>
            <Card size="small" title="数据处理配置">
              <Form form={tableDocumentForm} layout="vertical" initialValues={{ splitterType: 'STRUCTURED_TABLE', chunkSize: 200 }}>
                <Form.Item name="splitterType" label="分段方式">
                  <Radio.Group>
                    <Radio value="STRUCTURED_TABLE">结构化分段</Radio>
                  </Radio.Group>
                </Form.Item>
                <Form.Item name="chunkSize" label="分段长度" rules={[{ required: true, message: '请输入分段长度' }]}>
                  <InputNumber min={1} controls style={{ width: 180 }} />
                </Form.Item>
              </Form>
              {tableUploadFile ? <Tag color="cyan">{tableUploadFile.name} / {(tableUploadFile.size / 1024 / 1024).toFixed(2)} MB</Tag> : null}
              <Space style={{ marginTop: 16 }}>
                <Button icon={<FileSearchOutlined />} disabled={!tableUploadFile} loading={tableUploadPreviewMutation.isPending} onClick={() => tableUploadPreviewMutation.mutate()}>
                  预览分段
                </Button>
                <Button type="primary" icon={<FileAddOutlined />} disabled={!tableUploadFile || !tableUploadPreview} loading={tableUploadMutation.isPending} onClick={() => tableUploadMutation.mutate()}>
                  确认上传
                </Button>
              </Space>
            </Card>
          </div>
        </Card>
      ) : null}

      <Card variant="borderless" title={<Space><FileTextOutlined />分段预览</Space>}>
        <List
          dataSource={previews}
          locale={{ emptyText: '暂无分段预览' }}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={<Space><Tag color="blue">#{item.index + 1}</Tag><Typography.Text type="secondary">{item.tokenEstimate} tokens</Typography.Text></Space>}
                description={<Typography.Paragraph style={{ marginBottom: 0 }}>{item.content}</Typography.Paragraph>}
              />
            </List.Item>
          )}
        />
      </Card>
    </section>
  );
}

async function refreshKnowledgeQueries(queryClient: ReturnType<typeof useQueryClient>, knowledgeBaseId: string) {
  await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
  await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
}

function normalizedTextOptions(values: KnowledgeSplitOptions): KnowledgeSplitOptions {
  return {
    splitterType: values.splitterType || 'FIXED_LENGTH',
    chunkSize: values.chunkSize || 200,
    separator: values.separator || null
  };
}

function normalizedTableOptions(values: KnowledgeSplitOptions): KnowledgeSplitOptions {
  return {
    splitterType: 'STRUCTURED_TABLE',
    chunkSize: values.chunkSize || 200
  };
}

function parseCreateMode(): DataMode {
  const type = new URLSearchParams(window.location.search).get('type');
  if (type === 'manual' || type === 'table') {
    return type;
  }
  return 'text';
}

function getCreateModeLabel(mode: DataMode) {
  if (mode === 'manual') {
    return '手动数据集';
  }
  if (mode === 'table') {
    return '表格文档';
  }
  return '文本文档';
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

const uploadWizardStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(300px, 0.9fr) minmax(360px, 1.1fr)'
};

const uploadDropStyle: React.CSSProperties = {
  alignItems: 'center',
  border: '1px dashed #c9d7ef',
  borderRadius: 8,
  cursor: 'pointer',
  display: 'flex',
  flexDirection: 'column',
  gap: 8,
  justifyContent: 'center',
  minHeight: 180,
  textAlign: 'center'
};
