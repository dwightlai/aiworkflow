// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { WorkflowCardsPage } from './WorkflowCardsPage';

const workflowApiMock = vi.hoisted(() => ({
  archiveWorkflow: vi.fn(async () => ({
    id: 'workflow-1',
    name: '客服意图识别',
    description: '识别用户咨询意图并输出下一步动作',
    status: 'ARCHIVED',
    latestVersion: { version: 3 },
    updatedAt: '2026-05-28T10:20:00Z'
  })),
  listWorkflows: vi.fn(async () => ({
    items: [
      {
        id: 'workflow-1',
        name: '客服意图识别',
        description: '识别用户咨询意图并输出下一步动作',
        status: 'PUBLISHED',
        latestVersion: { version: 3 },
        updatedAt: '2026-05-28T10:20:00Z'
      }
    ],
    total: 1
  }))
}));

vi.mock('../../api/workflows', () => workflowApiMock);

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('WorkflowCardsPage', () => {
  it('renders workflow cards with search and expected actions', async () => {
    renderPage();

    expect(screen.getByPlaceholderText('请输入工作流名称')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /创建工作流/ })).toBeInTheDocument();

    await waitFor(() => expect(screen.getByText('客服意图识别')).toBeInTheDocument());

    expect(screen.getByText('识别用户咨询意图并输出下一步动作')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /运行/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /编辑/ })).toBeInTheDocument();
  });

  it('renders a workflow operations console with summary and templates', async () => {
    renderPage();

    expect(await screen.findByText('工作流运营台')).toBeInTheDocument();
    expect(screen.getByText('总工作流')).toBeInTheDocument();
    expect(screen.getAllByText('已发布').length).toBeGreaterThan(0);
    expect(screen.getByText('模板中心')).toBeInTheDocument();
    expect(screen.getByText('客服问答助手')).toBeInTheDocument();
  });

  it('archives a workflow from the card actions', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /归档工作流/ }));

    await waitFor(() => {
      expect(workflowApiMock.archiveWorkflow).toHaveBeenCalledWith('workflow-1');
    });
  });
});

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <WorkflowCardsPage />
    </QueryClientProvider>
  );
}
