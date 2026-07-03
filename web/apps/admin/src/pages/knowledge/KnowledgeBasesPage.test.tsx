// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { KnowledgeBasesPage } from './KnowledgeBasesPage';

const knowledgeApiMock = vi.hoisted(() => ({
  listKnowledgeBases: vi.fn(async () => ({
    items: [
      {
        id: 'kb_1',
        name: '产品知识库',
        description: '客服资料',
        embeddingModelId: 'model_embed',
        vectorStoreConfigId: 'vector_1',
        vectorDimension: 1536,
        splitterType: 'MARKDOWN_HEADING',
        chunkSize: 500,
        chunkOverlap: 50,
        retrievalMode: 'HYBRID',
        topK: 5,
        status: 'READY',
        documentCount: 1,
        chunkCount: 2
      }
    ],
    total: 1
  })),
  createKnowledgeBase: vi.fn(async () => ({
    id: 'kb_2',
    name: '售后知识库',
    description: null,
    documentCount: 0,
    chunkCount: 0
  })),
  updateKnowledgeBase: vi.fn(async () => ({
    id: 'kb_1',
    name: '运营知识库',
    description: '生产资料',
    embeddingModelId: 'embed-prod',
    vectorStoreConfigId: 'vector_1',
    splitterType: 'MARKDOWN_HEADING',
    chunkSize: 180,
    chunkOverlap: 30,
    retrievalMode: 'HYBRID',
    topK: 6,
    status: 'READY',
    documentCount: 1,
    chunkCount: 2
  })),
  deleteKnowledgeBase: vi.fn(async () => undefined),
  addKnowledgeDocument: vi.fn(async () => ({
    id: 'doc_1',
    knowledgeBaseId: 'kb_1',
    name: 'faq.txt',
    chunkCount: 1
  })),
  addManualKnowledgeDataset: vi.fn(async () => ({
    id: 'doc_manual',
    knowledgeBaseId: 'kb_1',
    name: '退费规则',
    chunkCount: 2,
    datasetType: 'MANUAL',
    processingStatus: 'READY'
  })),
  listVectorStoreConfigs: vi.fn(async () => ({
    items: [
      {
        id: 'vector_1',
        name: '本地内存向量库',
        storeType: 'MEMORY',
        endpoint: '',
        indexName: 'aiworkflow_kb',
        username: null,
        passwordConfigured: false,
        apiKeyConfigured: false,
        connectTimeoutMs: 5000,
        readTimeoutMs: 30000,
        enabled: true
      }
    ],
    total: 1
  })),
  createVectorStoreConfig: vi.fn(async () => ({
    id: 'vector_2',
    name: 'Elastic dev',
    storeType: 'ELASTICSEARCH',
    endpoint: 'http://localhost:9200',
    indexName: 'kb_dev',
    username: 'elastic',
    passwordConfigured: true,
    apiKeyConfigured: false,
    connectTimeoutMs: 7000,
    readTimeoutMs: 45000,
    enabled: true
  })),
  updateVectorStoreConfig: vi.fn(async () => ({
    id: 'vector_1',
    name: 'Elastic prod',
    storeType: 'ELASTICSEARCH',
    endpoint: 'https://es.example.com',
    indexName: 'kb_prod',
    username: 'elastic-prod',
    passwordConfigured: true,
    apiKeyConfigured: false,
    connectTimeoutMs: 9000,
    readTimeoutMs: 60000,
    enabled: false
  })),
  deleteVectorStoreConfig: vi.fn(async () => undefined),
  testVectorStoreConnection: vi.fn(async () => ({
    success: true,
    storeType: 'MILVUS',
    serverVersion: '2.6.0',
    latencyMs: 12,
    message: 'Connection successful'
  })),
  testSavedVectorStoreConnection: vi.fn(async () => ({
    success: true,
    storeType: 'MEMORY',
    latencyMs: 0,
    message: 'Local memory store'
  })),
  listKnowledgeDocuments: vi.fn(async () => ({
    items: [
      {
        id: 'doc_1',
        knowledgeBaseId: 'kb_1',
        name: 'faq.txt',
        chunkCount: 1,
        datasetType: 'TEXT_DOCUMENT',
        processingStatus: 'READY'
      }
    ],
    total: 1
  })),
  listKnowledgeDocumentChunks: vi.fn(async () => ({
    items: [
      {
        id: 'chunk_1',
        knowledgeBaseId: 'kb_1',
        documentId: 'doc_1',
        documentName: 'faq.txt',
        content: 'Refund requests are handled within seven days.',
        index: 0,
        enabled: true,
        tokenEstimate: 12
      }
    ],
    total: 1
  })),
  updateKnowledgeChunk: vi.fn(async () => ({
    id: 'chunk_1',
    knowledgeBaseId: 'kb_1',
    documentId: 'doc_1',
    documentName: 'faq.txt',
    content: 'Refund requests are handled within seven days.',
    index: 0,
    enabled: false,
    tokenEstimate: 12
  })),
  deleteKnowledgeDocument: vi.fn(async () => undefined),
  previewKnowledgeChunks: vi.fn(async () => [
    {
      index: 0,
      content: '# Refund\nRefund requests are handled within seven days.',
      tokenEstimate: 14
    }
  ]),
  previewUploadedKnowledgeDocumentFile: vi.fn(async () => ({
    fileName: 'policy.pdf',
    characterCount: 78,
    chunks: [
      {
        index: 0,
        content: 'Uploaded refund policy text',
        tokenEstimate: 7
      }
    ]
  })),
  uploadKnowledgeDocumentFile: vi.fn(async () => ({
    id: 'doc_upload',
    knowledgeBaseId: 'kb_1',
    name: 'policy.pdf',
    chunkCount: 1
  })),
  previewUploadedTextKnowledgeDocumentFile: vi.fn(async () => ({
    fileName: 'policy.pdf',
    characterCount: 78,
    chunks: [
      {
        index: 0,
        content: 'Uploaded refund policy text',
        tokenEstimate: 7
      }
    ]
  })),
  uploadTextKnowledgeDocumentFile: vi.fn(async () => ({
    id: 'doc_upload',
    knowledgeBaseId: 'kb_1',
    name: 'policy.pdf',
    chunkCount: 1,
    datasetType: 'TEXT_DOCUMENT',
    processingStatus: 'READY'
  })),
  previewUploadedTableKnowledgeDocumentFile: vi.fn(async () => ({
    fileName: 'ledger.xlsx',
    characterCount: 28,
    chunks: [
      {
        index: 0,
        content: '字段A:XX；字段B:XX',
        tokenEstimate: 6
      }
    ]
  })),
  uploadTableKnowledgeDocumentFile: vi.fn(async () => ({
    id: 'doc_table',
    knowledgeBaseId: 'kb_1',
    name: 'ledger.xlsx',
    chunkCount: 1,
    datasetType: 'TABLE_DOCUMENT',
    processingStatus: 'READY'
  })),
  reparseKnowledgeDocument: vi.fn(async () => ({
    id: 'doc_1',
    knowledgeBaseId: 'kb_1',
    name: 'faq.txt',
    chunkCount: 1,
    datasetType: 'TEXT_DOCUMENT',
    processingStatus: 'READY'
  })),
  searchKnowledgeBase: vi.fn(async () => [
    {
      id: 'chunk_1',
      documentName: 'faq.txt',
      content: '发票可以在订单完成后七日内申请。',
      score: 2
    }
  ])
}));

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
});

vi.mock('../../api/knowledge', () => knowledgeApiMock);

const modelApiMock = vi.hoisted(() => ({
  listModelProviders: vi.fn(async () => ({
    items: [
      {
        id: 'model_embed',
        name: 'Embedding Provider',
        modelType: 'OpenAI',
        modelUsage: 'EMBEDDING',
        description: 'Embedding model',
        visionSupport: false,
        pricePerMillionTokens: 1,
        baseUrl: 'https://api.example.com/v1',
        model: 'text-embedding-3-small',
        apiKeyRef: 'embedding-key',
        enabled: true
      },
      {
        id: 'model_chat',
        name: 'Chat Provider',
        modelType: 'OpenAI',
        modelUsage: 'CHAT',
        description: 'Chat model',
        visionSupport: false,
        pricePerMillionTokens: 1,
        baseUrl: 'https://api.example.com/v1',
        model: 'gpt-4.1-mini',
        apiKeyRef: 'chat-key',
        enabled: true
      }
    ],
    total: 1
  }))
}));

vi.mock('../../api/models', () => modelApiMock);

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('KnowledgeBasesPage', () => {
  it('lists knowledge bases with retrieval and vector store settings', async () => {
    renderPage();

    expect(await screen.findByText('产品知识库')).toBeInTheDocument();
    expect(screen.getAllByText('向量库配置').length).toBeGreaterThan(0);
    expect(screen.getByText('1536 维')).toBeInTheDocument();
    expect(screen.getByText('分段策略')).toBeInTheDocument();
    expect(screen.getByText('HYBRID')).toBeInTheDocument();
  });

  it('hides retrieval and splitter controls when creating a knowledge base', async () => {
    renderPage();

    await screen.findByText('HYBRID');
    await userEvent.click(screen.getAllByRole('button')[1]);

    const drawer = await screen.findByRole('dialog');
    expect(within(drawer).getAllByRole('spinbutton')).toHaveLength(1);
  });

  it('hides retrieval and splitter controls when editing a knowledge base', async () => {
    renderPage();

    await screen.findByText('HYBRID');
    await userEvent.click(screen.getAllByRole('button')[2]);

    const drawer = await screen.findByRole('dialog');
    expect(within(drawer).getAllByRole('spinbutton')).toHaveLength(1);
  });

  it('creates a knowledge base with hidden retrieval and splitter settings', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /新增知识库/ }));
    fireEvent.change(await screen.findByLabelText('知识库名称'), { target: { value: '售后知识库' } });
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.createKnowledgeBase).toHaveBeenCalledWith(expect.objectContaining({
        name: '售后知识库',
        vectorDimension: 1536,
        splitterType: 'SIMPLE_TEXT',
        chunkSize: 500,
        chunkOverlap: 50,
        retrievalMode: 'HYBRID',
        topK: 3
      }));
    });
  });

  it('selects a saved embedding model provider for a knowledge base', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /新增知识库/ }));
    fireEvent.mouseDown(await screen.findByRole('combobox', { name: '选择嵌入模型' }));
    await userEvent.click(await screen.findByText('Embedding Provider / text-embedding-3-small'));
    expect(screen.queryByText('Chat Provider / gpt-4.1-mini')).not.toBeInTheDocument();
    fireEvent.change(await screen.findByLabelText('知识库名称'), { target: { value: '检索知识库' } });
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.createKnowledgeBase).toHaveBeenCalledWith(expect.objectContaining({
        name: '检索知识库',
        embeddingModelId: 'model_embed'
      }));
    });
  });

  it('edits and deletes knowledge bases from the list', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /编辑知识库/ }));
    fireEvent.change(await screen.findByLabelText('知识库名称'), { target: { value: '运营知识库' } });
    fireEvent.change(screen.getByLabelText('知识库描述'), { target: { value: '生产资料' } });
    await userEvent.click(screen.getByRole('button', { name: /修改/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.updateKnowledgeBase).toHaveBeenCalledWith('kb_1', expect.objectContaining({
        name: '运营知识库',
        description: '生产资料',
        splitterType: 'SIMPLE_TEXT',
        retrievalMode: 'HYBRID'
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /删除知识库/ }));
    await waitFor(() => {
      expect(knowledgeApiMock.deleteKnowledgeBase).toHaveBeenCalledWith('kb_1');
    });
  });

  it('adds manual dataset entries and can search the selected base', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /管理文档/ }));
    await userEvent.click(await screen.findByRole('button', { name: /新增数据/ }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /手动数据集/ }));
    fireEvent.change(await screen.findByLabelText('标题'), { target: { value: '退费规则' } });
    fireEvent.change(screen.getByLabelText('正文内容'), { target: { value: 'Refund requests are handled within seven days.' } });
    fireEvent.change(screen.getByLabelText('标签'), { target: { value: 'refund,policy' } });
    await userEvent.click(screen.getByRole('button', { name: /新增一条/ }));
    fireEvent.change(screen.getAllByLabelText('标题')[1], { target: { value: '发票规则' } });
    fireEvent.change(screen.getAllByLabelText('正文内容')[1], { target: { value: 'Invoices can be downloaded after payment.' } });
    await userEvent.click(screen.getByRole('button', { name: /保存并入库/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.addManualKnowledgeDataset).toHaveBeenCalledWith('kb_1', {
        entries: [
          expect.objectContaining({
            title: '退费规则',
            content: 'Refund requests are handled within seven days.',
            tags: 'refund,policy'
          }),
          expect.objectContaining({
            title: '发票规则',
            content: 'Invoices can be downloaded after payment.'
          })
        ]
      });
    });

    fireEvent.change(screen.getByLabelText('检索测试'), { target: { value: '发票申请' } });
    await userEvent.click(screen.getByRole('button', { name: /检索/ }));

    expect(await screen.findByText('发票可以在订单完成后七日内申请。')).toBeInTheDocument();
  }, 20000);

  it('previews and uploads files from the document wizard', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /管理文档/ }));
    await userEvent.click(await screen.findByRole('button', { name: /新增数据/ }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /文本文档/ }));
    const file = new File(['Uploaded refund policy text'], 'policy.pdf', { type: 'application/pdf' });
    fireEvent.change(await screen.findByLabelText('选择文本文档'), {
      target: { files: [file] }
    });
    await userEvent.click(screen.getByRole('button', { name: /预览分段/ }));

    expect((await screen.findAllByText('Uploaded refund policy text')).length).toBeGreaterThan(0);
    await waitFor(() => {
      expect(knowledgeApiMock.previewUploadedTextKnowledgeDocumentFile).toHaveBeenCalledWith(file, expect.objectContaining({
        splitterType: 'FIXED_LENGTH',
        chunkSize: 200
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /确认上传/ }));
    await waitFor(() => {
      expect(knowledgeApiMock.uploadTextKnowledgeDocumentFile).toHaveBeenCalledWith('kb_1', file, expect.objectContaining({
        chunkSize: 200
      }));
    });
  }, 10000);

  it('manages vector store configs from the knowledge page', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /向量库配置/ }));
    expect(await screen.findByText('本地内存向量库')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /新增向量库/ }));
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '向量库类型' }));
    await userEvent.click(await screen.findByText('Elasticsearch'));
    fireEvent.change(await screen.findByLabelText('配置名称'), { target: { value: 'Elastic dev' } });
    fireEvent.change(await screen.findByLabelText('用户名'), { target: { value: 'elastic' } });
    fireEvent.change(await screen.findByLabelText('密码'), { target: { value: 'secret' } });
    fireEvent.change(await screen.findByLabelText('连接超时(ms)'), { target: { value: 7000 } });
    fireEvent.change(await screen.findByLabelText('读取超时(ms)'), { target: { value: 45000 } });
    fireEvent.change(screen.getByLabelText('索引名称'), { target: { value: 'kb_dev' } });
    await userEvent.click(screen.getByRole('button', { name: /保存配置/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.createVectorStoreConfig).toHaveBeenCalledWith(expect.objectContaining({
        name: 'Elastic dev',
        storeType: 'ELASTICSEARCH',
        indexName: 'kb_dev',
        username: 'elastic',
        password: 'secret',
        connectTimeoutMs: 7000,
        readTimeoutMs: 45000
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /编辑向量库/ }));
    fireEvent.change(await screen.findByLabelText('配置名称'), { target: { value: 'Elastic prod' } });
    await userEvent.click(screen.getByRole('button', { name: /保存配置/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.updateVectorStoreConfig).toHaveBeenCalledWith('vector_1', expect.objectContaining({
        name: 'Elastic prod'
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /删除向量库/ }));
    await waitFor(() => {
      expect(knowledgeApiMock.deleteVectorStoreConfig).toHaveBeenCalledWith('vector_1');
    });
  }, 10000);

  it('shows Milvus and Pgvector provider-specific connection fields', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /向量库配置/ }));
    await userEvent.click(screen.getByRole('button', { name: /新增向量库/ }));
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '向量库类型' }));
    await userEvent.click(await screen.findByText('Milvus'));

    expect(await screen.findByLabelText('数据库名')).toHaveValue('default');
    expect(screen.getByLabelText('集合名')).toHaveValue('agi_knowledge_vectors');
    expect(screen.getByLabelText('向量维度')).toHaveValue('1536');

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '向量库类型' }));
    await userEvent.click(await screen.findByText('Pgvector'));
    expect(await screen.findByLabelText('向量表名')).toHaveValue('agi_knowledge_vectors');
  }, 10000);

  it('lists documents, reparses, toggles chunks and deletes documents', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /管理文档/ }));
    expect(await screen.findByText('faq.txt')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /重新解析/ }));
    await waitFor(() => {
      expect(knowledgeApiMock.reparseKnowledgeDocument).toHaveBeenCalledWith('kb_1', 'doc_1');
    });
    await userEvent.click(screen.getByRole('button', { name: /查看切片/ }));
    expect(await screen.findByText(/Refund requests/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /禁用/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.updateKnowledgeChunk).toHaveBeenCalledWith('kb_1', 'chunk_1', expect.objectContaining({
        enabled: false
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /删除文档/ }));
    await waitFor(() => {
      expect(knowledgeApiMock.deleteKnowledgeDocument).toHaveBeenCalledWith('kb_1', 'doc_1');
    });
  }, 10000);

  it('edits chunk content from document management', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /管理文档/ }));
    await userEvent.click(await screen.findByRole('button', { name: /查看切片/ }));
    await userEvent.click(await screen.findByRole('button', { name: /编辑切片/ }));
    const chunkContentFields = await screen.findAllByLabelText('切片内容');
    fireEvent.change(chunkContentFields[chunkContentFields.length - 1], {
      target: { value: 'Updated refund requests are handled within three days.' }
    });
    await userEvent.click(screen.getByRole('button', { name: /保存切片/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.updateKnowledgeChunk).toHaveBeenCalledWith('kb_1', 'chunk_1', expect.objectContaining({
        content: 'Updated refund requests are handled within three days.',
        enabled: true
      }));
    });
  }, 10000);
});

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <KnowledgeBasesPage />
    </QueryClientProvider>
  );
}
