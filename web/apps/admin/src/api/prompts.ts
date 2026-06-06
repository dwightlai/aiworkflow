import { requestJson } from './auth';

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export interface PromptTemplate {
  id: string;
  name: string;
  template: string;
  description: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface SavePromptTemplateRequest {
  name: string;
  template: string;
  description: string | null;
}

export async function listPromptTemplates(): Promise<PageResponse<PromptTemplate>> {
  return requestJson<PageResponse<PromptTemplate>>('/api/prompts');
}

export async function createPromptTemplate(request: SavePromptTemplateRequest): Promise<PromptTemplate> {
  return requestJson<PromptTemplate>('/api/prompts', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updatePromptTemplate(id: string, request: SavePromptTemplateRequest): Promise<PromptTemplate> {
  return requestJson<PromptTemplate>(`/api/prompts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deletePromptTemplate(id: string): Promise<void> {
  await requestJson<void>(`/api/prompts/${id}`, {
    method: 'DELETE'
  });
}

