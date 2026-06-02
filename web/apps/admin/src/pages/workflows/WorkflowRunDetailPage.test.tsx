// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { WorkflowRunDetailPage } from './WorkflowRunDetailPage';

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

Object.defineProperty(window, 'getComputedStyle', {
  writable: true,
  value: vi.fn(() => ({
    getPropertyValue: vi.fn(() => '0px')
  }))
});

vi.mock('../../api/workflows', () => ({
  getWorkflowRun: vi.fn(async () => ({
    id: 'run-1',
    workflowId: 'workflow-1',
    workflowVersionId: 'version-1',
    status: 'FAILED',
    input: { name: 'Ada' },
    output: { answer: 'partial' },
    errorMessage: 'LLM timeout',
    startedAt: '2026-05-28T09:00:00Z',
    finishedAt: '2026-05-28T09:00:02Z',
    nodeExecutions: [
      {
        id: 'node-exec-1',
        workflowExecutionId: 'run-1',
        nodeId: 'prompt_1',
        nodeType: 'PROMPT',
        status: 'SUCCEEDED',
        input: { name: 'Ada' },
        output: { prompt: 'Hello Ada' },
        errorMessage: null,
        startedAt: '2026-05-28T09:00:00Z',
        finishedAt: '2026-05-28T09:00:01Z'
      },
      {
        id: 'node-exec-2',
        workflowExecutionId: 'run-1',
        nodeId: 'llm_1',
        nodeType: 'LLM',
        status: 'FAILED',
        input: { prompt: 'Hello Ada' },
        output: {},
        errorMessage: 'LLM timeout',
        startedAt: '2026-05-28T09:00:01Z',
        finishedAt: '2026-05-28T09:00:02Z'
      }
    ]
  }))
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('WorkflowRunDetailPage', () => {
  it('renders execution observability detail with timeline and diagnosis', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowRunDetailPage executionId="run-1" />
      </QueryClientProvider>
    );

    expect(screen.getByText('执行详情')).toBeInTheDocument();
    expect(await screen.findByText('运行诊断')).toBeInTheDocument();
    expect(screen.getByText('输入 / 输出')).toBeInTheDocument();
    expect(screen.getByText('节点时间线')).toBeInTheDocument();
    expect(screen.getAllByText('LLM timeout').length).toBeGreaterThan(0);
    expect(screen.getByText('prompt_1')).toBeInTheDocument();
    expect(screen.getByText('llm_1')).toBeInTheDocument();
    await waitFor(() => expect(screen.getAllByText('FAILED').length).toBeGreaterThan(0));
  });
});
