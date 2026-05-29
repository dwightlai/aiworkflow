import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  addKnowledgeDocument,
  createVectorStoreConfig,
  createKnowledgeBase,
  deleteKnowledgeBase,
  deleteKnowledgeDocument,
  deleteVectorStoreConfig,
  listKnowledgeDocumentChunks,
  listKnowledgeDocuments,
  listKnowledgeBases,
  listVectorStoreConfigs,
  previewKnowledgeChunks,
  searchKnowledgeBase,
  updateKnowledgeBase,
  updateKnowledgeChunk,
  updateVectorStoreConfig
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

  it('manages vector stores and previews document chunks', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { items: [{ id: 'vector_1', name: 'Memory', storeType: 'MEMORY', endpoint: '', indexName: 'aiworkflow_kb', enabled: true }], total: 1 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: [{ index: 0, content: '# Refund\nRefund policy', tokenEstimate: 6 }],
        error: null
      }));
    vi.stubGlobal('fetch', fetchMock);

    const configs = await listVectorStoreConfigs();
    const chunks = await previewKnowledgeChunks({
      content: '# Refund\nRefund policy',
      splitterType: 'MARKDOWN_HEADING',
      chunkSize: 120,
      chunkOverlap: 10
    });

    expect(configs.items[0].storeType).toBe('MEMORY');
    expect(chunks[0].content).toContain('Refund policy');
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/vector-store-configs');
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/knowledge-bases/chunks/preview', expect.objectContaining({ method: 'POST' }));
  });

  it('updates vector stores and manages ingested document chunks', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'vector_1', name: 'Elastic dev', storeType: 'ELASTICSEARCH', endpoint: 'http://localhost:9200', indexName: 'kb_dev', enabled: true },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'vector_1', name: 'Elastic prod', storeType: 'ELASTICSEARCH', endpoint: 'https://es.example.com', indexName: 'kb_prod', enabled: false },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({ success: true, data: null, error: null }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { items: [{ id: 'doc_1', knowledgeBaseId: 'kb_1', name: 'faq.txt', chunkCount: 1 }], total: 1 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { items: [{ id: 'chunk_1', knowledgeBaseId: 'kb_1', documentId: 'doc_1', documentName: 'faq.txt', content: 'Refund policy', index: 0, enabled: true, tokenEstimate: 4 }], total: 1 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'chunk_1', knowledgeBaseId: 'kb_1', documentId: 'doc_1', documentName: 'faq.txt', content: 'Updated refund policy', index: 0, enabled: false, tokenEstimate: 5 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({ success: true, data: null, error: null }));
    vi.stubGlobal('fetch', fetchMock);

    await createVectorStoreConfig({ name: 'Elastic dev', storeType: 'ELASTICSEARCH', endpoint: 'http://localhost:9200', indexName: 'kb_dev', enabled: true });
    await updateVectorStoreConfig('vector_1', { name: 'Elastic prod', storeType: 'ELASTICSEARCH', endpoint: 'https://es.example.com', indexName: 'kb_prod', enabled: false });
    await deleteVectorStoreConfig('vector_1');
    const documents = await listKnowledgeDocuments('kb_1');
    const chunks = await listKnowledgeDocumentChunks('kb_1', 'doc_1');
    const updatedChunk = await updateKnowledgeChunk('kb_1', 'chunk_1', { content: 'Updated refund policy', enabled: false });
    await deleteKnowledgeDocument('kb_1', 'doc_1');

    expect(documents.items[0].name).toBe('faq.txt');
    expect(chunks.items[0].content).toBe('Refund policy');
    expect(updatedChunk.enabled).toBe(false);
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/vector-store-configs', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/vector-store-configs/vector_1', expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, '/api/vector-store-configs/vector_1', expect.objectContaining({ method: 'DELETE' }));
    expect(fetchMock).toHaveBeenNthCalledWith(4, '/api/knowledge-bases/kb_1/documents');
    expect(fetchMock).toHaveBeenNthCalledWith(5, '/api/knowledge-bases/kb_1/documents/doc_1/chunks');
    expect(fetchMock).toHaveBeenNthCalledWith(6, '/api/knowledge-bases/kb_1/chunks/chunk_1', expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenNthCalledWith(7, '/api/knowledge-bases/kb_1/documents/doc_1', expect.objectContaining({ method: 'DELETE' }));
  });

  it('updates and deletes knowledge bases', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: {
          id: 'kb_1',
          name: 'ops-kb-prod',
          description: 'production docs',
          embeddingModelId: 'embed-prod',
          vectorStoreConfigId: 'vector-prod',
          splitterType: 'MARKDOWN_HEADING',
          chunkSize: 180,
          chunkOverlap: 30,
          retrievalMode: 'HYBRID',
          topK: 6,
          documentCount: 1,
          chunkCount: 3
        },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({ success: true, data: null, error: null }));
    vi.stubGlobal('fetch', fetchMock);

    const updated = await updateKnowledgeBase('kb_1', {
      name: 'ops-kb-prod',
      description: 'production docs',
      embeddingModelId: 'embed-prod',
      vectorStoreConfigId: 'vector-prod',
      splitterType: 'MARKDOWN_HEADING',
      chunkSize: 180,
      chunkOverlap: 30,
      retrievalMode: 'HYBRID',
      topK: 6
    });
    await deleteKnowledgeBase('kb_1');

    expect(updated.name).toBe('ops-kb-prod');
    expect(updated.vectorStoreConfigId).toBe('vector-prod');
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/knowledge-bases/kb_1', expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/knowledge-bases/kb_1', expect.objectContaining({ method: 'DELETE' }));
  });
});

function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body
  } as Response;
}
