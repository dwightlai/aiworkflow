import { CopyOutlined, EditOutlined, RightOutlined } from '@ant-design/icons';
import { Button, message as antMessage } from 'antd';
import { useMemo, useState } from 'react';
import {
  isCitationMessage,
  parseMessageParts,
  type CitationItem,
  type PlatformMessage
} from '../utils/sseAdapter';
import {
  CitationList,
  ConfirmCard,
  ErrorCard,
  JobCompletedCard,
  ProgressCard,
  citationsFromMessages,
  mergeCitations
} from './MessageCards';
import { MarkdownContent } from './MarkdownContent';

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(
    () => antMessage.success('已复制'),
    () => antMessage.error('复制失败')
  );
}

type RenderItem =
  | { kind: 'user'; msg: PlatformMessage }
  | { kind: 'assistant-turn'; msg: PlatformMessage; citations: CitationItem[] }
  | { kind: 'structured'; msg: PlatformMessage };

function renderAssistantTurn(turnMsgs: PlatformMessage[]): RenderItem[] {
  const items: RenderItem[] = [];
  const citationMsgs: PlatformMessage[] = [];
  const structured: PlatformMessage[] = [];
  let textMsg: PlatformMessage | null = null;

  for (const msg of turnMsgs) {
    if (isCitationMessage(msg)) {
      citationMsgs.push(msg);
      continue;
    }
    if (!msg.messageType) {
      if (textMsg) {
        textMsg = {
          ...textMsg,
          content: [textMsg.content, msg.content].filter(Boolean).join('\n\n'),
          citations: mergeCitations(textMsg.citations ?? [], msg.citations ?? [])
        };
      } else {
        textMsg = msg;
      }
      continue;
    }
    structured.push(msg);
  }

  const citations = mergeCitations(citationsFromMessages(citationMsgs), textMsg?.citations ?? []);
  if (textMsg || citations.length > 0) {
    items.push({
      kind: 'assistant-turn',
      msg: textMsg ?? {
        id: `turn-${turnMsgs[0]?.id ?? 'empty'}`,
        role: 'ASSISTANT',
        content: ''
      },
      citations
    });
  }
  for (const msg of structured) {
    items.push({ kind: 'structured', msg });
  }
  return items;
}

function groupMessages(messages: PlatformMessage[]): RenderItem[] {
  const items: RenderItem[] = [];
  let index = 0;
  while (index < messages.length) {
    const msg = messages[index];
    if (msg.role === 'USER') {
      items.push({ kind: 'user', msg });
      index += 1;
      const turnMsgs: PlatformMessage[] = [];
      while (index < messages.length && messages[index].role !== 'USER') {
        turnMsgs.push(messages[index]);
        index += 1;
      }
      items.push(...renderAssistantTurn(turnMsgs));
      continue;
    }
    const turnMsgs: PlatformMessage[] = [];
    while (index < messages.length && messages[index].role !== 'USER') {
      turnMsgs.push(messages[index]);
      index += 1;
    }
    items.push(...renderAssistantTurn(turnMsgs));
  }
  return items;
}

function ThinkingSection({
  thinking,
  streaming,
  waitingForAnswer,
  expanded,
  onToggle
}: {
  thinking: string;
  streaming?: boolean;
  waitingForAnswer: boolean;
  expanded: boolean;
  onToggle: () => void;
}) {
  if (waitingForAnswer) {
    return (
      <div className="chat-thinking-bar" style={{ cursor: 'default' }}>
        <span className="chat-thinking-dot" />
        正在思考...
      </div>
    );
  }
  if (!thinking) {
    return null;
  }
  const showThinkingBody = expanded || streaming;
  return (
    <div className="chat-thinking-wrap">
      <button type="button" className="chat-thinking-bar" onClick={onToggle}>
        {streaming ? (
          <>
            <span className="chat-thinking-dot" />
            正在思考...
          </>
        ) : (
          <>
            已完成思考
            <RightOutlined
              style={{
                fontSize: 11,
                transform: expanded ? 'rotate(90deg)' : 'none',
                transition: 'transform 0.15s'
              }}
            />
          </>
        )}
      </button>
      {showThinkingBody ? <div className="chat-thinking-content">{thinking}</div> : null}
    </div>
  );
}

function StructuredCard({
  msg,
  onConfirmDone
}: {
  msg: PlatformMessage;
  onConfirmDone?: () => void;
}) {
  const type = msg.messageType;
  if (type === 'confirm.required' || type === 'CONFIRM') {
    return <ConfirmCard msg={msg} onDone={onConfirmDone} />;
  }
  if (type === 'job.progress' || type === 'PROGRESS') {
    return <ProgressCard msg={msg} />;
  }
  if (type === 'job.completed') {
    return <JobCompletedCard msg={msg} />;
  }
  if (type === 'error' || type === 'ERROR') {
    return <ErrorCard msg={msg} />;
  }
  return <MarkdownContent content={msg.content} className="chat-assistant-content" />;
}

function AssistantTurn({
  msg,
  citations
}: {
  msg: PlatformMessage;
  citations: CitationItem[];
  onConfirmDone?: () => void;
}) {
  const parts = parseMessageParts(msg.content, msg.metadata, msg.thinking);
  const hasThinking = Boolean(parts.thinking);
  const hasAnswer = Boolean(parts.answer);
  const waitingForAnswer = Boolean(msg.streaming) && !hasAnswer && !hasThinking;
  const [thinkingExpanded, setThinkingExpanded] = useState(Boolean(msg.streaming && hasThinking));

  return (
    <div className="chat-assistant-turn">
      <ThinkingSection
        thinking={parts.thinking}
        streaming={msg.streaming}
        waitingForAnswer={waitingForAnswer}
        expanded={thinkingExpanded}
        onToggle={() => setThinkingExpanded((prev) => !prev)}
      />
      {hasAnswer ? (
        <>
          <MarkdownContent content={parts.answer} className="chat-assistant-content" />
          {msg.streaming ? <span className="chat-stream-cursor">▍</span> : null}
        </>
      ) : waitingForAnswer ? null : (
        <div className="chat-assistant-content chat-assistant-empty">暂无回复内容</div>
      )}
      <CitationList citations={citations} />
    </div>
  );
}
export function ChatMessageList({
  messages,
  onEditMessage,
  onConfirmDone
}: {
  messages: PlatformMessage[];
  botName?: string;
  onEditMessage?: (text: string) => void;
  onConfirmDone?: () => void;
}) {
  const renderItems = useMemo(() => groupMessages(messages), [messages]);

  return (
    <div className="chat-message-list">
      {renderItems.map((item) => {
        if (item.kind === 'user') {
          const msg = item.msg;
          return (
            <div key={msg.id} className="chat-user-row">
              <div className="chat-user-col">
                <div className="chat-user-bubble">{msg.content}</div>
                <div className="chat-user-actions">
                  <Button
                    type="text"
                    size="small"
                    className="chat-user-action-btn"
                    icon={<CopyOutlined />}
                    onClick={() => copyText(msg.content)}
                    aria-label="复制"
                  />
                  {onEditMessage ? (
                    <Button
                      type="text"
                      size="small"
                      className="chat-user-action-btn"
                      icon={<EditOutlined />}
                      onClick={() => onEditMessage(msg.content)}
                      aria-label="编辑"
                    />
                  ) : null}
                </div>
              </div>
            </div>
          );
        }
        if (item.kind === 'assistant-turn') {
          return (
            <div key={item.msg.id} className="chat-assistant-block">
              <AssistantTurn msg={item.msg} citations={item.citations} onConfirmDone={onConfirmDone} />
            </div>
          );
        }
        return (
          <div key={item.msg.id} className="chat-assistant-block">
            <StructuredCard msg={item.msg} onConfirmDone={onConfirmDone} />
          </div>
        );
      })}
    </div>
  );
}
