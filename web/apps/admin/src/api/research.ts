import { getAuthSession, requestJson } from './auth';
import type { WorkflowSnapshot } from './generationTemplates';

export interface ThemeLibrary {
  id: string;
  name: string;
  description?: string | null;
  itemCount?: number;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export interface ResearchCitation {
  sourceType: string;
  sourceId: string;
  title: string;
  metadata?: Record<string, string>;
  attachmentName?: string | null;
  chunkId?: string | null;
}

export interface ResearchJobOutlineSection {
  key: string;
  title: string;
}

export interface ResearchJobSectionOutput {
  key: string;
  title: string;
  contentMarkdown: string;
  citations?: ResearchCitation[];
}

export interface ResearchJob {
  id: string;
  templateId: string;
  status: string;
  variables: Record<string, string>;
  knowledgeBaseIds: string[];
  externalCorpusRef?: string | null;
  outline?: ResearchJobOutlineSection[];
  sectionOutputs?: ResearchJobSectionOutput[];
  workflowRunSnapshot?: WorkflowSnapshot | string;
  errorMessage?: string | null;
  startedAt?: string;
  completedAt?: string;
  createdAt?: string;
}

export interface GenerationOutput {
  id: string;
  jobId: string;
  title: string;
  outputType: string;
  contentMarkdown: string;
  contentJson?: Record<string, unknown> | null;
  hasDocx?: boolean;
  outputTemplateId?: string | null;
  citations?: ResearchCitation[];
  status: string;
  createdAt?: string;
}

export interface ResearchOutputTemplate {
  id: string;
  name: string;
  templateCategory: string;
  outputType: string;
}

export interface CreateResearchJobRequest {
  templateId: string;
  themeLibraryId: string;
  knowledgeBaseIds: string[];
  variables: Record<string, string>;
}

export interface CompileFromKnowledgeDatasetRequest {
  templateId: string;
  knowledgeBaseId: string;
  datasetId: string;
  compileType?: string;
  outputTemplateCode?: string;
  variables?: Record<string, string>;
}

export async function compileFromKnowledgeDataset(request: CompileFromKnowledgeDatasetRequest): Promise<ResearchJob> {
  return requestJson<ResearchJob>('/api/research/compile/from-knowledge-dataset', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function listThemeLibraries(): Promise<PageResponse<ThemeLibrary>> {
  return requestJson<PageResponse<ThemeLibrary>>('/api/research/theme-libraries');
}

export async function createResearchJob(request: CreateResearchJobRequest): Promise<ResearchJob> {
  return requestJson<ResearchJob>('/api/research/jobs', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function getResearchJob(id: string): Promise<ResearchJob> {
  return requestJson<ResearchJob>(`/api/research/jobs/${id}`);
}

export async function getResearchJobOutput(id: string): Promise<GenerationOutput> {
  return requestJson<GenerationOutput>(`/api/research/jobs/${id}/output`);
}

export function getResearchOutputDocxUrl(outputId: string): string {
  return `/api/research/outputs/${outputId}/docx`;
}

export function getResearchOutputHtmlPreviewUrl(outputId: string): string {
  return `/api/research/outputs/${outputId}/html-preview`;
}

async function authorizedFetch(url: string): Promise<Response> {
  const session = getAuthSession();
  const headers: Record<string, string> = {};
  if (session?.accessToken) {
    headers.Authorization = `Bearer ${session.accessToken}`;
  }
  const response = await fetch(url, { headers });
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  return response;
}

export async function fetchResearchOutputHtmlPreview(outputId: string): Promise<string> {
  const response = await authorizedFetch(getResearchOutputHtmlPreviewUrl(outputId));
  return response.text();
}

export async function fetchResearchOutputDocx(outputId: string): Promise<Blob> {
  const response = await authorizedFetch(getResearchOutputDocxUrl(outputId));
  return response.blob();
}

export async function listResearchOutputTemplates(
  outputType?: string,
  templateCategory?: string
): Promise<PageResponse<ResearchOutputTemplate>> {
  const params = new URLSearchParams();
  if (outputType) params.set('outputType', outputType);
  if (templateCategory) params.set('templateCategory', templateCategory);
  const query = params.toString();
  return requestJson<PageResponse<ResearchOutputTemplate>>(
    `/api/research/output-templates${query ? `?${query}` : ''}`
  );
}

export async function renderResearchOutputDocx(
  outputId: string,
  outputTemplateId?: string
): Promise<GenerationOutput> {
  return requestJson<GenerationOutput>(`/api/research/outputs/${outputId}/render-docx`, {
    method: 'POST',
    body: JSON.stringify({ outputTemplateId: outputTemplateId ?? null })
  });
}
