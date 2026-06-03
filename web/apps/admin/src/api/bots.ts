import type { WorkflowExecution } from './workflows';

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export type BotStatus = 'ENABLED' | 'DISABLED';

export interface Bot {
  id: string;
  name: string;
  description: string | null;
  avatar: string;
  workflowId: string | null;
  modelProviderId: string | null;
  knowledgeBaseId: string | null;
  systemPrompt: string;
  openingMessage: string;
  status: BotStatus;
  conversationCount: number;
  publishedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export type BotMessageRole = 'USER' | 'ASSISTANT';

export interface BotSession {
  id: string;
  botId: string;
  title: string;
  messageCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface BotMessage {
  id: string;
  sessionId: string;
  botId: string;
  role: BotMessageRole;
  content: string;
  createdAt?: string;
}

export interface SaveBotRequest {
  name: string;
  description: string | null;
  avatar: string;
  workflowId: string | null;
  modelProviderId: string | null;
  knowledgeBaseId: string | null;
  systemPrompt: string;
  openingMessage: string;
  status: BotStatus;
}

export interface RunBotRequest {
  message: string;
  input: Record<string, unknown>;
}

export interface ChatBotRequest extends RunBotRequest {
  sessionId?: string | null;
}

export interface BotRunResult {
  bot: Bot;
  execution: WorkflowExecution;
}

export interface BotChatResult {
  session: BotSession;
  messages: BotMessage[];
  reply: BotMessage;
  execution: WorkflowExecution;
}

export async function listBots(): Promise<PageResponse<Bot>> {
  return requestJson<PageResponse<Bot>>('/api/bots');
}

export async function createBot(request: SaveBotRequest): Promise<Bot> {
  return requestJson<Bot>('/api/bots', {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function updateBot(id: string, request: SaveBotRequest): Promise<Bot> {
  return requestJson<Bot>(`/api/bots/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request)
  });
}

export async function deleteBot(id: string): Promise<void> {
  await requestJson<void>(`/api/bots/${id}`, {
    method: 'DELETE'
  });
}

export async function runBot(id: string, request: RunBotRequest): Promise<BotRunResult> {
  return requestJson<BotRunResult>(`/api/bots/${id}/run`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

export async function listBotSessions(botId: string): Promise<PageResponse<BotSession>> {
  return requestJson<PageResponse<BotSession>>(`/api/bots/${botId}/sessions`);
}

export async function listBotMessages(botId: string, sessionId: string): Promise<PageResponse<BotMessage>> {
  return requestJson<PageResponse<BotMessage>>(`/api/bots/${botId}/sessions/${sessionId}/messages`);
}

export async function chatBot(id: string, request: ChatBotRequest): Promise<BotChatResult> {
  return requestJson<BotChatResult>(`/api/bots/${id}/chat`, {
    method: 'POST',
    body: JSON.stringify(request)
  });
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = init ? await fetch(url, withJsonHeaders(init)) : await fetch(url);
  const envelope = await response.json() as ApiEnvelope<T>;
  if (!response.ok || !envelope.success) {
    throw new Error(envelope.error?.message ?? `Request failed: ${response.status}`);
  }
  return envelope.data;
}

function withJsonHeaders(init?: RequestInit): RequestInit | undefined {
  if (!init) {
    return undefined;
  }
  return {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init.headers
    }
  };
}
