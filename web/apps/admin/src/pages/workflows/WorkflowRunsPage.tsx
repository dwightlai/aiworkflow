import { CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Input, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import type React from 'react';
import { useMemo, useState } from 'react';
import { listWorkflowRuns, type WorkflowExecution } from '../../api/workflows';

export function WorkflowRunsPage() {
  const [keyword, setKeyword] = useState('');
  const runQuery = useQuery({
    queryKey: ['workflow-runs'],
    queryFn: listWorkflowRuns
  });
  const runs = runQuery.data?.items ?? [];
  const visibleRuns = useMemo(() => {
    const trimmed = keyword.trim().toLowerCase();
    if (!trimmed) {
      return runs;
    }
    return runs.filter((run) => run.id.toLowerCase().includes(trimmed) || run.workflowId.toLowerCase().includes(trimmed));
  }, [keyword, runs]);
  const succeededRuns = runs.filter((run) => run.status === 'SUCCEEDED').length;
  const failedRuns = runs.filter((run) => run.status === 'FAILED').length;
  const successRate = runs.length ? Math.round((succeededRuns / runs.length) * 100) : 0;
  const averageDuration = calculateAverageDuration(runs);

  const columns: TableColumnsType<WorkflowExecution> = [
    {
      title: '执行 ID',
      dataIndex: 'id',
      width: 220,
      render: (value: string) => <Typography.Text copyable>{value}</Typography.Text>
    },
    {
      title: '工作流 ID',
      dataIndex: 'workflowId',
      width: 220,
      render: (value: string) => <Typography.Text type="secondary">{value}</Typography.Text>
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (status: WorkflowExecution['status']) => <StatusTag status={status} />
    },
    {
      title: '开始时间',
      dataIndex: 'startedAt',
      width: 190,
      render: (value: string) => new Date(value).toLocaleString()
    },
    {
      title: '耗时',
      width: 110,
      render: (_, record) => formatDuration(record.startedAt, record.finishedAt)
    },
    {
      title: '节点数',
      width: 90,
      render: (_, record) => record.nodeExecutions.length
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Button aria-label="详情" type="link" onClick={() => navigateTo(`/workflow-runs/${record.id}`)}>
          详情
        </Button>
      )
    }
  ];

  return (
    <div style={pageStyle}>
      <section style={heroStyle}>
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>AI 功能 / 工作流 / 运行历史</Typography.Text>
          <Typography.Title level={3} style={{ margin: '4px 0 6px' }}>运行监控台</Typography.Title>
          <Typography.Text type="secondary">运行历史：查看工作流执行状态、输入输出与节点执行过程</Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} loading={runQuery.isFetching} onClick={() => runQuery.refetch()}>
          刷新
        </Button>
      </section>

      <section style={metricGridStyle}>
        <MetricCard title="成功率" value={`${successRate}%`} detail={`${succeededRuns}/${runs.length} 次成功`} icon={<CheckCircleOutlined />} tone="green" />
        <MetricCard title="平均耗时" value={averageDuration} detail="仅统计已完成运行" icon={<ClockCircleOutlined />} tone="blue" />
        <MetricCard title="失败运行" value={String(failedRuns)} detail="需要排查输入或节点配置" icon={<CloseCircleOutlined />} tone="red" />
        <MetricCard title="执行记录" value={String(runs.length)} detail="当前保留的运行实例" icon={<SearchOutlined />} tone="slate" />
      </section>

      <Card styles={{ body: { padding: 16 } }} style={tableCardStyle}>
        <Space align="center" style={{ justifyContent: 'space-between', width: '100%', marginBottom: 14 }}>
          <Typography.Title level={5} style={{ margin: 0 }}>执行记录</Typography.Title>
          <Tag>运行历史</Tag>
        </Space>
        <Space size={10} style={{ marginBottom: 16 }} wrap>
          <Typography.Text strong>搜索：</Typography.Text>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="请输入执行 ID 或工作流 ID"
            style={{ width: 320 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
        </Space>
        <Table
          rowKey="id"
          loading={runQuery.isLoading}
          columns={columns}
          dataSource={visibleRuns}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>
    </div>
  );
}

function MetricCard({
  title,
  value,
  detail,
  icon,
  tone
}: {
  title: string;
  value: string;
  detail: string;
  icon: React.ReactNode;
  tone: 'green' | 'blue' | 'red' | 'slate';
}) {
  return (
    <Card styles={{ body: { padding: 16 } }} style={metricCardStyle}>
      <Space align="start" size={12}>
        <div style={metricIconStyle(tone)}>{icon}</div>
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{title}</Typography.Text>
          <Typography.Title level={4} style={{ margin: '2px 0' }}>{value}</Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{detail}</Typography.Text>
        </div>
      </Space>
    </Card>
  );
}

function StatusTag({ status }: { status: WorkflowExecution['status'] }) {
  const color = status === 'SUCCEEDED' ? 'green' : status === 'FAILED' ? 'red' : 'blue';
  return <Tag color={color}>{status}</Tag>;
}

function calculateAverageDuration(runs: WorkflowExecution[]) {
  const durations = runs
    .map((run) => durationMs(run.startedAt, run.finishedAt))
    .filter((value): value is number => typeof value === 'number');
  if (!durations.length) {
    return '-';
  }
  const average = Math.round(durations.reduce((sum, value) => sum + value, 0) / durations.length);
  return `${average}ms`;
}

function formatDuration(startedAt: string, finishedAt: string | null) {
  const ms = durationMs(startedAt, finishedAt);
  return typeof ms === 'number' ? `${ms}ms` : '-';
}

function durationMs(startedAt: string, finishedAt: string | null) {
  if (!finishedAt) {
    return null;
  }
  return Math.max(new Date(finishedAt).getTime() - new Date(startedAt).getTime(), 0);
}

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

const pageStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 16
};

const heroStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  borderBottom: '1px solid #edf0f5',
  display: 'flex',
  justifyContent: 'space-between',
  margin: '-16px -24px 0',
  padding: '20px 24px'
};

const metricGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 14,
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))'
};

const metricCardStyle: React.CSSProperties = {
  borderRadius: 8
};

function metricIconStyle(tone: 'green' | 'blue' | 'red' | 'slate'): React.CSSProperties {
  const colorMap = {
    green: ['#ecfdf3', '#16a34a'],
    blue: ['#eef6ff', '#1677ff'],
    red: ['#fef2f2', '#dc2626'],
    slate: ['#f1f5f9', '#475569']
  } satisfies Record<typeof tone, [string, string]>;
  const [background, color] = colorMap[tone];
  return {
    alignItems: 'center',
    background,
    borderRadius: 8,
    color,
    display: 'flex',
    fontSize: 18,
    height: 36,
    justifyContent: 'center',
    width: 36
  };
}

const tableCardStyle: React.CSSProperties = {
  borderRadius: 8
};
