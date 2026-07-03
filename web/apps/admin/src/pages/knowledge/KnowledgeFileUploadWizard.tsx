import {
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  LoadingOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  List,
  Progress,
  Radio,
  Space,
  Steps,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  previewUploadedTableKnowledgeDocumentFile,
  previewUploadedTextKnowledgeDocumentFile,
  uploadTableKnowledgeDocumentFile,
  uploadTextKnowledgeDocumentFile,
  type KnowledgeSplitOptions,
  type UploadedDocumentPreview
} from '../../api/knowledge';
import { navigateTo } from '../../navigation';

type UploadMode = 'text' | 'table';

interface KnowledgeFileUploadWizardProps {
  knowledgeBaseId: string;
  datasetId?: string;
  mode: UploadMode;
  defaultSemanticSimilarityThreshold?: number;
}

type ProgressState = 'waiting' | 'processing' | 'done' | 'failed';

interface UploadFileItem {
  id: string;
  file: File;
  readProgress: ProgressState;
  segmentProgress: ProgressState;
  status: ProgressState;
  preview?: UploadedDocumentPreview;
  errorMessage?: string;
}

const textAccept = '.txt,.md,.markdown,.doc,.docx,.pdf,.html,.htm,.ppt,.pptx';
const tableAccept = '.xls,.xlsx';
const maxFileSizeBytes = 100 * 1024 * 1024;

export function KnowledgeFileUploadWizard({
  knowledgeBaseId,
  datasetId,
  mode,
  defaultSemanticSimilarityThreshold = 0.78
}: KnowledgeFileUploadWizardProps) {
  const queryClient = useQueryClient();
  const [step, setStep] = useState(0);
  const [files, setFiles] = useState<UploadFileItem[]>([]);
  const [selectedFileId, setSelectedFileId] = useState<string | null>(null);
  const [processing, setProcessing] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [splitForm] = Form.useForm<KnowledgeSplitOptions>();
  const splitterType = Form.useWatch('splitterType', splitForm);
  const chunkSize = Form.useWatch('chunkSize', splitForm);
  const chunkOverlap = Form.useWatch('chunkOverlap', splitForm);
  const separator = Form.useWatch('separator', splitForm);
  const semanticSimilarityThreshold = Form.useWatch('semanticSimilarityThreshold', splitForm);

  const accept = mode === 'text' ? textAccept : tableAccept;
  const acceptHint = mode === 'text'
    ? '支持 txt、docx、pdf、md、html、pptx 类型文件'
    : '支持 xls、xlsx 类型文件';
  const modeLabel = mode === 'text' ? '文本文档' : '表格文档';

  const selectedFile = useMemo(
    () => files.find((item) => item.id === selectedFileId) ?? null,
    [files, selectedFileId]
  );

  const splitOptions = useCallback((): KnowledgeSplitOptions => {
    const values = splitForm.getFieldsValue();
    if (mode === 'table') {
      return { splitterType: 'STRUCTURED_TABLE', chunkSize: values.chunkSize || 500, chunkOverlap: 0, datasetId };
    }
    return {
      splitterType: values.splitterType || 'STRUCTURE_AWARE',
      chunkSize: values.chunkSize || 500,
      chunkOverlap: values.chunkOverlap ?? 50,
      semanticSimilarityThreshold: values.semanticSimilarityThreshold ?? defaultSemanticSimilarityThreshold,
      separator: values.separator || null,
      datasetId,
      knowledgeBaseId
    };
  }, [chunkOverlap, datasetId, defaultSemanticSimilarityThreshold, knowledgeBaseId, mode, splitForm]);

  const previewMutation = useMutation({
    mutationFn: async (item: UploadFileItem) => {
      if (mode === 'text') {
        return previewUploadedTextKnowledgeDocumentFile(item.file, splitOptions());
      }
      return previewUploadedTableKnowledgeDocumentFile(item.file, splitOptions());
    },
    onSuccess: (preview, item) => {
      setFiles((current) => current.map((file) => (
        file.id === item.id ? { ...file, preview, readProgress: 'done' } : file
      )));
    },
    onError: (error: Error, item) => {
      message.error(`${item.file.name} 预览失败：${error.message}`);
    }
  });

  useEffect(() => {
    if (step !== 1 || !selectedFileId) {
      return;
    }
    const timer = window.setTimeout(() => {
      setFiles((current) => {
        const item = current.find((file) => file.id === selectedFileId);
        if (item) {
          previewMutation.mutate(item);
        }
        return current;
      });
    }, 300);
    return () => window.clearTimeout(timer);
  }, [
    step,
    selectedFileId,
    splitterType,
    chunkSize,
    chunkOverlap,
    separator,
    semanticSimilarityThreshold,
    mode
  ]);

  function addFiles(fileList: FileList | File[]) {
    const incoming = Array.from(fileList);
    const next: UploadFileItem[] = [];
    for (const file of incoming) {
      if (file.size > maxFileSizeBytes) {
        message.warning(`${file.name} 超过 100MB，已跳过`);
        continue;
      }
      const exists = files.some((item) => item.file.name === file.name && item.file.size === file.size);
      if (exists) {
        continue;
      }
      next.push({
        id: `${file.name}-${file.size}-${file.lastModified}`,
        file,
        readProgress: 'waiting',
        segmentProgress: 'waiting',
        status: 'waiting'
      });
    }
    if (next.length === 0) {
      return;
    }
    setFiles((current) => [...current, ...next]);
    if (!selectedFileId) {
      setSelectedFileId(next[0].id);
    }
  }

  function removeFile(fileId: string) {
    setFiles((current) => {
      const filtered = current.filter((item) => item.id !== fileId);
      if (selectedFileId === fileId) {
        setSelectedFileId(filtered[0]?.id ?? null);
      }
      return filtered;
    });
  }

  async function startProcessing() {
    if (files.length === 0) {
      return;
    }
    setProcessing(true);
    const options = splitOptions();
    let failedCount = 0;
    for (const item of files) {
      setFiles((current) => current.map((file) => (
        file.id === item.id
          ? { ...file, readProgress: 'processing', segmentProgress: 'waiting', status: 'processing' }
          : file
      )));
      try {
        setFiles((current) => current.map((file) => (
          file.id === item.id ? { ...file, readProgress: 'done', segmentProgress: 'processing' } : file
        )));
        if (mode === 'text') {
          await uploadTextKnowledgeDocumentFile(knowledgeBaseId, item.file, options);
        } else {
          await uploadTableKnowledgeDocumentFile(knowledgeBaseId, item.file, options);
        }
        setFiles((current) => current.map((file) => (
          file.id === item.id
            ? { ...file, segmentProgress: 'done', status: 'done' }
            : file
        )));
      } catch (error) {
        failedCount += 1;
        const errorMessage = (error as Error).message || '处理失败';
        setFiles((current) => current.map((file) => (
          file.id === item.id
            ? { ...file, segmentProgress: 'failed', status: 'failed', errorMessage }
            : file
        )));
      }
    }
    setProcessing(false);
    await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
    if (failedCount === 0) {
      message.success('全部文件已处理完成');
    } else {
      message.warning(`处理完成，${failedCount} 个文件失败`);
    }
  }

  const stepItems = [
    { title: '选择文件', description: '选择需要上传的数据文件', icon: <UploadOutlined /> },
    { title: '数据处理', description: '配置数据处理参数', icon: <EditOutlined /> },
    { title: '确认上传', description: '确认并完成上传', icon: <CheckCircleOutlined /> }
  ];

  const confirmColumns: ColumnsType<UploadFileItem> = [
    {
      title: '来源名',
      dataIndex: 'file',
      render: (_, record) => (
        <Space>
          <FileTextOutlined />
          <Typography.Text>{record.file.name}</Typography.Text>
        </Space>
      )
    },
    {
      title: '文件大小',
      width: 120,
      render: (_, record) => formatFileSize(record.file.size)
    },
    {
      title: '读取进度',
      width: 120,
      render: (_, record) => renderProgressTag(record.readProgress)
    },
    {
      title: '分段进度',
      width: 140,
      render: (_, record) => renderProgressTag(record.segmentProgress)
    },
    {
      title: '状态',
      width: 120,
      render: (_, record) => renderStatusTag(record.status, record.errorMessage)
    }
  ];

  const allDone = files.length > 0 && files.every((item) => item.status === 'done' || item.status === 'failed');
  const hasFailed = files.some((item) => item.status === 'failed');
  const hasStructureWarnings = files.some((item) => (item.preview?.warnings?.length ?? 0) > 0);

  return (
    <Card variant="borderless" title={<Space><FileTextOutlined />{modeLabel}批量上传</Space>}>
      <Steps current={step} items={stepItems} style={{ marginBottom: 24 }} />

      {step === 0 ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div
            role="button"
            tabIndex={0}
            style={{
              ...dropZoneStyle,
              borderColor: dragOver ? '#1677ff' : '#c9d7ef',
              background: dragOver ? '#f0f7ff' : '#fafcff',
              cursor: 'pointer'
            }}
            onClick={() => fileInputRef.current?.click()}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                fileInputRef.current?.click();
              }
            }}
            onDragOver={(event) => {
              event.preventDefault();
              setDragOver(true);
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={(event) => {
              event.preventDefault();
              setDragOver(false);
              if (event.dataTransfer.files?.length) {
                addFiles(event.dataTransfer.files);
              }
            }}
          >
            <UploadOutlined style={{ color: '#1677ff', fontSize: 36 }} />
            <Typography.Text strong>点击或拖拽文件到此处上传</Typography.Text>
            <Typography.Text type="secondary">{acceptHint}</Typography.Text>
            <Typography.Text type="secondary">单个文件最大 100 MB</Typography.Text>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept={accept}
              style={{ display: 'none' }}
              onChange={(event) => {
                if (event.target.files?.length) {
                  addFiles(event.target.files);
                }
                event.target.value = '';
              }}
            />
            <Button
              type="primary"
              style={{ marginTop: 8 }}
              onClick={(event) => {
                event.stopPropagation();
                fileInputRef.current?.click();
              }}
            >
              选择文件
            </Button>
          </div>

          <List
            dataSource={files}
            locale={{ emptyText: '暂无文件，请先选择或拖拽上传' }}
            renderItem={(item) => (
              <List.Item
                actions={[
                  <Button key="remove" type="text" danger icon={<DeleteOutlined />} onClick={() => removeFile(item.id)} />
                ]}
              >
                <List.Item.Meta
                  avatar={<FileTextOutlined style={{ fontSize: 20, color: '#1677ff' }} />}
                  title={item.file.name}
                  description={`${formatFileSize(item.file.size)} · ${renderStatusTag(item.status)}`}
                />
              </List.Item>
            )}
          />

          <Space>
            <Button disabled={files.length === 0} type="primary" onClick={() => setStep(1)}>
              下一步
            </Button>
          </Space>
        </Space>
      ) : null}

      {step === 1 ? (
        <div style={processLayoutStyle}>
          <Card size="small" title="数据处理配置" style={{ minWidth: 0 }}>
            <Form
              form={splitForm}
              layout="vertical"
              initialValues={mode === 'text'
                ? {
                    splitterType: 'STRUCTURE_AWARE',
                    chunkSize: 500,
                    chunkOverlap: 50,
                    semanticSimilarityThreshold: defaultSemanticSimilarityThreshold,
                    separator: ''
                  }
                : { splitterType: 'STRUCTURED_TABLE', chunkSize: 500, chunkOverlap: 0 }}
            >
              {mode === 'text' ? (
                <Form.Item name="splitterType">
                  <Radio.Group>
                    <Space direction="vertical">
                      <Radio value="STRUCTURE_AWARE">结构感知分段（推荐）</Radio>
                      <Radio value="FIXED_LENGTH">固定长度分段</Radio>
                      <Radio value="PARAGRAPH">段落分段</Radio>
                      <Radio value="SENTENCE_BOUNDARY">句子边界分段</Radio>
                      <Radio value="SEMANTIC">语义分段</Radio>
                      <Radio value="SYMBOL">符号分段</Radio>
                    </Space>
                  </Radio.Group>
                </Form.Item>
              ) : (
                <Form.Item name="splitterType">
                  <Radio.Group>
                    <Radio value="STRUCTURED_TABLE">结构化分段</Radio>
                  </Radio.Group>
                </Form.Item>
              )}
              <Form.Item name="chunkSize" label="分段长度" rules={[{ required: true, message: '请输入分段长度' }]}>
                <InputNumber min={1} max={5000} style={{ width: '100%' }} />
              </Form.Item>
              {mode === 'text' ? (
                <Form.Item name="chunkOverlap" label="重叠 Token">
                  <InputNumber min={0} max={1000} style={{ width: '100%' }} />
                </Form.Item>
              ) : null}
              {mode === 'text' && splitterType === 'SEMANTIC' ? (
                <Form.Item
                  name="semanticSimilarityThreshold"
                  label="语义相似度阈值"
                  tooltip="相邻内容相似度低于该值时创建新的分段"
                >
                  <InputNumber min={0} max={1} step={0.01} precision={2} style={{ width: '100%' }} />
                </Form.Item>
              ) : null}
              {mode === 'text' && splitterType === 'SYMBOL' ? (
                <Form.Item name="separator" label="分段符">
                  <Input placeholder="请输入分段符号" />
                </Form.Item>
              ) : null}
            </Form>
          </Card>

          <Card
            size="small"
            title={<Space><FileTextOutlined />文件列表<Tag>{files.length} 个文件</Tag></Space>}
            style={{ minWidth: 0 }}
          >
            <List
              dataSource={files}
              renderItem={(item) => (
                <List.Item
                  style={{
                    cursor: 'pointer',
                    background: item.id === selectedFileId ? '#e6f4ff' : undefined,
                    borderRadius: 6,
                    paddingInline: 8
                  }}
                  onClick={() => setSelectedFileId(item.id)}
                >
                  <List.Item.Meta
                    title={item.file.name}
                    description={formatFileSize(item.file.size)}
                  />
                </List.Item>
              )}
            />
          </Card>

          <Card
            size="small"
            title={<Space><EyeOutlined />文件预览</Space>}
            style={{ minWidth: 0 }}
            extra={previewMutation.isPending ? <LoadingOutlined /> : null}
          >
            {selectedFile?.preview ? (
              <>
                {selectedFile.preview.warnings?.map((warning) => (
                  <Alert
                    key={warning}
                    type="error"
                    showIcon
                    message="文档结构质量异常"
                    description={warning}
                    style={{ marginBottom: 12 }}
                  />
                ))}
                <List
                size="small"
                dataSource={selectedFile.preview.chunks.slice(0, 8)}
                locale={{ emptyText: '暂无分段预览' }}
                renderItem={(chunk) => (
                  <List.Item>
                    <List.Item.Meta
                      title={(
                        <Space wrap>
                          <Tag color="blue">#{chunk.index + 1}</Tag>
                          <Tag>{chunk.chunkType || 'PARAGRAPH'}</Tag>
                          {chunk.atomic ? <Tag color="gold">原子块</Tag> : null}
                          <Typography.Text type="secondary">{chunk.tokenEstimate} tokens</Typography.Text>
                        </Space>
                      )}
                      description={(
                        <>
                          {chunk.sectionPath?.length ? (
                            <Typography.Text type="secondary">{chunk.sectionPath.join(' / ')}</Typography.Text>
                          ) : null}
                          <Typography.Paragraph ellipsis={{ rows: 4 }}>{chunk.content}</Typography.Paragraph>
                        </>
                      )}
                    />
                  </List.Item>
                )}
                />
              </>
            ) : (
              <Typography.Text type="secondary">选择文件后自动预览分段效果</Typography.Text>
            )}
          </Card>

          <Space style={{ gridColumn: '1 / -1' }}>
            <Button onClick={() => setStep(0)}>上一步</Button>
            <Button type="primary" disabled={files.length === 0 || hasStructureWarnings} onClick={() => setStep(2)}>
              下一步
            </Button>
          </Space>
        </div>
      ) : null}

      {step === 2 ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Card size="small" title="数据处理配置">
            <Table rowKey="id" columns={confirmColumns} dataSource={files} pagination={false} />
          </Card>
          <Space>
            <Button disabled={processing} onClick={() => setStep(1)}>上一步</Button>
            {!allDone ? (
              <Button type="primary" loading={processing} disabled={files.length === 0} onClick={() => void startProcessing()}>
                {processing ? '处理中...' : '开始处理'}
              </Button>
            ) : (
              <Button
                type="primary"
                onClick={() => navigateTo(`/knowledge/${knowledgeBaseId}/documents`)}
              >
                {hasFailed ? '返回文档列表' : '完成'}
              </Button>
            )}
          </Space>
          {processing ? <Progress percent={Math.round((files.filter((f) => f.status === 'done' || f.status === 'failed').length / files.length) * 100)} /> : null}
        </Space>
      ) : null}
    </Card>
  );
}

function formatFileSize(size: number) {
  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(2)} MB`;
  }
  return `${(size / 1024).toFixed(2)} KB`;
}

function renderProgressTag(state: ProgressState) {
  if (state === 'done') {
    return <Tag color="blue">已完成</Tag>;
  }
  if (state === 'processing') {
    return <Tag color="orange">处理中</Tag>;
  }
  if (state === 'failed') {
    return <Tag color="red">失败</Tag>;
  }
  return <Tag>等待处理</Tag>;
}

function renderStatusTag(state: ProgressState, errorMessage?: string) {
  if (state === 'done') {
    return <Tag color="success">已完成</Tag>;
  }
  if (state === 'processing') {
    return <Tag color="processing">处理中</Tag>;
  }
  if (state === 'failed') {
    return <Tag color="error" title={errorMessage}>失败</Tag>;
  }
  return <Tag>等待处理</Tag>;
}

const dropZoneStyle: React.CSSProperties = {
  alignItems: 'center',
  border: '1px dashed #c9d7ef',
  borderRadius: 8,
  display: 'flex',
  flexDirection: 'column',
  gap: 8,
  justifyContent: 'center',
  minHeight: 220,
  padding: 24,
  textAlign: 'center'
};

const processLayoutStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(220px, 0.8fr) minmax(220px, 0.9fr) minmax(280px, 1.2fr)'
};
