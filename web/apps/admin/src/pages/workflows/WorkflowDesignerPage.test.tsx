// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, createEvent, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { WorkflowDesignerPage } from './WorkflowDesignerPage';

const workflowApiMock = vi.hoisted(() => ({
  createWorkflow: vi.fn(async () => ({
    id: 'workflow-created',
    name: '新建工作流',
    description: null,
    status: 'DRAFT',
    latestVersion: null
  })),
  getWorkflow: vi.fn(async () => ({
    id: 'workflow-1',
    name: '客服意图识别',
    status: 'DRAFT',
    latestVersion: {
      definition: {
        nodes: [
          { id: 'start', type: 'START', name: '开始', config: {} },
          { id: 'end', type: 'END', name: '结束', config: {} }
        ],
        edges: [{ id: 'edge_start_end', sourceNodeId: 'start', targetNodeId: 'end', condition: null }],
        variables: []
      }
    }
  })),
  updateWorkflowDraft: vi.fn(),
  publishWorkflow: vi.fn(),
  runWorkflow: vi.fn(async () => ({
    id: 'run-1',
    workflowId: 'workflow-1',
    workflowVersionId: 'version-1',
    status: 'SUCCEEDED',
    input: {},
    output: {},
    errorMessage: null,
    startedAt: '2026-05-29T01:00:00Z',
    finishedAt: '2026-05-29T01:00:01Z',
    nodeExecutions: [
      {
        id: 'node-exec-start',
        workflowExecutionId: 'run-1',
        nodeId: 'start',
        nodeType: 'START',
        status: 'SUCCEEDED',
        input: {},
        output: {},
        errorMessage: null,
        startedAt: '2026-05-29T01:00:00Z',
        finishedAt: '2026-05-29T01:00:01Z'
      },
      {
        id: 'node-exec-end',
        workflowExecutionId: 'run-1',
        nodeId: 'end',
        nodeType: 'END',
        status: 'SUCCEEDED',
        input: {},
        output: {},
        errorMessage: null,
        startedAt: '2026-05-29T01:00:00Z',
        finishedAt: '2026-05-29T01:00:01Z'
      }
    ]
  }))
}));

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
});

vi.mock('../../api/workflows', () => workflowApiMock);

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  window.history.replaceState(null, '', '/');
});

describe('WorkflowDesignerPage', () => {
  it('renders a full-canvas designer and opens config/debug panels on demand', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('客服意图识别')).toBeInTheDocument();
    expect(screen.getByText('节点库')).toBeInTheDocument();
    expect(screen.getByText('PROMPT')).toBeInTheDocument();
    expect(screen.getByText('工作流-测试')).toBeInTheDocument();
    expect(screen.getByText('编排')).toBeInTheDocument();
    expect(screen.queryByText('节点配置')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '节点 结束' }));
    expect(await screen.findByText('节点配置')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /调试/ }));
    expect(await screen.findByText('运行调试')).toBeInTheDocument();
  });

  it('renders a product-grade designer workspace with draggable nodes and connection ports', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('工作流-测试')).toBeInTheDocument();
    expect(screen.getByText('节点编排')).toBeInTheDocument();
    expect(screen.getByText('拖拽节点')).toBeInTheDocument();
    expect(screen.getByText('端口连线')).toBeInTheDocument();
    expect(screen.getAllByLabelText(/从 .* 连线/).length).toBeGreaterThan(0);
    expect(screen.getAllByLabelText(/连接到 .*/).length).toBeGreaterThan(0);
    expect(screen.getByText('100% 配置')).toBeInTheDocument();
    expect(screen.getByText('待调试')).toBeInTheDocument();
  });

  it('persists node drag positions and newly connected edges into the workflow definition', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    const startNode = await screen.findByRole('button', { name: '节点 开始' });
    fireEvent.mouseDown(startNode, { clientX: 32, clientY: 32 });
    fireEvent.mouseMove(window, { clientX: 82, clientY: 92 });
    fireEvent.mouseUp(window, { clientX: 82, clientY: 92 });

    await userEvent.click(screen.getByLabelText('从 开始 连线'));
    await userEvent.click(screen.getByLabelText('连接到 结束'));
    await userEvent.click(screen.getByRole('button', { name: '保存草稿' }));

    expect(workflowApiMock.updateWorkflowDraft).toHaveBeenCalledWith(
      'workflow-1',
      expect.objectContaining({
        edges: [expect.objectContaining({
          id: 'edge_start_end',
          sourceNodeId: 'start',
          targetNodeId: 'end'
        })],
        nodes: expect.arrayContaining([
          expect.objectContaining({
            id: 'start',
            config: expect.objectContaining({
              ui: expect.objectContaining({
                position: { x: 82, y: 92 }
              })
            })
          })
        ])
      })
    );
  });

  it('adds palette nodes by dragging them onto the canvas at the drop point', async () => {
    const dataTransfer = createDataTransfer();
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    fireEvent.dragStart(screen.getByRole('button', { name: /Prompt 模板/ }), { dataTransfer });
    const dropTarget = screen.getByLabelText('工作流画布投放区');
    fireEvent.dragOver(dropTarget, {
      clientX: 420,
      clientY: 250,
      dataTransfer
    });
    const dropEvent = createEvent.drop(dropTarget, { dataTransfer });
    Object.defineProperties(dropEvent, {
      clientX: { value: 420 },
      clientY: { value: 250 },
      pageX: { value: 420 },
      pageY: { value: 250 }
    });
    fireEvent(dropTarget, dropEvent);

    expect(await screen.findByDisplayValue('Prompt 模板')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '保存草稿' }));

    expect(workflowApiMock.updateWorkflowDraft).toHaveBeenCalledWith(
      'workflow-1',
      expect.objectContaining({
        nodes: expect.arrayContaining([
          expect.objectContaining({
            type: 'PROMPT',
            config: expect.objectContaining({
              ui: expect.objectContaining({
                position: { x: 420, y: 250 }
              })
            })
          })
        ])
      })
    );
  });

  it('saves incomplete draft definitions while surfacing validation issues', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByLabelText('选择连线 edge_start_end'));
    await userEvent.click(screen.getByRole('button', { name: '删除连线' }));
    await userEvent.click(screen.getByRole('button', { name: '保存草稿' }));

    expect((await screen.findAllByText('流程结构校验未通过')).length).toBeGreaterThan(0);
    expect(workflowApiMock.updateWorkflowDraft).toHaveBeenCalledWith(
      'workflow-1',
      expect.objectContaining({ edges: [] })
    );
  });

  it('links right panel selection with the canvas and overlays run status on nodes', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByRole('button', { name: '节点 结束' }));
    expect(screen.getByDisplayValue('结束')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '运行' }));
    expect((await screen.findAllByText('SUCCEEDED')).length).toBeGreaterThanOrEqual(2);
  });

  it('creates a workflow from the new designer and navigates to its designer route', async () => {
    window.history.replaceState(null, '', '/workflows/new/designer');

    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="new" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('新建工作流')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '创建工作流' }));

    expect(workflowApiMock.createWorkflow).toHaveBeenCalledWith(expect.objectContaining({
      name: '新建工作流',
      description: null
    }));
    expect(window.location.pathname).toBe('/workflows/workflow-created/designer');
  });

  it('starts new workflows without an end node and allows saving incomplete drafts', async () => {
    window.history.replaceState(null, '', '/workflows/new/designer');

    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="new" />
      </QueryClientProvider>
    );

    expect(await screen.findByRole('button', { name: '节点 Start' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '节点 End' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '创建工作流' }));

    expect(workflowApiMock.createWorkflow).toHaveBeenCalledWith(expect.objectContaining({
      definition: expect.objectContaining({
        nodes: [expect.objectContaining({ type: 'START' })],
        edges: []
      })
    }));
  });
});

function createDataTransfer(): DataTransfer {
  const values = new Map<string, string>();
  const dataTransfer = {
    dropEffect: 'copy',
    effectAllowed: 'copy',
    files: [] as unknown as FileList,
    items: [] as unknown as DataTransferItemList,
    types: [] as unknown as string[],
    clearData: vi.fn((type?: string) => {
      if (type) {
        values.delete(type);
        return;
      }
      values.clear();
    }),
    getData: vi.fn((type: string) => values.get(type) ?? ''),
    setData: vi.fn((type: string, value: string) => {
      values.set(type, value);
      (dataTransfer.types as unknown as string[]) = Array.from(values.keys());
    }),
    setDragImage: vi.fn()
  } as DataTransfer;
  return dataTransfer;
}
