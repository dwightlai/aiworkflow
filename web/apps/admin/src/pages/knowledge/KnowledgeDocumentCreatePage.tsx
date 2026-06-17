import {
  ArrowLeftOutlined,
  FileAddOutlined,
  PlusOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, Space, Typography, message } from 'antd';
import type React from 'react';
import { useMemo } from 'react';
import {
  addManualKnowledgeDataset,
  listKnowledgeBases,
  type ManualDatasetEntryRequest
} from '../../api/knowledge';
import { navigateTo } from '../../navigation';
import { KnowledgeFileUploadWizard } from './KnowledgeFileUploadWizard';

interface KnowledgeDocumentCreatePageProps {
  knowledgeBaseId: string;
}

interface ManualDatasetFormValues {
  entries: ManualDatasetEntryRequest[];
}

type DataMode = 'manual' | 'text' | 'table';

export function KnowledgeDocumentCreatePage({ knowledgeBaseId }: KnowledgeDocumentCreatePageProps) {
  const queryClient = useQueryClient();
  const [manualDatasetForm] = Form.useForm<ManualDatasetFormValues>();
  const mode = parseCreateMode();

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const knowledgeBase = useMemo(
    () => basesQuery.data?.items.find((base) => base.id === knowledgeBaseId) ?? null,
    [basesQuery.data?.items, knowledgeBaseId]
  );
  const datasetId = parseDatasetId(knowledgeBase?.defaultDatasetId);

  const manualDatasetMutation = useMutation({
    mutationFn: async (values: ManualDatasetFormValues) => {
      const entries = (values.entries ?? []).map((entry) => ({
        title: entry.title,
        content: entry.content,
        tags: entry.tags || null,
        category: entry.category || null,
        source: entry.source || null
      }));
      return addManualKnowledgeDataset(knowledgeBaseId, { entries, datasetId });
    },
    onSuccess: async () => {
      message.success('手动数据集已入库');
      await refreshKnowledgeQueries(queryClient, knowledgeBaseId);
      navigateTo(`/knowledge/${knowledgeBaseId}/documents`);
    }
  });

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
            {knowledgeBase?.name ?? '知识库'} 的{modeLabel}入库入口。
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
        <KnowledgeFileUploadWizard knowledgeBaseId={knowledgeBaseId} datasetId={datasetId} mode="text" />
      ) : null}

      {mode === 'table' ? (
        <KnowledgeFileUploadWizard knowledgeBaseId={knowledgeBaseId} datasetId={datasetId} mode="table" />
      ) : null}
    </section>
  );
}

async function refreshKnowledgeQueries(queryClient: ReturnType<typeof useQueryClient>, knowledgeBaseId: string) {
  await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
  await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId] });
}

function parseDatasetId(fallbackDatasetId?: string | null): string | undefined {
  const datasetId = new URLSearchParams(window.location.search).get('datasetId');
  if (datasetId && datasetId.trim()) {
    return datasetId.trim();
  }
  if (fallbackDatasetId && fallbackDatasetId.trim()) {
    return fallbackDatasetId.trim();
  }
  return undefined;
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
