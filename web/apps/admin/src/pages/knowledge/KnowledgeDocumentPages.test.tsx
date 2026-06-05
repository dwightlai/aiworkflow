// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { KnowledgeDocumentEditPage } from './KnowledgeDocumentEditPage';

const chunks = [
  {
    id: 'chunk_1',
    knowledgeBaseId: 'kb_1',
    documentId: 'doc_1',
    documentName: 'faq.txt',
    content: 'Refund requests are handled within seven days.',
    index: 0,
    enabled: true,
    tokenEstimate: 12
  },
  {
    id: 'chunk_2',
    knowledgeBaseId: 'kb_1',
    documentId: 'doc_1',
    documentName: 'faq.txt',
    content: 'Invoices can be downloaded after payment.',
    index: 1,
    enabled: true,
    tokenEstimate: 9
  },
  {
    id: 'chunk_3',
    knowledgeBaseId: 'kb_1',
    documentId: 'doc_1',
    documentName: 'faq.txt',
    content: '4.1.1.1 电子文件来源真实性：应检查电子文件形成、捕获和归档过程记录。',
    index: 2,
    enabled: true,
    tokenEstimate: 16
  }
];

const knowledgeApiMock = vi.hoisted(() => ({
  listKnowledgeBases: vi.fn(async () => ({
    items: [
      {
        id: 'kb_1',
        name: '产品知识库',
        description: '客服资料',
        embeddingModelId: 'embed_1',
        vectorDimension: 1536,
        splitterType: 'SIMPLE_TEXT',
        chunkSize: 500,
        chunkOverlap: 50,
        retrievalMode: 'HYBRID',
        topK: 5,
        status: 'READY',
        documentCount: 1,
        chunkCount: 3
      }
    ],
    total: 1
  })),
  listKnowledgeDocuments: vi.fn(async () => ({
    items: [
      {
        id: 'doc_1',
        knowledgeBaseId: 'kb_1',
        name: 'faq.txt',
        chunkCount: 3
      }
    ],
    total: 1
  })),
  listKnowledgeDocumentChunks: vi.fn(async () => ({
    items: chunks,
    total: chunks.length
  })),
  searchKnowledgeDocument: vi.fn(async (_knowledgeBaseId: string, _documentId: string, request: { query: string }) => {
    if (request.query.toLowerCase().includes('invoice')) {
      return [{ id: 'chunk_2', documentName: 'faq.txt', content: chunks[1].content, score: 10 }];
    }
    return [{ id: 'chunk_3', documentName: 'faq.txt', content: chunks[2].content, score: 12 }];
  }),
  updateKnowledgeChunk: vi.fn(async () => ({
    ...chunks[0],
    content: 'Updated refund requests are handled within three days.'
  }))
}));

vi.mock('../../api/knowledge', () => knowledgeApiMock);

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

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  window.history.pushState(null, '', '/');
});

describe('knowledge document edit page', () => {
  it('edits chunks from the separated document edit page', async () => {
    renderPage(<KnowledgeDocumentEditPage knowledgeBaseId="kb_1" documentId="doc_1" />);

    expect(await screen.findByText(/Refund requests/)).toBeInTheDocument();
    await userEvent.click(screen.getAllByRole('button', { name: /编辑/ })[0]);
    fireEvent.change(await screen.findByLabelText('分段内容'), {
      target: { value: 'Updated refund requests are handled within three days.' }
    });
    await userEvent.click(screen.getByRole('button', { name: /保存分段/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.updateKnowledgeChunk).toHaveBeenCalledWith('kb_1', 'chunk_1', expect.objectContaining({
        content: 'Updated refund requests are handled within three days.',
        enabled: true
      }));
    });
  });

  it('searches chunks through the current document search endpoint', async () => {
    renderPage(<KnowledgeDocumentEditPage knowledgeBaseId="kb_1" documentId="doc_1" />);

    expect(await screen.findByText(/Refund requests/)).toBeInTheDocument();
    await userEvent.type(screen.getByPlaceholderText('搜索相关数据'), 'invoice');

    await waitFor(() => {
      expect(knowledgeApiMock.searchKnowledgeDocument).toHaveBeenLastCalledWith('kb_1', 'doc_1', expect.objectContaining({
        query: 'invoice',
        topK: 5
      }));
    });
    expect(screen.queryByText(/Refund requests/)).not.toBeInTheDocument();
    expect(screen.getByText(/Invoices can be downloaded/)).toBeInTheDocument();
    expect(screen.getByText('匹配 1 / 3 个分段')).toBeInTheDocument();
  });

  it('matches natural language questions inside the current document only', async () => {
    renderPage(<KnowledgeDocumentEditPage knowledgeBaseId="kb_1" documentId="doc_1" />);

    expect(await screen.findByText(/电子文件来源真实性/)).toBeInTheDocument();
    await userEvent.type(screen.getByPlaceholderText('搜索相关数据'), '归档时怎么判断电子文件来源是不是真实的？');

    await waitFor(() => {
      expect(knowledgeApiMock.searchKnowledgeDocument).toHaveBeenLastCalledWith('kb_1', 'doc_1', expect.objectContaining({
        query: '归档时怎么判断电子文件来源是不是真实的？',
        topK: 5
      }));
    });
    expect(screen.getByText(/电子文件来源真实性/)).toBeInTheDocument();
    expect(screen.getByText('匹配 1 / 3 个分段')).toBeInTheDocument();
  });
});

function renderPage(element: React.ReactNode) {
  render(
    <QueryClientProvider client={new QueryClient()}>
      {element}
    </QueryClientProvider>
  );
}
