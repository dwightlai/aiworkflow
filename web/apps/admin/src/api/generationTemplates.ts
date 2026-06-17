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
  templateCategory?: string | null;
  docxConfig?: string | Record<string, unknown> | null;
  layoutConfig?: string | Record<string, unknown> | null;
  linkedHtmlTemplateId?: string | null;
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
  templateCategory?: string | null;
  docxConfig?: string | null;
  layoutConfig?: string | null;
  linkedHtmlTemplateId?: string | null;
  templateSchema: GenerationTemplateSchema | string;
  workflowId?: string | null;
  workflowSnapshot?: WorkflowSnapshot | string | null;
  status: string;
}

export interface GenerationTemplateRuntimeView {
  id: string;
  name: string;
  outputType: string;
  sections: GenerationTemplateSection[];
  sectionCount: number;
  outlineSections: string;
  variables: GenerationTemplateVariable[];
}

export interface ParsedTemplateSchemaText {
  schema: GenerationTemplateSchema | null;
  error: string | null;
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

export async function getGenerationTemplateRuntime(id: string): Promise<GenerationTemplateRuntimeView> {
  return requestJson<GenerationTemplateRuntimeView>(`/api/generation-templates/${id}/runtime`);
}

export interface UploadDocxMasterResult {
  templateId: string;
  masterFile: string;
  fileName: string;
  docxConfig: string;
}

export async function uploadGenerationTemplateDocxMaster(
  id: string,
  file: File
): Promise<UploadDocxMasterResult> {
  const formData = new FormData();
  formData.append('file', file);
  return requestJson<UploadDocxMasterResult>(`/api/generation-templates/${id}/docx-master`, {
    method: 'POST',
    body: formData
  }, { jsonHeaders: false });
}

export function parseTemplateSchemaText(value: string): ParsedTemplateSchemaText {
  if (!value.trim()) {
    return { schema: null, error: '模板结构不能为空' };
  }
  try {
    const parsed = JSON.parse(value) as unknown;
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return { schema: null, error: '模板结构必须是 JSON 对象' };
    }
    return { schema: parsed as GenerationTemplateSchema, error: null };
  } catch {
    return { schema: null, error: 'JSON 格式无效，请检查语法' };
  }
}

export function buildTemplateSkeleton(
  schema: GenerationTemplateSchema,
  title?: string | null
): string {
  const docTitle = title?.trim() || schema.title?.trim() || '{成果标题}';
  const lines = [`# ${docTitle}`, ''];
  for (const section of schema.sections ?? []) {
    lines.push(`## ${section.title || section.key}`);
    lines.push(`（${section.instruction || '按模板说明生成'}）`);
    lines.push('');
  }
  return lines.join('\n').trim();
}

export function runtimeViewToSchema(runtime: GenerationTemplateRuntimeView): GenerationTemplateSchema {
  return {
    title: runtime.name,
    variables: runtime.variables ?? [],
    sections: runtime.sections ?? []
  };
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
