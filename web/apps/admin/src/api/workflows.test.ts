import { describe, expect, it, vi, afterEach } from 'vitest';
import {
  archiveWorkflow,
  createWorkflow,
  getWorkflowRun,
  listWorkflows,
  publishWorkflow,
  runWorkflow,
  updateWorkflowMetadata
} from './workflows';

describe('workflow admin api', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('lists workflows from the backend envelope', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: {
        items: [{ id: 'workflow-1', name: 'Greeting', status: 'DRAFT' }],
        total: 1
      },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const page = await listWorkflows();

    expect(fetchMock).toHaveBeenCalledWith('/api/workflows');
    expect(page.items).toHaveLength(1);
    expect(page.items[0].id).toBe('workflow-1');
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

    const workflow = await createWorkflow({
      name: 'Greeting',
      description: null,
      definition: {
        nodes: [],
        edges: [],
        variables: []
      }
    });
    const published = await publishWorkflow(workflow.id);
    const run = await runWorkflow(workflow.id, { name: 'Ada' });
    const detail = await getWorkflowRun(run.id);

    expect(published.status).toBe('PUBLISHED');
    expect(detail.output.message).toBe('Hello Ada');
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/workflows', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/workflows/workflow-1/publish', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, '/api/workflows/workflow-1/runs', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(4, '/api/workflow-runs/run-1');
  });

  it('archives workflows without deleting them', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: { id: 'workflow-1', name: 'Greeting', status: 'ARCHIVED' },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const archived = await archiveWorkflow('workflow-1');

    expect(archived.status).toBe('ARCHIVED');
    expect(fetchMock).toHaveBeenCalledWith('/api/workflows/workflow-1/archive', expect.objectContaining({ method: 'POST' }));
  });

  it('updates workflow metadata', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: { id: 'workflow-1', name: '售后处理流程', description: '售后自动化', status: 'DRAFT' },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const workflow = await updateWorkflowMetadata('workflow-1', {
      name: '售后处理流程',
      description: '售后自动化'
    });

    expect(workflow.name).toBe('售后处理流程');
    expect(fetchMock).toHaveBeenCalledWith('/api/workflows/workflow-1/metadata', {
      method: 'PUT',
      body: JSON.stringify({ name: '售后处理流程', description: '售后自动化' }),
      headers: { 'Content-Type': 'application/json' }
    });
  });
});

function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body
  } as Response;
}
