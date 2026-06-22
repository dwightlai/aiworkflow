import type { AuthSession } from './auth';
import { getAuthSession, notifyUnauthorized, requestJson, withRequestHeaders, type ApiEnvelope } from './auth';

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export interface ChatBot {
  id: string;
  name: string;
  description: string | null;
  avatar: string;
  openingMessage: string;
  capabilityHint?: string | null;
  suggestedQuestions?: string[];
}

export interface ChatSession {
  id: string;
  botId: string;
  title: string;
  messageCount: number;
  pinned?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export type ChatMessageRole = 'USER' | 'ASSISTANT';

export interface ChatMessage {
  id: string;
  sessionId: string;
  botId?: string;
  role: ChatMessageRole;
  content: string;
  messageType?: string;
  metadata?: Record<string, unknown>;
  createdAt?: string;
}

export interface StreamMessageRequest {
  message: string;
  input?: Record<string, unknown>;
}

export interface EmbedSessionExchangeRequest {
  ticket: string;
  botId?: string;
}

export async function exchangeEmbedSession(request: EmbedSessionExchangeRequest): Promise<AuthSession> {
  const { setAuthSession } = await import('./auth');
  const session = await requestJson<AuthSession>('/api/chat/embed-sessions/exchange', {
    method: 'POST',
    body: JSON.stringify(request)
  }, { skipAuth: true });
  setAuthSession(session);
  return session;
}

export type ChatSseEventType =
  | 'message.delta'
  | 'message.completed'
  | 'tool.started'
  | 'tool.completed'
  | 'tool.failed'
  | 'citation.added'
  | 'confirm.required'
  | 'job.started'
  | 'job.progress'
  | 'job.completed'
  | 'route.selected'
  | 'error'
  | 'done';

export interface ChatSseEvent {
  type: ChatSseEventType;
  data: Record<string, unknown>;
}

export async function listChatBots(): Promise<PageResponse<ChatBot>> {
  return requestJson<PageResponse<ChatBot>>('/api/chat/bots');
}

export async function getChatBot(botId: string): Promise<ChatBot> {
  return requestJson<ChatBot>(`/api/chat/bots/${botId}`);
}

export async function listChatSessions(botId: string): Promise<PageResponse<ChatSession>> {
  return requestJson<PageResponse<ChatSession>>(`/api/chat/bots/${botId}/sessions`);
}

export async function createChatSession(botId: string, title?: string): Promise<ChatSession> {
  return requestJson<ChatSession>(`/api/chat/bots/${botId}/sessions`, {
    method: 'POST',
    body: JSON.stringify({ title: title ?? '新对话' })
  });
}

export async function deleteChatSession(botId: string, sessionId: string): Promise<void> {
  await requestJson<{ deleted: boolean }>(`/api/chat/bots/${botId}/sessions/${sessionId}`, {
    method: 'DELETE'
  });
}

export async function updateChatSession(
  botId: string,
  sessionId: string,
  payload: { title?: string; pinned?: boolean }
): Promise<ChatSession> {
  return requestJson<ChatSession>(`/api/chat/bots/${botId}/sessions/${sessionId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  });
}

export async function listChatMessages(botId: string, sessionId: string): Promise<PageResponse<ChatMessage>> {
  return requestJson<PageResponse<ChatMessage>>(`/api/chat/bots/${botId}/sessions/${sessionId}/messages`);
}

export interface ConfirmTaskResponse {
  taskId: string;
  status: string;
  reply?: ChatMessage;
}

export async function confirmChatTask(taskId: string): Promise<ConfirmTaskResponse> {
  return requestJson<ConfirmTaskResponse>(`/api/chat/confirm-tasks/${taskId}/confirm`, { method: 'POST' });
}

export async function rejectChatTask(taskId: string): Promise<ConfirmTaskResponse> {
  return requestJson<ConfirmTaskResponse>(`/api/chat/confirm-tasks/${taskId}/reject`, { method: 'POST' });
}

export interface ConfirmTaskView {
  taskId: string;
  status: string;
  summary: string;
  payloadSnapshot?: Record<string, unknown>;
  connectorCode?: string;
  operationCode?: string;
}

export async function getConfirmTask(taskId: string): Promise<ConfirmTaskView> {
  return requestJson<ConfirmTaskView>(`/api/chat/confirm-tasks/${taskId}`);
}

export interface AgentJobView {
  id: string;
  status: string;
  progress?: number | null;
  currentStep?: string | null;
  errorMessage?: string | null;
  result?: string | null;
  downloadUrl?: string | null;
  resultUrl?: string | null;
  title?: string | null;
}

export async function getAgentJob(jobId: string): Promise<AgentJobView> {
  return requestJson<AgentJobView>(`/api/chat/agent-jobs/${jobId}`);
}

export async function streamChatMessage(
  botId: string,
  sessionId: string,
  request: StreamMessageRequest,
  onEvent: (event: ChatSseEvent) => void,
  options?: { signal?: AbortSignal }
): Promise<void> {
  const init = withRequestHeaders({
    method: 'POST',
    body: JSON.stringify(request),
    headers: {
      Accept: 'text/event-stream'
    },
    signal: options?.signal
  });
  const response = await fetch(`/api/chat/bots/${botId}/sessions/${sessionId}/messages/stream`, init);
  if (!response.ok) {
    let message = `Stream failed: ${response.status}`;
    let code: string | undefined;
    try {
      const envelope = await response.json() as ApiEnvelope<unknown>;
      message = envelope.error?.message ?? message;
      code = envelope.error?.code;
    } catch {
      // ignore
    }
    if (response.status === 401 || code === 'CHAT_AUTH_REQUIRED' || code === 'API_AUTH_REQUIRED') {
      notifyUnauthorized();
      throw new Error(message || '登录已失效，请重新登录');
    }
    throw new Error(message);
  }
  if (!response.body) {
    throw new Error('Stream body is empty');
  }
  await parseSseStream(response.body, onEvent);
}

async function parseSseStream(body: ReadableStream<Uint8Array>, onEvent: (event: ChatSseEvent) => void) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let eventType: ChatSseEventType | null = null;
  let dataLines: string[] = [];

  const flush = () => {
    if (!eventType && dataLines.length === 0) {
      return;
    }
    const rawData = dataLines.join('\n');
    let parsed: Record<string, unknown> = {};
    if (rawData) {
      try {
        parsed = JSON.parse(rawData) as Record<string, unknown>;
      } catch {
        parsed = { content: rawData };
      }
    }
    if (eventType) {
      onEvent({ type: eventType, data: parsed });
    }
    eventType = null;
    dataLines = [];
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      flush();
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() ?? '';
    for (const line of lines) {
      if (line === '') {
        flush();
        continue;
      }
      if (line.startsWith('event:')) {
        eventType = line.slice(6).trim() as ChatSseEventType;
        continue;
      }
      if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim());
      }
    }
  }
}

export function getStreamAuthHeaders(): Record<string, string> {
  const session = getAuthSession();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream'
  };
  if (session?.accessToken) {
    headers.Authorization = `Bearer ${session.accessToken}`;
  }
  return headers;
}
