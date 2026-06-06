// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
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
  runWorkflow: vi.fn(async () => ({
    id: 'run-1',
    workflowId: 'workflow-1',
    workflowVersionId: 'version-1',
    status: 'SUCCEEDED',
    input: { question: '发票怎么申请' },
    output: { result: '发票可以在订单完成后七日内申请。' },
    errorMessage: null,
    startedAt: '2026-05-28T10:21:00Z',
    finishedAt: '2026-05-28T10:21:01Z',
    nodeExecutions: []
  })),
  updateWorkflowMetadata: vi.fn(async (_workflowId: string, request: { name: string; description: string | null }) => ({
    id: 'workflow-1',
    name: request.name,
    description: request.description,
    status: 'PUBLISHED',
    latestVersion: { version: 3 },
    updatedAt: '2026-05-28T10:22:00Z'
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

  it('runs a published workflow with JSON input and navigates to run detail', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /运行/ }));
    const inputEditor = await screen.findByLabelText('运行输入 JSON');
    await userEvent.clear(inputEditor);
    fireEvent.change(inputEditor, { target: { value: '{"question":"发票怎么申请"}' } });
    await userEvent.click(screen.getByRole('button', { name: /开始运行/ }));

    await waitFor(() => {
      expect(workflowApiMock.runWorkflow).toHaveBeenCalledWith('workflow-1', { question: '发票怎么申请' });
    });
    expect(window.location.pathname).toBe('/workflow-runs/run-1');
  });

  it('opens workflow settings from the card action and saves metadata', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /设置/ }));
    expect(await screen.findByText(/工作流设置/)).toBeInTheDocument();

    const nameInput = screen.getByLabelText('工作流名称');
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, '售后处理流程');
    await userEvent.click(screen.getByRole('button', { name: '保存设置' }));

    await waitFor(() => {
      expect(workflowApiMock.updateWorkflowMetadata).toHaveBeenCalledWith('workflow-1', {
        name: '售后处理流程',
        description: '识别用户咨询意图并输出下一步动作'
      });
    });
  });
});

function renderPage() {
  window.history.pushState(null, '', '/workflows');
  render(
    <QueryClientProvider client={new QueryClient()}>
      <WorkflowCardsPage />
    </QueryClientProvider>
  );
}
