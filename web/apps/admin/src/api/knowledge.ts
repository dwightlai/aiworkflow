import { requestBlob as authRequestBlob, requestJson as authRequestJson } from './auth';

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
  ownerUnitId?: string | null;
  embeddingModelId?: string | null;
  vectorStoreConfigId?: string | null;
  vectorDimension?: number;
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  semanticSimilarityThreshold?: number;
  retrievalMode?: string;
  topK?: number;
  status?: string;
  documentCount: number;
  chunkCount: number;
  kbType?: string;
  datasetMode?: string;
  defaultDatasetId?: string | null;
  datasetCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface KnowledgeDataset {
  id: string;
  knowledgeBaseId: string;
  name: string;
  code?: string | null;
  description?: string | null;
  topicId?: string | null;
  topicTitle?: string | null;
  datasetType: string;
  documentCount: number;
  chunkCount: number;
  sourceCount: number;
  indexStatus: string;
  lastSyncTime?: string | null;
  lastIndexTime?: string | null;
  status: string;
}

export interface KnowledgeSourceIndex {
  id: string;
  datasetId: string;
  topicId?: string | null;
  sourceTitleSnapshot?: string | null;
  materialSourceType?: string | null;
  materialType?: string | null;
  sourceArchiveFileId?: string | null;
  sourceRefId: string;
  indexStatus: string;
  documentId?: string | null;
  errorMessage?: string | null;
  lastSyncTime?: string | null;
  lastIndexTime?: string | null;
}

export interface SyncArchiveTopicResult {
  datasetId: string;
  sourceCount: number;
  newCount: number;
  changedCount: number;
  unchangedCount: number;
  failedCount: number;
}

export interface KnowledgeDocument {
  id: string;
  knowledgeBaseId: string;
  name: string;
  chunkCount: number;
  datasetType?: string;
  datasetId?: string | null;
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
  originalFileAvailable?: boolean;
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
  logicalChunkId?: string | null;
  parentChunkId?: string | null;
  groupId?: string | null;
  chunkLevel?: 'PARENT' | 'CHILD' | string;
  sectionPath?: string | null;
  chunkTitle?: string | null;
  chunkType?: string | null;
}

export interface KnowledgeChunkPreview {
  index: number;
  content: string;
  tokenEstimate: number;
  chunkType?: string;
  chunkTitle?: string | null;
  sectionPath?: string[];
  parentChunkId?: string | null;
  groupId?: string | null;
  chunkLevel?: 'PARENT' | 'CHILD' | string;
  atomic?: boolean;
  splitReason?: string;
}

export interface ChunkProfileVersion {
  id: string;
  knowledgeBaseId: string;
  version: number;
  name: string;
  strategy: string;
  chunkSize: number;
  chunkOverlap: number;
  configJson: string;
  status: string;
  createdAt: string;
}

export interface ChunkingQualityReport {
  chunkCount: number;
  totalTokens: number;
  minTokens: number;
  maxTokens: number;
  averageTokens: number;
  atomicChunkCount: number;
  atomicChunkRatio: number;
  parentLinkedChunkCount: number;
  parentCoverageRatio: number;
}

export interface ChunkProfileComparison {
  left: { profile: ChunkProfileVersion; quality: ChunkingQualityReport; chunks: KnowledgeChunkPreview[] };
  right: { profile: ChunkProfileVersion; quality: ChunkingQualityReport; chunks: KnowledgeChunkPreview[] };
}

export interface UploadedDocumentPreview {
  fileName: string;
  characterCount: number;
  chunks: KnowledgeChunkPreview[];
  warnings: string[];
}

export interface VectorStoreConfig {
  id: string;
  name: string;
  storeType: string;
  endpoint: string | null;
  indexName: string;
  host?: string | null;
  port?: number | null;
  databaseName?: string | null;
  namespaceName?: string | null;
  vectorDimension?: number;
  sslEnabled?: boolean;
  optionsJson?: string;
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
  host?: string | null;
  port?: number | null;
  databaseName?: string | null;
  namespaceName?: string | null;
  vectorDimension?: number;
  sslEnabled?: boolean;
  optionsJson?: string;
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
  ownerUnitId?: string | null;
  embeddingModelId?: string | null;
  vectorStoreConfigId?: string | null;
  vectorDimension?: number;
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  semanticSimilarityThreshold?: number;
  retrievalMode?: string;
  topK?: number;
  kbType?: string;
  datasetMode?: string;
}

export interface AddKnowledgeDocumentRequest {
  name: string;
  content: string;
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  semanticSimilarityThreshold?: number;
}

export interface UploadKnowledgeDocumentOptions {
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  semanticSimilarityThreshold?: number;
  knowledgeBaseId?: string;
}

export interface KnowledgeSplitOptions {
  splitterType?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  semanticSimilarityThreshold?: number;
  separator?: string | null;
  datasetId?: string;
  knowledgeBaseId?: string;
}

export interface ManualDatasetEntryRequest {
  title: string;
  content: string;
  tags?: string | null;
  category?: string | null;
  source?: string | null;
}

export interface CreateKnowledgeDatasetRequest {
  name: string;
  code?: string;
  description?: string;
  topicId?: string;
  topicTitle?: string;
}

export interface AddManualKnowledgeSourceRequest {
  title: string;
  content: string;
}

export interface AddManualDatasetRequest {
  entries: ManualDatasetEntryRequest[];
  datasetId?: string;
}

export interface UpdateKnowledgeDatasetRequest {
  name: string;
  description?: string;
}

export interface SearchKnowledgeBaseRequest {
  query: string;
  topK: number;
  datasetId?: string;
}

export interface KnowledgeRetrievalRequest {
  knowledgeBaseId: string;
  datasetId?: string;
  query: string;
  retrievalMode?: string;
  topK?: number;
  filters?: Record<string, unknown>;
}

export interface KnowledgeRetrievalItem {
  chunkId: string;
  documentId?: string | null;
  sourceIndexId?: string | null;
  content: string;
  score: number;
  sourceTitle?: string | null;
  sourceRefId?: string | null;
  sourceArchiveFileId?: string | null;
  sourcePage?: string | null;
  citationText?: string | null;
  metadata?: Record<string, unknown>;
}

export interface PreviewKnowledgeChunksRequest {
  content: string;
  splitterType: string;
  chunkSize: number;
  chunkOverlap: number;
  semanticSimilarityThreshold?: number;
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

export interface VectorStoreConnectionResult {
  success: boolean;
  storeType: string;
  serverVersion?: string | null;
  latencyMs: number;
  message: string;
}

export async function testVectorStoreConnection(
  request: SaveVectorStoreConfigRequest
): Promise<VectorStoreConnectionResult> {
  return requestJson<VectorStoreConnectionResult>('/api/vector-store-configs/test-connection', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function testSavedVectorStoreConnection(id: string): Promise<VectorStoreConnectionResult> {
  return requestJson<VectorStoreConnectionResult>(`/api/vector-store-configs/${id}/test-connection`, {
    method: 'POST'
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
  appendSemanticSimilarityThreshold(formData, options.semanticSimilarityThreshold);
  appendKnowledgeBaseId(formData, options.knowledgeBaseId);
  return requestJson<KnowledgeDocument>(`/api/knowledge-bases/${knowledgeBaseId}/documents/upload`, {
    method: 'POST',
    body: formData
  }, false);
}

export async function createKnowledgeDataset(
  knowledgeBaseId: string,
  request: CreateKnowledgeDatasetRequest
): Promise<KnowledgeDataset> {
  return requestJson<KnowledgeDataset>(`/api/knowledge-bases/${knowledgeBaseId}/datasets`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function addManualKnowledgeSource(
  datasetId: string,
  request: AddManualKnowledgeSourceRequest
): Promise<KnowledgeSourceIndex> {
  return requestJson<KnowledgeSourceIndex>(`/api/knowledge-datasets/${datasetId}/sources/manual`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateKnowledgeDataset(
  datasetId: string,
  request: UpdateKnowledgeDatasetRequest
): Promise<KnowledgeDataset> {
  return requestJson<KnowledgeDataset>(`/api/knowledge-datasets/${datasetId}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteKnowledgeDataset(datasetId: string): Promise<void> {
  await requestJson<void>(`/api/knowledge-datasets/${datasetId}`, { method: 'DELETE' });
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
  appendSemanticSimilarityThreshold(formData, options.semanticSimilarityThreshold);
  appendKnowledgeBaseId(formData, options.knowledgeBaseId);
  return requestJson<UploadedDocumentPreview>('/api/knowledge-bases/documents/upload/preview', {
    method: 'POST',
    body: formData
  }, false);
}

export async function listKnowledgeDocuments(
  knowledgeBaseId: string,
  datasetId?: string
): Promise<PageResponse<KnowledgeDocument>> {
  const query = datasetId ? `?datasetId=${encodeURIComponent(datasetId)}` : '';
  return requestJson<PageResponse<KnowledgeDocument>>(`/api/knowledge-bases/${knowledgeBaseId}/documents${query}`);
}

export async function deleteKnowledgeDocument(knowledgeBaseId: string, documentId: string): Promise<void> {
  await requestJson<void>(`/api/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`, {
    method: 'DELETE'
  });
}

export async function downloadKnowledgeDocumentOriginal(
  knowledgeBaseId: string,
  documentId: string
): Promise<Blob> {
  return authRequestBlob(
    `/api/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/original`
  );
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

export async function splitKnowledgeChunk(
  knowledgeBaseId: string,
  chunkId: string,
  offset: number
): Promise<KnowledgeChunk[]> {
  return requestJson<KnowledgeChunk[]>(
    `/api/knowledge-bases/${knowledgeBaseId}/chunk-operations/${chunkId}/split`,
    { method: 'POST', body: JSON.stringify({ offset }) }
  );
}

export async function mergeKnowledgeChunks(
  knowledgeBaseId: string,
  chunkIds: string[]
): Promise<KnowledgeChunk> {
  return requestJson<KnowledgeChunk>(
    `/api/knowledge-bases/${knowledgeBaseId}/chunk-operations/merge`,
    { method: 'POST', body: JSON.stringify({ chunkIds }) }
  );
}

export async function adjustKnowledgeChunkStructure(
  knowledgeBaseId: string,
  chunkId: string,
  sectionPath: string | null,
  parentChunkId: string | null
): Promise<KnowledgeChunk> {
  return requestJson<KnowledgeChunk>(
    `/api/knowledge-bases/${knowledgeBaseId}/chunk-operations/${chunkId}/structure`,
    { method: 'PUT', body: JSON.stringify({ sectionPath, parentChunkId }) }
  );
}

export async function listChunkProfiles(knowledgeBaseId: string): Promise<ChunkProfileVersion[]> {
  return requestJson<ChunkProfileVersion[]>(`/api/knowledge-bases/${knowledgeBaseId}/chunk-profiles`);
}

export async function createChunkProfile(
  knowledgeBaseId: string,
  request: {
    name: string;
    strategy: string;
    chunkSize: number;
    chunkOverlap: number;
    configJson?: string;
  }
): Promise<ChunkProfileVersion> {
  return requestJson<ChunkProfileVersion>(`/api/knowledge-bases/${knowledgeBaseId}/chunk-profiles`, {
    method: 'POST',
    body: JSON.stringify({ ...request, configJson: request.configJson || '{}' })
  });
}

export async function activateChunkProfile(
  knowledgeBaseId: string,
  profileId: string
): Promise<ChunkProfileVersion> {
  return requestJson<ChunkProfileVersion>(
    `/api/knowledge-bases/${knowledgeBaseId}/chunk-profiles/${profileId}/activate`,
    { method: 'POST' }
  );
}

export async function compareChunkProfiles(
  knowledgeBaseId: string,
  documentId: string,
  leftProfileId: string,
  rightProfileId: string
): Promise<ChunkProfileComparison> {
  return requestJson<ChunkProfileComparison>(
    `/api/knowledge-bases/${knowledgeBaseId}/chunk-profiles/compare`,
    {
      method: 'POST',
      body: JSON.stringify({ documentId, leftProfileId, rightProfileId })
    }
  );
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

export async function listKnowledgeDatasets(knowledgeBaseId: string): Promise<PageResponse<KnowledgeDataset>> {
  return requestJson<PageResponse<KnowledgeDataset>>(`/api/knowledge-bases/${knowledgeBaseId}/datasets`);
}

export async function getKnowledgeDataset(datasetId: string): Promise<KnowledgeDataset> {
  return requestJson<KnowledgeDataset>(`/api/knowledge-datasets/${datasetId}`);
}

export async function listKnowledgeSourceIndexes(datasetId: string): Promise<PageResponse<KnowledgeSourceIndex>> {
  return requestJson<PageResponse<KnowledgeSourceIndex>>(`/api/knowledge-datasets/${datasetId}/sources`);
}

export async function reindexKnowledgeSource(sourceIndexId: string): Promise<void> {
  await requestJson<void>(`/api/knowledge-sources/${sourceIndexId}/reindex`, { method: 'POST' });
}

export async function retrieveKnowledge(request: KnowledgeRetrievalRequest): Promise<{ items: KnowledgeRetrievalItem[] }> {
  return requestJson<{ items: KnowledgeRetrievalItem[] }>('/api/knowledge/retrieval', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

async function requestJson<T>(url: string, init?: RequestInit, jsonHeaders = true): Promise<T> {
  return authRequestJson<T>(url, init, { jsonHeaders });
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
  if (options.chunkOverlap !== undefined) {
    formData.append('chunkOverlap', String(options.chunkOverlap));
  }
  appendSemanticSimilarityThreshold(formData, options.semanticSimilarityThreshold);
  if (options.separator) {
    formData.append('separator', options.separator);
  }
  if (options.datasetId) {
    formData.append('datasetId', options.datasetId);
  }
  appendKnowledgeBaseId(formData, options.knowledgeBaseId);
  return requestJson<T>(url, {
    method: 'POST',
    body: formData
  }, false);
}

function appendSemanticSimilarityThreshold(formData: FormData, threshold?: number): void {
  if (threshold !== undefined) {
    formData.append('semanticSimilarityThreshold', String(threshold));
  }
}

function appendKnowledgeBaseId(formData: FormData, knowledgeBaseId?: string): void {
  if (knowledgeBaseId) {
    formData.append('knowledgeBaseId', knowledgeBaseId);
  }
}
