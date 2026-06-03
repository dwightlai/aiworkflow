// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { BotsPage } from './BotsPage';

const botsApiMock = vi.hoisted(() => ({
  listBots: vi.fn(async () => ({
    items: [
      {
        id: 'bot_1',
        name: '客服助手',
        description: '处理售后咨询',
        avatar: 'robot',
        workflowId: 'wf_1',
        modelProviderId: 'model_chat',
        knowledgeBaseId: 'kb_1',
        systemPrompt: '回答要准确',
        openingMessage: '你好，我是客服助手',
        status: 'ENABLED',
        conversationCount: 12
      }
    ],
    total: 1
  })),
  createBot: vi.fn(async () => ({
    id: 'bot_2',
    name: '销售助手',
    workflowId: 'wf_1',
    status: 'ENABLED',
    conversationCount: 0
  })),
  updateBot: vi.fn(async () => ({
    id: 'bot_1',
    name: '客服助手 Pro',
    workflowId: 'wf_1',
    status: 'DISABLED',
    conversationCount: 12
  })),
  deleteBot: vi.fn(async () => undefined),
  listBotSessions: vi.fn(async () => ({
    items: [
      {
        id: 'session_1',
        botId: 'bot_1',
        title: '退货怎么处理',
        messageCount: 2
      }
    ],
    total: 1
  })),
  listBotMessages: vi.fn(async () => ({
    items: [
      {
        id: 'msg_1',
        sessionId: 'session_1',
        botId: 'bot_1',
        role: 'USER',
        content: '退货怎么处理'
      },
      {
        id: 'msg_2',
        sessionId: 'session_1',
        botId: 'bot_1',
        role: 'ASSISTANT',
        content: '七天内可申请退货'
      }
    ],
    total: 2
  })),
  chatBot: vi.fn(async () => ({
    session: {
      id: 'session_1',
      botId: 'bot_1',
      title: '退货怎么处理',
      messageCount: 2
    },
    messages: [
      {
        id: 'msg_1',
        sessionId: 'session_1',
        botId: 'bot_1',
        role: 'USER',
        content: '退货怎么处理'
      },
      {
        id: 'msg_2',
        sessionId: 'session_1',
        botId: 'bot_1',
        role: 'ASSISTANT',
        content: '七天内可申请退货'
      }
    ],
    reply: {
      id: 'msg_2',
      sessionId: 'session_1',
      botId: 'bot_1',
      role: 'ASSISTANT',
      content: '七天内可申请退货'
    },
    execution: {
      id: 'run_1',
      workflowId: 'wf_1',
      status: 'SUCCEEDED',
      input: { message: '退货怎么处理', history: [] },
      output: { answer: '七天内可申请退货' },
      nodeExecutions: []
    }
  })),
  runBot: vi.fn(async () => ({
    bot: {
      id: 'bot_1',
      name: '客服助手',
      workflowId: 'wf_1',
      status: 'ENABLED',
      conversationCount: 13
    },
    execution: {
      id: 'run_1',
      workflowId: 'wf_1',
      status: 'SUCCEEDED',
      input: { message: '退货怎么处理' },
      output: { answer: '七天内可申请退货' },
      nodeExecutions: []
    }
  }))
}));

vi.mock('../../api/bots', () => botsApiMock);

const workflowApiMock = vi.hoisted(() => ({
  listWorkflows: vi.fn(async () => ({
    items: [
      {
        id: 'wf_1',
        name: '客服问答流程',
        description: '售后咨询',
        status: 'PUBLISHED',
        latestVersion: { version: 2 }
      }
    ],
    total: 1
  }))
}));

vi.mock('../../api/workflows', () => workflowApiMock);

const modelApiMock = vi.hoisted(() => ({
  listModelProviders: vi.fn(async () => ({
    items: [
      {
        id: 'model_chat',
        name: 'DeepSeek Chat',
        modelType: 'DeepSeek',
        modelUsage: 'CHAT',
        model: 'deepseek-chat',
        enabled: true
      }
    ],
    total: 1
  }))
}));

vi.mock('../../api/models', () => modelApiMock);

const knowledgeApiMock = vi.hoisted(() => ({
  listKnowledgeBases: vi.fn(async () => ({
    items: [
      {
        id: 'kb_1',
        name: '售后知识库',
        documentCount: 3,
        chunkCount: 15
      }
    ],
    total: 1
  }))
}));

vi.mock('../../api/knowledge', () => knowledgeApiMock);

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

describe('BotsPage', () => {
  it('lists bots with workflow and binding details', async () => {
    renderPage();

    expect(await screen.findByText('客服助手')).toBeInTheDocument();
    expect(screen.getByText('客服问答流程')).toBeInTheDocument();
    expect(screen.getByText('售后知识库')).toBeInTheDocument();
    expect(screen.getAllByText('12').length).toBeGreaterThan(0);
  });

  it('creates edits runs and deletes bots', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /新增智能体/ }));
    fireEvent.change(await screen.findByLabelText('智能体名称'), { target: { value: '销售助手' } });
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '绑定工作流' }));
    await userEvent.click((await screen.findAllByText('客服问答流程')).at(-1)!);
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      expect(botsApiMock.createBot).toHaveBeenCalledWith(expect.objectContaining({
        name: '销售助手',
        workflowId: 'wf_1',
        status: 'ENABLED'
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /编辑智能体/ }));
    fireEvent.change(await screen.findByLabelText('智能体名称'), { target: { value: '客服助手 Pro' } });
    await userEvent.click(screen.getByRole('button', { name: /修改/ }));
    await waitFor(() => {
      expect(botsApiMock.updateBot).toHaveBeenCalledWith('bot_1', expect.objectContaining({
        name: '客服助手 Pro'
      }));
    });

    await userEvent.click(screen.getByRole('button', { name: /运行智能体/ }));
    fireEvent.change(await screen.findByLabelText('测试消息'), { target: { value: '退货怎么处理' } });
    await userEvent.click(screen.getByRole('button', { name: /发送消息/ }));
    await waitFor(() => {
      expect(botsApiMock.chatBot).toHaveBeenCalledWith('bot_1', expect.objectContaining({
        message: '退货怎么处理'
      }));
    });
    expect(await screen.findByText(/七天内可申请退货/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /删除智能体/ }));
    await waitFor(() => {
      expect(botsApiMock.deleteBot).toHaveBeenCalledWith('bot_1');
    });
  });

  it('creates a direct bot with model provider and without workflow binding', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /新增智能体/ }));
    fireEvent.change(await screen.findByLabelText('智能体名称'), { target: { value: '直连模型助手' } });
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '默认模型' }));
    await userEvent.click((await screen.findAllByText('DeepSeek Chat / deepseek-chat')).at(-1)!);
    await userEvent.click(screen.getByRole('button', { name: /保存/ }));

    await waitFor(() => {
      expect(botsApiMock.createBot).toHaveBeenCalledWith(expect.objectContaining({
        name: '直连模型助手',
        workflowId: null,
        modelProviderId: 'model_chat',
        status: 'ENABLED'
      }));
    });
  });
});

function renderPage() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <BotsPage />
    </QueryClientProvider>
  );
}
