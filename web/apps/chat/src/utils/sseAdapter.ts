import type { ChatMessage, ChatSseEvent } from '../api/chat';

export interface CitationItem {
  id: string;
  title: string;
  excerpt?: string;
  sourceId?: string;
  score?: number;
}

export interface PlatformMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  thinking?: string;
  citations?: CitationItem[];
  messageType?: string;
  metadata?: Record<string, unknown>;
  streaming?: boolean;
}

export function citationFromMetadata(id: string, metadata?: Record<string, unknown>, fallbackTitle?: string): CitationItem {
  const meta = metadata ?? {};
  return {
    id,
    title: String(meta.title ?? meta.documentName ?? fallbackTitle ?? meta.sourceId ?? '引用来源'),
    excerpt: meta.excerpt != null ? String(meta.excerpt) : meta.content != null ? String(meta.content).slice(0, 240) : undefined,
    sourceId: meta.sourceId != null ? String(meta.sourceId) : meta.id != null ? String(meta.id) : undefined,
    score: typeof meta.score === 'number' ? meta.score : undefined
  };
}

export function isCitationMessage(msg: PlatformMessage): boolean {
  return msg.messageType === 'citation.added';
}

export function parseMessageParts(content: string, metadata?: Record<string, unknown>, thinking?: string) {
  const metaThinking = thinking ?? metadata?.reasoning_content ?? metadata?.thinking;
  if (metaThinking && String(metaThinking).trim()) {
    return { thinking: String(metaThinking).trim(), answer: content };
  }
  const thinkOpen = '<' + 'think' + '>';
  const thinkClose = '<' + '/' + 'think' + '>';
  const closeIdx = content.toLowerCase().indexOf(thinkClose);
  if (closeIdx >= 0) {
    const beforeClose = content.slice(0, closeIdx);
    const openIdx = beforeClose.toLowerCase().indexOf(thinkOpen);
    const thinkingText = (openIdx >= 0 ? beforeClose.slice(openIdx + thinkOpen.length) : beforeClose).trim();
    const answer = content.slice(closeIdx + thinkClose.length).trim();
    return { thinking: thinkingText, answer: answer || content };
  }
  return { thinking: '', answer: content };
}

function citationKey(title: string): string {
  return title.replace(/\s+/g, ' ').trim().toLowerCase();
}

function mergeCitationList(existing: CitationItem[], incoming: CitationItem): CitationItem[] {
  const bestByTitle = new Map<string, CitationItem>();
  for (const item of [...existing, incoming]) {
    const key = citationKey(item.title);
    const prev = bestByTitle.get(key);
    if (!prev || (item.score ?? 0) > (prev.score ?? 0)) {
      bestByTitle.set(key, item);
    }
  }
  return [...bestByTitle.values()];
}

export function applySseEvent(messages: PlatformMessage[], event: ChatSseEvent, assistantId: string): PlatformMessage[] {
  switch (event.type) {
    case 'message.delta': {
      const delta = String(event.data.content ?? '');
      const existing = messages.find((m) => m.id === assistantId);
      if (!existing) {
        return [...messages, { id: assistantId, role: 'ASSISTANT', content: delta, streaming: true }];
      }
      return messages.map((m) =>
        m.id === assistantId ? { ...m, content: m.content + delta, streaming: true } : m
      );
    }
    case 'message.completed': {
      const content = String(event.data.content ?? '');
      const existing = messages.find((m) => m.id === assistantId);
      if (!existing) {
        return [...messages, { id: assistantId, role: 'ASSISTANT', content, streaming: false }];
      }
      return messages.map((m) =>
        m.id === assistantId ? { ...m, content: content || m.content, streaming: false } : m
      );
    }
    case 'citation.added': {
      const citation = citationFromMetadata(`cite-${Date.now()}`, event.data);
      const existing = messages.find((m) => m.id === assistantId);
      if (existing) {
        const merged = mergeCitationList(existing.citations ?? [], citation);
        return messages.map((m) => (m.id === assistantId ? { ...m, citations: merged } : m));
      }
      return [
        ...messages,
        {
          id: assistantId,
          role: 'ASSISTANT',
          content: '',
          citations: [citation],
          streaming: true
        }
      ];
    }
    case 'confirm.required':
    case 'job.progress':
    case 'error':
    case 'tool.started':
    case 'tool.completed':
    case 'tool.failed':
    case 'job.started':
    case 'job.completed': {
      const cardId = `${event.type}-${Date.now()}`;
      return [
        ...messages,
        {
          id: cardId,
          role: 'ASSISTANT',
          content: formatStructuredEvent(event),
          messageType: event.type,
          metadata: event.data
        }
      ];
    }
    case 'done':
      return messages.map((m) => (m.id === assistantId ? { ...m, streaming: false } : m));
    default:
      return messages;
  }
}

function formatStructuredEvent(event: ChatSseEvent): string {
  if (event.type === 'citation.added') {
    return `引用：${event.data.title ?? event.data.sourceId ?? '来源'}`;
  }
  if (event.type === 'confirm.required') {
    return `待确认：${event.data.summary ?? '请确认操作'}`;
  }
  if (event.type === 'error') {
    return `错误：${event.data.message ?? JSON.stringify(event.data)}`;
  }
  if (event.type === 'job.progress') {
    return `任务进度：${event.data.progress ?? ''} ${event.data.currentStep ?? ''}`.trim();
  }
  if (event.type.startsWith('tool.')) {
    return `${event.data.name ?? event.data.operationCode ?? '工具'}：${event.type}`;
  }
  return JSON.stringify(event.data);
}

export function toPlatformMessages(items: ChatMessage[]): PlatformMessage[] {
  return items.map((m) => {
    const parts = parseMessageParts(m.content, m.metadata);
    return {
      id: m.id,
      role: m.role,
      content: parts.answer,
      thinking: parts.thinking || undefined,
      messageType: normalizeMessageType(m.messageType),
      metadata: m.metadata
    };
  });
}

function normalizeMessageType(type?: string): string | undefined {
  if (!type || type === 'TEXT') {
    return undefined;
  }
  if (type === 'CONFIRM') {
    return 'confirm.required';
  }
  if (type === 'CITATION') {
    return 'citation.added';
  }
  if (type === 'PROGRESS') {
    return 'job.progress';
  }
  if (type === 'JOB') {
    return 'job.completed';
  }
  if (type === 'ERROR') {
    return 'error';
  }
  return type;
}
