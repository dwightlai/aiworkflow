export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export interface KnowledgeBase {
  id: string;
  name: string;
  description: string | null;
  embeddingModelId?: string | null;
  vectorStoreConfigId?: string | null;
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  retrievalMode?: string;
  topK?: number;
  status?: string;
  documentCount: number;
  chunkCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface KnowledgeDocument {
  id: string;
  knowledgeBaseId: string;
  name: string;
  chunkCount: number;
  createdAt?: string;
}

export interface KnowledgeSearchResult {
  id: string;
  documentName: string;
  content: string;
  score: number;
}

export interface KnowledgeChunk {
  id: string;
  knowledgeBaseId: string;
  documentId: string;
  documentName: string;
  content: string;
  index: number;
  enabled: boolean;
  tokenEstimate: number;
}

export interface KnowledgeChunkPreview {
  index: number;
  content: string;
  tokenEstimate: number;
}

export interface VectorStoreConfig {
  id: string;
  name: string;
  storeType: string;
  endpoint: string | null;
  indexName: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveKnowledgeBaseRequest {
  name: string;
  description: string | null;
  embeddingModelId?: string | null;
  vectorStoreConfigId?: string | null;
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  retrievalMode?: string;
  topK?: number;
}

export interface AddKnowledgeDocumentRequest {
  name: string;
  content: string;
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
}

export interface SearchKnowledgeBaseRequest {
  query: string;
  topK: number;
}

export interface PreviewKnowledgeChunksRequest {
  content: string;
  splitterType: string;
  chunkSize: number;
  chunkOverlap: number;
}

export async function listKnowledgeBases(): Promise<PageResponse<KnowledgeBase>> {
  return requestJson<PageResponse<KnowledgeBase>>('/api/knowledge-bases');
}

export async function listVectorStoreConfigs(): Promise<PageResponse<VectorStoreConfig>> {
  return requestJson<PageResponse<VectorStoreConfig>>('/api/vector-store-configs');
}

export async function createKnowledgeBase(request: SaveKnowledgeBaseRequest): Promise<KnowledgeBase> {
  return requestJson<KnowledgeBase>('/api/knowledge-bases', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function addKnowledgeDocument(
  knowledgeBaseId: string,
  request: AddKnowledgeDocumentRequest
): Promise<KnowledgeDocument> {
  return requestJson<KnowledgeDocument>(`/api/knowledge-bases/${knowledgeBaseId}/documents`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function previewKnowledgeChunks(request: PreviewKnowledgeChunksRequest): Promise<KnowledgeChunkPreview[]> {
  return requestJson<KnowledgeChunkPreview[]>('/api/knowledge-bases/chunks/preview', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function listKnowledgeDocumentChunks(
  knowledgeBaseId: string,
  documentId: string
): Promise<PageResponse<KnowledgeChunk>> {
  return requestJson<PageResponse<KnowledgeChunk>>(`/api/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/chunks`);
}

export async function searchKnowledgeBase(
  knowledgeBaseId: string,
  request: SearchKnowledgeBaseRequest
): Promise<KnowledgeSearchResult[]> {
  return requestJson<KnowledgeSearchResult[]>(`/api/knowledge-bases/${knowledgeBaseId}/search`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = init ? await fetch(url, withJsonHeaders(init)) : await fetch(url);
  const envelope = await response.json() as ApiEnvelope<T>;
  if (!response.ok || !envelope.success) {
    throw new Error(envelope.error?.message ?? `Request failed: ${response.status}`);
  }
  return envelope.data;
}

function withJsonHeaders(init?: RequestInit): RequestInit | undefined {
  if (!init) {
    return undefined;
  }
  return {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init.headers
    }
  };
}
