// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { WorkflowDesignerPage } from './WorkflowDesignerPage';

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

vi.mock('../../api/workflows', () => ({
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
});
