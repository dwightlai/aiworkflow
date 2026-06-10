import { afterEach, describe, expect, it, vi } from 'vitest';
import { chatBot, createBot, deleteBot, listBotMessages, listBotSessions, listBots, runBot, updateBot } from './bots';

describe('bots api', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('manages bots and runs a bot workflow', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { items: [{ id: 'bot_1', name: 'Support Bot', workflowId: 'wf_1', status: 'ENABLED', conversationCount: 0 }], total: 1 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'bot_2', name: 'Sales Bot', workflowId: 'wf_1', status: 'ENABLED', conversationCount: 0 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { id: 'bot_2', name: 'Sales Bot Pro', workflowId: 'wf_1', status: 'DISABLED', conversationCount: 3 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: {
          bot: { id: 'bot_1', name: 'Support Bot', workflowId: 'wf_1', status: 'ENABLED', conversationCount: 1 },
          execution: { id: 'run_1', workflowId: 'wf_1', status: 'SUCCEEDED', input: { message: 'hello' }, output: { answer: 'ok' }, nodeExecutions: [] }
        },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { items: [{ id: 'session_1', botId: 'bot_1', title: 'hello', messageCount: 2 }], total: 1 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: { items: [{ id: 'msg_1', role: 'USER', content: 'hello' }, { id: 'msg_2', role: 'ASSISTANT', content: 'ok' }], total: 2 },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({
        success: true,
        data: {
          session: { id: 'session_1', botId: 'bot_1', title: 'hello', messageCount: 4 },
          messages: [{ id: 'msg_1', role: 'USER', content: 'hello' }, { id: 'msg_2', role: 'ASSISTANT', content: 'ok' }],
          reply: { id: 'msg_4', role: 'ASSISTANT', content: 'still ok' },
          execution: { id: 'run_2', workflowId: 'wf_1', status: 'SUCCEEDED', input: { history: [] }, output: { answer: 'still ok' }, nodeExecutions: [] }
        },
        error: null
      }))
      .mockResolvedValueOnce(jsonResponse({ success: true, data: null, error: null }));
    vi.stubGlobal('fetch', fetchMock);

    const page = await listBots();
    await createBot({
      name: 'Sales Bot',
      description: null,
      avatar: 'robot',
      workflowId: 'wf_1',
      modelProviderId: null,
      knowledgeBaseIds: [],
      systemPrompt: '',
      openingMessage: 'Hi',
      status: 'ENABLED'
    });
    const updated = await updateBot('bot_2', {
      name: 'Sales Bot Pro',
      description: null,
      avatar: 'robot',
      workflowId: 'wf_1',
      modelProviderId: null,
      knowledgeBaseIds: [],
      systemPrompt: '',
      openingMessage: 'Hi',
      status: 'DISABLED'
    });
    const run = await runBot('bot_1', { message: 'hello', input: { channel: 'web' } });
    const sessions = await listBotSessions('bot_1');
    const messages = await listBotMessages('bot_1', 'session_1');
    const chat = await chatBot('bot_1', { sessionId: 'session_1', message: 'again', input: {} });
    await deleteBot('bot_1');

    expect(page.items[0].name).toBe('Support Bot');
    expect(updated.status).toBe('DISABLED');
    expect(run.execution.output.answer).toBe('ok');
    expect(sessions.items[0].messageCount).toBe(2);
    expect(messages.items[1].content).toBe('ok');
    expect(chat.reply.content).toBe('still ok');
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/bots');
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/bots', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, '/api/bots/bot_2', expect.objectContaining({ method: 'PUT' }));
    expect(fetchMock).toHaveBeenNthCalledWith(4, '/api/bots/bot_1/run', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(5, '/api/bots/bot_1/sessions');
    expect(fetchMock).toHaveBeenNthCalledWith(6, '/api/bots/bot_1/sessions/session_1/messages');
    expect(fetchMock).toHaveBeenNthCalledWith(7, '/api/bots/bot_1/chat', expect.objectContaining({ method: 'POST' }));
    expect(fetchMock).toHaveBeenNthCalledWith(8, '/api/bots/bot_1', expect.objectContaining({ method: 'DELETE' }));
  });
});

function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body
  } as Response;
}
