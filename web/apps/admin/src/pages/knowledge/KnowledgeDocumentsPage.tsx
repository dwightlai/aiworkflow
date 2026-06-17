import {
  ArrowLeftOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  FileAddOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  FolderOutlined,
  PlusOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  List,
  Modal,
  Popconfirm,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type React from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import './knowledgeDatasetTree.css';
import {
  addManualKnowledgeSource,
  createKnowledgeDataset,
  deleteKnowledgeDataset,
  deleteKnowledgeDocument,
  listKnowledgeBases,
  listKnowledgeDatasets,
  listKnowledgeDocuments,
  retrieveKnowledge,
  updateKnowledgeDataset,
  type KnowledgeDataset,
  type KnowledgeDocument,
  type KnowledgeRetrievalItem
} from '../../api/knowledge';
import { navigateTo, readDatasetIdFromSearch, withDatasetQuery } from '../../navigation';

interface KnowledgeDocumentsPageProps {
  knowledgeBaseId: string;
}

export function KnowledgeDocumentsPage({ knowledgeBaseId }: KnowledgeDocumentsPageProps) {
  const queryClient = useQueryClient();
  const [selectedDatasetId, setSelectedDatasetId] = useState<string | null>(null);
  const [createDatasetOpen, setCreateDatasetOpen] = useState(false);
  const [editDatasetOpen, setEditDatasetOpen] = useState(false);
  const [editingDataset, setEditingDataset] = useState<KnowledgeDataset | null>(null);
  const [addTextOpen, setAddTextOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [topK, setTopK] = useState(5);
  const [retrievalResults, setRetrievalResults] = useState<KnowledgeRetrievalItem[]>([]);
  const [createDatasetForm] = Form.useForm<{ name: string; code?: string; topicId?: string; topicTitle?: string; description?: string }>();
  const [editDatasetForm] = Form.useForm<{ name: string; description?: string }>();
  const [addTextForm] = Form.useForm<{ title: string; content: string }>();

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const datasetsQuery = useQuery({
    queryKey: ['knowledge-datasets', knowledgeBaseId],
    queryFn: () => listKnowledgeDatasets(knowledgeBaseId)
  });
  const documentsQuery = useQuery({
    queryKey: ['knowledge-documents', knowledgeBaseId, selectedDatasetId],
    queryFn: () => listKnowledgeDocuments(knowledgeBaseId, selectedDatasetId ?? undefined),
    enabled: selectedDatasetId !== null
  });

  const knowledgeBase = useMemo(
    () => basesQuery.data?.items.find((base) => base.id === knowledgeBaseId) ?? null,
    [basesQuery.data?.items, knowledgeBaseId]
  );
  const datasets = datasetsQuery.data?.items ?? [];
  const documents = documentsQuery.data?.items ?? [];
  const selectedDataset = datasets.find((item) => item.id === selectedDatasetId) ?? null;
  const defaultDatasetId = resolveDefaultDatasetId(knowledgeBase, datasets);
  const chunkTotal = documents.reduce((sum, document) => sum + document.chunkCount, 0);
  const showDatasetTree = datasets.length > 0;
  const isSelectedDefaultDataset = selectedDataset ? isDefaultDataset(selectedDataset, defaultDatasetId) : false;

  useEffect(() => {
    if (datasets.length === 0) {
      return;
    }
    const fromUrl = readDatasetIdFromSearch();
    if (fromUrl && datasets.some((item) => item.id === fromUrl)) {
      setSelectedDatasetId((current) => (current === fromUrl ? current : fromUrl));
      return;
    }
    setSelectedDatasetId((current) => {
      if (current && datasets.some((item) => item.id === current)) {
        return current;
      }
      return defaultDatasetId ?? datasets[0].id;
    });
  }, [datasets, defaultDatasetId]);

  function selectDataset(datasetId: string) {
    setSelectedDatasetId(datasetId);
    window.history.replaceState(
      null,
      '',
      withDatasetQuery(`/knowledge/${knowledgeBaseId}/documents`, datasetId)
    );
  }

  function navigateToDocumentEdit(document: KnowledgeDocument) {
    const datasetId = selectedDatasetId ?? document.datasetId ?? defaultDatasetId;
    navigateTo(withDatasetQuery(
      `/knowledge/${knowledgeBaseId}/documents/${document.id}/edit`,
      datasetId
    ));
  }

  const editDatasetMutation = useMutation({
    mutationFn: (values: { name: string; description?: string }) =>
      updateKnowledgeDataset(editingDataset!.id, values),
    onSuccess: async () => {
      message.success('分类已更新');
      setEditDatasetOpen(false);
      setEditingDataset(null);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-datasets', knowledgeBaseId] });
    },
    onError: (error: Error) => message.error(error.message || '更新失败')
  });

  const deleteDatasetMutation = useMutation({
    mutationFn: (datasetId: string) => deleteKnowledgeDataset(datasetId),
    onSuccess: async () => {
      message.success('分类已删除');
      setSelectedDatasetId(defaultDatasetId);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-datasets', knowledgeBaseId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    },
    onError: (error: Error) => message.error(error.message || '删除失败')
  });

  const createDatasetMutation = useMutation({
    mutationFn: (values: { name: string; code?: string; topicId?: string; topicTitle?: string; description?: string }) =>
      createKnowledgeDataset(knowledgeBaseId, values),
    onSuccess: async (dataset) => {
      message.success('分类已创建');
      setCreateDatasetOpen(false);
      createDatasetForm.resetFields();
      setSelectedDatasetId(dataset.id);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-datasets', knowledgeBaseId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    },
    onError: (error: Error) => message.error(error.message || '创建失败')
  });

  const addTextMutation = useMutation({
    mutationFn: (values: { title: string; content: string }) =>
      addManualKnowledgeSource(selectedDatasetId!, values),
    onSuccess: async () => {
      message.success('资料已添加');
      setAddTextOpen(false);
      addTextForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId, selectedDatasetId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-datasets', knowledgeBaseId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    },
    onError: (error: Error) => message.error(error.message || '添加失败')
  });

  const deleteMutation = useMutation({
    mutationFn: (document: KnowledgeDocument) => deleteKnowledgeDocument(document.knowledgeBaseId, document.id),
    onSuccess: async () => {
      message.success('文档已删除');
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', knowledgeBaseId, selectedDatasetId] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-datasets', knowledgeBaseId] });
    }
  });

  const searchMutation = useMutation({
    mutationFn: () => retrieveKnowledge({
      knowledgeBaseId,
      datasetId: selectedDatasetId ?? undefined,
      query,
      topK
    }),
    onSuccess: (response) => setRetrievalResults(response.items),
    onError: (error: Error) => {
      message.error(error.message || '检索失败');
      setRetrievalResults([]);
    }
  });

  function navigateToCreate(type: 'manual' | 'text' | 'table') {
    const targetDatasetId = selectedDatasetId ?? defaultDatasetId;
    const datasetQuery = targetDatasetId ? `&datasetId=${encodeURIComponent(targetDatasetId)}` : '';
    navigateTo(`/knowledge/${knowledgeBaseId}/documents/new?type=${type}${datasetQuery}`);
  }

  function openEditDataset(dataset?: KnowledgeDataset) {
    const target = dataset ?? selectedDataset;
    if (!target || isDefaultDataset(target, defaultDatasetId)) {
      return;
    }
    setEditingDataset(target);
    editDatasetForm.setFieldsValue({
      name: target.name,
      description: target.description ?? ''
    });
    setEditDatasetOpen(true);
  }

  const handleDeleteDataset = useCallback((dataset: KnowledgeDataset) => {
    if (isDefaultDataset(dataset, defaultDatasetId)) {
      return;
    }
    deleteDatasetMutation.mutate(dataset.id);
  }, [defaultDatasetId, deleteDatasetMutation]);

  const sortedDatasets = useMemo(
    () => sortDatasets(datasets, defaultDatasetId),
    [datasets, defaultDatasetId]
  );

  const columns: ColumnsType<KnowledgeDocument> = [
    {
      title: '文档名称',
      dataIndex: 'name',
      render: (_, document) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{document.name}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {document.createdAt ? `创建于 ${document.createdAt}` : '已入库文档'}
          </Typography.Text>
        </Space>
      )
    },
    {
      title: '切片数',
      dataIndex: 'chunkCount',
      width: 120,
      render: (value) => <Tag color="blue">{value} 个切片</Tag>
    },
    {
      title: '状态',
      width: 140,
      render: (_, document) => (
        <Tag color={document.processingStatus === 'READY' || !document.processingStatus ? 'green' : 'orange'}>
          {document.processingStatus === 'READY' || !document.processingStatus ? '可检索' : document.processingStatus}
        </Tag>
      )
    },
    {
      title: '操作',
      width: 310,
      render: (_, document) => (
        <Space size={6} wrap>
          <Button
            size="small"
            icon={<FileSearchOutlined />}
            onClick={() => navigateToDocumentEdit(document)}
          >
            查看切片
          </Button>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => navigateToDocumentEdit(document)}
          >
            编辑
          </Button>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            loading={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate(document)}
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  const contentPanel = (
    <>
      <div style={metricRowStyle}>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="文档数" value={documents.length} prefix={<FileTextOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="切片数" value={chunkTotal} prefix={<FileSearchOutlined />} />
        </Card>
        <Card variant="borderless" style={metricCardStyle}>
          <Statistic title="默认 Top K" value={knowledgeBase?.topK ?? topK} prefix={<SearchOutlined />} />
        </Card>
      </div>

      {selectedDataset ? (
        <Space wrap style={{ marginBottom: 12 }}>
          {isSelectedDefaultDataset ? <Tag color="blue">默认分类</Tag> : null}
          <Tag>资料 {selectedDataset.sourceCount}</Tag>
          <Tag>文档 {selectedDataset.documentCount}</Tag>
          <Tag>分块 {selectedDataset.chunkCount}</Tag>
          <Tag color={selectedDataset.indexStatus === 'READY' ? 'success' : 'default'}>{selectedDataset.indexStatus}</Tag>
          {!isSelectedDefaultDataset ? (
            <>
              <Button size="small" icon={<EditOutlined />} onClick={() => openEditDataset()}>编辑分类</Button>
              <Popconfirm
                title="确定删除该分类？"
                description="分类下不能有资料。"
                okText="删除"
                cancelText="取消"
                okButtonProps={{ danger: true }}
                onConfirm={() => selectedDataset && handleDeleteDataset(selectedDataset)}
              >
                <Button
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  loading={deleteDatasetMutation.isPending}
                >
                  删除分类
                </Button>
              </Popconfirm>
            </>
          ) : (
            <Typography.Text type="secondary">默认分类不可编辑或删除</Typography.Text>
          )}
        </Space>
      ) : null}

      <Card variant="borderless" title={<Space><FileTextOutlined />{selectedDataset ? `${displayDatasetName(selectedDataset)} · 文档列表` : '文档列表'}</Space>}>
        <Table
          rowKey="id"
          loading={documentsQuery.isLoading || datasetsQuery.isLoading}
          columns={columns}
          dataSource={documents}
          pagination={{ pageSize: 8, showSizeChanger: false }}
        />
      </Card>

      <Card variant="borderless" title={<Space><SearchOutlined />检索测试</Space>} style={{ marginTop: 16 }}>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            style={{ width: 360 }}
            placeholder="输入用户问题或关键词"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <InputNumber min={1} max={20} value={topK} onChange={(value) => setTopK(value ?? 5)} />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            loading={searchMutation.isPending}
            disabled={!query.trim()}
            onClick={() => searchMutation.mutate()}
          >
            检索
          </Button>
        </Space>
        <List
          dataSource={retrievalResults}
          locale={{ emptyText: '暂无检索结果' }}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={(
                  <Space>
                    <Tag color="blue">{item.sourceTitle ?? item.chunkId}</Tag>
                    <Typography.Text type="secondary">score {item.score}</Typography.Text>
                  </Space>
                )}
                description={<Typography.Text>{item.content}</Typography.Text>}
              />
            </List.Item>
          )}
        />
      </Card>
    </>
  );

  return (
    <section style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={8}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigateTo('/knowledge')} style={{ width: 'fit-content' }}>
            返回知识库
          </Button>
          <Space wrap>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {knowledgeBase?.name ?? '知识库资料'}
            </Typography.Title>
            {knowledgeBase ? <Tag color="geekblue">{knowledgeBase.vectorDimension ?? 1536} 维</Tag> : null}
          </Space>
          <Typography.Text type="secondary">
            左侧选择分类，右侧维护文档与切片；可随时新建分类。
          </Typography.Text>
        </Space>
        <Space wrap>
          <Button icon={<PlusOutlined />} onClick={() => setCreateDatasetOpen(true)}>
            新建分类
          </Button>
          <Button icon={<FileAddOutlined />} onClick={() => navigateToCreate('manual')}>
            手动数据集
          </Button>
          <Button type="primary" icon={<FileTextOutlined />} onClick={() => navigateToCreate('text')}>
            文本文档
          </Button>
          <Button icon={<DatabaseOutlined />} onClick={() => navigateToCreate('table')}>
            表格文档
          </Button>
          <Button onClick={() => setAddTextOpen(true)} disabled={!selectedDatasetId}>
            添加文本资料
          </Button>
        </Space>
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

      <div style={showDatasetTree ? bodyLayoutStyle : undefined}>
        {showDatasetTree ? (
          <aside style={treePanelStyle}>
            <div style={treeHeaderStyle}>
              <Typography.Text strong>分类</Typography.Text>
              <Button type="text" size="small" icon={<PlusOutlined />} onClick={() => setCreateDatasetOpen(true)} />
            </div>
            <div className="knowledge-dataset-tree">
              {sortedDatasets.map((dataset) => {
                const label = displayDatasetName(dataset);
                const selected = dataset.id === selectedDatasetId;
                const mutable = !isDefaultDataset(dataset, defaultDatasetId);
                return (
                  <div
                    key={dataset.id}
                    className={`knowledge-dataset-tree__item${selected ? ' is-selected' : ''}`}
                    onClick={() => selectDataset(dataset.id)}
                  >
                    <FolderOutlined className="knowledge-dataset-tree__icon" />
                    <Tooltip title={label} mouseEnterDelay={0.3}>
                      <span className="knowledge-dataset-tree__label">{label}</span>
                    </Tooltip>
                    {mutable ? (
                      <div className="knowledge-dataset-tree__actions" onClick={(event) => event.stopPropagation()}>
                        <Button
                          type="text"
                          size="small"
                          icon={<EditOutlined />}
                          aria-label="编辑分类"
                          onClick={() => openEditDataset(dataset)}
                        />
                        <Popconfirm
                          title="确定删除该分类？"
                          description="分类下不能有资料。"
                          okText="删除"
                          cancelText="取消"
                          okButtonProps={{ danger: true }}
                          onConfirm={() => handleDeleteDataset(dataset)}
                        >
                          <Button
                            type="text"
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                            aria-label="删除分类"
                            loading={deleteDatasetMutation.isPending}
                          />
                        </Popconfirm>
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
          </aside>
        ) : null}
        <div style={showDatasetTree ? contentAreaStyle : undefined}>{contentPanel}</div>
      </div>

      <Modal
        title="新建分类"
        open={createDatasetOpen}
        onCancel={() => setCreateDatasetOpen(false)}
        onOk={() => createDatasetForm.submit()}
        confirmLoading={createDatasetMutation.isPending}
        destroyOnClose
      >
        <Form form={createDatasetForm} layout="vertical" onFinish={(values) => createDatasetMutation.mutate(values)}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="编码">
            <Input />
          </Form.Item>
          <Form.Item name="topicId" label="专题ID">
            <Input placeholder="对接数字档案专题时使用" />
          </Form.Item>
          <Form.Item name="topicTitle" label="专题标题">
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="编辑分类"
        open={editDatasetOpen}
        onCancel={() => {
          setEditDatasetOpen(false);
          setEditingDataset(null);
        }}
        onOk={() => editDatasetForm.submit()}
        confirmLoading={editDatasetMutation.isPending}
        destroyOnClose
      >
        <Form form={editDatasetForm} layout="vertical" onFinish={(values) => editDatasetMutation.mutate(values)}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="添加文本资料"
        open={addTextOpen}
        onCancel={() => setAddTextOpen(false)}
        onOk={() => addTextForm.submit()}
        confirmLoading={addTextMutation.isPending}
        destroyOnClose
      >
        <Form form={addTextForm} layout="vertical" onFinish={(values) => addTextMutation.mutate(values)}>
          <Form.Item label="所属分类">
            <Select value={selectedDatasetId ?? undefined} disabled options={datasets.map((d) => ({ value: d.id, label: displayDatasetName(d) }))} />
          </Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="content" label="内容" rules={[{ required: true, message: '请输入内容' }]}>
            <Input.TextArea rows={8} />
          </Form.Item>
        </Form>
      </Modal>
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

const bodyLayoutStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: '280px minmax(0, 1fr)'
};

const treePanelStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  minHeight: 520,
  overflow: 'hidden',
  padding: '12px 8px'
};

const treeHeaderStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  justifyContent: 'space-between',
  marginBottom: 8,
  padding: '0 8px'
};

const contentAreaStyle: React.CSSProperties = {
  minWidth: 0
};

function displayDatasetName(dataset: KnowledgeDataset) {
  if (isDefaultDatasetType(dataset) || dataset.name === '默认数据集') {
    return '默认分类';
  }
  return dataset.name;
}

function isDefaultDatasetType(dataset: KnowledgeDataset) {
  return dataset.datasetType === 'DEFAULT';
}

function isDefaultDataset(dataset: KnowledgeDataset, defaultDatasetId?: string | null) {
  return isDefaultDatasetType(dataset) || (!!defaultDatasetId && dataset.id === defaultDatasetId);
}

function compareDatasetByCode(left: KnowledgeDataset, right: KnowledgeDataset) {
  const leftCode = left.code?.trim() ?? '';
  const rightCode = right.code?.trim() ?? '';
  if (!leftCode && !rightCode) {
    return left.name.localeCompare(right.name, 'zh-CN');
  }
  if (!leftCode) {
    return 1;
  }
  if (!rightCode) {
    return -1;
  }
  return leftCode.localeCompare(rightCode, 'zh-CN', { numeric: true, sensitivity: 'base' });
}

function resolveDefaultDatasetId(
  knowledgeBase: { defaultDatasetId?: string | null } | null,
  datasets: KnowledgeDataset[]
) {
  if (knowledgeBase?.defaultDatasetId && datasets.some((item) => item.id === knowledgeBase.defaultDatasetId)) {
    return knowledgeBase.defaultDatasetId;
  }
  return datasets.find((item) => item.datasetType === 'DEFAULT')?.id ?? null;
}

function sortDatasets(datasets: KnowledgeDataset[], defaultDatasetId?: string | null) {
  return [...datasets].sort((left, right) => {
    const leftDefault = isDefaultDataset(left, defaultDatasetId);
    const rightDefault = isDefaultDataset(right, defaultDatasetId);
    if (leftDefault !== rightDefault) {
      return leftDefault ? -1 : 1;
    }
    return compareDatasetByCode(left, right);
  });
}
