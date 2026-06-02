import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  FileAddOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, InputNumber, List, Select, Space, Steps, Tag, Typography, message } from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import {
  addKnowledgeDocument,
  listKnowledgeBases,
  previewKnowledgeChunks,
  previewUploadedKnowledgeDocumentFile,
  uploadKnowledgeDocumentFile,
  type AddKnowledgeDocumentRequest,
  type KnowledgeChunkPreview,
  type UploadedDocumentPreview
} from '../../api/knowledge';
import { navigateTo } from '../../navigation';

interface KnowledgeDocumentCreatePageProps {
  knowledgeBaseId: string;
}

const supportedUploadTypes = '.txt,.doc,.docx,.pdf,.md,.markdown,.html,.htm,.ppt,.pptx,.xls,.xlsx';

export function KnowledgeDocumentCreatePage({ knowledgeBaseId }: KnowledgeDocumentCreatePageProps) {
  const [form] = Form.useForm<AddKnowledgeDocumentRequest>();
  const queryClient = useQueryClient();
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadPreview, setUploadPreview] = useState<UploadedDocumentPreview | null>(null);
  const [chunkPreviews, setChunkPreviews] = useState<KnowledgeChunkPreview[]>([]);
  const [uploadStep, setUploadStep] = useState(0);

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const knowledgeBase = useMemo(
    () => basesQuery.data?.items.find((base) => base.id === knowledgeBaseId) ?? null,
    [basesQuery.data?.items, knowledgeBaseId]
  );

  const initialValues: AddKnowledgeDocumentRequest = {
    name: '',
    content: '',
    splitterType: knowledgeBase?.splitterType || 'SIMPLE_TEXT',
    chunkSize: knowledgeBase?.chunkSize || 500,
    chunkOverlap: knowledgeBase?.chunkOverlap ?? 50
  };

  const previewMutation = useMutation({
    mutationFn: (values: AddKnowledgeDocumentRequest) => previewKnowledgeChunks({
      content: values.content,
      splitterType: values.splitterType || 'SIMPLE_TEXT',
      chunkSize: values.chunkSize || 500,
      chunkOverlap: values.chunkOverlap ?? 0
    }),
    onSuccess: (previews) => {
      setUploadPreview(null);
      setChunkPreviews(previews);
      setUploadStep(1);
    }
  });

  const documentMutation = useMutation({
    mutationFn: (values: AddKnowledgeDocumentRequest) => addKnowledgeDocument(knowledgeBaseId, {
      ...initialValues,
      ...values
    }),
    onSuccess: async () => {
      message.success('文档已入库');
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
      navigateTo(`/knowledge/${knowledgeBaseId}/documents`);
    }
  });

  const uploadPreviewMutation = useMutation({
    mutationFn: async () => {
      if (!uploadFile) {
        throw new Error('请先选择文件');
      }
      const values = form.getFieldsValue();
      return previewUploadedKnowledgeDocumentFile(uploadFile, {
        splitterType: values.splitterType || 'SIMPLE_TEXT',
        chunkSize: values.chunkSize || 500,
        chunkOverlap: values.chunkOverlap ?? 0
      });
    },
    onSuccess: (preview) => {
      setUploadPreview(preview);
      setChunkPreviews(preview.chunks);
      setUploadStep(1);
    }
  });

  const uploadFileMutation = useMutation({
    mutationFn: async () => {
      if (!uploadFile) {
        throw new Error('请先选择文件');
      }
      const values = form.getFieldsValue();
      return uploadKnowledgeDocumentFile(knowledgeBaseId, uploadFile, {
        splitterType: values.splitterType || 'SIMPLE_TEXT',
        chunkSize: values.chunkSize || 500,
        chunkOverlap: values.chunkOverlap ?? 0
      });
    },
    onSuccess: async () => {
      message.success('文件已抽取并入库');
      setUploadStep(2);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
      navigateTo(`/knowledge/${knowledgeBaseId}/documents`);
    }
  });

  const previews = uploadPreview?.chunks ?? chunkPreviews;

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
            新增文档
          </Typography.Title>
          <Typography.Text type="secondary">
            {knowledgeBase?.name ?? '知识库'} 的文档入库入口，支持文件抽取、分段预览和手工录入。
          </Typography.Text>
        </Space>
      </div>

      <Card variant="borderless" title={<Space><UploadOutlined />文件上传与数据处理</Space>} style={{ marginBottom: 16 }}>
        <Steps
          current={uploadStep}
          items={[
            { title: '选择文件', description: '选择需要上传的数据文件' },
            { title: '数据处理', description: '配置分段并预览' },
            { title: '确认上传', description: '写入知识库' }
          ]}
          style={{ marginBottom: 24 }}
        />
        <div style={uploadWizardStyle}>
          <label htmlFor="knowledge-upload-file" style={uploadDropStyle}>
            <UploadOutlined style={{ color: '#1677ff', fontSize: 32 }} />
            <Typography.Text strong>点击或拖拽文件到此处上传</Typography.Text>
            <Typography.Text type="secondary">支持 txt、docx、pdf、md、html、pptx、xlsx 等类型文件</Typography.Text>
            <Typography.Text type="secondary">单个文件建议不超过 100 MB</Typography.Text>
            <input
              id="knowledge-upload-file"
              aria-label="选择知识库文件"
              accept={supportedUploadTypes}
              type="file"
              style={{ display: 'none' }}
              onChange={(event) => {
                const nextFile = event.target.files?.[0] ?? null;
                setUploadFile(nextFile);
                setUploadPreview(null);
                setChunkPreviews([]);
                setUploadStep(0);
              }}
            />
          </label>

          <div style={uploadPanelStyle}>
            <Typography.Text strong>数据处理配置</Typography.Text>
            <Form form={form} layout="vertical" initialValues={initialValues} style={{ marginTop: 12 }}>
              <div style={configGridStyle}>
                <Form.Item name="splitterType" label="分段策略">
                  <Select
                    options={[
                      { value: 'SIMPLE_TEXT', label: '固定长度分段' },
                      { value: 'MARKDOWN_HEADING', label: 'Markdown 标题' },
                      { value: 'REGEX', label: '段落分段' }
                    ]}
                  />
                </Form.Item>
                <Form.Item name="chunkSize" label="分段长度">
                  <InputNumber min={80} max={2000} style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name="chunkOverlap" label="重叠字符">
                  <InputNumber min={0} max={500} style={{ width: '100%' }} />
                </Form.Item>
              </div>
            </Form>
            {uploadFile ? (
              <Tag color="blue" style={{ marginBottom: 16 }}>
                {uploadFile.name} / {(uploadFile.size / 1024 / 1024).toFixed(2)} MB
              </Tag>
            ) : null}
            <Space>
              <Button
                icon={<FileSearchOutlined />}
                disabled={!uploadFile}
                loading={uploadPreviewMutation.isPending}
                onClick={() => uploadPreviewMutation.mutate()}
              >
                预览分段
              </Button>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                disabled={!uploadFile || !uploadPreview}
                loading={uploadFileMutation.isPending}
                onClick={() => uploadFileMutation.mutate()}
              >
                确认上传
              </Button>
            </Space>
          </div>
        </div>
      </Card>

      <Card variant="borderless" title={<Space><FileAddOutlined />手工新增文档</Space>} style={{ marginBottom: 16 }}>
        <Form form={form} layout="vertical" initialValues={initialValues} onFinish={(values) => documentMutation.mutate(values)}>
          <Form.Item name="name" label="文档名称" rules={[{ required: true, message: '请输入文档名称' }]}>
            <Input placeholder="faq.md" />
          </Form.Item>
          <Form.Item name="content" label="文档内容" rules={[{ required: true, message: '请输入文档内容' }]}>
            <Input.TextArea autoSize={{ minRows: 7, maxRows: 14 }} placeholder="粘贴文档内容，系统会按所选策略预览切片后入库。" />
          </Form.Item>
          <Space>
            <Button
              icon={<FileSearchOutlined />}
              loading={previewMutation.isPending}
              onClick={() => previewMutation.mutate(form.getFieldsValue())}
            >
              预览切片
            </Button>
            <Button type="primary" icon={<FileAddOutlined />} loading={documentMutation.isPending} onClick={() => form.submit()}>
              入库
            </Button>
          </Space>
        </Form>
      </Card>

      <Card variant="borderless" title={<Space><FileTextOutlined />切片预览</Space>}>
        <List
          dataSource={previews}
          locale={{ emptyText: '暂无切片预览' }}
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

const uploadWizardStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(320px, 0.9fr) minmax(420px, 1.1fr)'
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
  minHeight: 230,
  textAlign: 'center'
};

const uploadPanelStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  padding: 16
};

const configGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 12,
  gridTemplateColumns: 'minmax(180px, 1.2fr) minmax(120px, 0.8fr) minmax(120px, 0.8fr)'
};
