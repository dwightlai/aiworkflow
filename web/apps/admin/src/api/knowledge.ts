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
  vectorDimension?: number;
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
  datasetType?: string;
  processingStatus?: string;
  tags?: string | null;
  category?: string | null;
  source?: string | null;
  rowCount?: number | null;
  parserType?: string | null;
  splitterType?: string | null;
  splitterConfig?: string | null;
  errorMessage?: string | null;
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

export interface UploadedDocumentPreview {
  fileName: string;
  characterCount: number;
  chunks: KnowledgeChunkPreview[];
}

export interface VectorStoreConfig {
  id: string;
  name: string;
  storeType: string;
  endpoint: string | null;
  indexName: string;
  username?: string | null;
  passwordConfigured?: boolean;
  apiKeyConfigured?: boolean;
  connectTimeoutMs?: number;
  readTimeoutMs?: number;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveVectorStoreConfigRequest {
  name: string;
  storeType: string;
  endpoint: string | null;
  indexName: string;
  username?: string | null;
  password?: string | null;
  apiKey?: string | null;
  connectTimeoutMs?: number;
  readTimeoutMs?: number;
  enabled: boolean;
}

export interface SaveKnowledgeBaseRequest {
  name: string;
  description: string | null;
  embeddingModelId?: string | null;
  vectorStoreConfigId?: string | null;
  vectorDimension?: number;
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

export interface UploadKnowledgeDocumentOptions {
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
}

export interface KnowledgeSplitOptions {
  splitterType?: string;
  chunkSize?: number;
  separator?: string | null;
}

export interface ManualDatasetEntryRequest {
  title: string;
  content: string;
  tags?: string | null;
  category?: string | null;
  source?: string | null;
}

export interface AddManualDatasetRequest {
  entries: ManualDatasetEntryRequest[];
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

export interface UpdateKnowledgeChunkRequest {
  content: string;
  enabled: boolean;
}

export async function listKnowledgeBases(): Promise<PageResponse<KnowledgeBase>> {
  return requestJson<PageResponse<KnowledgeBase>>('/api/knowledge-bases');
}

export async function listVectorStoreConfigs(): Promise<PageResponse<VectorStoreConfig>> {
  return requestJson<PageResponse<VectorStoreConfig>>('/api/vector-store-configs');
}

export async function createVectorStoreConfig(request: SaveVectorStoreConfigRequest): Promise<VectorStoreConfig> {
  return requestJson<VectorStoreConfig>('/api/vector-store-configs', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateVectorStoreConfig(
  id: string,
  request: SaveVectorStoreConfigRequest
): Promise<VectorStoreConfig> {
  return requestJson<VectorStoreConfig>(`/api/vector-store-configs/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteVectorStoreConfig(id: string): Promise<void> {
  await requestJson<void>(`/api/vector-store-configs/${id}`, {
    method: 'DELETE'
  });
}

export async function createKnowledgeBase(request: SaveKnowledgeBaseRequest): Promise<KnowledgeBase> {
  return requestJson<KnowledgeBase>('/api/knowledge-bases', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateKnowledgeBase(id: string, request: SaveKnowledgeBaseRequest): Promise<KnowledgeBase> {
  return requestJson<KnowledgeBase>(`/api/knowledge-bases/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteKnowledgeBase(id: string): Promise<void> {
  await requestJson<void>(`/api/knowledge-bases/${id}`, {
    method: 'DELETE'
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

export async function uploadKnowledgeDocumentFile(
  knowledgeBaseId: string,
  file: File,
  options: UploadKnowledgeDocumentOptions = {}
): Promise<KnowledgeDocument> {
  const formData = new FormData();
  formData.append('file', file);
  if (options.splitterType) {
    formData.append('splitterType', options.splitterType);
  }
  if (options.chunkSize) {
    formData.append('chunkSize', String(options.chunkSize));
  }
  if (options.chunkOverlap !== undefined) {
    formData.append('chunkOverlap', String(options.chunkOverlap));
  }
  return requestJson<KnowledgeDocument>(`/api/knowledge-bases/${knowledgeBaseId}/documents/upload`, {
    method: 'POST',
    body: formData
  }, false);
}

export async function addManualKnowledgeDataset(
  knowledgeBaseId: string,
  request: AddManualDatasetRequest
): Promise<KnowledgeDocument> {
  return requestJson<KnowledgeDocument>(`/api/knowledge-bases/${knowledgeBaseId}/documents/manual`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function uploadTextKnowledgeDocumentFile(
  knowledgeBaseId: string,
  file: File,
  options: KnowledgeSplitOptions = {}
): Promise<KnowledgeDocument> {
  return uploadDatasetFile(`/api/knowledge-bases/${knowledgeBaseId}/documents/text/upload`, file, options);
}

export async function uploadTableKnowledgeDocumentFile(
  knowledgeBaseId: string,
  file: File,
  options: KnowledgeSplitOptions = {}
): Promise<KnowledgeDocument> {
  return uploadDatasetFile(`/api/knowledge-bases/${knowledgeBaseId}/documents/table/upload`, file, options);
}

export async function previewUploadedTextKnowledgeDocumentFile(
  file: File,
  options: KnowledgeSplitOptions = {}
): Promise<UploadedDocumentPreview> {
  return uploadDatasetFile('/api/knowledge-bases/documents/text/upload/preview', file, options);
}

export async function previewUploadedTableKnowledgeDocumentFile(
  file: File,
  options: KnowledgeSplitOptions = {}
): Promise<UploadedDocumentPreview> {
  return uploadDatasetFile('/api/knowledge-bases/documents/table/upload/preview', file, options);
}

export async function reparseKnowledgeDocument(
  knowledgeBaseId: string,
  documentId: string
): Promise<KnowledgeDocument> {
  return requestJson<KnowledgeDocument>(`/api/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/reparse`, {
    method: 'POST'
  });
}

export async function previewUploadedKnowledgeDocumentFile(
  file: File,
  options: UploadKnowledgeDocumentOptions = {}
): Promise<UploadedDocumentPreview> {
  const formData = new FormData();
  formData.append('file', file);
  if (options.splitterType) {
    formData.append('splitterType', options.splitterType);
  }
  if (options.chunkSize) {
    formData.append('chunkSize', String(options.chunkSize));
  }
  if (options.chunkOverlap !== undefined) {
    formData.append('chunkOverlap', String(options.chunkOverlap));
  }
  return requestJson<UploadedDocumentPreview>('/api/knowledge-bases/documents/upload/preview', {
    method: 'POST',
    body: formData
  }, false);
}

export async function listKnowledgeDocuments(knowledgeBaseId: string): Promise<PageResponse<KnowledgeDocument>> {
  return requestJson<PageResponse<KnowledgeDocument>>(`/api/knowledge-bases/${knowledgeBaseId}/documents`);
}

export async function deleteKnowledgeDocument(knowledgeBaseId: string, documentId: string): Promise<void> {
  await requestJson<void>(`/api/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`, {
    method: 'DELETE'
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

export async function updateKnowledgeChunk(
  knowledgeBaseId: string,
  chunkId: string,
  request: UpdateKnowledgeChunkRequest
): Promise<KnowledgeChunk> {
  return requestJson<KnowledgeChunk>(`/api/knowledge-bases/${knowledgeBaseId}/chunks/${chunkId}`, {
    method: 'PUT',
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

export async function searchKnowledgeDocument(
  knowledgeBaseId: string,
  documentId: string,
  request: SearchKnowledgeBaseRequest
): Promise<KnowledgeSearchResult[]> {
  return requestJson<KnowledgeSearchResult[]>(`/api/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/search`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

async function requestJson<T>(url: string, init?: RequestInit, jsonHeaders = true): Promise<T> {
  const response = init ? await fetch(url, jsonHeaders ? withJsonHeaders(init) : init) : await fetch(url);
  const envelope = await response.json() as ApiEnvelope<T>;
  if (!response.ok || !envelope.success) {
    throw new Error(envelope.error?.message ?? `Request failed: ${response.status}`);
  }
  return envelope.data;
}

function uploadDatasetFile<T>(
  url: string,
  file: File,
  options: KnowledgeSplitOptions = {}
): Promise<T> {
  const formData = new FormData();
  formData.append('file', file);
  if (options.splitterType) {
    formData.append('splitterType', options.splitterType);
  }
  if (options.chunkSize) {
    formData.append('chunkSize', String(options.chunkSize));
  }
  if (options.separator) {
    formData.append('separator', options.separator);
  }
  return requestJson<T>(url, {
    method: 'POST',
    body: formData
  }, false);
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
