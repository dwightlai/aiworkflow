export interface WorkflowRunRequest {
  input: Record<string, unknown>;
}

export interface WorkflowRunResponse {
  workflowId: string;
  runId: string;
}

export class AiWorkflowClient {
  constructor(
    private readonly baseUrl: string,
    private readonly apiKey: string
  ) {}

  async createRun(workflowId: string, request: WorkflowRunRequest): Promise<WorkflowRunResponse> {
    const response = await fetch(`${this.baseUrl}/openapi/v1/workflows/${workflowId}/runs`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${this.apiKey}`
      },
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      throw new Error(`Failed to create workflow run: ${response.status}`);
    }

    const body = await response.json();
    return body.data;
  }
}
