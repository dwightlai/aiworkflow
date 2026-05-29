import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  addKnowledgeDocument,
  createKnowledgeBase,
  listKnowledgeBases,
  searchKnowledgeBase
} from './knowledge';

describe('knowledge api', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('lists knowledge bases from the backend envelope', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: {
        items: [{ id: 'kb_1', name: '产品知识库', description: '客服资料', documentCount: 1, chunkCount: 2 }],
        total: 1
      },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const page = await listKnowledgeBases();

    expect(fetchMock).toHaveBeenCalledWith('/api/knowledge-bases');
    expect(page.items[0].name).toBe('产品知识库');
    expect(page.items[0].chunkCount).toBe(2);
  });

  it('creates bases, uploads documents and searches chunks', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'kb_1', name: '产品知识库', description: null, documentCount: 0, chunkCount: 0 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'doc_1', knowledgeBaseId: 'kb_1', name: 'faq.txt', chunkCount: 1 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: [{ id: 'chunk_1', documentName: 'faq.txt', content: '发票可以在订单完成后七日内申请。', score: 2 }],
        error: null
      }));
    vi.stubGlobal('fetch', fetchMock);

    const base = await createKnowledgeBase({ name: '产品知识库', description: null });
    await addKnowledgeDocument(base.id, { name: 'faq.txt', content: '发票可以在订单完成后七日内申请。' });
    const results = await searchKnowledgeBase(base.id, { query: '发票申请', topK: 3 });

    expect(results[0].documentName).toBe('faq.txt');
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/knowledge-bases', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/knowledge-bases/kb_1/documents', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, '/api/knowledge-bases/kb_1/search', expect.objectContaining({ method: 'POST' }));
  });
});

function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body
  } as Response;
}
