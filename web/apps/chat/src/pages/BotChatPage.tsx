import { ArrowLeftOutlined, ArrowDownOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Layout, Spin, Typography, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import {
  createChatSession,
  deleteChatSession,
  exchangeEmbedSession,
  getChatBot,
  listChatMessages,
  listChatSessions,
  streamChatMessage,
  updateChatSession,
  type ChatSession
} from '../api/chat';
import { ChatInputBar } from '../components/ChatInputBar';
import { ChatMessageList } from '../components/ChatMessageList';
import { buildSessionShareUrl, ChatSessionList } from '../components/ChatSessionList';
import { applySseEvent, toPlatformMessages, type PlatformMessage } from '../utils/sseAdapter';

const { Sider, Content } = Layout;

export function BotChatPage({
  botId,
  ticket,
  initialSessionId,
  onNavigate,
  onTicketConsumed
}: {
  botId: string;
  ticket?: string | null;
  initialSessionId?: string | null;
  onNavigate: (path: string) => void;
  onTicketConsumed: () => void;
}) {
  const queryClient = useQueryClient();
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<PlatformMessage[]>([]);
  const [streaming, setStreaming] = useState(false);
  const [streamError, setStreamError] = useState<string | null>(null);
  const [draftMessage, setDraftMessage] = useState('');
  const [showScrollBottom, setShowScrollBottom] = useState(false);
  const assistantIdRef = useRef(`assistant-${Date.now()}`);
  const scrollRef = useRef<HTMLDivElement>(null);
  const stickToBottomRef = useRef(true);

  const botQuery = useQuery({ queryKey: ['chat-bot', botId], queryFn: () => getChatBot(botId) });
  const sessionsQuery = useQuery({
    queryKey: ['chat-sessions', botId],
    queryFn: () => listChatSessions(botId)
  });
  const messagesQuery = useQuery({
    queryKey: ['chat-messages', botId, selectedSessionId],
    queryFn: () => listChatMessages(botId, selectedSessionId!),
    enabled: Boolean(selectedSessionId)
  });

  const ticketMutation = useMutation({
    mutationFn: (t: string) => exchangeEmbedSession({ ticket: t, botId }),
    onSuccess: (result) => {
      setSelectedSessionId(result.sessionId);
      onTicketConsumed();
      message.success('嵌入会话已就绪');
      queryClient.invalidateQueries({ queryKey: ['chat-sessions', botId] });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '票据校验失败');
    }
  });

  const ticketHandledRef = useRef(false);
  const initialSessionHandledRef = useRef(false);
  useEffect(() => {
    if (!ticket || ticketHandledRef.current) {
      return;
    }
    ticketHandledRef.current = true;
    ticketMutation.mutate(ticket);
  }, [ticket]);

  useEffect(() => {
    if (!initialSessionId || initialSessionHandledRef.current || ticket) {
      return;
    }
    initialSessionHandledRef.current = true;
    setSelectedSessionId(initialSessionId);
  }, [initialSessionId, ticket]);

  useEffect(() => {
    if (streaming) {
      return;
    }
    if (messagesQuery.data?.items) {
      setMessages(toPlatformMessages(messagesQuery.data.items));
    }
  }, [messagesQuery.data, streaming]);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el || !stickToBottomRef.current) {
      return;
    }
    el.scrollTop = el.scrollHeight;
  }, [messages, streaming]);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) {
      return;
    }
    const onScroll = () => {
      const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
      stickToBottomRef.current = distance < 80;
      setShowScrollBottom(distance > 120);
    };
    el.addEventListener('scroll', onScroll);
    return () => el.removeEventListener('scroll', onScroll);
  }, []);

  const sessions = sessionsQuery.data?.items ?? [];
  const bot = botQuery.data;

  async function handleDeleteSession(session: ChatSession) {
    if (streaming) {
      message.warning('请等待当前回复完成');
      return;
    }
    try {
      await deleteChatSession(botId, session.id);
      if (selectedSessionId === session.id) {
        setSelectedSessionId(null);
        setMessages([]);
        setStreamError(null);
      }
      await queryClient.invalidateQueries({ queryKey: ['chat-sessions', botId] });
      message.success('已删除');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除失败');
    }
  }

  async function handleRenameSession(session: ChatSession, title: string) {
    try {
      await updateChatSession(botId, session.id, { title });
      await queryClient.invalidateQueries({ queryKey: ['chat-sessions', botId] });
      message.success('已重命名');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '重命名失败');
    }
  }

  async function handleTogglePinSession(session: ChatSession) {
    try {
      await updateChatSession(botId, session.id, { pinned: !session.pinned });
      await queryClient.invalidateQueries({ queryKey: ['chat-sessions', botId] });
      message.success(session.pinned ? '已取消置顶' : '已置顶');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败');
    }
  }

  async function handleShareSession(session: ChatSession) {
    const url = buildSessionShareUrl(botId, session.id);
    try {
      await navigator.clipboard.writeText(url);
      message.success('链接已复制');
    } catch {
      message.info(url);
    }
  }

  async function handleNewSession() {
    try {
      const session = await createChatSession(botId);
      setSelectedSessionId(session.id);
      setMessages([]);
      await queryClient.invalidateQueries({ queryKey: ['chat-sessions', botId] });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '创建会话失败');
    }
  }

  async function handleSend(text: string) {
    const trimmed = text.trim();
    if (!trimmed || streaming) {
      return;
    }
    setStreamError(null);
    let sessionId = selectedSessionId;
    if (!sessionId) {
      try {
        const session = await createChatSession(botId);
        sessionId = session.id;
        setSelectedSessionId(sessionId);
        await queryClient.invalidateQueries({ queryKey: ['chat-sessions', botId] });
      } catch (error) {
        message.error(error instanceof Error ? error.message : '创建会话失败');
        return;
      }
    }
    const userMessage: PlatformMessage = {
      id: `user-${Date.now()}`,
      role: 'USER',
      content: trimmed
    };
    assistantIdRef.current = `assistant-${Date.now()}`;
    const pendingAssistant: PlatformMessage = {
      id: assistantIdRef.current,
      role: 'ASSISTANT',
      content: '',
      streaming: true
    };
    stickToBottomRef.current = true;
    setMessages((prev) => [...prev, userMessage, pendingAssistant]);
    setStreaming(true);
    try {
      await streamChatMessage(botId, sessionId, { message: trimmed, input: {} }, (event) => {
        setMessages((prev) => applySseEvent(prev, event, assistantIdRef.current));
      });
      await queryClient.invalidateQueries({ queryKey: ['chat-messages', botId, sessionId] });
      await queryClient.invalidateQueries({ queryKey: ['chat-sessions', botId] });
    } catch (error) {
      const errMsg = error instanceof Error ? error.message : '发送失败';
      setStreamError(errMsg);
      if (selectedSessionId) {
        try {
          const data = await listChatMessages(botId, selectedSessionId);
          setMessages(toPlatformMessages(data.items));
        } catch {
          setMessages((prev) => prev.filter((m) => m.id !== assistantIdRef.current || m.content));
        }
      } else {
        setMessages((prev) => prev.filter((m) => m.id !== assistantIdRef.current || m.content));
      }
      message.error(errMsg);
    } finally {
      setStreaming(false);
    }
  }

  function scrollToBottom() {
    const el = scrollRef.current;
    if (!el) {
      return;
    }
    stickToBottomRef.current = true;
    el.scrollTop = el.scrollHeight;
    setShowScrollBottom(false);
  }

  function selectSession(session: ChatSession) {
    setSelectedSessionId(session.id);
    setStreamError(null);
  }

  if (botQuery.isLoading || (ticket && ticketMutation.isPending)) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
        <Spin size="large" tip={ticket ? '校验嵌入票据...' : '加载中...'} />
      </div>
    );
  }

  const isEmpty = messages.length === 0;

  return (
    <Layout style={{ height: '100vh', overflow: 'hidden', background: '#fff' }}>
      <Sider
        width={280}
        theme="light"
        style={{ borderRight: '1px solid #e7ecf3', height: '100vh', overflow: 'hidden', padding: 12 }}
      >
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => onNavigate('/')}>
          返回列表
        </Button>
        <Typography.Title level={5} style={{ margin: '12px 0 8px' }}>
          {bot?.name ?? '智能体'}
        </Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} block onClick={handleNewSession} style={{ marginBottom: 12 }}>
          新对话
        </Button>
        <ChatSessionList
          sessions={sessions}
          activeSessionId={selectedSessionId}
          onSelect={selectSession}
          onRename={handleRenameSession}
          onTogglePin={handleTogglePinSession}
          onShare={handleShareSession}
          onDelete={handleDeleteSession}
        />
      </Sider>
      <Content className="chat-main">
        <div ref={scrollRef} className="chat-scroll chat-content">
          <div className="chat-content-inner">
            {isEmpty ? (
              <div className="chat-empty">
                <h2 className="chat-empty-title">{bot?.name ?? '智能体'}</h2>
                <p className="chat-empty-desc">
                  {bot?.openingMessage || bot?.capabilityHint || bot?.description || '发送消息开始对话'}
                </p>
                {bot?.suggestedQuestions?.length ? (
                  <div className="chat-suggest-wrap">
                    {bot.suggestedQuestions.map((q) => (
                      <Button key={q} className="chat-suggest-btn" onClick={() => handleSend(q)}>
                        {q}
                      </Button>
                    ))}
                  </div>
                ) : null}
              </div>
            ) : (
              <ChatMessageList
                messages={messages}
                onEditMessage={setDraftMessage}
                onConfirmDone={async () => {
                  if (!selectedSessionId) {
                    return;
                  }
                  const data = await listChatMessages(botId, selectedSessionId);
                  setMessages(toPlatformMessages(data.items));
                  await queryClient.invalidateQueries({ queryKey: ['chat-messages', botId, selectedSessionId] });
                }}
              />
            )}
            {streamError ? (
              <Typography.Text type="danger" style={{ display: 'block', marginTop: 12, textAlign: 'center' }}>
                {streamError}
              </Typography.Text>
            ) : null}
          </div>
        </div>
        {showScrollBottom && !isEmpty ? (
          <button type="button" className="chat-scroll-bottom" aria-label="回到底部" onClick={scrollToBottom}>
            <ArrowDownOutlined />
          </button>
        ) : null}
        <ChatInputBar
          value={draftMessage}
          loading={streaming}
          placeholder={`给 ${bot?.name ?? '智能体'} 发送消息`}
          onChange={setDraftMessage}
          onSubmit={handleSend}
        />
      </Content>
    </Layout>
  );
}
