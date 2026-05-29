import {
  BookOutlined,
  CheckCircleOutlined,
  DatabaseOutlined,
  FileAddOutlined,
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
  searchKnowledgeBase,
  type AddKnowledgeDocumentRequest,
  type KnowledgeBase,
  type KnowledgeSearchResult,
  type SaveKnowledgeBaseRequest
} from '../../api/knowledge';

const initialBaseValues: SaveKnowledgeBaseRequest = {
  name: '',
  description: null
};

const initialDocumentValues: AddKnowledgeDocumentRequest = {
  name: '',
  content: ''
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

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const bases = basesQuery.data?.items ?? [];
  const documentCount = useMemo(() => bases.reduce((sum, base) => sum + base.documentCount, 0), [bases]);
  const chunkCount = useMemo(() => bases.reduce((sum, base) => sum + base.chunkCount, 0), [bases]);

  const createMutation = useMutation({
    mutationFn: (values: SaveKnowledgeBaseRequest) => createKnowledgeBase({
      name: values.name,
      description: values.description || null
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
      return addKnowledgeDocument(selectedBase.id, values);
    },
    onSuccess: async () => {
      message.success('文档已入库');
      documentForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    }
  });

  const searchMutation = useMutation({
    mutationFn: async (values: { query: string; topK: number }) => {
      if (!selectedBase) {
        throw new Error('请选择知识库');
      }
      return searchKnowledgeBase(selectedBase.id, values);
    },
    onSuccess: (results) => setSearchResults(results)
  });

  function openCreateDrawer() {
    baseForm.setFieldsValue(initialBaseValues);
    setCreateOpen(true);
  }

  function openDocumentDrawer(base: KnowledgeBase) {
    setSelectedBase(base);
    setSearchResults([]);
    documentForm.setFieldsValue(initialDocumentValues);
    searchForm.setFieldsValue({ query: '', topK: 3 });
    setDocumentOpen(true);
  }

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
      title: '文档数',
      dataIndex: 'documentCount',
      width: 120,
      render: (value: number) => <Tag color="blue">{value}</Tag>
    },
    {
      title: '切片数',
      dataIndex: 'chunkCount',
      width: 120,
      render: (value: number) => <Tag color="green">{value}</Tag>
    },
    {
      title: '状态',
      width: 140,
      render: () => <Tag color="geekblue">工作流可引用</Tag>
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
          <Typography.Text type="secondary">管理可被工作流知识库检索节点引用的文档集合、切片和检索测试。</Typography.Text>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>新增知识库</Button>
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
        width={520}
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
            <Input placeholder="请输入知识库用途说明" />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={selectedBase ? `管理文档 - ${selectedBase.name}` : '管理文档'}
        open={documentOpen}
        width={720}
        onClose={() => setDocumentOpen(false)}
      >
        <Card size="small" title={<Space><FileAddOutlined />文档入库</Space>} style={{ marginBottom: 16 }}>
          <Form form={documentForm} layout="vertical" initialValues={initialDocumentValues} onFinish={(values) => documentMutation.mutate(values)}>
            <Form.Item name="name" label="文档名称" rules={[{ required: true, message: '请输入文档名称' }]}>
              <Input placeholder="faq.txt" />
            </Form.Item>
            <Form.Item name="content" label="文档内容" rules={[{ required: true, message: '请输入文档内容' }]}>
              <Input.TextArea autoSize={{ minRows: 6, maxRows: 12 }} placeholder="粘贴第一版文档内容，系统会自动按段落和标点切片。" />
            </Form.Item>
            <Button type="primary" icon={<FileAddOutlined />} loading={documentMutation.isPending} onClick={() => documentForm.submit()}>
              入库
            </Button>
          </Form>
        </Card>

        <Card size="small" title={<Space><SearchOutlined />检索测试</Space>}>
          <Form
            form={searchForm}
            layout="inline"
            initialValues={{ query: '', topK: 3 }}
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
