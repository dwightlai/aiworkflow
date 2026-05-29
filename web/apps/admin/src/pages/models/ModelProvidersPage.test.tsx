// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ModelProvidersPage } from './ModelProvidersPage';

const modelApiMock = vi.hoisted(() => ({
  listModelProviders: vi.fn(async () => ({
    items: [
      {
        id: 'model_provider_1',
        name: 'OpenAI Compatible',
        baseUrl: 'https://api.example.com/v1',
        model: 'gpt-4.1-mini',
        apiKeyRef: 'dev-key',
        enabled: true
      }
    ],
    total: 1
  })),
  createModelProvider: vi.fn(async () => ({
    id: 'model_provider_2',
    name: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com/v1',
    model: 'deepseek-chat',
    apiKeyRef: 'deepseek-key',
    enabled: true
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

vi.mock('../../api/models', () => modelApiMock);

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('ModelProvidersPage', () => {
  it('lists saved model providers and shows workflow integration context', async () => {
    renderPage();

    expect(await screen.findByText('OpenAI Compatible')).toBeInTheDocument();
    expect(screen.getByText('gpt-4.1-mini')).toBeInTheDocument();
    expect(screen.getByText('工作流可调用')).toBeInTheDocument();
    expect(screen.getByText('LLM 节点选择')).toBeInTheDocument();
  });

  it('creates a model provider from the form', async () => {
    renderPage();

    await userEvent.type(screen.getByLabelText('配置名称'), 'DeepSeek');
    await userEvent.type(screen.getByLabelText('Base URL'), 'https://api.deepseek.com/v1');
    await userEvent.type(screen.getByLabelText('模型标识'), 'deepseek-chat');
    await userEvent.type(screen.getByLabelText('API Key 引用'), 'deepseek-key');
    await userEvent.click(screen.getByRole('button', { name: /保存模型配置/ }));

    await waitFor(() => {
      const firstCall = (modelApiMock.createModelProvider as unknown as { mock: { calls: unknown[][] } }).mock.calls[0];
      expect(firstCall?.[0]).toEqual({
        name: 'DeepSeek',
        baseUrl: 'https://api.deepseek.com/v1',
        model: 'deepseek-chat',
        apiKeyRef: 'deepseek-key',
        enabled: true
      });
    });
  });
});

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ModelProvidersPage />
    </QueryClientProvider>
  );
}
