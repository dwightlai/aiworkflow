// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { WorkflowCardsPage } from './WorkflowCardsPage';

vi.mock('../../api/workflows', () => ({
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

describe('WorkflowCardsPage', () => {
  it('renders workflow cards with search and expected actions', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowCardsPage />
      </QueryClientProvider>
    );

    expect(screen.getByPlaceholderText('请输入工作流名称')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /创建工作流/ })).toBeInTheDocument();

    await waitFor(() => expect(screen.getByText('客服意图识别')).toBeInTheDocument());

    expect(screen.getByText('识别用户咨询意图并输出下一步动作')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /运行/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /编辑/ })).toBeInTheDocument();
  });
});
