import { afterEach, describe, expect, it, vi } from 'vitest';
import { createPromptTemplate, deletePromptTemplate, listPromptTemplates, updatePromptTemplate } from './prompts';

describe('prompt template api', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('lists prompt templates from the backend envelope', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: {
        items: [{ id: 'prompt_1', name: 'Greeting', template: 'Hello {{name}}', description: 'Greeting prompt' }],
        total: 1
      },
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    const page = await listPromptTemplates();

    expect(fetchMock).toHaveBeenCalledWith('/api/prompts');
    expect(page.items[0].template).toBe('Hello {{name}}');
  });

  it('creates and updates prompt templates', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'prompt_1', name: 'Greeting', template: 'Hello {{name}}', description: null },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'prompt_1', name: 'Greeting v2', template: 'Hi {{name}}', description: 'Updated' },
        error: null
      }));
    vi.stubGlobal('fetch', fetchMock);

    const created = await createPromptTemplate({ name: 'Greeting', template: 'Hello {{name}}', description: null });
    const updated = await updatePromptTemplate(created.id, { name: 'Greeting v2', template: 'Hi {{name}}', description: 'Updated' });

    expect(updated.name).toBe('Greeting v2');
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/prompts', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/prompts/prompt_1', expect.objectContaining({ method: 'PUT' }));
  });

  it('deletes prompt templates', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      success: true,
      data: null,
      error: null
    }));
    vi.stubGlobal('fetch', fetchMock);

    await deletePromptTemplate('prompt_1');

    expect(fetchMock).toHaveBeenCalledWith('/api/prompts/prompt_1', expect.objectContaining({ method: 'DELETE' }));
  });
});

function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body
  } as Response;
}
