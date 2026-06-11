import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';
import { requestJson } from './auth';

export type WorkflowStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type WorkflowVersionStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED';
export type WorkflowExecutionStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED';
export type NodeExecutionStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED';

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export interface WorkflowVersion {
  id: string;
  workflowId: string;
  version: number;
  definition: WorkflowDefinition;
  status: WorkflowVersionStatus;
  publishedBy: string | null;
  publishedAt: string | null;
  createdAt: string;
}

export interface Workflow {
  id: string;
  tenantId?: string;
  ownerUnitId?: string | null;
  name: string;
  description: string | null;
  status: WorkflowStatus;
  currentVersionId?: string | null;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  latestVersion?: WorkflowVersion | null;
}

export interface CreateWorkflowRequest {
  name: string;
  description: string | null;
  definition: WorkflowDefinition;
}

export interface UpdateWorkflowMetadataRequest {
  name: string;
  description: string | null;
}

export interface NodeExecution {
  id: string;
  workflowExecutionId: string;
  nodeId: string;
  nodeType: string;
  status: NodeExecutionStatus;
  input: Record<string, unknown>;
  output: Record<string, unknown>;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
}

export interface WorkflowExecution {
  id: string;
  workflowId: string;
  workflowVersionId: string;
  status: WorkflowExecutionStatus;
  input: Record<string, unknown>;
  output: Record<string, unknown>;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
  nodeExecutions: NodeExecution[];
}

export async function listWorkflows(): Promise<PageResponse<Workflow>> {
  return requestJson<PageResponse<Workflow>>('/api/workflows');
}

export async function createWorkflow(request: CreateWorkflowRequest): Promise<Workflow> {
  return requestJson<Workflow>('/api/workflows', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function getWorkflow(workflowId: string): Promise<Workflow> {
  return requestJson<Workflow>(`/api/workflows/${workflowId}`);
}

export async function updateWorkflowDraft(
  workflowId: string,
  definition: WorkflowDefinition
): Promise<Workflow> {
  return requestJson<Workflow>(`/api/workflows/${workflowId}/draft`, {
    method: 'PUT',
    body: JSON.stringify({ definition })
  });
}

export async function updateWorkflowMetadata(
  workflowId: string,
  request: UpdateWorkflowMetadataRequest
): Promise<Workflow> {
  return requestJson<Workflow>(`/api/workflows/${workflowId}/metadata`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function publishWorkflow(workflowId: string): Promise<Workflow> {
  return requestJson<Workflow>(`/api/workflows/${workflowId}/publish`, {
    method: 'POST'
  });
}

export async function archiveWorkflow(workflowId: string): Promise<Workflow> {
  return requestJson<Workflow>(`/api/workflows/${workflowId}/archive`, {
    method: 'POST'
  });
}

export async function restoreWorkflow(workflowId: string): Promise<Workflow> {
  return requestJson<Workflow>(`/api/workflows/${workflowId}/restore`, {
    method: 'POST'
  });
}

export async function deleteWorkflow(workflowId: string): Promise<void> {
  await requestJson<void>(`/api/workflows/${workflowId}`, {
    method: 'DELETE'
  });
}

export async function runWorkflow(
  workflowId: string,
  input: Record<string, unknown>
): Promise<WorkflowExecution> {
  return requestJson<WorkflowExecution>(`/api/workflows/${workflowId}/runs`, {
    method: 'POST',
    body: JSON.stringify({ input })
  });
}

export async function getWorkflowRun(executionId: string): Promise<WorkflowExecution> {
  return requestJson<WorkflowExecution>(`/api/workflow-runs/${executionId}`);
}

export async function listWorkflowRuns(): Promise<PageResponse<WorkflowExecution>> {
  return requestJson<PageResponse<WorkflowExecution>>('/api/workflow-runs');
}

export function buildWorkflowRunInput(definition?: WorkflowDefinition | null): string {
  const startNode = definition?.nodes.find((node) => node.type === 'START');
  const defaultInputJson = startNode?.config?.defaultInputJson;
  if (typeof defaultInputJson === 'string' && defaultInputJson.trim()) {
    try {
      const parsed = JSON.parse(defaultInputJson);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return JSON.stringify(parsed, null, 2);
      }
    } catch {
      // fallback below
    }
  }
  const input: Record<string, unknown> = {};
  const inputParams = startNode?.config?.inputParams;
  if (Array.isArray(inputParams)) {
    inputParams.forEach((item) => {
      if (!item || typeof item !== 'object') {
        return;
      }
      const param = item as Record<string, unknown>;
      const name = typeof param.name === 'string' ? param.name.trim() : '';
      if (name) {
        input[name] = defaultRunInputValue(param.type);
      }
    });
  }
  if (Object.keys(input).length > 0) {
    return JSON.stringify(input, null, 2);
  }
  return '{\n  "input": "请在这里填写运行参数"\n}';
}

function defaultRunInputValue(type: unknown) {
  if (type === 'Number') {
    return 0;
  }
  if (type === 'Boolean') {
    return false;
  }
  if (type === 'Array') {
    return [];
  }
  if (type === 'Object') {
    return {};
  }
  return '';
}

