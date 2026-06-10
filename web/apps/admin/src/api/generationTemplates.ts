import { requestJson } from './auth';

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export interface GenerationTemplateVariable {
  name: string;
  label: string;
  type: string;
  required?: boolean;
}

export interface GenerationTemplateSection {
  key: string;
  title: string;
  instruction: string;
  requiredSources?: string[];
  citationRequired?: boolean;
  outputFormat?: string;
}

export interface GenerationTemplateSchema {
  title?: string;
  variables?: GenerationTemplateVariable[];
  sections?: GenerationTemplateSection[];
}

export interface WorkflowSnapshotNode {
  id: string;
  name: string;
  type?: string;
  order?: number;
}

export interface WorkflowSnapshot {
  workflowId?: string;
  name?: string;
  nodes?: WorkflowSnapshotNode[];
}

export interface GenerationTemplate {
  id: string;
  name: string;
  code: string;
  description: string | null;
  category: string;
  ownerUnitId?: string | null;
  outputType: string;
  templateSchema: GenerationTemplateSchema | string;
  workflowId?: string | null;
  workflowSnapshot?: WorkflowSnapshot | string | null;
  status: string;
  version?: number;
  createdBy?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveGenerationTemplateRequest {
  name: string;
  code: string;
  description: string | null;
  category: string;
  outputType: string;
  templateSchema: GenerationTemplateSchema | string;
  workflowId?: string | null;
  workflowSnapshot?: WorkflowSnapshot | string | null;
  status: string;
}

export async function listGenerationTemplates(): Promise<PageResponse<GenerationTemplate>> {
  return requestJson<PageResponse<GenerationTemplate>>('/api/generation-templates');
}

export async function createGenerationTemplate(request: SaveGenerationTemplateRequest): Promise<GenerationTemplate> {
  return requestJson<GenerationTemplate>('/api/generation-templates', {
    method: 'POST',
    body: JSON.stringify(normalizeRequest(request))
  });
}

export async function updateGenerationTemplate(
  id: string,
  request: SaveGenerationTemplateRequest
): Promise<GenerationTemplate> {
  return requestJson<GenerationTemplate>(`/api/generation-templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(normalizeRequest(request))
  });
}

export async function deleteGenerationTemplate(id: string): Promise<void> {
  await requestJson<void>(`/api/generation-templates/${id}`, {
    method: 'DELETE'
  });
}

export function parseTemplateSchema(value: GenerationTemplateSchema | string | null | undefined): GenerationTemplateSchema {
  if (!value) {
    return { sections: [] };
  }
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as GenerationTemplateSchema;
    } catch {
      return { sections: [] };
    }
  }
  return value;
}

export function parseWorkflowSnapshot(value: WorkflowSnapshot | string | null | undefined): WorkflowSnapshot {
  if (!value) {
    return { nodes: [] };
  }
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as WorkflowSnapshot;
    } catch {
      return { nodes: [] };
    }
  }
  return value;
}

function normalizeRequest(request: SaveGenerationTemplateRequest): SaveGenerationTemplateRequest {
  return {
    ...request,
    description: request.description || null,
    workflowId: request.workflowId || null,
    templateSchema: typeof request.templateSchema === 'string'
      ? request.templateSchema
      : JSON.stringify(request.templateSchema),
    workflowSnapshot: request.workflowSnapshot == null
      ? null
      : typeof request.workflowSnapshot === 'string'
        ? request.workflowSnapshot
        : JSON.stringify(request.workflowSnapshot)
  };
}
