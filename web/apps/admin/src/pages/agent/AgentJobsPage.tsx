import { useQuery } from '@tanstack/react-query';
import { Button, Card, Drawer, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { getAgentJob, listAgentJobs, type AgentJob } from '../../api/agentJobs';
import { listBots } from '../../api/bots';

const STATUS_OPTIONS = ['PENDING', 'RUNNING', 'COMPLETED', 'SUCCEEDED', 'FAILED', 'CANCELLED'];

export function AgentJobsPage() {
  const [botId, setBotId] = useState<string | undefined>();
  const [status, setStatus] = useState<string | undefined>();
  const [conversationId, setConversationId] = useState('');
  const [detailId, setDetailId] = useState<string | null>(null);
  const botsQuery = useQuery({ queryKey: ['bots'], queryFn: listBots });
  const jobsQuery = useQuery({
    queryKey: ['agent-jobs', botId, status, conversationId],
    queryFn: () =>
      listAgentJobs({
        botId,
        status,
        conversationId: conversationId || undefined,
        limit: 100
      })
  });
  const detailQuery = useQuery({
    queryKey: ['agent-job', detailId],
    queryFn: () => getAgentJob(detailId!),
    enabled: Boolean(detailId)
  });

  const columns: ColumnsType<AgentJob> = [
    { title: '任务 ID', dataIndex: 'id', width: 200, ellipsis: true },
    { title: '类型', dataIndex: 'jobType', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value: string) => (
        <Tag color={value === 'FAILED' ? 'red' : value === 'COMPLETED' || value === 'SUCCEEDED' ? 'green' : 'blue'}>{value}</Tag>
      )
    },
    { title: '进度', dataIndex: 'progress', width: 80, render: (value: number | null) => (value == null ? '-' : `${value}%`) },
    { title: '当前步骤', dataIndex: 'currentStep', ellipsis: true },
    { title: '来源任务', dataIndex: 'sourceJobId', width: 180, ellipsis: true },
    { title: 'Bot', dataIndex: 'botId', width: 140, ellipsis: true },
    { title: '更新时间', dataIndex: 'updatedAt', width: 180 },
    {
      title: '操作',
      key: 'actions',
      width: 80,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => setDetailId(record.id)}>
          详情
        </Button>
      )
    }
  ];

  const detail = detailQuery.data;

  return (
    <section style={{ padding: 24 }}>
      <Typography.Title level={3}>异步任务</Typography.Title>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
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
            placeholder="状态"
            style={{ width: 160 }}
            value={status}
            onChange={setStatus}
            options={STATUS_OPTIONS.map((item) => ({ value: item, label: item }))}
          />
          <Input allowClear placeholder="会话 ID" style={{ width: 220 }} value={conversationId} onChange={(e) => setConversationId(e.target.value)} />
          <Button type="primary" onClick={() => jobsQuery.refetch()}>
            查询
          </Button>
        </Space>
      </Card>
      <Card>
        <Table rowKey="id" loading={jobsQuery.isLoading} columns={columns} dataSource={jobsQuery.data?.items ?? []} pagination={{ pageSize: 20 }} />
      </Card>
      <Drawer open={detailId != null} title="任务详情" width={560} onClose={() => setDetailId(null)}>
        {detail ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <div>ID：{detail.id}</div>
            <div>来源任务：{detail.sourceJobId ?? '-'}</div>
            <div>类型：{detail.jobType}</div>
            <div>状态：{detail.status}</div>
            <div>进度：{detail.progress ?? 0}%</div>
            <div>步骤：{detail.currentStep ?? '-'}</div>
            <div>Bot：{detail.botId ?? '-'}</div>
            <div>会话：{detail.conversationId ?? '-'}</div>
            <div>创建：{detail.createdAt}</div>
            <div>更新：{detail.updatedAt}</div>
            {detail.errorMessage ? <Typography.Text type="danger">错误：{detail.errorMessage}</Typography.Text> : null}
            {detail.result ? (
              <>
                <Typography.Text strong>结果</Typography.Text>
                <Input.TextArea value={detail.result} readOnly autoSize={{ minRows: 4, maxRows: 12 }} />
              </>
            ) : null}
          </Space>
        ) : null}
      </Drawer>
    </section>
  );
}
