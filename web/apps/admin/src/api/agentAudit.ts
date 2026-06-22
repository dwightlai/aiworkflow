import { requestJson } from './auth';

export interface AgentAuditLog {
  id: string;
  userId: string | null;
  botId: string | null;
  conversationId: string | null;
  messageId: string | null;
  connectorCode: string | null;
  operationCode: string | null;
  eventType: string;
  requestSummary: string | null;
  responseSummary: string | null;
  status: string | null;
  errorMessage: string | null;
  traceId: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export async function listAgentAuditLogs(params: {
  traceId?: string;
  botId?: string;
  eventType?: string;
  connectorCode?: string;
  operationCode?: string;
  status?: string;
  limit?: number;
}): Promise<PageResponse<AgentAuditLog>> {
  const query = new URLSearchParams();
  if (params.traceId) query.set('traceId', params.traceId);
  if (params.botId) query.set('botId', params.botId);
  if (params.eventType) query.set('eventType', params.eventType);
  if (params.connectorCode) query.set('connectorCode', params.connectorCode);
  if (params.operationCode) query.set('operationCode', params.operationCode);
  if (params.status) query.set('status', params.status);
  if (params.limit) query.set('limit', String(params.limit));
  const suffix = query.toString() ? `?${query.toString()}` : '';
  return requestJson<PageResponse<AgentAuditLog>>(`/api/agent-audit/logs${suffix}`);
}
