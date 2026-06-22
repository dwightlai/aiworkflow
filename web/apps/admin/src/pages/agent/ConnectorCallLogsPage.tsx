import { useQuery } from '@tanstack/react-query';
import { Button, Card, Drawer, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listAgentAuditLogs, type AgentAuditLog } from '../../api/agentAudit';
import { listBots } from '../../api/bots';
import { listConnectors } from '../../api/connectors';

export function ConnectorCallLogsPage() {
  const [traceId, setTraceId] = useState('');
  const [botId, setBotId] = useState<string | undefined>();
  const [connectorCode, setConnectorCode] = useState<string | undefined>();
  const [operationCode, setOperationCode] = useState('');
  const [status, setStatus] = useState<string | undefined>();
  const [detail, setDetail] = useState<AgentAuditLog | null>(null);
  const botsQuery = useQuery({ queryKey: ['bots'], queryFn: listBots });
  const connectorsQuery = useQuery({ queryKey: ['connectors'], queryFn: listConnectors });
  const logsQuery = useQuery({
    queryKey: ['connector-call-logs', traceId, botId, connectorCode, operationCode, status],
    queryFn: () =>
      listAgentAuditLogs({
        traceId: traceId || undefined,
        botId,
        eventType: 'CONNECTOR_CALL',
        connectorCode,
        operationCode: operationCode || undefined,
        status,
        limit: 200
      })
  });

  const columns: ColumnsType<AgentAuditLog> = [
    { title: '时间', dataIndex: 'createdAt', width: 180 },
    { title: '连接器', dataIndex: 'connectorCode', width: 140 },
    { title: '接口', dataIndex: 'operationCode', width: 140 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (value: string | null) => (
        <Tag color={value === 'SUCCESS' ? 'green' : value === 'FAILED' ? 'red' : 'default'}>{value ?? '-'}</Tag>
      )
    },
    {
      title: 'traceId',
      dataIndex: 'traceId',
      width: 200,
      ellipsis: true,
      render: (value: string | null) => value ?? '-'
    },
    { title: 'Bot', dataIndex: 'botId', width: 140, ellipsis: true },
    { title: '请求摘要', dataIndex: 'requestSummary', ellipsis: true },
    {
      title: '操作',
      key: 'actions',
      width: 80,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => setDetail(record)}>
          详情
        </Button>
      )
    }
  ];

  return (
    <section style={{ padding: 24 }}>
      <Typography.Title level={3}>连接器调用日志</Typography.Title>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input allowClear placeholder="traceId" style={{ width: 220 }} value={traceId} onChange={(e) => setTraceId(e.target.value)} />
          <Select
            allowClear
            placeholder="Bot"
            style={{ width: 200 }}
            value={botId}
            onChange={setBotId}
            options={(botsQuery.data?.items ?? []).map((bot) => ({ value: bot.id, label: bot.name }))}
          />
          <Select
            allowClear
            placeholder="连接器"
            style={{ width: 180 }}
            value={connectorCode}
            onChange={setConnectorCode}
            options={(connectorsQuery.data?.items ?? []).map((item) => ({ value: item.code, label: item.name }))}
          />
          <Input allowClear placeholder="接口编码" style={{ width: 160 }} value={operationCode} onChange={(e) => setOperationCode(e.target.value)} />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={status}
            onChange={setStatus}
            options={[
              { value: 'SUCCESS', label: 'SUCCESS' },
              { value: 'FAILED', label: 'FAILED' }
            ]}
          />
          <Button type="primary" onClick={() => logsQuery.refetch()}>
            查询
          </Button>
        </Space>
      </Card>
      <Card>
        <Table rowKey="id" loading={logsQuery.isLoading} columns={columns} dataSource={logsQuery.data?.items ?? []} pagination={{ pageSize: 20 }} />
      </Card>
      <Drawer open={detail != null} title="调用详情" width={560} onClose={() => setDetail(null)}>
        {detail ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <div>时间：{detail.createdAt}</div>
            <div>连接器：{detail.connectorCode ?? '-'}</div>
            <div>接口：{detail.operationCode ?? '-'}</div>
            <div>状态：{detail.status ?? '-'}</div>
            <div>traceId：{detail.traceId ?? '-'}</div>
            <div>Bot：{detail.botId ?? '-'}</div>
            <div>会话：{detail.conversationId ?? '-'}</div>
            {detail.errorMessage ? <Typography.Text type="danger">错误：{detail.errorMessage}</Typography.Text> : null}
            <Typography.Text strong>请求摘要</Typography.Text>
            <Input.TextArea value={detail.requestSummary ?? ''} readOnly autoSize={{ minRows: 4, maxRows: 10 }} />
            <Typography.Text strong>响应摘要</Typography.Text>
            <Input.TextArea value={detail.responseSummary ?? ''} readOnly autoSize={{ minRows: 4, maxRows: 12 }} />
          </Space>
        ) : null}
      </Drawer>
    </section>
  );
}
