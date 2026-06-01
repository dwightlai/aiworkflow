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
  it('applies a saved prompt template to PROMPT node config', async () => {
    const onChange = vi.fn();
    render(
      <NodeConfigPanel
        node={promptNode}
        onChange={onChange}
        promptTemplates={[
          {
            id: 'prompt_1',
            name: 'Greeting',
            template: 'Hello {{name}}',
            description: 'Greeting prompt'
          }
        ]}
      />
    );

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择已保存 Prompt' }));
    await userEvent.click(await screen.findByText('Greeting'));

    expect(onChange).toHaveBeenCalledWith('prompt_1', {
      config: expect.objectContaining({
        promptTemplateId: 'prompt_1',
        template: 'Hello {{name}}'
      })
    });
  });

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
            modelUsage: 'CHAT',
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

  it('applies a saved knowledge base to KNOWLEDGE_RETRIEVAL node config', async () => {
    const onChange = vi.fn();
    render(
      <NodeConfigPanel
        node={knowledgeNode}
        onChange={onChange}
        knowledgeBases={[
          {
            id: 'kb_1',
            name: '产品知识库',
            description: '客服资料',
            documentCount: 2,
            chunkCount: 8
          }
        ]}
      />
    );

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择已保存知识库' }));
    await userEvent.click(await screen.findByText('产品知识库'));

    expect(onChange).toHaveBeenCalledWith('knowledge_1', {
      config: expect.objectContaining({
        knowledgeBaseId: 'kb_1'
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

const promptNode: WorkflowNode = {
  id: 'prompt_1',
  type: 'PROMPT',
  name: 'Prompt 模板',
  config: {
    template: '',
    outputKey: 'prompt'
  }
};

const knowledgeNode: WorkflowNode = {
  id: 'knowledge_1',
  type: 'KNOWLEDGE_RETRIEVAL',
  name: '知识库检索',
  config: {
    knowledgeBaseId: '',
    queryKey: 'question',
    outputKey: 'contexts',
    topK: 3
  }
};
