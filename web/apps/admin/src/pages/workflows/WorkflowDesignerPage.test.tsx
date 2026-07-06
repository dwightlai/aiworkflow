// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, createEvent, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { WorkflowDesignerPage } from './WorkflowDesignerPage';

const workflowApiMock = vi.hoisted(() => ({
  createWorkflow: vi.fn(async () => ({
    id: 'workflow-created',
    name: '新建工作流',
    description: null,
    status: 'DRAFT',
    latestVersion: null
  })),
  getWorkflow: vi.fn(async () => ({
    id: 'workflow-1',
    name: '客服意图识别',
    status: 'DRAFT',
    latestVersion: {
      definition: {
        nodes: [
          { id: 'start', type: 'START', name: '开始', config: {} },
          { id: 'end', type: 'END', name: '结束', config: {} }
        ],
        edges: [{ id: 'edge_start_end', sourceNodeId: 'start', targetNodeId: 'end', condition: null }],
        variables: []
      }
    }
  })),
  updateWorkflowDraft: vi.fn(),
  updateWorkflowMetadata: vi.fn(async (_workflowId: string, request: { name: string; description: string | null }) => ({
    id: 'workflow-1',
    name: request.name,
    description: request.description,
    status: 'DRAFT',
    latestVersion: {
      definition: {
        nodes: [
          { id: 'start', type: 'START', name: '开始', config: {} },
          { id: 'end', type: 'END', name: '结束', config: {} }
        ],
        edges: [{ id: 'edge_start_end', sourceNodeId: 'start', targetNodeId: 'end', condition: null }],
        variables: []
      }
    }
  })),
  publishWorkflow: vi.fn(),
  runWorkflow: vi.fn(async () => ({
    id: 'run-1',
    workflowId: 'workflow-1',
    workflowVersionId: 'version-1',
    status: 'SUCCEEDED',
    input: {},
    output: {},
    errorMessage: null,
    startedAt: '2026-05-29T01:00:00Z',
    finishedAt: '2026-05-29T01:00:01Z',
    nodeExecutions: [
      {
        id: 'node-exec-start',
        workflowExecutionId: 'run-1',
        nodeId: 'start',
        nodeType: 'START',
        status: 'SUCCEEDED',
        input: {},
        output: {},
        errorMessage: null,
        startedAt: '2026-05-29T01:00:00Z',
        finishedAt: '2026-05-29T01:00:01Z'
      },
      {
        id: 'node-exec-end',
        workflowExecutionId: 'run-1',
        nodeId: 'end',
        nodeType: 'END',
        status: 'SUCCEEDED',
        input: {},
        output: {},
        errorMessage: null,
        startedAt: '2026-05-29T01:00:00Z',
        finishedAt: '2026-05-29T01:00:01Z'
      }
    ]
  }))
}));

const modelApiMock = vi.hoisted(() => ({
  listModelProviders: vi.fn(async () => ({
    items: [
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
    ],
    total: 1
  }))
}));

const promptApiMock = vi.hoisted(() => ({
  listPromptTemplates: vi.fn(async () => ({
    items: [
      {
        id: 'prompt_1',
        name: 'Greeting',
        template: 'Hello {{name}}',
        description: 'Greeting prompt'
      }
    ],
    total: 1
  }))
}));

const knowledgeApiMock = vi.hoisted(() => ({
  listKnowledgeBases: vi.fn(async () => ({
    items: [
      {
        id: 'kb_1',
        name: '产品知识库',
        description: '客服资料',
        documentCount: 1,
        chunkCount: 3
      }
    ],
    total: 1
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

vi.mock('../../api/workflows', () => workflowApiMock);
vi.mock('../../api/models', () => modelApiMock);
vi.mock('../../api/prompts', () => promptApiMock);
vi.mock('../../api/knowledge', () => knowledgeApiMock);

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  window.history.replaceState(null, '', '/');
});

describe('WorkflowDesignerPage', () => {
  it('renders a full-canvas designer and opens config/debug panels on demand', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('客服意图识别')).toBeInTheDocument();
    expect(screen.getByText('节点库')).toBeInTheDocument();
    expect(screen.getByText('PROMPT')).toBeInTheDocument();
    expect(screen.queryByText('工作流-测试')).not.toBeInTheDocument();
    expect(screen.getByText('编排')).toBeInTheDocument();
    expect(screen.queryByText('节点属性：结束')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '节点 结束' }));
    expect(await screen.findByText('节点属性：结束')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('工作流画布视口'));
    expect(screen.queryByText('节点属性：结束')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'bug 调试' }));
    expect(await screen.findByText('运行调试')).toBeInTheDocument();
  }, 15000);

  it('renders a product-grade designer workspace with draggable nodes and connection ports', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(screen.queryByText('工作流-测试')).not.toBeInTheDocument();
    expect(screen.getByText('节点编排')).toBeInTheDocument();
    expect(screen.getByText('拖拽节点')).toBeInTheDocument();
    expect(screen.getByText('端口连线')).toBeInTheDocument();
    expect(screen.getAllByLabelText(/从 .* 连线/).length).toBeGreaterThan(0);
    expect(screen.getAllByLabelText(/连接到 .*/).length).toBeGreaterThan(0);
    expect(screen.getByText('100% 配置')).toBeInTheDocument();
    expect(screen.getByText('待调试')).toBeInTheDocument();
  }, 15000);

  it('persists node drag positions and newly connected edges into the workflow definition', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    const startNode = await screen.findByRole('button', { name: '节点 开始' });
    fireEvent.mouseDown(startNode, { clientX: 32, clientY: 32 });
    fireEvent.mouseMove(window, { clientX: 82, clientY: 92 });
    fireEvent.mouseUp(window, { clientX: 82, clientY: 92 });

    await userEvent.click(screen.getByLabelText('从 开始 连线'));
    await userEvent.click(screen.getByLabelText('连接到 结束'));
    await userEvent.click(screen.getByRole('button', { name: '保存草稿' }));

    expect(workflowApiMock.updateWorkflowDraft).toHaveBeenCalledWith(
      'workflow-1',
      expect.objectContaining({
        edges: [expect.objectContaining({
          id: 'edge_start_end',
          sourceNodeId: 'start',
          targetNodeId: 'end'
        })],
        nodes: expect.arrayContaining([
          expect.objectContaining({
            id: 'start',
            config: expect.objectContaining({
              ui: expect.objectContaining({
                position: { x: 82, y: 92 }
              })
            })
          })
        ])
      })
    );
  });

  it('publishes current metadata and unsaved designer definition in one request', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByLabelText('选择连线 edge_start_end'));
    await userEvent.type(screen.getByLabelText('连线条件表达式'), 'intent == refund');

    await userEvent.click(screen.getByRole('button', { name: '发布' }));

    expect(workflowApiMock.updateWorkflowDraft).not.toHaveBeenCalled();
    expect(workflowApiMock.publishWorkflow).toHaveBeenCalledWith(
      'workflow-1',
      expect.objectContaining({
        name: '客服意图识别',
        description: null,
        definition: expect.objectContaining({
          edges: expect.arrayContaining([
            expect.objectContaining({
              id: 'edge_start_end',
              condition: 'intent == refund'
            })
          ])
        })
      })
    );
  });

  it('adds palette nodes by dragging them onto the canvas at the drop point', async () => {
    const dataTransfer = createDataTransfer();
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    fireEvent.dragStart(screen.getByRole('button', { name: /Prompt 模板/ }), { dataTransfer });
    const dropTarget = screen.getByLabelText('工作流画布投放区');
    fireEvent.dragOver(dropTarget, {
      clientX: 420,
      clientY: 250,
      dataTransfer
    });
    const dropEvent = createEvent.drop(dropTarget, { dataTransfer });
    Object.defineProperties(dropEvent, {
      clientX: { value: 420 },
      clientY: { value: 250 },
      pageX: { value: 420 },
      pageY: { value: 250 }
    });
    fireEvent(dropTarget, dropEvent);

    expect(await screen.findByDisplayValue('Prompt 模板')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '保存草稿' }));

    expect(workflowApiMock.updateWorkflowDraft).toHaveBeenCalledWith(
      'workflow-1',
      expect.objectContaining({
        nodes: expect.arrayContaining([
          expect.objectContaining({
            type: 'PROMPT',
            config: expect.objectContaining({
              ui: expect.objectContaining({
                position: { x: 420, y: 250 }
              })
            })
          })
        ])
      })
    );
  });

  it('saves incomplete draft definitions while surfacing validation issues', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByLabelText('选择连线 edge_start_end'));
    await userEvent.click(screen.getByRole('button', { name: '删除连线' }));
    await userEvent.click(screen.getByRole('button', { name: '保存草稿' }));

    expect((await screen.findAllByText('流程结构校验未通过')).length).toBeGreaterThan(0);
    expect(workflowApiMock.updateWorkflowDraft).toHaveBeenCalledWith(
      'workflow-1',
      expect.objectContaining({ edges: [] })
    );
  });

  it('opens edge properties and saves edge conditions into the draft definition', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByLabelText('选择连线 edge_start_end'));

    expect(await screen.findByText('连线属性：edge_start_end')).toBeInTheDocument();
    const conditionInput = screen.getByLabelText('连线条件表达式');
    await userEvent.type(conditionInput, 'intent == refund');
    await userEvent.click(screen.getByRole('button', { name: '保存草稿' }));

    expect(workflowApiMock.updateWorkflowDraft).toHaveBeenCalledWith(
      'workflow-1',
      expect.objectContaining({
        edges: [expect.objectContaining({
          id: 'edge_start_end',
          condition: 'intent == refund'
        })]
      })
    );
  });

  it('does not switch properties when hovering an edge until it is clicked', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    const edge = screen.getByLabelText('选择连线 edge_start_end');
    fireEvent.mouseEnter(edge);

    expect(screen.queryByText('连线属性：edge_start_end')).not.toBeInTheDocument();

    await userEvent.click(edge);
    expect(await screen.findByText('连线属性：edge_start_end')).toBeInTheDocument();
  });

  it('links right panel selection with the canvas and overlays run status on nodes', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByRole('button', { name: '节点 结束' }));
    expect(screen.getByDisplayValue('结束')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '运行' }));
    expect((await screen.findAllByText('SUCCEEDED')).length).toBeGreaterThanOrEqual(2);
  });

  it('creates a workflow from the new designer and navigates to its designer route', async () => {
    window.history.replaceState(null, '', '/workflows/new/designer');

    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="new" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('新建工作流')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '创建工作流' }));

    expect(workflowApiMock.createWorkflow).toHaveBeenCalledWith(expect.objectContaining({
      name: '新建工作流',
      description: null
    }));
    expect(window.location.pathname).toBe('/workflows/workflow-created/designer');
  });

  it('opens workflow properties from blank canvas selection and saves metadata', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('客服意图识别')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('工作流画布视口'));

    expect(await screen.findByText('工作流属性')).toBeInTheDocument();
    const nameInput = screen.getByLabelText('工作流名称');
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, '售后处理流程');
    await userEvent.click(screen.getByRole('button', { name: '保存属性' }));

    expect(workflowApiMock.updateWorkflowMetadata).toHaveBeenCalledWith('workflow-1', {
      name: '售后处理流程',
      description: null
    });
  });

  it('switches from node properties to workflow properties when clicking blank canvas', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByRole('button', { name: '节点 结束' }));
    expect(screen.getByDisplayValue('结束')).toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('工作流画布投放区'));

    expect(await screen.findByDisplayValue('客服意图识别')).toBeInTheDocument();
    expect(screen.queryByDisplayValue('结束')).not.toBeInTheDocument();
  });

  it('loads saved model providers for LLM node configuration', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByRole('button', { name: /大模型/ }));

    expect(await screen.findByDisplayValue('大模型')).toBeInTheDocument();
    expect(modelApiMock.listModelProviders).toHaveBeenCalled();
  });

  it('loads saved prompt templates for PROMPT node configuration', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    await screen.findByText('客服意图识别');
    await userEvent.click(screen.getByRole('button', { name: /Prompt 模板/ }));

    expect(await screen.findByText('已保存 Prompt')).toBeInTheDocument();
    expect(promptApiMock.listPromptTemplates).toHaveBeenCalled();
  });

  it('starts new workflows without an end node and allows saving incomplete drafts', async () => {
    window.history.replaceState(null, '', '/workflows/new/designer');

    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkflowDesignerPage workflowId="new" />
      </QueryClientProvider>
    );

    expect(await screen.findByRole('button', { name: '节点 Start' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '节点 End' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '创建工作流' }));

    expect(workflowApiMock.createWorkflow).toHaveBeenCalledWith(expect.objectContaining({
      definition: expect.objectContaining({
        nodes: [expect.objectContaining({ type: 'START' })],
        edges: []
      })
    }));
  });
});

function createDataTransfer(): DataTransfer {
  const values = new Map<string, string>();
  const dataTransfer = {
    dropEffect: 'copy',
    effectAllowed: 'copy',
    files: [] as unknown as FileList,
    items: [] as unknown as DataTransferItemList,
    types: [] as unknown as string[],
    clearData: vi.fn((type?: string) => {
      if (type) {
        values.delete(type);
        return;
      }
      values.clear();
    }),
    getData: vi.fn((type: string) => values.get(type) ?? ''),
    setData: vi.fn((type: string, value: string) => {
      values.set(type, value);
      (dataTransfer.types as unknown as string[]) = Array.from(values.keys());
    }),
    setDragImage: vi.fn()
  } as DataTransfer;
  return dataTransfer;
}
