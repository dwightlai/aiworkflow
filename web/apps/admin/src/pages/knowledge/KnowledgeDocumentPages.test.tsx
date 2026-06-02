// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { KnowledgeDocumentCreatePage } from './KnowledgeDocumentCreatePage';
import { KnowledgeDocumentEditPage } from './KnowledgeDocumentEditPage';
import { KnowledgeDocumentsPage } from './KnowledgeDocumentsPage';

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
        chunkCount: 2
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
  deleteKnowledgeDocument: vi.fn(async () => undefined),
  searchKnowledgeBase: vi.fn(async () => [
    {
      id: 'chunk_1',
      documentName: 'faq.txt',
      content: 'Refund requests are handled within seven days.',
      score: 2
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
  previewKnowledgeChunks: vi.fn(async () => [
    {
      index: 0,
      content: 'Manual refund policy text',
      tokenEstimate: 5
    }
  ]),
  addKnowledgeDocument: vi.fn(async () => ({
    id: 'doc_2',
    knowledgeBaseId: 'kb_1',
    name: 'manual.txt',
    chunkCount: 1
  })),
  updateKnowledgeChunk: vi.fn(async () => ({
    id: 'chunk_1',
    knowledgeBaseId: 'kb_1',
    documentId: 'doc_1',
    documentName: 'faq.txt',
    content: 'Updated refund requests are handled within three days.',
    index: 0,
    enabled: true,
    tokenEstimate: 12
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

describe('knowledge document pages', () => {
  it('shows the separated document list and navigates to create/edit pages', async () => {
    renderPage(<KnowledgeDocumentsPage knowledgeBaseId="kb_1" />);

    expect(await screen.findByText('产品知识库')).toBeInTheDocument();
    expect(await screen.findByText('faq.txt')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /新增文档/ }));
    expect(window.location.pathname).toBe('/knowledge/kb_1/documents/new');

    window.history.pushState(null, '', '/knowledge/kb_1/documents');
    await userEvent.click(screen.getByRole('button', { name: /查看切片/ }));
    expect(window.location.pathname).toBe('/knowledge/kb_1/documents/doc_1/edit');
  });

  it('previews and uploads a file from the separated create page', async () => {
    renderPage(<KnowledgeDocumentCreatePage knowledgeBaseId="kb_1" />);

    const file = new File(['Uploaded refund policy text'], 'policy.pdf', { type: 'application/pdf' });
    fireEvent.change(await screen.findByLabelText('选择知识库文件'), {
      target: { files: [file] }
    });
    await userEvent.click(screen.getByRole('button', { name: /预览分段/ }));

    expect(await screen.findByText('Uploaded refund policy text')).toBeInTheDocument();
    await waitFor(() => {
      expect(knowledgeApiMock.previewUploadedKnowledgeDocumentFile).toHaveBeenCalledWith(file, expect.objectContaining({
        splitterType: 'SIMPLE_TEXT',
        chunkSize: 500
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /确认上传/ }));
    await waitFor(() => {
      expect(knowledgeApiMock.uploadKnowledgeDocumentFile).toHaveBeenCalledWith('kb_1', file, expect.objectContaining({
        chunkOverlap: 50
      }));
    });
    expect(window.location.pathname).toBe('/knowledge/kb_1/documents');
  });

  it('edits chunks from the separated document edit page', async () => {
    renderPage(<KnowledgeDocumentEditPage knowledgeBaseId="kb_1" documentId="doc_1" />);

    expect(await screen.findByText(/Refund requests/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /编辑/ }));
    fireEvent.change(await screen.findByLabelText('切片内容'), {
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

function renderPage(element: React.ReactNode) {
  render(
    <QueryClientProvider client={new QueryClient()}>
      {element}
    </QueryClientProvider>
  );
}
