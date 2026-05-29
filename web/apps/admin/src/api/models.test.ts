import { afterEach, describe, expect, it, vi } from 'vitest';
import { createModelProvider, listModelProviders } from './models';

describe('model provider api', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('lists saved model providers from the backend envelope', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: {
        items: [{ id: 'model_provider_1', name: 'OpenAI Compatible', baseUrl: 'https://api.example.com/v1', model: 'gpt-4.1-mini', apiKeyRef: 'dev-key', enabled: true }],
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
      data: { id: 'model_provider_1', name: 'OpenAI Compatible', baseUrl: 'https://api.example.com/v1', model: 'gpt-4.1-mini', apiKeyRef: 'dev-key', enabled: true },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const provider = await createModelProvider({
      name: 'OpenAI Compatible',
      baseUrl: 'https://api.example.com/v1',
      model: 'gpt-4.1-mini',
      apiKeyRef: 'dev-key',
      enabled: true
    });

    expect(provider.id).toBe('model_provider_1');
    expect(fetchMock).toHaveBeenCalledWith('/api/model-providers', expect.objectContaining({ method: 'POST' }));
  });
});

function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body
  } as Response;
}
