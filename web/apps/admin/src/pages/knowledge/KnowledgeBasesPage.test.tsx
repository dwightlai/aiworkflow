// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
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
  listVectorStoreConfigs: vi.fn(async () => ({
    items: [
      {
        id: 'vector_1',
        name: '本地内存向量库',
        storeType: 'MEMORY',
        endpoint: '',
        indexName: 'aiworkflow_kb',
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
    enabled: true
  })),
  updateVectorStoreConfig: vi.fn(async () => ({
    id: 'vector_1',
    name: 'Elastic prod',
    storeType: 'ELASTICSEARCH',
    endpoint: 'https://es.example.com',
    indexName: 'kb_prod',
    enabled: false
  })),
  deleteVectorStoreConfig: vi.fn(async () => undefined),
  listKnowledgeDocuments: vi.fn(async () => ({
    items: [
      {
        id: 'doc_1',
        knowledgeBaseId: 'kb_1',
        name: 'faq.txt',
        chunkCount: 1
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
        description: 'Embedding model',
        visionSupport: false,
        pricePerMillionTokens: 1,
        baseUrl: 'https://api.example.com/v1',
        model: 'text-embedding-3-small',
        apiKeyRef: 'embedding-key',
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
    expect(screen.getByText('分段策略')).toBeInTheDocument();
    expect(screen.getByText('HYBRID')).toBeInTheDocument();
  });

  it('creates a knowledge base with retrieval settings', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /新增知识库/ }));
    fireEvent.change(await screen.findByLabelText('知识库名称'), { target: { value: '售后知识库' } });
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.createKnowledgeBase).toHaveBeenCalledWith(expect.objectContaining({
        name: '售后知识库',
        splitterType: 'SIMPLE_TEXT',
        retrievalMode: 'KEYWORD'
      }));
    });
  });

  it('selects a saved embedding model provider for a knowledge base', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /新增知识库/ }));
    fireEvent.mouseDown(await screen.findByRole('combobox', { name: '选择嵌入模型' }));
    await userEvent.click(await screen.findByText('Embedding Provider / text-embedding-3-small'));
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
        splitterType: 'MARKDOWN_HEADING',
        retrievalMode: 'HYBRID'
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /删除知识库/ }));
    await waitFor(() => {
      expect(knowledgeApiMock.deleteKnowledgeBase).toHaveBeenCalledWith('kb_1');
    });
  });

  it('previews chunks before document ingestion and can search the selected base', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /管理文档/ }));
    fireEvent.change(await screen.findByLabelText('文档名称'), { target: { value: 'faq.txt' } });
    fireEvent.change(screen.getByLabelText('文档内容'), { target: { value: '# Refund\nRefund requests are handled within seven days.' } });
    await userEvent.click(screen.getByRole('button', { name: /预览切片/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.previewKnowledgeChunks).toHaveBeenCalledWith(expect.objectContaining({
        content: '# Refund\nRefund requests are handled within seven days.'
      }));
    });
    expect((await screen.findAllByText(/Refund requests/)).length).toBeGreaterThan(0);

    await userEvent.click(screen.getByRole('button', { name: /入库/ }));
    await waitFor(() => {
      expect(knowledgeApiMock.addKnowledgeDocument).toHaveBeenCalledWith('kb_1', expect.objectContaining({
        name: 'faq.txt'
      }));
    });

    fireEvent.change(screen.getByLabelText('检索测试'), { target: { value: '发票申请' } });
    await userEvent.click(screen.getByRole('button', { name: /检索/ }));

    expect(await screen.findByText('发票可以在订单完成后七日内申请。')).toBeInTheDocument();
  });

  it('manages vector store configs from the knowledge page', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /向量库配置/ }));
    expect(await screen.findByText('本地内存向量库')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /新增向量库/ }));
    fireEvent.change(await screen.findByLabelText('配置名称'), { target: { value: 'Elastic dev' } });
    fireEvent.change(screen.getByLabelText('索引名称'), { target: { value: 'kb_dev' } });
    await userEvent.click(screen.getByRole('button', { name: /保存配置/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.createVectorStoreConfig).toHaveBeenCalledWith(expect.objectContaining({
        name: 'Elastic dev',
        indexName: 'kb_dev'
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
  });

  it('lists documents, toggles chunks and deletes documents', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /管理文档/ }));
    expect(await screen.findByText('faq.txt')).toBeInTheDocument();
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
  });

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
  });
});

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <KnowledgeBasesPage />
    </QueryClientProvider>
  );
}
