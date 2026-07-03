// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { KnowledgeDocumentsPage } from './KnowledgeDocumentsPage';

Object.defineProperty(window, 'matchMedia', {
  configurable: true,
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

const api = vi.hoisted(() => ({
  listKnowledgeBases: vi.fn(async () => ({
    items: [{
      id: 'kb_1',
      name: '制度库',
      description: null,
      documentCount: 1,
      chunkCount: 3,
      defaultDatasetId: 'dataset_1'
    }],
    total: 1
  })),
  listKnowledgeDatasets: vi.fn(async () => ({
    items: [{
      id: 'dataset_1',
      knowledgeBaseId: 'kb_1',
      name: '默认分类',
      datasetType: 'DEFAULT',
      documentCount: 1,
      chunkCount: 3,
      sourceCount: 1,
      indexStatus: 'READY',
      status: 'ENABLED'
    }],
    total: 1
  })),
  listKnowledgeDocuments: vi.fn(async () => ({
    items: [{
      id: 'doc_1',
      knowledgeBaseId: 'kb_1',
      name: '办公用品申领制度.docx',
      chunkCount: 3,
      processingStatus: 'READY',
      originalFileAvailable: true
    }],
    total: 1
  })),
  downloadKnowledgeDocumentOriginal: vi.fn(async () => new Blob(['original'])),
  addManualKnowledgeSource: vi.fn(),
  createKnowledgeDataset: vi.fn(),
  deleteKnowledgeDataset: vi.fn(),
  deleteKnowledgeDocument: vi.fn(),
  retrieveKnowledge: vi.fn(),
  updateKnowledgeDataset: vi.fn()
}));

vi.mock('../../api/knowledge', () => api);
vi.mock('../../navigation', () => ({
  navigateTo: vi.fn(),
  readDatasetIdFromSearch: vi.fn(() => null),
  withDatasetQuery: vi.fn((path: string) => path)
}));

describe('knowledge document original download', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('shows an authenticated download action beside documents that retain an original file', async () => {
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: vi.fn(() => 'blob:original')
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: vi.fn()
    });
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } }
    });
    render(
      <QueryClientProvider client={queryClient}>
        <KnowledgeDocumentsPage knowledgeBaseId="kb_1" />
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(api.listKnowledgeDocuments).toHaveBeenCalledWith('kb_1', 'dataset_1');
    });
    await screen.findByText('办公用品申领制度.docx');
    const download = await screen.findByRole('button', {
      name: /办公用品申领制度\.docx/
    });
    await userEvent.click(download);

    await waitFor(() => {
      expect(api.downloadKnowledgeDocumentOriginal).toHaveBeenCalledWith('kb_1', 'doc_1');
    });
  });
});
