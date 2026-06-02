// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PromptTemplatesPage, extractVariables } from './PromptTemplatesPage';

const promptApiMock = vi.hoisted(() => ({
  listPromptTemplates: vi.fn(async () => ({
    items: [
      {
        id: 'prompt_1',
        name: 'Greeting',
        template: 'Hello {{name}}, question: {{question}}',
        description: 'Greeting prompt'
      }
    ],
    total: 1
  })),
  createPromptTemplate: vi.fn(async () => ({
    id: 'prompt_2',
    name: 'Classifier',
    template: 'Classify {{text}}',
    description: null
  })),
  deletePromptTemplate: vi.fn(async () => undefined),
  updatePromptTemplate: vi.fn(async () => ({
    id: 'prompt_1',
    name: 'Greeting v2',
    template: 'Hi {{name}}',
    description: 'Updated'
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

vi.mock('../../api/prompts', () => promptApiMock);

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('PromptTemplatesPage', () => {
  it('extracts variables from prompt templates', () => {
    expect(extractVariables('Hello {{name}} {{ name }} {{user.id}}')).toEqual(['name', 'user.id']);
  });

  it('lists prompt templates and workflow integration context', async () => {
    renderPage();

    expect(await screen.findByText('Greeting')).toBeInTheDocument();
    expect(screen.getByText('工作流可引用')).toBeInTheDocument();
    expect(screen.getByText('PROMPT 节点选择')).toBeInTheDocument();
    expect(screen.getByText('name')).toBeInTheDocument();
    expect(screen.getByText('question')).toBeInTheDocument();
  });

  it('creates a prompt template from the drawer form', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /新增 Prompt/ }));
    fireEvent.change(await screen.findByLabelText('模板名称'), { target: { value: 'Classifier' } });
    fireEvent.change(screen.getByLabelText('Prompt 内容'), { target: { value: 'Classify {{text}}' } });
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      const firstCall = (promptApiMock.createPromptTemplate as unknown as { mock: { calls: unknown[][] } }).mock.calls[0];
      expect(firstCall?.[0]).toEqual({
        name: 'Classifier',
        template: 'Classify {{text}}',
        description: null
      });
    });
  }, 10000);

  it('updates an existing prompt template', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /编辑/ }));
    fireEvent.change(await screen.findByLabelText('模板描述'), { target: { value: 'Updated' } });
    await userEvent.click(screen.getByRole('button', { name: /修改/ }));

    await waitFor(() => {
      expect(promptApiMock.updatePromptTemplate).toHaveBeenCalledWith(
        'prompt_1',
        expect.objectContaining({ description: 'Updated' })
      );
    });
  }, 10000);

  it('deletes an existing prompt template', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /删除 Prompt/ }));

    await waitFor(() => {
      expect(promptApiMock.deletePromptTemplate).toHaveBeenCalledWith('prompt_1');
    });
  }, 10000);
});

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <PromptTemplatesPage />
    </QueryClientProvider>
  );
}
