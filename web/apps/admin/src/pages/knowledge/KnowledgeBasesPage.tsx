import {
  BookOutlined,
  CheckCircleOutlined,
  DatabaseOutlined,
  FileAddOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  PlusOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Drawer,
  Form,
  Input,
  InputNumber,
  List,
  Select,
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
  addKnowledgeDocument,
  createKnowledgeBase,
  listKnowledgeBases,
  listVectorStoreConfigs,
  previewKnowledgeChunks,
  searchKnowledgeBase,
  type AddKnowledgeDocumentRequest,
  type KnowledgeBase,
  type KnowledgeChunkPreview,
  type KnowledgeSearchResult,
  type SaveKnowledgeBaseRequest
} from '../../api/knowledge';

const initialBaseValues: SaveKnowledgeBaseRequest = {
  name: '',
  description: null,
  embeddingModelId: null,
  vectorStoreConfigId: null,
  splitterType: 'SIMPLE_TEXT',
  chunkSize: 500,
  chunkOverlap: 50,
  retrievalMode: 'KEYWORD',
  topK: 3
};

const initialDocumentValues: AddKnowledgeDocumentRequest = {
  name: '',
  content: '',
  splitterType: 'SIMPLE_TEXT',
  chunkSize: 500,
  chunkOverlap: 50
};

export function KnowledgeBasesPage() {
  const [baseForm] = Form.useForm<SaveKnowledgeBaseRequest>();
  const [documentForm] = Form.useForm<AddKnowledgeDocumentRequest>();
  const [searchForm] = Form.useForm<{ query: string; topK: number }>();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [documentOpen, setDocumentOpen] = useState(false);
  const [selectedBase, setSelectedBase] = useState<KnowledgeBase | null>(null);
  const [searchResults, setSearchResults] = useState<KnowledgeSearchResult[]>([]);
  const [chunkPreviews, setChunkPreviews] = useState<KnowledgeChunkPreview[]>([]);

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const vectorStoresQuery = useQuery({
    queryKey: ['vector-store-configs'],
    queryFn: listVectorStoreConfigs
  });

  const bases = basesQuery.data?.items ?? [];
  const vectorStores = vectorStoresQuery.data?.items ?? [];
  const documentCount = useMemo(() => bases.reduce((sum, base) => sum + base.documentCount, 0), [bases]);
  const chunkCount = useMemo(() => bases.reduce((sum, base) => sum + base.chunkCount, 0), [bases]);

  const createMutation = useMutation({
    mutationFn: (values: SaveKnowledgeBaseRequest) => createKnowledgeBase({
      ...initialBaseValues,
      ...values,
      description: values.description || null,
      embeddingModelId: values.embeddingModelId || null,
      vectorStoreConfigId: values.vectorStoreConfigId || null
    }),
    onSuccess: async () => {
      message.success('知识库已保存');
      setCreateOpen(false);
      baseForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    }
  });

  const documentMutation = useMutation({
    mutationFn: (values: AddKnowledgeDocumentRequest) => {
      if (!selectedBase) {
        throw new Error('请选择知识库');
      }
      return addKnowledgeDocument(selectedBase.id, {
        ...initialDocumentValues,
        ...values
      });
    },
    onSuccess: async () => {
      message.success('文档已入库');
      documentForm.resetFields();
      setChunkPreviews([]);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    }
  });

  const previewMutation = useMutation({
    mutationFn: (values: AddKnowledgeDocumentRequest) => previewKnowledgeChunks({
      content: values.content,
      splitterType: values.splitterType || 'SIMPLE_TEXT',
      chunkSize: values.chunkSize || 500,
      chunkOverlap: values.chunkOverlap || 0
    }),
    onSuccess: setChunkPreviews
  });

  const searchMutation = useMutation({
    mutationFn: async (values: { query: string; topK: number }) => {
      if (!selectedBase) {
        throw new Error('请选择知识库');
      }
      return searchKnowledgeBase(selectedBase.id, values);
    },
    onSuccess: setSearchResults
  });

  function openCreateDrawer() {
    baseForm.setFieldsValue(initialBaseValues);
    setCreateOpen(true);
  }

  function openDocumentDrawer(base: KnowledgeBase) {
    setSelectedBase(base);
    setSearchResults([]);
    setChunkPreviews([]);
    documentForm.setFieldsValue({
      ...initialDocumentValues,
      splitterType: base.splitterType || 'SIMPLE_TEXT',
      chunkSize: base.chunkSize || 500,
      chunkOverlap: base.chunkOverlap ?? 50
    });
    searchForm.setFieldsValue({ query: '', topK: base.topK || 3 });
    setDocumentOpen(true);
  }

  const vectorStoreOptions = vectorStores.map((store) => ({
    value: store.id,
    label: `${store.name} · ${store.storeType}`
  }));

  const columns: ColumnsType<KnowledgeBase> = [
    {
      title: '知识库名称',
      dataIndex: 'name',
      render: (_, base) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{base.name}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{base.description || '暂无描述'}</Typography.Text>
        </Space>
      )
    },
    {
      title: '向量库配置',
      width: 180,
      render: (_, base) => <Tag color={base.vectorStoreConfigId ? 'geekblue' : 'default'}>{base.vectorStoreConfigId || '默认内存'}</Tag>
    },
    {
      title: '分段策略',
      width: 160,
      render: (_, base) => <Tag color="blue">{base.splitterType || 'SIMPLE_TEXT'}</Tag>
    },
    {
      title: '检索模式',
      width: 130,
      render: (_, base) => <Tag color={base.retrievalMode === 'HYBRID' ? 'purple' : 'green'}>{base.retrievalMode || 'KEYWORD'}</Tag>
    },
    {
      title: '文档/切片',
      width: 120,
      render: (_, base) => `${base.documentCount} / ${base.chunkCount}`
    },
    {
      title: '状态',
      width: 120,
      render: (_, base) => <Tag color="geekblue">{base.status === 'READY' || !base.status ? '工作流可引用' : base.status}</Tag>
    },
    {
      title: '操作',
      width: 120,
      render: (_, base) => (
        <Button size="small" icon={<FileAddOutlined />} onClick={() => openDocumentDrawer(base)}>
          管理文档
        </Button>
      )
    }
  ];

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={3} style={{ margin: 0 }}>知识库中心</Typography.Title>
          <Typography.Text type="secondary">管理文档集合、分段策略、向量库配置和工作流检索节点可引用的数据源。</Typography.Text>
        </Space>
        <Space>
          <Tag icon={<DatabaseOutlined />} color="blue">向量库配置</Tag>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>新增知识库</Button>
        </Space>
      </div>

      <div style={metricRowStyle}>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="知识库总数" value={bases.length} prefix={<BookOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="文档数" value={documentCount} prefix={<FileTextOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="切片数" value={chunkCount} prefix={<DatabaseOutlined />} />
        </Card>
      </div>

      {basesQuery.isError ? (
        <Alert
          type="error"
          showIcon
          message="知识库加载失败"
          description={(basesQuery.error as Error).message}
          style={{ marginBottom: 12 }}
        />
      ) : null}

      <Card
        variant="borderless"
        title={<Space><BookOutlined />知识库清单</Space>}
        extra={<Tag color="geekblue">工作流可引用</Tag>}
      >
        <Table
          rowKey="id"
          loading={basesQuery.isLoading}
          columns={columns}
          dataSource={bases}
          pagination={{ pageSize: 8, showSizeChanger: false }}
          size="middle"
        />
      </Card>

      <Drawer
        title="新增知识库"
        open={createOpen}
        width={620}
        onClose={() => setCreateOpen(false)}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setCreateOpen(false)}>取消</Button>
            <Button type="primary" icon={<CheckCircleOutlined />} loading={createMutation.isPending} onClick={() => baseForm.submit()}>
              保存
            </Button>
          </Space>
        )}
      >
        <Form form={baseForm} layout="vertical" initialValues={initialBaseValues} onFinish={(values) => createMutation.mutate(values)}>
          <Form.Item name="name" label="知识库名称" rules={[{ required: true, message: '请输入知识库名称' }]}>
            <Input placeholder="产品知识库" />
          </Form.Item>
          <Form.Item name="description" label="知识库描述">
            <Input placeholder="用于客服问答、产品手册或内部制度检索" />
          </Form.Item>
          <Form.Item name="embeddingModelId" label="嵌入模型ID">
            <Input placeholder="可填写已保存的 embedding 模型 ID" />
          </Form.Item>
          <Form.Item name="vectorStoreConfigId" label="向量库配置">
            <Select allowClear placeholder="默认使用内存向量库" options={vectorStoreOptions} />
          </Form.Item>
          <Form.Item label="分段策略" style={{ marginBottom: 0 }}>
            <Space align="start" wrap>
              <Form.Item name="splitterType" noStyle>
                <Select
                  style={{ width: 190 }}
                  options={[
                    { value: 'SIMPLE_TEXT', label: '普通文本' },
                    { value: 'MARKDOWN_HEADING', label: 'Markdown 标题' },
                    { value: 'REGEX', label: '正则分隔' }
                  ]}
                />
              </Form.Item>
              <Typography.Text type="secondary">大小</Typography.Text>
              <Form.Item name="chunkSize" noStyle>
                <InputNumber min={80} max={2000} />
              </Form.Item>
              <Typography.Text type="secondary">重叠</Typography.Text>
              <Form.Item name="chunkOverlap" noStyle>
                <InputNumber min={0} max={500} />
              </Form.Item>
            </Space>
          </Form.Item>
          <Form.Item label="检索设置" style={{ marginTop: 24, marginBottom: 0 }}>
            <Space align="start" wrap>
              <Form.Item name="retrievalMode" noStyle>
                <Select
                  style={{ width: 160 }}
                  options={[
                    { value: 'KEYWORD', label: '关键词' },
                    { value: 'VECTOR', label: '向量' },
                    { value: 'HYBRID', label: '混合检索' }
                  ]}
                />
              </Form.Item>
              <Typography.Text type="secondary">Top K</Typography.Text>
              <Form.Item name="topK" noStyle>
                <InputNumber min={1} max={20} />
              </Form.Item>
            </Space>
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={selectedBase ? `管理文档 - ${selectedBase.name}` : '管理文档'}
        open={documentOpen}
        width={840}
        onClose={() => setDocumentOpen(false)}
      >
        <Card size="small" title={<Space><FileAddOutlined />文档入库</Space>} style={{ marginBottom: 16 }}>
          <Form form={documentForm} layout="vertical" initialValues={initialDocumentValues} onFinish={(values) => documentMutation.mutate(values)}>
            <Form.Item name="name" label="文档名称" rules={[{ required: true, message: '请输入文档名称' }]}>
              <Input placeholder="faq.md" />
            </Form.Item>
            <Form.Item name="content" label="文档内容" rules={[{ required: true, message: '请输入文档内容' }]}>
              <Input.TextArea autoSize={{ minRows: 6, maxRows: 12 }} placeholder="粘贴文档内容，系统会按所选策略预览切片后入库。" />
            </Form.Item>
            <Space wrap>
              <Form.Item name="splitterType" label="分段策略">
                <Select
                  style={{ width: 190 }}
                  options={[
                    { value: 'SIMPLE_TEXT', label: '普通文本' },
                    { value: 'MARKDOWN_HEADING', label: 'Markdown 标题' },
                    { value: 'REGEX', label: '正则分隔' }
                  ]}
                />
              </Form.Item>
              <Form.Item name="chunkSize" label="切片大小">
                <InputNumber min={80} max={2000} />
              </Form.Item>
              <Form.Item name="chunkOverlap" label="重叠字符">
                <InputNumber min={0} max={500} />
              </Form.Item>
            </Space>
            <Space>
              <Button icon={<FileSearchOutlined />} loading={previewMutation.isPending} onClick={() => previewMutation.mutate(documentForm.getFieldsValue())}>
                预览切片
              </Button>
              <Button type="primary" icon={<FileAddOutlined />} loading={documentMutation.isPending} onClick={() => documentForm.submit()}>
                入库
              </Button>
            </Space>
          </Form>
        </Card>

        <Card size="small" title={<Space><FileTextOutlined />切片预览</Space>} style={{ marginBottom: 16 }}>
          <List
            dataSource={chunkPreviews}
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

        <Card size="small" title={<Space><SearchOutlined />检索测试</Space>}>
          <Form
            form={searchForm}
            layout="inline"
            initialValues={{ query: '', topK: selectedBase?.topK || 3 }}
            onFinish={(values) => searchMutation.mutate(values)}
            style={{ marginBottom: 16 }}
          >
            <Form.Item name="query" label="检索测试" rules={[{ required: true, message: '请输入检索内容' }]}>
              <Input style={{ width: 320 }} placeholder="输入用户问题或关键词" />
            </Form.Item>
            <Form.Item name="topK" label="Top K">
              <InputNumber min={1} max={10} />
            </Form.Item>
            <Button icon={<SearchOutlined />} loading={searchMutation.isPending} onClick={() => searchForm.submit()}>
              检索
            </Button>
          </Form>
          <List
            dataSource={searchResults}
            locale={{ emptyText: '暂无检索结果' }}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  title={<Space><Tag color="blue">{item.documentName}</Tag><Typography.Text type="secondary">score {item.score}</Typography.Text></Space>}
                  description={<Typography.Text>{item.content}</Typography.Text>}
                />
              </List.Item>
            )}
          />
        </Card>
      </Drawer>
    </section>
  );
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
