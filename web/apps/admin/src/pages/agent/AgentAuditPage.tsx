import { useQuery } from '@tanstack/react-query';
import { Button, Card, Input, Select, Space, Table, Tag, Timeline, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { listAgentAuditLogs, type AgentAuditLog } from '../../api/agentAudit';
import { listBots } from '../../api/bots';

const EVENT_TYPES = [
  'CHAT_MESSAGE',
  'CHAT_REPLY',
  'CHAT_STREAM_FAILED',
  'WORKFLOW_RUN',
  'CHAT_AUTH_DENIED',
  'CONNECTOR_CALL',
  'HTTP_TOOL_CALL',
  'CONFIRM_REQUIRED',
  'HITL_CONFIRM',
  'HITL_REJECT',
  'EMBED_TICKET_ISSUED',
  'EMBED_TICKET_USED',
  'EMBED_TICKET_FAILED'
];

export function AgentAuditPage() {
  const [traceId, setTraceId] = useState('');
  const [botId, setBotId] = useState<string | undefined>();
  const [eventType, setEventType] = useState<string | undefined>();
  const botsQuery = useQuery({ queryKey: ['bots'], queryFn: listBots });
  const logsQuery = useQuery({
    queryKey: ['agent-audit', traceId, botId, eventType],
    queryFn: () => listAgentAuditLogs({ traceId: traceId || undefined, botId, eventType, limit: 200 })
  });

  const traceChain = useMemo(() => {
    if (!traceId.trim()) {
      return [];
    }
    return [...(logsQuery.data?.items ?? [])].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }, [logsQuery.data?.items, traceId]);

  const columns: ColumnsType<AgentAuditLog> = [
    { title: '时间', dataIndex: 'createdAt', width: 180 },
    { title: '事件', dataIndex: 'eventType', width: 140, render: (value) => <Tag>{value}</Tag> },
    {
      title: 'traceId',
      dataIndex: 'traceId',
      width: 220,
      ellipsis: true,
      render: (value: string | null) =>
        value ? (
          <Button type="link" size="small" style={{ padding: 0 }} onClick={() => setTraceId(value)}>
            {value}
          </Button>
        ) : (
          '-'
        )
    },
    { title: 'Bot', dataIndex: 'botId', width: 160, ellipsis: true },
    { title: '状态', dataIndex: 'status', width: 100 },
    { title: '请求摘要', dataIndex: 'requestSummary', ellipsis: true },
    { title: '响应摘要', dataIndex: 'responseSummary', ellipsis: true }
  ];

  return (
    <section style={{ padding: 24 }}>
      <Typography.Title level={3}>智能体审计</Typography.Title>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input allowClear placeholder="traceId" style={{ width: 260 }} value={traceId} onChange={(e) => setTraceId(e.target.value)} />
          <Select
            allowClear
            placeholder="Bot"
            style={{ width: 220 }}
            value={botId}
            onChange={setBotId}
            options={(botsQuery.data?.items ?? []).map((bot) => ({ value: bot.id, label: bot.name }))}
          />
          <Select
            allowClear
            placeholder="事件类型"
            style={{ width: 180 }}
            value={eventType}
            onChange={setEventType}
            options={EVENT_TYPES.map((item) => ({ value: item, label: item }))}
          />
          <Button type="primary" onClick={() => logsQuery.refetch()}>查询</Button>
        </Space>
      </Card>
      {traceId.trim() ? (
        <Card title={`链路追踪 · ${traceId}`} style={{ marginBottom: 16 }}>
          <Timeline
            items={traceChain.map((item) => ({
              color: item.status === 'FAILED' ? 'red' : item.status === 'SUCCESS' ? 'green' : 'blue',
              children: (
                <div>
                  <Typography.Text strong>{item.eventType}</Typography.Text>
                  <Typography.Text type="secondary" style={{ marginLeft: 8 }}>{item.createdAt}</Typography.Text>
                  {item.requestSummary ? <div>{item.requestSummary}</div> : null}
                  {item.responseSummary ? <div>{item.responseSummary}</div> : null}
                  {item.errorMessage ? <Typography.Text type="danger">{item.errorMessage}</Typography.Text> : null}
                </div>
              )
            }))}
          />
        </Card>
      ) : null}
      <Card>
        <Table rowKey="id" loading={logsQuery.isLoading} columns={columns} dataSource={logsQuery.data?.items ?? []} pagination={{ pageSize: 20 }} />
      </Card>
    </section>
  );
}
