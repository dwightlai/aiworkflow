import { RobotOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Alert, Avatar, Card, Col, Empty, Row, Spin, Typography } from 'antd';
import { listChatBots } from '../api/chat';

export function BotListPage({ onNavigate }: { onNavigate: (path: string) => void }) {
  const botsQuery = useQuery({ queryKey: ['chat-bots'], queryFn: listChatBots });
  const bots = botsQuery.data?.items ?? [];

  if (botsQuery.isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <section style={{ padding: 24, maxWidth: 1080, margin: '0 auto' }}>
      <Typography.Title level={3} style={{ marginBottom: 8 }}>智能体</Typography.Title>
      <Typography.Paragraph type="secondary" style={{ marginBottom: 24 }}>
        选择智能体开始对话
      </Typography.Paragraph>
      {botsQuery.isError ? (
        <Alert type="error" showIcon message="加载智能体失败" description={(botsQuery.error as Error).message} />
      ) : null}
      {bots.length === 0 ? (
        <Empty description="暂无可用智能体" />
      ) : (
        <Row gutter={[16, 16]}>
          {bots.map((bot) => (
            <Col key={bot.id} xs={24} sm={12} md={8}>
              <Card
                hoverable
                onClick={() => onNavigate(`/bots/${bot.id}`)}
                style={{ border: '1px solid #e7ecf3' }}
              >
                <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                  <Avatar size={48} icon={<RobotOutlined />} style={{ background: '#1677ff' }} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Typography.Text strong>{bot.name}</Typography.Text>
                    <Typography.Paragraph type="secondary" ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>
                      {bot.description || bot.openingMessage || '开始对话'}
                    </Typography.Paragraph>
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </section>
  );
}
