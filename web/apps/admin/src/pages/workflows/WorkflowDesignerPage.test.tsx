// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
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
  runWorkflow: vi.fn()
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
  it('renders palette canvas config panel and debug panel', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('客服意图识别')).toBeInTheDocument();
    expect(screen.getByText('节点库')).toBeInTheDocument();
    expect(screen.getByText('PROMPT')).toBeInTheDocument();
    expect(screen.getByText('节点配置')).toBeInTheDocument();
    expect(screen.getByText('运行调试')).toBeInTheDocument();
  });

  it('renders a product-grade designer workspace with draggable nodes and connection ports', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('工作流概览')).toBeInTheDocument();
    expect(screen.getByText('节点编排')).toBeInTheDocument();
    expect(screen.getByText('拖拽节点')).toBeInTheDocument();
    expect(screen.getByText('端口连线')).toBeInTheDocument();
    expect(screen.getAllByLabelText(/从 .* 连线/).length).toBeGreaterThan(0);
    expect(screen.getAllByLabelText(/连接到 .*/).length).toBeGreaterThan(0);
    expect(screen.getByText('配置完整度')).toBeInTheDocument();
    expect(screen.getByText('调试控制台')).toBeInTheDocument();
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

  it('blocks saving invalid graph definitions before sending them to the backend', async () => {
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
    expect(workflowApiMock.updateWorkflowDraft).not.toHaveBeenCalled();
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
});
