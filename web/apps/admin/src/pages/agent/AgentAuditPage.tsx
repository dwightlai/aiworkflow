import { useQuery } from '@tanstack/react-query';
import { Button, Card, Form, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listAgentAuditLogs, type AgentAuditLog } from '../../api/agentAudit';
import { listBots } from '../../api/bots';

export function AgentAuditPage() {
  const [traceId, setTraceId] = useState('');
  const [botId, setBotId] = useState<string | undefined>();
  const [eventType, setEventType] = useState<string | undefined>();
  const botsQuery = useQuery({ queryKey: ['bots'], queryFn: listBots });
  const logsQuery = useQuery({
    queryKey: ['agent-audit', traceId, botId, eventType],
    queryFn: () => listAgentAuditLogs({ traceId: traceId || undefined, botId, eventType, limit: 100 })
  });

  const columns: ColumnsType<AgentAuditLog> = [
    { title: '时间', dataIndex: 'createdAt', width: 180 },
    { title: '事件', dataIndex: 'eventType', width: 140, render: (value) => <Tag>{value}</Tag> },
    { title: 'traceId', dataIndex: 'traceId', width: 220, ellipsis: true },
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
            options={['CHAT_MESSAGE', 'CHAT_REPLY', 'HITL_CONFIRM', 'HITL_REJECT', 'EMBED_TICKET_ISSUED', 'EMBED_TICKET_USED'].map((item) => ({
              value: item,
              label: item
            }))}
          />
          <Button type="primary" onClick={() => logsQuery.refetch()}>查询</Button>
        </Space>
      </Card>
      <Card>
        <Table rowKey="id" loading={logsQuery.isLoading} columns={columns} dataSource={logsQuery.data?.items ?? []} pagination={{ pageSize: 20 }} />
      </Card>
    </section>
  );
}
