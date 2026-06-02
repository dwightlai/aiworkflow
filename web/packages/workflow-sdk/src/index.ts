import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';

export type WorkflowStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type WorkflowVersionStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED';
export type WorkflowExecutionStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED';
export type NodeExecutionStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED';

export interface AiWorkflowClientOptions {
  baseUrl?: string;
  apiKey?: string;
}

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

export interface WorkflowRunRequest {
  input: Record<string, unknown>;
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

export class AiWorkflowClient {
  private readonly baseUrl: string;
  private readonly apiKey?: string;

  constructor(options?: AiWorkflowClientOptions);
  constructor(baseUrl: string, apiKey?: string);
  constructor(optionsOrBaseUrl: AiWorkflowClientOptions | string = {}, apiKey?: string) {
    if (typeof optionsOrBaseUrl === 'string') {
      this.baseUrl = trimTrailingSlash(optionsOrBaseUrl);
      this.apiKey = apiKey;
      return;
    }
    this.baseUrl = trimTrailingSlash(optionsOrBaseUrl.baseUrl ?? '');
    this.apiKey = optionsOrBaseUrl.apiKey;
  }

  listWorkflows(): Promise<PageResponse<Workflow>> {
    return this.request<PageResponse<Workflow>>('/api/workflows');
  }

  createWorkflow(request: CreateWorkflowRequest): Promise<Workflow> {
    return this.request<Workflow>('/api/workflows', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  }

  publishWorkflow(workflowId: string): Promise<Workflow> {
    return this.request<Workflow>(`/api/workflows/${workflowId}/publish`, {
      method: 'POST'
    });
  }

  runWorkflow(workflowId: string, input: Record<string, unknown>): Promise<WorkflowExecution> {
    return this.request<WorkflowExecution>(`/api/workflows/${workflowId}/runs`, {
      method: 'POST',
      body: JSON.stringify({ input })
    });
  }

  createRun(workflowId: string, request: WorkflowRunRequest): Promise<WorkflowExecution> {
    return this.runWorkflow(workflowId, request.input);
  }

  getWorkflowRun(executionId: string): Promise<WorkflowExecution> {
    return this.request<WorkflowExecution>(`/api/workflow-runs/${executionId}`);
  }

  private async request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(this.baseUrl + path, this.withHeaders(init));
    const envelope = await response.json() as ApiEnvelope<T>;
    if (!response.ok || !envelope.success) {
      throw new Error(envelope.error?.message ?? `Request failed: ${response.status}`);
    }
    return envelope.data;
  }

  private withHeaders(init?: RequestInit): RequestInit | undefined {
    const headers: Record<string, string> = {};
    if (init?.body) {
      headers['Content-Type'] = 'application/json';
    }
    if (this.apiKey) {
      headers.Authorization = `Bearer ${this.apiKey}`;
    }
    if (!init && Object.keys(headers).length === 0) {
      return undefined;
    }
    return {
      ...init,
      headers: {
        ...headers,
        ...init?.headers
      }
    };
  }
}

function trimTrailingSlash(value: string): string {
  return value.endsWith('/') ? value.slice(0, -1) : value;
}
