import { afterEach, describe, expect, it, vi } from 'vitest';
import { createModelProvider, deleteModelProvider, listModelProviders, updateModelProvider } from './models';

describe('model provider api', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('lists saved model providers from the backend envelope', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: {
        items: [{ id: 'model_provider_1', name: 'OpenAI Compatible', modelType: 'OpenAI', description: '通用模型', visionSupport: true, pricePerMillionTokens: 12.5, baseUrl: 'https://api.example.com/v1', model: 'gpt-4.1-mini', apiKeyRef: 'dev-key', enabled: true }],
        total: 1
      },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const page = await listModelProviders();

    expect(fetchMock).toHaveBeenCalledWith('/api/model-providers');
    expect(page.items[0].model).toBe('gpt-4.1-mini');
  });

  it('creates a saved model provider', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: { id: 'model_provider_1', name: 'OpenAI Compatible', modelType: 'OpenAI', description: null, visionSupport: false, pricePerMillionTokens: null, baseUrl: 'https://api.example.com/v1', model: 'gpt-4.1-mini', apiKeyRef: 'dev-key', enabled: true },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const provider = await createModelProvider({
      name: 'OpenAI Compatible',
      modelType: 'OpenAI',
      description: null,
      visionSupport: false,
      pricePerMillionTokens: null,
      baseUrl: 'https://api.example.com/v1',
      model: 'gpt-4.1-mini',
      apiKeyRef: 'dev-key',
      enabled: true
    });

    expect(provider.id).toBe('model_provider_1');
    expect(fetchMock).toHaveBeenCalledWith('/api/model-providers', expect.objectContaining({ method: 'POST' }));
  });

  it('updates a saved model provider', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: { id: 'model_provider_1', name: 'DeepSeek', modelType: 'DeepSeek', description: '推理模型', visionSupport: false, pricePerMillionTokens: 1, baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat', apiKeyRef: 'deepseek-key', enabled: true },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const provider = await updateModelProvider('model_provider_1', {
      name: 'DeepSeek',
      modelType: 'DeepSeek',
      description: '推理模型',
      visionSupport: false,
      pricePerMillionTokens: 1,
      baseUrl: 'https://api.deepseek.com/v1',
      model: 'deepseek-chat',
      apiKeyRef: 'deepseek-key',
      enabled: true
    });

    expect(provider.modelType).toBe('DeepSeek');
    expect(fetchMock).toHaveBeenCalledWith('/api/model-providers/model_provider_1', expect.objectContaining({ method: 'PUT' }));
  });

  it('deletes a saved model provider', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: null,
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    await deleteModelProvider('model_provider_1');

    expect(fetchMock).toHaveBeenCalledWith('/api/model-providers/model_provider_1', expect.objectContaining({ method: 'DELETE' }));
  });
});

function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body
  } as Response;
}
