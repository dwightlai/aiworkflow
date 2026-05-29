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
