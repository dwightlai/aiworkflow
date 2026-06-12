import { PlusOutlined, SendOutlined } from '@ant-design/icons';
import { Alert, Button, Drawer, Empty, Form, Input, List, Space, Typography } from 'antd';
import type { FormInstance } from 'antd';
import type { Bot, BotMessage, BotSession } from '../../api/bots';
import { BotRunMessageList, BotRunStatusLine } from './BotRunMessageList';
import '../../styles/botRunChat.css';

export function BotRunChatDrawer({
  open,
  bot,
  sessions,
  sessionsLoading,
  selectedSession,
  messages,
  runError,
  executionStatus,
  chatLoading,
  chatForm,
  onClose,
  onNewSession,
  onSelectSession,
  onSubmit
}: {
  open: boolean;
  bot: Bot | null;
  sessions: BotSession[];
  sessionsLoading: boolean;
  selectedSession: BotSession | null;
  messages: BotMessage[];
  runError: string | null;
  executionStatus?: string;
  chatLoading: boolean;
  chatForm: FormInstance<{ message: string; input: string }>;
  onClose: () => void;
  onNewSession: () => void;
  onSelectSession: (session: BotSession) => void;
  onSubmit: (values: { message: string; input: string }) => void;
}) {
  return (
    <Drawer
      title={bot ? `多轮对话 - ${bot.name}` : '多轮对话'}
      open={open}
      width={960}
      onClose={onClose}
      styles={{ body: { padding: 0, overflow: 'hidden', height: 'calc(100vh - 55px)' } }}
      footer={null}
    >
      <div className="bot-run-shell">
        <aside className="bot-run-sidebar">
          <Button block icon={<PlusOutlined />} onClick={onNewSession} style={{ marginBottom: 12 }}>
            新会话
          </Button>
          <div className="bot-run-session-list">
            <List
              loading={sessionsLoading}
              dataSource={sessions}
              locale={{ emptyText: '暂无会话' }}
              renderItem={(session) => (
                <div
                  className={`bot-run-session-item${selectedSession?.id === session.id ? ' active' : ''}`}
                  onClick={() => onSelectSession(session)}
                >
                  <Typography.Text strong>{session.title}</Typography.Text>
                  <div>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {session.messageCount} 条消息
                    </Typography.Text>
                  </div>
                </div>
              )}
            />
          </div>
        </aside>
        <section className="bot-run-main">
          <div className="bot-run-scroll">
            <div className="bot-run-scroll-inner">
              {messages.length === 0 && !runError ? (
                <Empty description={bot?.openingMessage || '发送第一条消息开始多轮对话'} />
              ) : (
                <BotRunMessageList messages={messages} emptyText={bot?.openingMessage} />
              )}
              {runError ? <Alert type="error" showIcon message={runError} style={{ marginTop: 16 }} /> : null}
              <div style={{ marginTop: 12 }}>
                <BotRunStatusLine status={executionStatus} />
              </div>
            </div>
          </div>
          <div className="bot-run-dock">
            <div className="bot-run-dock-inner">
              <Form form={chatForm} layout="vertical" initialValues={{ message: '', input: '{}' }} onFinish={onSubmit}>
                <div className="bot-run-form-grid">
                  <Form.Item name="message" label="测试消息" rules={[{ required: true, message: '请输入测试消息' }]}>
                    <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} placeholder="输入一条用户消息，系统会携带当前会话历史" />
                  </Form.Item>
                  <Form.Item name="input" label="附加变量 JSON">
                    <Input.TextArea style={{ fontFamily: 'Consolas, monospace' }} autoSize={{ minRows: 2, maxRows: 4 }} />
                  </Form.Item>
                  <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <Button onClick={onClose}>关闭</Button>
                    <Button type="primary" icon={<SendOutlined />} htmlType="submit" loading={chatLoading}>
                      发送消息
                    </Button>
                  </Space>
                </div>
              </Form>
            </div>
          </div>
        </section>
      </div>
    </Drawer>
  );
}
