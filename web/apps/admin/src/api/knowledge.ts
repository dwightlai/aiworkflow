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

export interface SaveKnowledgeBaseRequest {
  name: string;
  description: string | null;
}

export interface AddKnowledgeDocumentRequest {
  name: string;
  content: string;
}

export interface SearchKnowledgeBaseRequest {
  query: string;
  topK: number;
}

export async function listKnowledgeBases(): Promise<PageResponse<KnowledgeBase>> {
  return requestJson<PageResponse<KnowledgeBase>>('/api/knowledge-bases');
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
