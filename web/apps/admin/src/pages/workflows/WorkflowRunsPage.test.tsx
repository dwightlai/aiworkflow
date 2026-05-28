// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { WorkflowRunsPage } from './WorkflowRunsPage';

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
  listWorkflowRuns: vi.fn(async () => ({
    items: [
      {
        id: 'run-1',
        workflowId: 'workflow-1',
        workflowVersionId: 'version-1',
        status: 'SUCCEEDED',
        input: { name: 'Ada' },
        output: { answer: 'ok' },
        errorMessage: null,
        startedAt: '2026-05-28T09:00:00Z',
        finishedAt: '2026-05-28T09:00:01Z',
        nodeExecutions: []
      }
    ],
    total: 1
  }))
}));

describe('WorkflowRunsPage', () => {
  it('renders workflow run history with status and detail action', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowRunsPage />
      </QueryClientProvider>
    );

    expect(screen.getByText('运行历史')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('SUCCEEDED')).toBeInTheDocument());
    expect(screen.getByText('run-1')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '详情' })).toBeInTheDocument();
  });
});
