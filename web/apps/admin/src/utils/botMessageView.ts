import type { BotMessage } from '../api/bots';

export interface CitationItem {
  id: string;
  title: string;
}

export interface PlatformMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  messageType?: string;
  metadata?: Record<string, unknown>;
}

function normalizeMessageType(type?: string): string | undefined {
  if (!type || type === 'TEXT') return undefined;
  if (type === 'CITATION') return 'citation.added';
  return type;
}

function isCitationMessage(msg: PlatformMessage): boolean {
  return msg.messageType === 'citation.added';
}

function citationFromMetadata(id: string, metadata?: Record<string, unknown>, fallbackTitle?: string): CitationItem {
  const meta = metadata ?? {};
  return {
    id,
    title: String(meta.title ?? meta.documentName ?? fallbackTitle ?? '引用来源')
  };
}

function citationKey(title: string): string {
  return title.replace(/\s+/g, ' ').trim().toLowerCase();
}

function citationsFromMessages(messages: PlatformMessage[]): CitationItem[] {
  const bestByTitle = new Map<string, CitationItem>();
  for (const msg of messages) {
    const item = citationFromMetadata(msg.id, msg.metadata, msg.content.replace(/^引用：/, ''));
    bestByTitle.set(citationKey(item.title), item);
  }
  return [...bestByTitle.values()];
}

function renderAssistantTurn(turnMsgs: PlatformMessage[]) {
  const citationMsgs: PlatformMessage[] = [];
  let textMsg: PlatformMessage | null = null;
  for (const msg of turnMsgs) {
    if (isCitationMessage(msg)) {
      citationMsgs.push(msg);
      continue;
    }
    if (!msg.messageType) {
      if (textMsg) {
        textMsg.content = [textMsg.content, msg.content].filter(Boolean).join('\n\n');
      } else {
        textMsg = { ...msg };
      }
    }
  }
  return {
    text: textMsg?.content ?? '',
    citations: citationsFromMessages(citationMsgs)
  };
}

export type RenderItem =
  | { kind: 'user'; content: string }
  | { kind: 'assistant'; content: string; citations: CitationItem[] };

export function groupBotMessages(messages: BotMessage[]): RenderItem[] {
  const platform = messages.map((m) => ({
    id: m.id,
    role: m.role,
    content: m.content,
    messageType: normalizeMessageType(m.messageType),
    metadata: m.metadata
  })) as PlatformMessage[];

  const items: RenderItem[] = [];
  let index = 0;
  while (index < platform.length) {
    const msg = platform[index];
    if (msg.role === 'USER') {
      items.push({ kind: 'user', content: msg.content });
      index += 1;
      const turnMsgs: PlatformMessage[] = [];
      while (index < platform.length && platform[index].role !== 'USER') {
        turnMsgs.push(platform[index]);
        index += 1;
      }
      const turn = renderAssistantTurn(turnMsgs);
      if (turn.text || turn.citations.length > 0) {
        items.push({ kind: 'assistant', content: turn.text, citations: turn.citations });
      }
      continue;
    }
    const turnMsgs: PlatformMessage[] = [];
    while (index < platform.length && platform[index].role !== 'USER') {
      turnMsgs.push(platform[index]);
      index += 1;
    }
    const turn = renderAssistantTurn(turnMsgs);
    if (turn.text || turn.citations.length > 0) {
      items.push({ kind: 'assistant', content: turn.text, citations: turn.citations });
    }
  }
  return items;
}
