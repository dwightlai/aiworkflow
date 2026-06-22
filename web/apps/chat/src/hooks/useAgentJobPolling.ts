import { useEffect, useRef, type Dispatch, type SetStateAction } from 'react';
import { getAgentJob, type AgentJobView } from '../api/chat';
import { applySseEvent, type PlatformMessage } from '../utils/sseAdapter';

const TERMINAL = new Set(['COMPLETED', 'SUCCEEDED', 'FAILED', 'CANCELLED']);
const RUNNING = new Set(['RUNNING', 'PENDING', '']);

function isRunningJob(msg: PlatformMessage): string | null {
  const jobId = msg.metadata?.jobId;
  if (typeof jobId !== 'string' || !jobId) {
    return null;
  }
  if (msg.messageType === 'job.started') {
    return jobId;
  }
  if (msg.messageType !== 'job.progress' && msg.messageType !== 'PROGRESS') {
    return null;
  }
  const status = String(msg.metadata?.jobStatus ?? 'RUNNING').toUpperCase();
  if (TERMINAL.has(status)) {
    return null;
  }
  return jobId;
}

function collectRunningJobIds(messages: PlatformMessage[]): string[] {
  const ids = new Set<string>();
  for (const msg of messages) {
    const jobId = isRunningJob(msg);
    if (jobId) {
      ids.add(jobId);
    }
  }
  return [...ids];
}

function jobEventFromView(job: AgentJobView) {
  const status = (job.status ?? '').toUpperCase();
  if (status === 'FAILED') {
    return { type: 'error' as const, data: { message: job.errorMessage ?? '任务执行失败' } };
  }
  if (TERMINAL.has(status)) {
    return {
      type: 'job.completed' as const,
      data: {
        jobId: job.id,
        title: job.title ?? '任务已完成',
        downloadUrl: job.downloadUrl,
        resultUrl: job.resultUrl
      }
    };
  }
  return {
    type: 'job.progress' as const,
    data: {
      jobId: job.id,
      progress: job.progress ?? 0,
      currentStep: job.currentStep ?? '任务执行中',
      jobStatus: status || 'RUNNING'
    }
  };
}

function updateJobMessages(messages: PlatformMessage[], job: AgentJobView): PlatformMessage[] {
  const event = jobEventFromView(job);
  const status = (job.status ?? '').toUpperCase();
  let next = messages;
  const targets = messages.filter((msg) => msg.metadata?.jobId === job.id || msg.metadata?.jobId === job.id);
  if (targets.length === 0) {
    return applySseEvent(messages, event, `job-${job.id}`);
  }
  for (const target of targets) {
    if (event.type === 'job.progress') {
      next = next.map((msg) =>
        msg.id === target.id
          ? {
              ...msg,
              messageType: 'job.progress',
              content: `任务进度：${event.data.progress ?? 0} ${event.data.currentStep ?? ''}`.trim(),
              metadata: { ...msg.metadata, ...event.data }
            }
          : msg
      );
    } else if (event.type === 'job.completed') {
      next = next.map((msg) =>
        msg.id === target.id
          ? {
              ...msg,
              messageType: 'job.completed',
              content: String(event.data.title ?? '任务已完成'),
              metadata: { ...msg.metadata, ...event.data, jobStatus: status }
            }
          : msg
      );
    } else {
      next = applySseEvent(next, event, target.id);
    }
  }
  return next;
}

export function useAgentJobPolling(
  messages: PlatformMessage[],
  setMessages: Dispatch<SetStateAction<PlatformMessage[]>>,
  enabled: boolean
) {
  const messagesRef = useRef(messages);
  messagesRef.current = messages;

  useEffect(() => {
    if (!enabled) {
      return;
    }
    const jobIds = collectRunningJobIds(messages);
    if (jobIds.length === 0) {
      return;
    }

    let cancelled = false;

    const poll = async () => {
      for (const jobId of jobIds) {
        try {
          const job = await getAgentJob(jobId);
          if (cancelled) {
            return;
          }
          const status = (job.status ?? '').toUpperCase();
          if (!TERMINAL.has(status) && !RUNNING.has(status) && status !== 'RUNNING') {
            continue;
          }
          setMessages((prev) => updateJobMessages(prev, job));
        } catch {
        }
      }
    };

    void poll();
    const timer = window.setInterval(() => {
      const active = collectRunningJobIds(messagesRef.current);
      if (active.length === 0) {
        return;
      }
      void poll();
    }, 2000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [enabled, messages, setMessages]);
}
