// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DashboardPage } from './DashboardPage';

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
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
  value: () => ({
    getPropertyValue: () => ''
  })
});

vi.mock('../api/workflows', () => ({
  listWorkflows: vi.fn(async () => ({
    items: [
      {
        id: 'workflow-1',
        name: '线索评分流程',
        description: '为 CRM 线索自动评分并生成跟进建议',
        status: 'PUBLISHED',
        createdAt: '2026-05-28T08:00:00Z',
        updatedAt: '2026-05-28T09:00:00Z',
        latestVersion: { version: 3 }
      },
      {
        id: 'workflow-2',
        name: '合同摘要流程',
        description: '抽取合同条款并输出风险点',
        status: 'DRAFT',
        createdAt: '2026-05-28T08:30:00Z',
        updatedAt: '2026-05-28T10:00:00Z',
        latestVersion: { version: 1 }
      }
    ],
    total: 2
  })),
  listWorkflowRuns: vi.fn(async () => ({
    items: [
      {
        id: 'run-1',
        workflowId: 'workflow-1',
        workflowVersionId: 'version-1',
        status: 'SUCCEEDED',
        input: {},
        output: {},
        errorMessage: null,
        startedAt: '2026-05-28T09:00:00Z',
        finishedAt: '2026-05-28T09:00:01Z',
        nodeExecutions: []
      },
      {
        id: 'run-2',
        workflowId: 'workflow-1',
        workflowVersionId: 'version-1',
        status: 'FAILED',
        input: {},
        output: {},
        errorMessage: 'LLM timeout',
        startedAt: '2026-05-28T10:00:00Z',
        finishedAt: '2026-05-28T10:00:02Z',
        nodeExecutions: []
      }
    ],
    total: 2
  }))
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('DashboardPage', () => {
  it('renders a complete AI Studio dashboard with workflow and run operations', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <DashboardPage />
      </QueryClientProvider>
    );

    expect(await screen.findByText('AI Studio 工作台')).toBeInTheDocument();
    expect(screen.getByText('总工作流')).toBeInTheDocument();
    expect(screen.getByText('今日执行')).toBeInTheDocument();
    expect(screen.getByText('运行成功率')).toBeInTheDocument();
    expect(screen.getByText('运行态势')).toBeInTheDocument();
    expect(screen.getByText('快捷入口')).toBeInTheDocument();
    expect(screen.getByText('最近工作流')).toBeInTheDocument();
    expect(await screen.findByText('线索评分流程')).toBeInTheDocument();
    expect(screen.getByText('合同摘要流程')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /新建工作流/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /查看运行监控/ })).toBeInTheDocument();
  });
});
