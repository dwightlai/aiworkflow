import { Empty, Typography } from 'antd';
import type { BotMessage } from '../../api/bots';
import { groupBotMessages } from '../../utils/botMessageView';
import '../../styles/botRunChat.css';

function CitationFoot({ citations }: { citations: { id: string; title: string }[] }) {
  if (citations.length === 0) return null;
  return (
    <div className="bot-run-citation-foot">
      <div className="bot-run-citation-foot-title">引用文档</div>
      <ul className="bot-run-citation-foot-list">
        {citations.map((item, index) => (
          <li key={item.id} className="bot-run-citation-foot-item">
            <span className="bot-run-citation-foot-index">{index + 1}.</span>
            <span>{item.title}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function BotRunMessageList({ messages, emptyText }: { messages: BotMessage[]; emptyText?: string }) {
  const items = groupBotMessages(messages);
  if (items.length === 0) {
    return <Empty description={emptyText || '发送第一条消息开始多轮对话'} />;
  }
  return (
    <div className="bot-run-message-list">
      {items.map((item, index) =>
        item.kind === 'user' ? (
          <div key={`user-${index}`} className="bot-run-user-row">
            <div className="bot-run-user-bubble">{item.content}</div>
          </div>
        ) : (
          <div key={`assistant-${index}`} className="bot-run-assistant-block">
            {item.content ? <div className="bot-run-assistant-content">{item.content}</div> : null}
            <CitationFoot citations={item.citations} />
          </div>
        )
      )}
    </div>
  );
}

export function BotRunStatusLine({ status }: { status?: string }) {
  if (!status) return null;
  return (
    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
      最近执行：{status}
    </Typography.Text>
  );
}
