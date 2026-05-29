// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import type { WorkflowNode } from '@aiworkflow/workflow-schema';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { NodeConfigPanel } from './NodeConfigPanel';

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

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('NodeConfigPanel', () => {
  it('applies a saved model provider to LLM node config', async () => {
    const onChange = vi.fn();
    render(
      <NodeConfigPanel
        node={llmNode}
        onChange={onChange}
        modelProviders={[
          {
            id: 'model_provider_1',
            name: 'OpenAI Compatible',
            modelType: 'OpenAI',
            description: null,
            visionSupport: false,
            pricePerMillionTokens: null,
            baseUrl: 'https://api.example.com/v1',
            model: 'gpt-4.1-mini',
            apiKeyRef: 'dev-key',
            enabled: true
          }
        ]}
      />
    );

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择已保存模型' }));
    await userEvent.click(await screen.findByText('OpenAI Compatible / gpt-4.1-mini'));

    expect(onChange).toHaveBeenCalledWith('llm_1', {
      config: expect.objectContaining({
        providerId: 'model_provider_1',
        model: 'gpt-4.1-mini'
      })
    });
  });
});

const llmNode: WorkflowNode = {
  id: 'llm_1',
  type: 'LLM',
  name: '大模型调用',
  config: {
    providerId: '',
    model: '',
    promptKey: 'prompt',
    outputKey: 'answer'
  }
};
