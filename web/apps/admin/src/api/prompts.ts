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
