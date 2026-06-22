import { requestJson } from './auth';

export interface AgentJob {
  id: string;
  botId: string | null;
  conversationId: string | null;
  sourceJobId: string | null;
  jobType: string;
  status: string;
  progress: number | null;
  currentStep: string | null;
  result: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
}

export async function listAgentJobs(params: {
  botId?: string;
  status?: string;
  conversationId?: string;
  limit?: number;
}): Promise<PageResponse<AgentJob>> {
  const query = new URLSearchParams();
  if (params.botId) query.set('botId', params.botId);
  if (params.status) query.set('status', params.status);
  if (params.conversationId) query.set('conversationId', params.conversationId);
  if (params.limit) query.set('limit', String(params.limit));
  const suffix = query.toString() ? `?${query.toString()}` : '';
  return requestJson<PageResponse<AgentJob>>(`/api/agent-jobs${suffix}`);
}

export async function getAgentJob(jobId: string): Promise<AgentJob> {
  return requestJson<AgentJob>(`/api/agent-jobs/${jobId}`);
}
