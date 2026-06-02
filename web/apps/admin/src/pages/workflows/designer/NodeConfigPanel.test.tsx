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
  it('renders AIFlowy-style LLM properties and saves runnable model config', async () => {
    const onChange = vi.fn();
    render(
      <NodeConfigPanel
        node={llmNode}
        onChange={onChange}
        modelProviders={[
          {
            id: 'model_provider_1',
            name: '火山引擎',
            modelType: 'VolcEngine',
            modelUsage: 'CHAT',
            description: null,
            visionSupport: false,
            pricePerMillionTokens: null,
            baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
            model: 'DS-V3',
            apiKeyRef: 'dev-key',
            enabled: true
          }
        ] as never}
      />
    );

    expect(screen.getByText('TinyFlow.ai')).toBeInTheDocument();
    expect(screen.getByText('使用大模型处理问题')).toBeInTheDocument();
    expect(screen.getByText('输入参数')).toBeInTheDocument();
    expect(screen.getByText('无输入参数')).toBeInTheDocument();
    expect(screen.getByText('模型设置')).toBeInTheDocument();
    expect(screen.getByText('Temperature: 0.5')).toBeInTheDocument();
    expect(screen.getByText('Top P: 0.9')).toBeInTheDocument();
    expect(screen.getByText('Top K: 50')).toBeInTheDocument();
    expect(screen.getByText('输出参数')).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择模型' }));
    await userEvent.click(await screen.findByText('火山引擎-DS-V3'));

    expect(onChange).toHaveBeenCalledWith('llm_1', {
      config: expect.objectContaining({
        providerId: 'model_provider_1',
        model: 'DS-V3'
      })
    });

    fireEvent.change(screen.getByPlaceholderText('请输入用户提示词，如：{{question}}'), {
      target: { value: '请回答：{{question}}' }
    });

    expect(onChange).toHaveBeenCalledWith('llm_1', {
      config: expect.objectContaining({
        userPrompt: '请回答：{{question}}'
      })
    });
  });

  it('renders AIFlowy-style knowledge properties and saves runnable retrieval config', async () => {
    const onChange = vi.fn();
    render(
      <NodeConfigPanel
        node={knowledgeNode}
        onChange={onChange}
        knowledgeBases={[
          {
            id: 'kb_1',
            name: '客服手册',
            description: '客服问答资料',
            documentCount: 2,
            chunkCount: 8
          }
        ] as never}
      />
    );

    expect(screen.getByText('通过知识库获取内容')).toBeInTheDocument();
    expect(screen.getByText('输入参数')).toBeInTheDocument();
    expect(screen.getByDisplayValue('search_key')).toBeInTheDocument();
    expect(screen.getByDisplayValue('keyword')).toBeInTheDocument();
    expect(screen.getByText('知识库设置')).toBeInTheDocument();
    expect(screen.getByText('关键字')).toBeInTheDocument();
    expect(screen.getByText('获取数据量')).toBeInTheDocument();
    expect(screen.getByDisplayValue('{{search_key}}')).toBeInTheDocument();
    expect(screen.getByDisplayValue('documents')).toBeInTheDocument();
    expect(screen.getByDisplayValue('documentId')).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择知识库' }));
    await userEvent.click(await screen.findByText('客服手册'));

    expect(onChange).toHaveBeenCalledWith('knowledge_1', {
      config: expect.objectContaining({
        knowledgeBaseId: 'kb_1'
      })
    });

    fireEvent.change(screen.getByDisplayValue('{{search_key}}'), {
      target: { value: '{{question}}' }
    });

    expect(onChange).toHaveBeenCalledWith('knowledge_1', {
      config: expect.objectContaining({
        keywordTemplate: '{{question}}',
        queryKey: 'question'
      })
    });
  });

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
        ] as never}
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
});

const llmNode: WorkflowNode = {
  id: 'llm_1',
  type: 'LLM',
  name: '大模型',
  config: {
    inputParams: [],
    providerId: '',
    model: '',
    temperature: 0.5,
    topP: 0.9,
    topK: 50,
    systemPrompt: '',
    userPrompt: '{{question}}',
    outputKey: 'output',
    outputFormat: 'TEXT',
    outputParams: [{ name: 'output', type: 'String' }]
  }
};

const knowledgeNode: WorkflowNode = {
  id: 'knowledge_1',
  type: 'KNOWLEDGE_RETRIEVAL',
  name: '知识库',
  config: {
    inputParams: [{ name: 'search_key', value: 'keyword', type: 'String' }],
    knowledgeBaseId: '',
    keywordTemplate: '{{search_key}}',
    queryKey: 'keyword',
    fetchCount: 5,
    outputKey: 'documents',
    outputFormat: 'ARRAY',
    outputParams: [
      { name: 'documents', type: 'Array' },
      { name: 'title', type: 'String' },
      { name: 'content', type: 'String' },
      { name: 'documentId', type: 'Number' },
      { name: 'knowledgeId', type: 'Number' }
    ]
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
