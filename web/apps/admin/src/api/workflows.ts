import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';

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

export async function publishWorkflow(workflowId: string): Promise<Workflow> {
  return requestJson<Workflow>(`/api/workflows/${workflowId}/publish`, {
    method: 'POST'
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
