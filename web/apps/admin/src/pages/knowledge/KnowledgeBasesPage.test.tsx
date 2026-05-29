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
  addKnowledgeDocument: vi.fn(async () => ({
    id: 'doc_1',
    knowledgeBaseId: 'kb_1',
    name: 'faq.txt',
    chunkCount: 1
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

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('KnowledgeBasesPage', () => {
  it('lists knowledge bases and ingestion metrics', async () => {
    renderPage();

    expect(await screen.findByText('产品知识库')).toBeInTheDocument();
    expect(screen.getByText('知识库总数')).toBeInTheDocument();
    expect(screen.getAllByText('文档数').length).toBeGreaterThan(0);
    expect(screen.getAllByText('切片数').length).toBeGreaterThan(0);
    expect(screen.getAllByText('工作流可引用').length).toBeGreaterThan(0);
  });

  it('creates a knowledge base from the drawer form', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /新增知识库/ }));
    fireEvent.change(await screen.findByLabelText('知识库名称'), { target: { value: '售后知识库' } });
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.createKnowledgeBase).toHaveBeenCalledWith({
        name: '售后知识库',
        description: null
      });
    });
  });

  it('uploads documents and searches the selected knowledge base', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /管理文档/ }));
    fireEvent.change(await screen.findByLabelText('文档名称'), { target: { value: 'faq.txt' } });
    fireEvent.change(screen.getByLabelText('文档内容'), { target: { value: '发票可以在订单完成后七日内申请。' } });
    await userEvent.click(screen.getByRole('button', { name: /入库/ }));

    await waitFor(() => {
      expect(knowledgeApiMock.addKnowledgeDocument).toHaveBeenCalledWith('kb_1', {
        name: 'faq.txt',
        content: '发票可以在订单完成后七日内申请。'
      });
    });

    fireEvent.change(screen.getByLabelText('检索测试'), { target: { value: '发票申请' } });
    await userEvent.click(screen.getByRole('button', { name: /检索/ }));

    expect(await screen.findByText('发票可以在订单完成后七日内申请。')).toBeInTheDocument();
  });
});

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <KnowledgeBasesPage />
    </QueryClientProvider>
  );
}
