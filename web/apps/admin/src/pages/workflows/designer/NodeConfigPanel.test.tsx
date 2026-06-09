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
  it.skip('renders AIFlowy-style LLM properties and saves runnable model config', async () => {
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

    expect(screen.getByLabelText('节点名称')).toBeInTheDocument();
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

  it.skip('renders AIFlowy-style knowledge properties and saves runnable retrieval config', async () => {
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

    await userEvent.click(screen.getByRole('button', { name: '删除输入参数 1' }));
    expect(onChange).toHaveBeenCalledWith('knowledge_1', {
      config: expect.objectContaining({
        inputParams: []
      })
    });

    await userEvent.click(screen.getByRole('button', { name: '删除输出参数 2' }));
    expect(onChange).toHaveBeenCalledWith('knowledge_1', {
      config: expect.objectContaining({
        outputParams: expect.arrayContaining([
          expect.objectContaining({ name: 'documents' }),
          expect.objectContaining({ name: 'content' })
        ]),
        outputKey: 'documents'
      })
    });
  });

  it('renders runnable LLM message properties and fixed outputs', async () => {
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

    expect(screen.getByText('变量输入')).toBeInTheDocument();
    expect(screen.getByText('模型配置')).toBeInTheDocument();
    expect(screen.getByText('系统消息')).toBeInTheDocument();
    expect(screen.getByText('用户消息')).toBeInTheDocument();
    expect(screen.getByText('回复格式')).toBeInTheDocument();
    expect(screen.getByText('流式输出')).toBeInTheDocument();
    expect(screen.getByText('content')).toBeInTheDocument();
    expect(screen.getByText('reasoning_content')).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择模型' }));
    await userEvent.click(await screen.findByText('火山引擎-DS-V3'));
    expect(onChange).toHaveBeenCalledWith('llm_1', {
      config: expect.objectContaining({
        providerId: 'model_provider_1',
        model: 'DS-V3'
      })
    });

    fireEvent.change(screen.getByLabelText('用户消息'), { target: { value: '${input}' } });
    expect(onChange).toHaveBeenCalledWith('llm_1', {
      config: expect.objectContaining({ userMessage: '${input}' })
    });
  });

  it('renders runnable knowledge retrieval properties and fixed outputs', async () => {
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

    expect(screen.getByText('变量输入')).toBeInTheDocument();
    expect(screen.getByText('知识库配置')).toBeInTheDocument();
    expect(screen.getByText('查询文本')).toBeInTheDocument();
    expect(screen.getByText('检索数量(Top-K)')).toBeInTheDocument();
    expect(screen.getByText('相似度阈值')).toBeInTheDocument();
    expect(screen.getByText('content')).toBeInTheDocument();
    expect(screen.getByText('sources')).toBeInTheDocument();
    expect(screen.getByText('query')).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择知识库' }));
    await userEvent.click(await screen.findByText('客服手册'));
    expect(onChange).toHaveBeenCalledWith('knowledge_1', {
      config: expect.objectContaining({ knowledgeBaseId: 'kb_1' })
    });

    fireEvent.change(screen.getByLabelText('查询文本'), { target: { value: '如何退款' } });
    expect(onChange).toHaveBeenCalledWith('knowledge_1', {
      config: expect.objectContaining({
        queryText: '如何退款',
        keywordTemplate: '如何退款'
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

  it('renders content template node config and updates template fields', () => {
    const onChange = vi.fn();
    render(<NodeConfigPanel node={contentTemplateNode} onChange={onChange} />);

    expect(screen.getByText('通过模板生成文本或 JSON 内容')).toBeInTheDocument();
    expect(screen.getByText('模板设置')).toBeInTheDocument();
    expect(screen.getByText('模板内容')).toBeInTheDocument();
    expect(screen.getByText('输出参数')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('使用 {{question}}、{{documents}} 等上下文变量'), {
      target: { value: '答案：{{question}}' }
    });

    expect(onChange).toHaveBeenCalledWith('template_1', {
      config: expect.objectContaining({
        template: '答案：{{question}}'
      })
    });
  });

  it('renders HTTP node config and updates runnable request fields', () => {
    const onChange = vi.fn();
    render(<NodeConfigPanel node={httpNode} onChange={onChange} />);

    expect(screen.getByText('调用外部 API 并输出响应结构')).toBeInTheDocument();
    expect(screen.getByText('请求设置')).toBeInTheDocument();
    expect(screen.getByText('Params')).toBeInTheDocument();
    expect(screen.getByText('Headers')).toBeInTheDocument();
    expect(screen.getByText('Body 设置')).toBeInTheDocument();
    expect(screen.getByDisplayValue('httpResult')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('https://api.example.com/orders/{{orderId}}'), {
      target: { value: 'https://api.example.com/search' }
    });

    expect(onChange).toHaveBeenCalledWith('http_1', {
      config: expect.objectContaining({
        url: 'https://api.example.com/search'
      })
    });
  });

  it('renders loop node config and updates loop variables', () => {
    const onChange = vi.fn();
    render(<NodeConfigPanel node={loopNode} onChange={onChange} />);

    expect(screen.getByText('遍历数组并执行循环体步骤')).toBeInTheDocument();
    expect(screen.getByText('循环设置')).toBeInTheDocument();
    expect(screen.getByText('循环变量')).toBeInTheDocument();
    expect(screen.getByText('循环体步骤')).toBeInTheDocument();
    expect(screen.getByDisplayValue('loopResults')).toBeInTheDocument();

    fireEvent.change(screen.getByDisplayValue('items'), {
      target: { value: 'documents' }
    });

    expect(onChange).toHaveBeenCalledWith('loop_1', {
      config: expect.objectContaining({
        loopVar: 'documents'
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

const contentTemplateNode: WorkflowNode = {
  id: 'template_1',
  type: 'CONTENT_TEMPLATE',
  name: '内容模板',
  config: {
    template: '请回答：{{question}}',
    outputKey: 'content',
    outputFormat: 'TEXT',
    outputParams: [{ name: 'content', type: 'String' }]
  }
};

const httpNode: WorkflowNode = {
  id: 'http_1',
  type: 'HTTP_TOOL',
  name: 'HTTP 请求',
  config: {
    method: 'POST',
    url: '',
    params: [],
    headers: [{ key: 'Content-Type', value: 'application/json' }],
    bodyType: 'JSON',
    bodyTemplate: '{\n  "question": "{{question}}"\n}',
    responseBodyType: 'TEXT',
    outputKey: 'httpResult',
    outputParams: [
      { name: 'headers', type: 'Object' },
      { name: 'statusCode', type: 'Number' },
      { name: 'body', type: 'String' }
    ]
  }
};

const loopNode: WorkflowNode = {
  id: 'loop_1',
  type: 'LOOP',
  name: '循环',
  config: {
    loopVar: 'items',
    itemVar: 'loopItem',
    indexVar: 'index',
    maxIterations: 100,
    outputKey: 'loopResults',
    loopSteps: [{ type: 'CONTENT_TEMPLATE', name: '模板处理', template: '{{loopItem}}', outputKey: 'text' }],
    outputParams: [{ name: 'loopResults', type: 'Array' }]
  }
};
