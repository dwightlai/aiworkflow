import { requestJson } from './auth';
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
  hasDocx?: boolean;
  citations?: ResearchCitation[];
  status: string;
  createdAt?: string;
}

export interface CreateResearchJobRequest {
  templateId: string;
  themeLibraryId: string;
  knowledgeBaseIds: string[];
  variables: Record<string, string>;
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
