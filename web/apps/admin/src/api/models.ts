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

export interface ModelProvider {
  id: string;
  name: string;
  modelType: string;
  modelUsage: string;
  description: string | null;
  visionSupport: boolean;
  pricePerMillionTokens: number | null;
  baseUrl: string;
  model: string;
  apiKeyRef: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateModelProviderRequest {
  name: string;
  modelType: string;
  modelUsage: string;
  description: string | null;
  visionSupport: boolean;
  pricePerMillionTokens: number | null;
  baseUrl: string;
  model: string;
  apiKeyRef: string;
  enabled: boolean;
}

export async function listModelProviders(): Promise<PageResponse<ModelProvider>> {
  return requestJson<PageResponse<ModelProvider>>('/api/model-providers');
}

export async function createModelProvider(request: CreateModelProviderRequest): Promise<ModelProvider> {
  return requestJson<ModelProvider>('/api/model-providers', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateModelProvider(id: string, request: CreateModelProviderRequest): Promise<ModelProvider> {
  return requestJson<ModelProvider>(`/api/model-providers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteModelProvider(id: string): Promise<void> {
  await requestJson<void>(`/api/model-providers/${id}`, {
    method: 'DELETE'
  });
}

