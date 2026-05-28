import { afterEach, describe, expect, it, vi } from 'vitest';
import { AiWorkflowClient } from './index';

describe('AiWorkflowClient', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('lists workflows with optional bearer auth', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: { items: [{ id: 'workflow-1', name: 'Greeting', status: 'PUBLISHED' }], total: 1 },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const client = new AiWorkflowClient({ baseUrl: 'https://example.test', apiKey: 'secret' });
    const page = await client.listWorkflows();

    expect(page.items[0].id).toBe('workflow-1');
    expect(fetchMock).toHaveBeenCalledWith('https://example.test/api/workflows', {
      headers: { Authorization: 'Bearer secret' }
    });
  });

  it('creates publishes runs and fetches execution detail', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'workflow-1', name: 'Greeting', status: 'DRAFT' },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'workflow-1', name: 'Greeting', status: 'PUBLISHED' },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'run-1', workflowId: 'workflow-1', status: 'SUCCEEDED', output: { message: 'Hello Ada' }, nodeExecutions: [] },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'run-1', workflowId: 'workflow-1', status: 'SUCCEEDED', output: { message: 'Hello Ada' }, nodeExecutions: [] },
        error: null
      }));
    vi.stubGlobal('fetch', fetchMock);

    const client = new AiWorkflowClient({ baseUrl: '' });
    const workflow = await client.createWorkflow({
      name: 'Greeting',
      description: null,
      definition: { nodes: [], edges: [], variables: [] }
    });
    const published = await client.publishWorkflow(workflow.id);
    const run = await client.runWorkflow(workflow.id, { name: 'Ada' });
    const detail = await client.getWorkflowRun(run.id);

    expect(published.status).toBe('PUBLISHED');
    expect(detail.output.message).toBe('Hello Ada');
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/workflows', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/workflows/workflow-1/publish', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, '/api/workflows/workflow-1/runs', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(4, '/api/workflow-runs/run-1', undefined);
  });

  it('keeps createRun as a compatibility alias', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: { id: 'run-1', workflowId: 'workflow-1', status: 'SUCCEEDED', output: {}, nodeExecutions: [] },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const client = new AiWorkflowClient({ baseUrl: '' });
    const run = await client.createRun('workflow-1', { input: { name: 'Ada' } });

    expect(run.id).toBe('run-1');
    expect(fetchMock).toHaveBeenCalledWith('/api/workflows/workflow-1/runs', expect.objectContaining({ method: 'POST' }));
  });
});

function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body
  } as Response;
}
