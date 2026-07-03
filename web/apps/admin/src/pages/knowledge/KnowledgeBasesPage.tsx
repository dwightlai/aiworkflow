import {
  BookOutlined,
  CheckCircleOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  FileAddOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  PlusOutlined,
  SearchOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Dropdown,
  Drawer,
  Form,
  Input,
  InputNumber,
  List,
  Radio,
  Select,
  Space,
  Steps,
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
  addManualKnowledgeDataset,
  addKnowledgeDocument,
  createKnowledgeBase,
  createVectorStoreConfig,
  deleteKnowledgeBase,
  deleteKnowledgeDocument,
  deleteVectorStoreConfig,
  listKnowledgeBases,
  listKnowledgeDocumentChunks,
  listKnowledgeDocuments,
  listVectorStoreConfigs,
  previewUploadedTableKnowledgeDocumentFile,
  previewUploadedTextKnowledgeDocumentFile,
  previewUploadedKnowledgeDocumentFile,
  previewKnowledgeChunks,
  reparseKnowledgeDocument,
  searchKnowledgeBase,
  testSavedVectorStoreConnection,
  testVectorStoreConnection,
  updateKnowledgeBase,
  updateKnowledgeChunk,
  updateVectorStoreConfig,
  uploadTableKnowledgeDocumentFile,
  uploadTextKnowledgeDocumentFile,
  uploadKnowledgeDocumentFile,
  type AddKnowledgeDocumentRequest,
  type KnowledgeSplitOptions,
  type ManualDatasetEntryRequest,
  type KnowledgeBase,
  type KnowledgeChunk,
  type KnowledgeChunkPreview,
  type KnowledgeDocument,
  type KnowledgeSearchResult,
  type SaveKnowledgeBaseRequest,
  type SaveVectorStoreConfigRequest,
  type UploadedDocumentPreview,
  type VectorStoreConfig
} from '../../api/knowledge';
import { listModelProviders } from '../../api/models';
import { navigateTo } from '../../navigation';

const initialBaseValues: SaveKnowledgeBaseRequest = {
  name: '',
  description: null,
  embeddingModelId: null,
  vectorStoreConfigId: null,
  vectorDimension: 1536,
  splitterType: 'SIMPLE_TEXT',
  chunkSize: 500,
  chunkOverlap: 50,
  semanticSimilarityThreshold: 0.78,
  retrievalMode: 'HYBRID',
  topK: 3
};

const initialDocumentValues: AddKnowledgeDocumentRequest = {
  name: '',
  content: '',
  splitterType: 'SIMPLE_TEXT',
  chunkSize: 500,
  chunkOverlap: 50,
  semanticSimilarityThreshold: 0.78
};

const initialVectorValues: SaveVectorStoreConfigRequest = {
  name: '',
  storeType: 'MEMORY',
  endpoint: '',
  indexName: 'aiworkflow_kb',
  host: '',
  port: null,
  databaseName: '',
  namespaceName: 'aiworkflow_kb',
  vectorDimension: 1536,
  sslEnabled: false,
  optionsJson: '{}',
  username: null,
  password: null,
  apiKey: null,
  connectTimeoutMs: 5000,
  readTimeoutMs: 30000,
  enabled: true
};

const supportedUploadTypes = '.txt,.doc,.docx,.pdf,.md,.markdown,.html,.htm,.ppt,.pptx,.xls,.xlsx';
const textDocumentTypes = '.txt,.md,.markdown,.doc,.docx,.pdf,.html,.htm';
const tableDocumentTypes = '.xls,.xlsx';

interface ManualDatasetFormValues {
  entries: ManualDatasetEntryRequest[];
}

export function KnowledgeBasesPage() {
  const [baseForm] = Form.useForm<SaveKnowledgeBaseRequest>();
  const [documentForm] = Form.useForm<AddKnowledgeDocumentRequest>();
  const [manualDatasetForm] = Form.useForm<ManualDatasetFormValues>();
  const [textDocumentForm] = Form.useForm<KnowledgeSplitOptions>();
  const [tableDocumentForm] = Form.useForm<KnowledgeSplitOptions>();
  const [searchForm] = Form.useForm<{ query: string; topK: number }>();
  const [vectorForm] = Form.useForm<SaveVectorStoreConfigRequest>();
  const vectorStoreType = Form.useWatch('storeType', vectorForm);
  const documentSplitterType = Form.useWatch('splitterType', documentForm);
  const textSplitterType = Form.useWatch('splitterType', textDocumentForm);
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [documentOpen, setDocumentOpen] = useState(false);
  const [vectorOpen, setVectorOpen] = useState(false);
  const [dataDrawerType, setDataDrawerType] = useState<'manual' | 'text' | 'table' | null>(null);
  const [editingBase, setEditingBase] = useState<KnowledgeBase | null>(null);
  const [selectedBase, setSelectedBase] = useState<KnowledgeBase | null>(null);
  const [selectedDocument, setSelectedDocument] = useState<KnowledgeDocument | null>(null);
  const [editingChunk, setEditingChunk] = useState<KnowledgeChunk | null>(null);
  const [editingChunkContent, setEditingChunkContent] = useState('');
  const [editingVectorStore, setEditingVectorStore] = useState<VectorStoreConfig | null>(null);
  const [searchResults, setSearchResults] = useState<KnowledgeSearchResult[]>([]);
  const [chunkPreviews, setChunkPreviews] = useState<KnowledgeChunkPreview[]>([]);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadPreview, setUploadPreview] = useState<UploadedDocumentPreview | null>(null);
  const [uploadStep, setUploadStep] = useState(0);
  const [textUploadFile, setTextUploadFile] = useState<File | null>(null);
  const [textUploadPreview, setTextUploadPreview] = useState<UploadedDocumentPreview | null>(null);
  const [tableUploadFile, setTableUploadFile] = useState<File | null>(null);
  const [tableUploadPreview, setTableUploadPreview] = useState<UploadedDocumentPreview | null>(null);

  const basesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
  });
  const vectorStoresQuery = useQuery({
    queryKey: ['vector-store-configs'],
    queryFn: listVectorStoreConfigs
  });
  const modelProvidersQuery = useQuery({
    queryKey: ['model-providers'],
    queryFn: listModelProviders
  });
  const documentsQuery = useQuery({
    queryKey: ['knowledge-documents', selectedBase?.id],
    queryFn: () => listKnowledgeDocuments(selectedBase!.id),
    enabled: Boolean(selectedBase)
  });
  const chunksQuery = useQuery({
    queryKey: ['knowledge-chunks', selectedBase?.id, selectedDocument?.id],
    queryFn: () => listKnowledgeDocumentChunks(selectedBase!.id, selectedDocument!.id),
    enabled: Boolean(selectedBase && selectedDocument)
  });

  const bases = basesQuery.data?.items ?? [];
  const vectorStores = vectorStoresQuery.data?.items ?? [];
  const modelProviders = modelProvidersQuery.data?.items ?? [];
  const documents = documentsQuery.data?.items ?? [];
  const chunks = chunksQuery.data?.items ?? [];
  const documentCount = useMemo(() => bases.reduce((sum, base) => sum + base.documentCount, 0), [bases]);
  const chunkCount = useMemo(() => bases.reduce((sum, base) => sum + base.chunkCount, 0), [bases]);

  const saveBaseMutation = useMutation({
    mutationFn: (values: SaveKnowledgeBaseRequest) => {
      const request = {
        ...initialBaseValues,
        ...values,
        description: values.description || null,
        embeddingModelId: values.embeddingModelId || null,
        vectorStoreConfigId: values.vectorStoreConfigId || null,
        kbType: 'NORMAL',
        datasetMode: 'MULTI'
      };
      return editingBase ? updateKnowledgeBase(editingBase.id, request) : createKnowledgeBase(request);
    },
    onSuccess: async () => {
      message.success(editingBase ? '知识库已修改' : '知识库已保存');
      setCreateOpen(false);
      setEditingBase(null);
      baseForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    }
  });

  const deleteBaseMutation = useMutation({
    mutationFn: (base: KnowledgeBase) => deleteKnowledgeBase(base.id),
    onSuccess: async () => {
      message.success('知识库已删除');
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
    }
  });

  const documentMutation = useMutation({
    mutationFn: (values: AddKnowledgeDocumentRequest) => {
      if (!selectedBase) {
        throw new Error('请选择知识库');
      }
      return addKnowledgeDocument(selectedBase.id, { ...initialDocumentValues, ...values });
    },
    onSuccess: async () => {
      message.success('文档已入库');
      documentForm.resetFields();
      setChunkPreviews([]);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', selectedBase?.id] });
    }
  });

  const deleteDocumentMutation = useMutation({
    mutationFn: (document: KnowledgeDocument) => deleteKnowledgeDocument(document.knowledgeBaseId, document.id),
    onSuccess: async () => {
      message.success('文档已删除');
      setSelectedDocument(null);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', selectedBase?.id] });
    }
  });

  const updateChunkMutation = useMutation({
    mutationFn: (payload: { chunk: KnowledgeChunk; content: string; enabled: boolean }) => updateKnowledgeChunk(payload.chunk.knowledgeBaseId, payload.chunk.id, {
      content: payload.content,
      enabled: payload.enabled
    }),
    onSuccess: async () => {
      message.success('切片已更新');
      setEditingChunk(null);
      setEditingChunkContent('');
      await queryClient.invalidateQueries({ queryKey: ['knowledge-chunks', selectedBase?.id, selectedDocument?.id] });
    }
  });

  const vectorMutation = useMutation({
    mutationFn: (values: SaveVectorStoreConfigRequest) => {
      const request = {
        ...values,
        endpoint: values.endpoint || '',
        username: values.username || null,
        password: values.password || null,
        apiKey: values.apiKey || null,
        connectTimeoutMs: values.connectTimeoutMs || 5000,
        readTimeoutMs: values.readTimeoutMs || 30000
      };
      return editingVectorStore
        ? updateVectorStoreConfig(editingVectorStore.id, request)
        : createVectorStoreConfig(request);
    },
    onSuccess: async () => {
      message.success('向量库配置已保存');
      setEditingVectorStore(null);
      vectorForm.setFieldsValue(initialVectorValues);
      await queryClient.invalidateQueries({ queryKey: ['vector-store-configs'] });
    }
  });

  const deleteVectorMutation = useMutation({
    mutationFn: (store: VectorStoreConfig) => deleteVectorStoreConfig(store.id),
    onSuccess: async () => {
      message.success('向量库配置已删除');
      setEditingVectorStore(null);
      vectorForm.setFieldsValue(initialVectorValues);
      await queryClient.invalidateQueries({ queryKey: ['vector-store-configs'] });
    }
  });

  const testVectorMutation = useMutation({
    mutationFn: (values: SaveVectorStoreConfigRequest) => testVectorStoreConnection({
      ...initialVectorValues,
      ...values,
      endpoint: values.endpoint || '',
      host: values.host || null,
      username: values.username || null,
      password: values.password || null,
      apiKey: values.apiKey || null
    }),
    onSuccess: (result) => {
      message.success(`连接成功，耗时 ${result.latencyMs} ms`);
    },
    onError: (error: Error) => {
      message.error(`连接失败：${error.message}`);
    }
  });

  const testSavedVectorMutation = useMutation({
    mutationFn: (store: VectorStoreConfig) => testSavedVectorStoreConnection(store.id),
    onSuccess: (result) => {
      message.success(`连接成功，耗时 ${result.latencyMs} ms`);
    },
    onError: (error: Error) => {
      message.error(`连接失败：${error.message}`);
    }
  });

  const previewMutation = useMutation({
    mutationFn: (values: AddKnowledgeDocumentRequest) => previewKnowledgeChunks({
      content: values.content,
      splitterType: values.splitterType || 'SIMPLE_TEXT',
      chunkSize: values.chunkSize || 500,
      chunkOverlap: values.chunkOverlap || 0,
      semanticSimilarityThreshold: values.semanticSimilarityThreshold
    }),
    onSuccess: setChunkPreviews
  });

  const uploadPreviewMutation = useMutation({
    mutationFn: async () => {
      if (!uploadFile) {
        throw new Error('请先选择文件');
      }
      const values = documentForm.getFieldsValue();
      return previewUploadedKnowledgeDocumentFile(uploadFile, {
        splitterType: values.splitterType || 'SIMPLE_TEXT',
        chunkSize: values.chunkSize || 500,
        chunkOverlap: values.chunkOverlap ?? 0,
        semanticSimilarityThreshold: values.semanticSimilarityThreshold,
        knowledgeBaseId: selectedBase?.id
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
      if (!selectedBase || !uploadFile) {
        throw new Error('请先选择知识库和文件');
      }
      const values = documentForm.getFieldsValue();
      return uploadKnowledgeDocumentFile(selectedBase.id, uploadFile, {
        splitterType: values.splitterType || 'SIMPLE_TEXT',
        chunkSize: values.chunkSize || 500,
        chunkOverlap: values.chunkOverlap ?? 0,
        semanticSimilarityThreshold: values.semanticSimilarityThreshold
      });
    },
    onSuccess: async () => {
      message.success('文件已抽取并入库');
      setUploadFile(null);
      setUploadPreview(null);
      setUploadStep(2);
      setChunkPreviews([]);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', selectedBase?.id] });
    }
  });

  const manualDatasetMutation = useMutation({
    mutationFn: async (values: ManualDatasetFormValues) => {
      if (!selectedBase) {
        throw new Error('请选择知识库');
      }
      const entries = (values.entries ?? []).map((entry) => ({
        title: entry.title,
        content: entry.content,
        tags: entry.tags || null,
        category: entry.category || null,
        source: entry.source || null
      }));
      return addManualKnowledgeDataset(selectedBase.id, { entries });
    },
    onSuccess: async () => {
      message.success('手动数据集已入库');
      setDataDrawerType(null);
      manualDatasetForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', selectedBase?.id] });
    }
  });

  const textUploadPreviewMutation = useMutation({
    mutationFn: async () => {
      if (!textUploadFile) {
        throw new Error('请先选择文本文档');
      }
      return previewUploadedTextKnowledgeDocumentFile(textUploadFile, {
        ...textDocumentForm.getFieldsValue(),
        knowledgeBaseId: selectedBase?.id
      });
    },
    onSuccess: (preview) => {
      setTextUploadPreview(preview);
      setChunkPreviews(preview.chunks);
    }
  });

  const textUploadMutation = useMutation({
    mutationFn: async () => {
      if (!selectedBase || !textUploadFile) {
        throw new Error('请先选择知识库和文本文档');
      }
      return uploadTextKnowledgeDocumentFile(selectedBase.id, textUploadFile, textDocumentForm.getFieldsValue());
    },
    onSuccess: async () => {
      message.success('文本文档已解析并入库');
      setDataDrawerType(null);
      setTextUploadFile(null);
      setTextUploadPreview(null);
      setChunkPreviews([]);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', selectedBase?.id] });
    }
  });

  const tableUploadPreviewMutation = useMutation({
    mutationFn: async () => {
      if (!tableUploadFile) {
        throw new Error('请先选择表格文档');
      }
      return previewUploadedTableKnowledgeDocumentFile(tableUploadFile, {
        splitterType: 'STRUCTURED',
        chunkSize: tableDocumentForm.getFieldValue('chunkSize') || 200
      });
    },
    onSuccess: (preview) => {
      setTableUploadPreview(preview);
      setChunkPreviews(preview.chunks);
    }
  });

  const tableUploadMutation = useMutation({
    mutationFn: async () => {
      if (!selectedBase || !tableUploadFile) {
        throw new Error('请先选择知识库和表格文档');
      }
      return uploadTableKnowledgeDocumentFile(selectedBase.id, tableUploadFile, {
        splitterType: 'STRUCTURED',
        chunkSize: tableDocumentForm.getFieldValue('chunkSize') || 200
      });
    },
    onSuccess: async () => {
      message.success('表格文档已解析并入库');
      setDataDrawerType(null);
      setTableUploadFile(null);
      setTableUploadPreview(null);
      setChunkPreviews([]);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', selectedBase?.id] });
    }
  });

  const reparseDocumentMutation = useMutation({
    mutationFn: async (document: KnowledgeDocument) => reparseKnowledgeDocument(document.knowledgeBaseId, document.id),
    onSuccess: async () => {
      message.success('文档已重新解析');
      setSelectedDocument(null);
      await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      await queryClient.invalidateQueries({ queryKey: ['knowledge-documents', selectedBase?.id] });
    }
  });

  const searchMutation = useMutation({
    mutationFn: async (values: { query: string; topK: number }) => {
      if (!selectedBase) {
        throw new Error('请选择知识库');
      }
      return searchKnowledgeBase(selectedBase.id, values);
    },
    onSuccess: setSearchResults,
    onError: (error) => {
      message.error((error as Error).message);
      setSearchResults([]);
    }
  });

  function openCreateDrawer() {
    setEditingBase(null);
    baseForm.setFieldsValue(initialBaseValues);
    setCreateOpen(true);
  }

  function openEditDrawer(base: KnowledgeBase) {
    setEditingBase(base);
    baseForm.setFieldsValue({
      name: base.name,
      description: base.description || null,
      ownerUnitId: base.ownerUnitId || null,
      embeddingModelId: base.embeddingModelId || null,
      vectorStoreConfigId: base.vectorStoreConfigId || null,
      vectorDimension: base.vectorDimension || 1536,
      splitterType: base.splitterType || 'SIMPLE_TEXT',
      chunkSize: base.chunkSize || 500,
      chunkOverlap: base.chunkOverlap ?? 50,
      semanticSimilarityThreshold: base.semanticSimilarityThreshold ?? 0.78,
      retrievalMode: base.retrievalMode || 'HYBRID',
      topK: base.topK || 3
    });
    setCreateOpen(true);
  }

  function openDocumentDrawer(base: KnowledgeBase) {
    setSelectedBase(base);
    setSelectedDocument(null);
    setEditingChunk(null);
    setEditingChunkContent('');
    setSearchResults([]);
    setChunkPreviews([]);
    setUploadFile(null);
    setUploadPreview(null);
    setUploadStep(0);
    setDataDrawerType(null);
    setTextUploadFile(null);
    setTextUploadPreview(null);
    setTableUploadFile(null);
    setTableUploadPreview(null);
    manualDatasetForm.setFieldsValue({
      entries: [{ title: '', content: '', tags: '', category: '', source: '' }]
    });
    textDocumentForm.setFieldsValue({
      splitterType: 'FIXED_LENGTH',
      chunkSize: 200,
      semanticSimilarityThreshold: base.semanticSimilarityThreshold ?? 0.78,
      separator: ''
    });
    tableDocumentForm.setFieldsValue({ splitterType: 'STRUCTURED', chunkSize: 200 });
    documentForm.setFieldsValue({
      ...initialDocumentValues,
      splitterType: base.splitterType || 'SIMPLE_TEXT',
      chunkSize: base.chunkSize || 500,
      chunkOverlap: base.chunkOverlap ?? 50,
      semanticSimilarityThreshold: base.semanticSimilarityThreshold ?? 0.78
    });
    searchForm.setFieldsValue({ query: '', topK: 3 });
    setDocumentOpen(true);
  }

  function openDataDrawer(type: 'manual' | 'text' | 'table') {
    setChunkPreviews([]);
    setDataDrawerType(type);
    if (type === 'manual') {
      manualDatasetForm.setFieldsValue({
        entries: [{ title: '', content: '', tags: '', category: '', source: '' }]
      });
    }
    if (type === 'text') {
      setTextUploadFile(null);
      setTextUploadPreview(null);
      textDocumentForm.setFieldsValue({
        splitterType: 'FIXED_LENGTH',
        chunkSize: 200,
        semanticSimilarityThreshold: selectedBase?.semanticSimilarityThreshold ?? 0.78,
        separator: ''
      });
    }
    if (type === 'table') {
      setTableUploadFile(null);
      setTableUploadPreview(null);
      tableDocumentForm.setFieldsValue({ splitterType: 'STRUCTURED', chunkSize: 200 });
    }
  }

  function openChunkEditor(chunk: KnowledgeChunk) {
    setEditingChunk(chunk);
    setEditingChunkContent(chunk.content);
  }

  function openVectorDrawer() {
    setEditingVectorStore(null);
    vectorForm.setFieldsValue(initialVectorValues);
    setVectorOpen(true);
  }

  function startCreateVectorStore() {
    setEditingVectorStore(null);
    vectorForm.setFieldsValue(initialVectorValues);
  }

  function startEditVectorStore(store: VectorStoreConfig) {
    setEditingVectorStore(store);
    vectorForm.setFieldsValue({
      name: store.name,
      storeType: store.storeType,
      endpoint: store.endpoint || '',
      indexName: store.indexName,
      host: store.host || '',
      port: store.port || null,
      databaseName: store.databaseName || '',
      namespaceName: store.namespaceName || store.indexName,
      vectorDimension: store.vectorDimension || 1536,
      sslEnabled: store.sslEnabled || false,
      optionsJson: store.optionsJson || '{}',
      username: store.username || null,
      password: null,
      apiKey: null,
      connectTimeoutMs: store.connectTimeoutMs || 5000,
      readTimeoutMs: store.readTimeoutMs || 30000,
      enabled: store.enabled
    });
  }

  const isElasticsearchVectorStore = vectorStoreType === 'ELASTICSEARCH';
  const isMilvusVectorStore = vectorStoreType === 'MILVUS';
  const isPgvectorVectorStore = vectorStoreType === 'PGVECTOR';
  const isExternalVectorStore = isElasticsearchVectorStore || isMilvusVectorStore || isPgvectorVectorStore;

  const vectorStoreOptions = vectorStores.map((store) => ({
    value: store.id,
    label: `${store.name} · ${store.storeType}`
  }));
  const vectorStoreNameMap = useMemo(
    () => new Map(vectorStores.map((store) => [store.id, store.name])),
    [vectorStores]
  );
  const embeddingModelOptions = modelProviders
    .filter((provider) => provider.enabled && provider.modelUsage === 'EMBEDDING')
    .map((provider) => ({
      value: provider.id,
      label: `${provider.name} / ${provider.model}`
    }));

  const columns: ColumnsType<KnowledgeBase> = [
    {
      title: '知识库名称',
      dataIndex: 'name',
      render: (_, base) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{base.name}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{base.description || '暂无描述'}</Typography.Text>
          <Tag color="purple">向量维度 {base.vectorDimension || 1536}</Tag>
        </Space>
      )
    },
    {
      title: '分类',
      width: 90,
      render: (_, base) => base.datasetCount ?? 0
    },
    {
      title: '向量库配置',
      width: 180,
      render: (_, base) => {
        const label = base.vectorStoreConfigId
          ? vectorStoreNameMap.get(base.vectorStoreConfigId) ?? '未知配置'
          : '默认内存';
        return <Tag color={base.vectorStoreConfigId ? 'geekblue' : 'default'}>{label}</Tag>;
      }
    },
    {
      title: '向量维度',
      width: 110,
      render: (_, base) => <Tag color="purple">{base.vectorDimension || 1536} 维</Tag>
    },
    {
      title: '分段策略',
      width: 160,
      render: (_, base) => <Tag color="blue">{base.splitterType || 'SIMPLE_TEXT'}</Tag>
    },
    {
      title: '检索模式',
      width: 130,
      render: (_, base) => <Tag color={base.retrievalMode === 'HYBRID' || !base.retrievalMode ? 'purple' : 'green'}>{base.retrievalMode || 'HYBRID'}</Tag>
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
      width: 360,
      render: (_, base) => (
        <div style={{ display: 'flex', gap: 6, flexWrap: 'nowrap' }}>
          <Button size="small" icon={<EditOutlined />} aria-label="编辑知识库" onClick={() => openEditDrawer(base)}>
            编辑
          </Button>
          <Button
            size="small"
            icon={<FileAddOutlined />}
            onClick={() => navigateTo(`/knowledge/${base.id}/documents`)}
          >
            管理资料
          </Button>
          <Button size="small" danger icon={<DeleteOutlined />} aria-label="删除知识库" onClick={() => deleteBaseMutation.mutate(base)}>
            删除
          </Button>
        </div>
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
          <Button icon={<DatabaseOutlined />} onClick={openVectorDrawer}>向量库配置</Button>
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
        title="向量库配置"
        open={vectorOpen}
        width={760}
        onClose={() => setVectorOpen(false)}
      >
        <Card
          size="small"
          title={<Space><DatabaseOutlined />配置清单</Space>}
          extra={<Button size="small" icon={<PlusOutlined />} onClick={startCreateVectorStore}>新增向量库</Button>}
          style={{ marginBottom: 16 }}
        >
          <List
            dataSource={vectorStores}
            locale={{ emptyText: '暂无向量库配置' }}
            renderItem={(store) => (
              <List.Item
                actions={[
                  <Button
                    key="test"
                    size="small"
                    icon={<CheckCircleOutlined />}
                    loading={testSavedVectorMutation.isPending}
                    onClick={() => testSavedVectorMutation.mutate(store)}
                  >
                    测试
                  </Button>,
                  <Button key="edit" size="small" icon={<EditOutlined />} aria-label="编辑向量库" onClick={() => startEditVectorStore(store)}>编辑</Button>,
                  <Button
                    key="delete"
                    danger
                    size="small"
                    icon={<DeleteOutlined />}
                    aria-label="删除向量库"
                    loading={deleteVectorMutation.isPending}
                    onClick={() => deleteVectorMutation.mutate(store)}
                  >
                    删除
                  </Button>
                ]}
              >
                <List.Item.Meta
                  title={<Space><Typography.Text strong>{store.name}</Typography.Text><Tag>{store.storeType}</Tag><Tag color={store.enabled ? 'green' : 'default'}>{store.enabled ? '启用' : '停用'}</Tag></Space>}
                  description={`${store.namespaceName || store.indexName} ${
                    store.endpoint || (store.host ? `${store.host}:${store.port || ''}` : '本地内存')
                  }`}
                />
              </List.Item>
            )}
          />
        </Card>

        <Card size="small" title={editingVectorStore ? '编辑向量库' : '新增向量库'}>
          <Form form={vectorForm} layout="vertical" initialValues={initialVectorValues} onFinish={(values) => vectorMutation.mutate(values)}>
            <Form.Item name="name" label="配置名称" rules={[{ required: true, message: '请输入配置名称' }]}>
              <Input placeholder="Elastic dev" />
            </Form.Item>
            <Form.Item name="storeType" label="向量库类型">
              <Select
                onChange={(type) => {
                  if (type === 'MILVUS') {
                    vectorForm.setFieldsValue({
                      port: 19530,
                      databaseName: 'default',
                      namespaceName: 'agi_knowledge_vectors'
                    });
                  } else if (type === 'PGVECTOR') {
                    vectorForm.setFieldsValue({
                      port: 5432,
                      databaseName: 'pgvector',
                      namespaceName: 'agi_knowledge_vectors'
                    });
                  }
                }}
                options={[
                  { value: 'MEMORY', label: 'Memory' },
                  { value: 'ELASTICSEARCH', label: 'Elasticsearch' },
                  { value: 'MILVUS', label: 'Milvus' },
                  { value: 'PGVECTOR', label: 'Pgvector' }
                ]}
              />
            </Form.Item>
            {isElasticsearchVectorStore ? (
              <>
                <Form.Item name="endpoint" label="连接地址" rules={[{ required: true, message: '请输入 Elasticsearch 地址' }]}>
                  <Input placeholder="http://localhost:9200" />
                </Form.Item>
                <Form.Item name="username" label="用户名">
                  <Input placeholder="elastic" autoComplete="username" />
                </Form.Item>
                <Form.Item name="password" label="密码">
                  <Input.Password
                    placeholder={editingVectorStore?.passwordConfigured ? '已配置，留空则保持不变' : '请输入密码'}
                    autoComplete="new-password"
                  />
                </Form.Item>
                <Form.Item name="apiKey" label="API Key">
                  <Input.Password
                    placeholder={editingVectorStore?.apiKeyConfigured ? '已配置，留空则保持不变' : '可选，填写后优先使用 API Key'}
                    autoComplete="new-password"
                  />
                </Form.Item>
                <Space align="start" style={{ width: '100%' }}>
                  <Form.Item name="connectTimeoutMs" label="连接超时(ms)" rules={[{ required: true, message: '请输入连接超时' }]}>
                    <InputNumber min={100} max={120000} style={{ width: 180 }} />
                  </Form.Item>
                  <Form.Item name="readTimeoutMs" label="读取超时(ms)" rules={[{ required: true, message: '请输入读取超时' }]}>
                    <InputNumber min={100} max={300000} style={{ width: 180 }} />
                  </Form.Item>
                </Space>
              </>
            ) : null}
            {isMilvusVectorStore || isPgvectorVectorStore ? (
              <>
                <Space align="start" style={{ width: '100%' }}>
                  <Form.Item name="host" label="连接地址" rules={[{ required: true, message: '请输入主机地址' }]}>
                    <Input style={{ width: 300 }} placeholder="127.0.0.1" />
                  </Form.Item>
                  <Form.Item name="port" label="连接端口" rules={[{ required: true, message: '请输入端口' }]}>
                    <InputNumber
                      min={1}
                      max={65535}
                      style={{ width: 160 }}
                      placeholder={isMilvusVectorStore ? '19530' : '5432'}
                    />
                  </Form.Item>
                </Space>
                <Space align="start" style={{ width: '100%' }}>
                  <Form.Item name="databaseName" label="数据库名" rules={[{ required: true, message: '请输入数据库名' }]}>
                    <Input style={{ width: 300 }} placeholder={isMilvusVectorStore ? 'default' : 'pgvector'} />
                  </Form.Item>
                  <Form.Item
                    name="namespaceName"
                    label={isMilvusVectorStore ? '集合名' : '向量表名'}
                    rules={[
                      { required: true, message: isMilvusVectorStore ? '请输入集合名' : '请输入向量表名' },
                      { pattern: /^[A-Za-z_][A-Za-z0-9_]{0,62}$/, message: '仅支持字母、数字和下划线，且不能以数字开头' }
                    ]}
                  >
                    <Input style={{ width: 300 }} placeholder="agi_knowledge_vectors" />
                  </Form.Item>
                </Space>
                <Form.Item name="username" label="用户名" rules={isPgvectorVectorStore ? [{ required: true, message: '请输入用户名' }] : []}>
                  <Input autoComplete="username" placeholder={isMilvusVectorStore ? 'root（可选）' : 'vector_user'} />
                </Form.Item>
                <Form.Item
                  name="password"
                  label={isMilvusVectorStore ? '密码 / Token' : '密码'}
                  rules={isPgvectorVectorStore && !editingVectorStore ? [{ required: true, message: '请输入密码' }] : []}
                >
                  <Input.Password
                    autoComplete="new-password"
                    placeholder={editingVectorStore?.passwordConfigured ? '已配置，留空保持不变' : '请输入认证信息'}
                  />
                </Form.Item>
                <Space align="start" style={{ width: '100%' }}>
                  <Form.Item name="vectorDimension" label="向量维度" rules={[{ required: true, message: '请输入向量维度' }]}>
                    <InputNumber min={1} max={32768} style={{ width: 180 }} />
                  </Form.Item>
                  <Form.Item name="sslEnabled" label={isMilvusVectorStore ? 'TLS' : 'SSL'}>
                    <Select
                      style={{ width: 180 }}
                      options={[{ value: false, label: '关闭' }, { value: true, label: '开启' }]}
                    />
                  </Form.Item>
                </Space>
              </>
            ) : null}
            {isExternalVectorStore && !isElasticsearchVectorStore ? (
              <Space align="start" style={{ width: '100%' }}>
                <Form.Item name="connectTimeoutMs" label="连接超时(ms)" rules={[{ required: true }]}>
                  <InputNumber min={100} max={120000} style={{ width: 180 }} />
                </Form.Item>
                <Form.Item name="readTimeoutMs" label="读取超时(ms)" rules={[{ required: true }]}>
                  <InputNumber min={100} max={300000} style={{ width: 180 }} />
                </Form.Item>
              </Space>
            ) : null}
            {isElasticsearchVectorStore ? (
              <Form.Item name="indexName" label="索引名称" rules={[{ required: true, message: '请输入索引名称' }]}>
                <Input placeholder="aiworkflow_kb" />
              </Form.Item>
            ) : (
              <Form.Item name="indexName" hidden><Input /></Form.Item>
            )}
            <Form.Item name="enabled" label="状态">
              <Select options={[{ value: true, label: '启用' }, { value: false, label: '停用' }]} />
            </Form.Item>
            <Space>
              {isExternalVectorStore ? (
                <Button
                  icon={<DatabaseOutlined />}
                  loading={testVectorMutation.isPending}
                  onClick={async () => {
                    const values = await vectorForm.validateFields();
                    testVectorMutation.mutate(values);
                  }}
                >
                  测试连接
                </Button>
              ) : null}
              <Button type="primary" icon={<CheckCircleOutlined />} loading={vectorMutation.isPending} onClick={() => vectorForm.submit()}>
                保存配置
              </Button>
            </Space>
          </Form>
        </Card>
      </Drawer>

      <Drawer
        title={editingBase ? '编辑知识库' : '新增知识库'}
        open={createOpen}
        width={620}
        onClose={() => {
          setCreateOpen(false);
          setEditingBase(null);
        }}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => {
              setCreateOpen(false);
              setEditingBase(null);
            }}>取消</Button>
            <Button type="primary" icon={<CheckCircleOutlined />} loading={saveBaseMutation.isPending} onClick={() => baseForm.submit()}>
              {editingBase ? '修改' : '保存'}
            </Button>
          </Space>
        )}
      >
        <Form form={baseForm} layout="vertical" initialValues={initialBaseValues} onFinish={(values) => saveBaseMutation.mutate(values)}>
          <Form.Item name="name" label="知识库名称" rules={[{ required: true, message: '请输入知识库名称' }]}>
            <Input placeholder="产品知识库" />
          </Form.Item>
          <Form.Item name="description" label="知识库描述">
            <Input placeholder="用于客服问答、产品手册或内部制度检索" />
          </Form.Item>
          <Form.Item name="embeddingModelId" label="嵌入模型">
            <Select
              allowClear
              aria-label="选择嵌入模型"
              loading={modelProvidersQuery.isLoading}
              options={embeddingModelOptions}
              placeholder="选择已保存的 embedding 模型"
            />
          </Form.Item>
          <Form.Item name="vectorDimension" label="向量维度" rules={[{ required: true, message: '请输入向量维度' }]}>
            <InputNumber min={1} max={32768} style={{ width: '100%' }} placeholder="1536" />
          </Form.Item>
          <Form.Item name="vectorStoreConfigId" label="向量库配置">
            <Select allowClear placeholder="默认使用内存向量库" options={vectorStoreOptions} />
          </Form.Item>
          <Form.Item label="分段策略" style={{ display: 'none', marginBottom: 0 }}>
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
          <Form.Item label="检索设置" style={{ display: 'none', marginTop: 24, marginBottom: 0 }}>
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
        width={980}
        onClose={() => setDocumentOpen(false)}
      >
        <Card
          size="small"
          title={<Space><FileAddOutlined />新增数据</Space>}
          extra={(
            <Dropdown
              menu={{
                items: [
                  { key: 'manual', label: '手动数据集', icon: <FileAddOutlined /> },
                  { key: 'text', label: '文本文档', icon: <FileTextOutlined /> },
                  { key: 'table', label: '表格文档', icon: <DatabaseOutlined /> }
                ],
                onClick: ({ key }) => openDataDrawer(key as 'manual' | 'text' | 'table')
              }}
              trigger={['click']}
            >
              <Button type="primary" icon={<PlusOutlined />}>新增数据</Button>
            </Dropdown>
          )}
          style={{ marginBottom: 16 }}
        >
          <Typography.Text type="secondary">
            当前知识库支持手动录入、文本文档上传和表格文档上传；入库后自动执行分段、向量化和索引构建。
          </Typography.Text>
        </Card>

        <Card size="small" title={<Space><UploadOutlined />文件上传与数据处理</Space>} style={{ display: 'none', marginBottom: 16 }}>
          <Steps
            current={uploadStep}
            items={[
              { title: '选择文件', description: '选择要上传的数据文件' },
              { title: '数据处理', description: '配置分段并预览' },
              { title: '确认上传', description: '写入知识库' }
            ]}
            style={{ marginBottom: 20 }}
          />
          <div style={uploadWizardStyle}>
            <div style={uploadPanelStyle}>
              <label htmlFor="knowledge-upload-file" style={uploadDropStyle}>
                <UploadOutlined style={{ color: '#1677ff', fontSize: 28 }} />
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
                    setUploadStep(0);
                    setChunkPreviews([]);
                  }}
                />
              </label>
              {uploadFile ? (
                <Tag color="blue" style={{ marginTop: 12 }}>
                  {uploadFile.name} / {(uploadFile.size / 1024 / 1024).toFixed(2)} MB
                </Tag>
              ) : null}
            </div>
            <div style={uploadPanelStyle}>
              <Typography.Text strong>数据处理配置</Typography.Text>
              <Form form={documentForm} layout="inline" style={{ marginTop: 12 }}>
                <Space wrap>
                  <Form.Item name="splitterType" label="分段策略" style={{ marginBottom: 8 }}>
                    <Select
                      style={{ width: 170 }}
                      options={[
                        { value: 'SIMPLE_TEXT', label: '固定长度分段' },
                        { value: 'MARKDOWN_HEADING', label: 'Markdown 标题' },
                        { value: 'REGEX', label: '段落分段' },
                        { value: 'SEMANTIC', label: '语义分段' }
                      ]}
                    />
                  </Form.Item>
                  <Form.Item name="chunkSize" label="分段长度" style={{ marginBottom: 8 }}>
                    <InputNumber min={80} max={2000} />
                  </Form.Item>
                  <Form.Item name="chunkOverlap" label="重叠 Token" style={{ marginBottom: 8 }}>
                    <InputNumber min={0} max={500} />
                  </Form.Item>
                  {documentSplitterType === 'SEMANTIC' ? (
                    <Form.Item name="semanticSimilarityThreshold" label="相似度阈值" style={{ marginBottom: 8 }}>
                      <InputNumber min={0} max={1} step={0.01} precision={2} />
                    </Form.Item>
                  ) : null}
                </Space>
              </Form>
              <Space style={{ marginTop: 16 }}>
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
                  icon={<FileAddOutlined />}
                  disabled={!uploadFile || !uploadPreview}
                  loading={uploadFileMutation.isPending}
                  onClick={() => uploadFileMutation.mutate()}
                >
                  确认上传
                </Button>
              </Space>
            </div>
          </div>
          {uploadPreview ? (
            <Card size="small" title={<Space><FileTextOutlined />文件预览</Space>} style={{ marginTop: 16 }}>
              <Space style={{ marginBottom: 12 }} wrap>
                <Tag color="blue">{uploadPreview.fileName}</Tag>
                <Tag>{uploadPreview.characterCount} 字符</Tag>
                <Tag>{uploadPreview.chunks.length} 个分段</Tag>
              </Space>
              <List
                dataSource={uploadPreview.chunks.slice(0, 5)}
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
          ) : null}
        </Card>

        <Card size="small" title={<Space><FileAddOutlined />文档入库</Space>} style={{ display: 'none', marginBottom: 16 }}>
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
                    { value: 'REGEX', label: '正则分隔' },
                    { value: 'SEMANTIC', label: '语义分段' }
                  ]}
                />
              </Form.Item>
              <Form.Item name="chunkSize" label="切片大小">
                <InputNumber min={80} max={2000} />
              </Form.Item>
              <Form.Item name="chunkOverlap" label="重叠 Token">
                <InputNumber min={0} max={500} />
              </Form.Item>
              {documentSplitterType === 'SEMANTIC' ? (
                <Form.Item name="semanticSimilarityThreshold" label="相似度阈值">
                  <InputNumber min={0} max={1} step={0.01} precision={2} />
                </Form.Item>
              ) : null}
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

        <div style={documentGridStyle}>
          <Card size="small" title={<Space><FileTextOutlined />文档列表</Space>}>
            <List
              loading={documentsQuery.isLoading}
              dataSource={documents}
              locale={{ emptyText: '暂无文档' }}
              renderItem={(document) => (
                <List.Item
                  actions={[
                    <Button key="chunks" size="small" icon={<FileSearchOutlined />} onClick={() => setSelectedDocument(document)}>查看切片</Button>,
                    <Button
                      key="reparse"
                      size="small"
                      loading={reparseDocumentMutation.isPending}
                      disabled={document.processingStatus === 'PROCESSING'}
                      onClick={() => reparseDocumentMutation.mutate(document)}
                    >
                      重新解析
                    </Button>,
                    <Button key="delete" danger size="small" icon={<DeleteOutlined />} onClick={() => deleteDocumentMutation.mutate(document)}>删除文档</Button>
                  ]}
                >
                  <List.Item.Meta
                    title={(
                      <Space wrap>
                        <Typography.Text>{document.name}</Typography.Text>
                        <Tag color={document.datasetType === 'TABLE_DOCUMENT' ? 'cyan' : document.datasetType === 'MANUAL' ? 'green' : 'blue'}>
                          {datasetTypeLabel(document.datasetType)}
                        </Tag>
                        <Tag color={document.processingStatus === 'FAILED' ? 'red' : document.processingStatus === 'PROCESSING' ? 'orange' : 'geekblue'}>
                          {document.processingStatus || 'READY'}
                        </Tag>
                      </Space>
                    )}
                    description={(
                      <Space direction="vertical" size={2}>
                        <Typography.Text type="secondary">
                          {document.chunkCount} 个切片{document.rowCount ? ` / ${document.rowCount} 行数据` : ''}
                        </Typography.Text>
                        {document.errorMessage ? <Typography.Text type="danger">{document.errorMessage}</Typography.Text> : null}
                      </Space>
                    )}
                  />
                </List.Item>
              )}
            />
          </Card>

          <Card size="small" title={<Space><FileTextOutlined />切片管理</Space>}>
            <List
              loading={chunksQuery.isLoading}
              dataSource={chunks}
              locale={{ emptyText: selectedDocument ? '暂无切片' : '请选择文档' }}
              renderItem={(chunk) => (
                <List.Item
                  actions={[
                    <Button key="edit" size="small" icon={<EditOutlined />} aria-label="编辑切片" onClick={() => openChunkEditor(chunk)}>
                      编辑
                    </Button>,
                    <Button key="toggle" size="small" aria-label={chunk.enabled ? '禁用' : '启用'} onClick={() => updateChunkMutation.mutate({ chunk, content: chunk.content, enabled: !chunk.enabled })}>
                      {chunk.enabled ? '禁用' : '启用'}
                    </Button>
                  ]}
                >
                  <List.Item.Meta
                    title={(
                      <Space wrap>
                        <Tag color={chunk.enabled ? 'green' : 'default'}>{chunk.enabled ? '启用' : '停用'}</Tag>
                        <Tag color={chunk.chunkLevel === 'PARENT' ? 'purple' : 'blue'}>
                          {chunk.chunkLevel === 'PARENT' ? '父块' : '检索子块'}
                        </Tag>
                        {chunk.chunkType ? <Tag>{chunk.chunkType}</Tag> : null}
                        <Typography.Text type="secondary">{chunk.tokenEstimate} tokens</Typography.Text>
                      </Space>
                    )}
                    description={(
                      <>
                        {chunk.sectionPath ? <Typography.Text type="secondary">{chunk.sectionPath}</Typography.Text> : null}
                        <Typography.Paragraph style={{ marginBottom: 0 }}>{chunk.content}</Typography.Paragraph>
                      </>
                    )}
                  />
                </List.Item>
              )}
            />
          </Card>
        </div>

        <Card size="small" title={<Space><FileTextOutlined />切片预览</Space>} style={{ marginTop: 16, marginBottom: 16 }}>
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

      <Drawer
        title="手动数据集"
        open={dataDrawerType === 'manual'}
        width={720}
        onClose={() => setDataDrawerType(null)}
        footer={(
          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setDataDrawerType(null)}>取消</Button>
            <Button type="primary" icon={<FileAddOutlined />} loading={manualDatasetMutation.isPending} onClick={() => manualDatasetForm.submit()}>
              保存并入库
            </Button>
          </Space>
        )}
      >
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
        </Form>
      </Drawer>

      <Drawer
        title="文本文档"
        open={dataDrawerType === 'text'}
        width={760}
        onClose={() => setDataDrawerType(null)}
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
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
          {textUploadFile ? <Tag color="blue">{textUploadFile.name} / {(textUploadFile.size / 1024 / 1024).toFixed(2)} MB</Tag> : null}

          <Card size="small" title="分段策略">
            <Form
              form={textDocumentForm}
              layout="vertical"
              initialValues={{
                splitterType: 'FIXED_LENGTH',
                chunkSize: 200,
                semanticSimilarityThreshold: 0.78,
                separator: ''
              }}
            >
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
              {textSplitterType === 'SEMANTIC' ? (
                <Form.Item
                  name="semanticSimilarityThreshold"
                  label="语义相似度阈值"
                  tooltip="相邻内容相似度低于该值时创建新的分段"
                >
                  <InputNumber min={0} max={1} step={0.01} precision={2} style={{ width: 180 }} />
                </Form.Item>
              ) : null}
              {textSplitterType === 'SYMBOL' ? (
                <Form.Item name="separator" label="分段符">
                  <Input placeholder="请输入分段符号" />
                </Form.Item>
              ) : null}
            </Form>
          </Card>

          <Space>
            <Button icon={<FileSearchOutlined />} disabled={!textUploadFile} loading={textUploadPreviewMutation.isPending} onClick={() => textUploadPreviewMutation.mutate()}>
              预览分段
            </Button>
            <Button type="primary" icon={<FileAddOutlined />} disabled={!textUploadFile || !textUploadPreview} loading={textUploadMutation.isPending} onClick={() => textUploadMutation.mutate()}>
              确认上传
            </Button>
          </Space>
          {textUploadPreview ? <UploadPreviewCard preview={textUploadPreview} /> : null}
        </Space>
      </Drawer>

      <Drawer
        title="表格文档"
        open={dataDrawerType === 'table'}
        width={760}
        onClose={() => setDataDrawerType(null)}
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
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
          {tableUploadFile ? <Tag color="cyan">{tableUploadFile.name} / {(tableUploadFile.size / 1024 / 1024).toFixed(2)} MB</Tag> : null}

          <Card size="small" title="数据处理配置">
            <Form form={tableDocumentForm} layout="vertical" initialValues={{ splitterType: 'STRUCTURED', chunkSize: 200 }}>
              <Form.Item name="splitterType" label="分段方式">
                <Radio.Group>
                  <Radio value="STRUCTURED">结构化分段</Radio>
                </Radio.Group>
              </Form.Item>
              <Form.Item name="chunkSize" label="分段长度" rules={[{ required: true, message: '请输入分段长度' }]}>
                <InputNumber min={1} controls style={{ width: 180 }} />
              </Form.Item>
            </Form>
          </Card>

          <Space>
            <Button icon={<FileSearchOutlined />} disabled={!tableUploadFile} loading={tableUploadPreviewMutation.isPending} onClick={() => tableUploadPreviewMutation.mutate()}>
              预览分段
            </Button>
            <Button type="primary" icon={<FileAddOutlined />} disabled={!tableUploadFile || !tableUploadPreview} loading={tableUploadMutation.isPending} onClick={() => tableUploadMutation.mutate()}>
              确认上传
            </Button>
          </Space>
          {tableUploadPreview ? <UploadPreviewCard preview={tableUploadPreview} /> : null}
        </Space>
      </Drawer>

      <Drawer
        title={editingChunk ? `编辑切片 #${editingChunk.index + 1}` : '编辑切片'}
        open={Boolean(editingChunk)}
        width={640}
        onClose={() => {
          setEditingChunk(null);
          setEditingChunkContent('');
        }}
      >
        <Form layout="vertical">
          <Form.Item
            name="content"
            label="切片内容"
            rules={[{ required: true, message: '请输入切片内容' }]}
          >
            <Input.TextArea
              aria-label="切片内容"
              autoSize={{ minRows: 10, maxRows: 18 }}
              value={editingChunkContent}
              onChange={(event) => setEditingChunkContent(event.target.value)}
            />
          </Form.Item>
          <Space>
            <Button
              type="primary"
              loading={updateChunkMutation.isPending}
              onClick={() => {
                const content = editingChunkContent.trim();
                if (editingChunk && content) {
                  updateChunkMutation.mutate({
                    chunk: editingChunk,
                    content,
                    enabled: editingChunk.enabled
                  });
                }
              }}
            >
              保存切片
            </Button>
            <Button onClick={() => {
              setEditingChunk(null);
              setEditingChunkContent('');
            }}>
              取消
            </Button>
          </Space>
        </Form>
      </Drawer>
    </section>
  );
}

function UploadPreviewCard({ preview }: { preview: UploadedDocumentPreview }) {
  return (
    <Card size="small" title={<Space><FileTextOutlined />分段预览</Space>}>
      <Space style={{ marginBottom: 12 }} wrap>
        <Tag color="blue">{preview.fileName}</Tag>
        <Tag>{preview.characterCount} 字符</Tag>
        <Tag>{preview.chunks.length} 个分段</Tag>
      </Space>
      <List
        dataSource={preview.chunks.slice(0, 5)}
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
  );
}

function datasetTypeLabel(datasetType?: string) {
  if (datasetType === 'MANUAL') {
    return '手动数据集';
  }
  if (datasetType === 'TABLE_DOCUMENT') {
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

const metricRowStyle: React.CSSProperties = {
  display: 'grid',
  gap: 12,
  gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
  marginBottom: 16
};

const metricCardStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3'
};

const documentGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(280px, 0.8fr) minmax(360px, 1.2fr)'
};

const uploadWizardStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(300px, 0.9fr) minmax(360px, 1.1fr)'
};

const uploadPanelStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  padding: 16
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
  minHeight: 150,
  textAlign: 'center'
};
