// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
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
        nodes: [{ id: 'start', type: 'START', name: '开始', config: {} }],
        edges: [],
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

  it('renders a product-grade designer workspace with overview and execution map', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('工作流概览')).toBeInTheDocument();
    expect(screen.getByText('节点编排')).toBeInTheDocument();
    expect(screen.getAllByText('执行链路').length).toBeGreaterThan(0);
    expect(screen.getByText('配置完整度')).toBeInTheDocument();
    expect(screen.getByText('调试控制台')).toBeInTheDocument();
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
