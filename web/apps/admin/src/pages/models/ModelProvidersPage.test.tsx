// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ModelProvidersPage } from './ModelProvidersPage';

const modelApiMock = vi.hoisted(() => ({
  listModelProviders: vi.fn(async () => ({
    items: [
      {
        id: 'model_provider_1',
        name: 'OpenAI Compatible',
        modelType: 'OpenAI',
        modelUsage: 'CHAT',
        description: '通用模型',
        visionSupport: true,
        pricePerMillionTokens: 12.5,
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
    modelType: 'DeepSeek',
    modelUsage: 'CHAT',
    description: null,
    visionSupport: false,
    pricePerMillionTokens: 1,
    baseUrl: 'https://api.deepseek.com/v1',
    model: 'deepseek-chat',
    apiKeyRef: 'deepseek-key',
    enabled: true
  })),
  updateModelProvider: vi.fn(async () => ({
    id: 'model_provider_1',
    name: 'OpenAI Compatible',
    modelType: 'OpenAI',
    modelUsage: 'CHAT',
    description: '通用模型更新',
    visionSupport: true,
    pricePerMillionTokens: 13,
    baseUrl: 'https://api.example.com/v1',
    model: 'gpt-4.1-mini',
    apiKeyRef: 'dev-key',
    enabled: true
  })),
  deleteModelProvider: vi.fn(async () => undefined)
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
    expect(screen.getByText('视觉')).toBeInTheDocument();
    expect(screen.getByText('对话')).toBeInTheDocument();
    expect(screen.getByText('工作流可调用')).toBeInTheDocument();
    expect(screen.getByText('嵌入模型')).toBeInTheDocument();
  });

  it('creates a model provider from the drawer form', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /新增模型/ }));
    await userEvent.clear(await screen.findByLabelText('模型名称'));
    await userEvent.type(screen.getByLabelText('模型名称'), 'DeepSeek');
    await userEvent.clear(screen.getByLabelText('Base URL'));
    await userEvent.type(screen.getByLabelText('Base URL'), 'https://api.deepseek.com/v1');
    await userEvent.clear(screen.getByLabelText('调用模型'));
    await userEvent.type(screen.getByLabelText('调用模型'), 'deepseek-chat');
    await userEvent.type(screen.getByLabelText('API Key'), 'deepseek-key');
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      const firstCall = (modelApiMock.createModelProvider as unknown as { mock: { calls: unknown[][] } }).mock.calls[0];
      expect(firstCall?.[0]).toEqual({
        name: 'DeepSeek',
        modelType: 'DeepSeek',
        modelUsage: 'CHAT',
        description: null,
        visionSupport: false,
        pricePerMillionTokens: null,
        baseUrl: 'https://api.deepseek.com/v1',
        model: 'deepseek-chat',
        apiKeyRef: 'deepseek-key',
        enabled: true
      });
    });
  }, 10000);

  it('updates an existing model provider from the edit drawer', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /编辑/ }));
    await userEvent.clear(await screen.findByLabelText('模型描述'));
    await userEvent.type(screen.getByLabelText('模型描述'), '通用模型更新');
    await userEvent.click(screen.getByRole('button', { name: /修改/ }));

    await waitFor(() => {
      expect(modelApiMock.updateModelProvider).toHaveBeenCalledWith(
        'model_provider_1',
        expect.objectContaining({
          description: '通用模型更新',
          modelType: 'OpenAI',
          modelUsage: 'CHAT'
        })
      );
    });
  }, 10000);

  it('creates an embedding model provider that can be used by knowledge bases', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /新增模型/ }));
    fireEvent.mouseDown(await screen.findByRole('combobox', { name: '模型用途' }));
    await userEvent.click(await screen.findByText('嵌入'));
    await userEvent.clear(screen.getByLabelText('模型名称'));
    await userEvent.type(screen.getByLabelText('模型名称'), 'OpenAI Embedding');
    await userEvent.clear(screen.getByLabelText('调用模型'));
    await userEvent.type(screen.getByLabelText('调用模型'), 'text-embedding-3-small');
    await userEvent.clear(screen.getByLabelText('Base URL'));
    await userEvent.type(screen.getByLabelText('Base URL'), 'https://api.openai.com/v1');
    await userEvent.type(screen.getByLabelText('API Key'), 'embedding-key');
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      expect(modelApiMock.createModelProvider).toHaveBeenCalledWith(expect.objectContaining({
        name: 'OpenAI Embedding',
        modelUsage: 'EMBEDDING',
        model: 'text-embedding-3-small'
      }));
    });
  }, 10000);

  it('deletes an existing model provider from the list', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /删除模型/ }));

    await waitFor(() => {
      expect(modelApiMock.deleteModelProvider).toHaveBeenCalledWith('model_provider_1');
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
