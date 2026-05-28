import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Input, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
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

  const columns: TableColumnsType<WorkflowExecution> = [
    {
      title: '执行 ID',
      dataIndex: 'id',
      width: 240,
      render: (value: string) => <Typography.Text copyable>{value}</Typography.Text>
    },
    {
      title: '工作流 ID',
      dataIndex: 'workflowId',
      width: 240,
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
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Button type="link" onClick={() => navigateTo(`/workflow-runs/${record.id}`)}>
          详情
        </Button>
      )
    }
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>运行历史</Typography.Title>
          <Typography.Text type="secondary">查看工作流执行状态、输入输出与节点执行过程</Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} loading={runQuery.isFetching} onClick={() => runQuery.refetch()}>
          刷新
        </Button>
      </Space>

      <Card styles={{ body: { padding: 16 } }}>
        <Space size={10} style={{ marginBottom: 16 }}>
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
    </Space>
  );
}

function StatusTag({ status }: { status: WorkflowExecution['status'] }) {
  const color = status === 'SUCCEEDED' ? 'green' : status === 'FAILED' ? 'red' : 'blue';
  return <Tag color={color}>{status}</Tag>;
}

function formatDuration(startedAt: string, finishedAt: string | null) {
  if (!finishedAt) {
    return '-';
  }
  const ms = new Date(finishedAt).getTime() - new Date(startedAt).getTime();
  return `${Math.max(ms, 0)}ms`;
}

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}
