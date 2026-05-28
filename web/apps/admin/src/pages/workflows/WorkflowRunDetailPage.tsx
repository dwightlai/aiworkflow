import { useQuery } from '@tanstack/react-query';
import { Card, Descriptions, Space, Table, Tag, Typography } from 'antd';
import { getWorkflowRun, type NodeExecution, type WorkflowExecution } from '../../api/workflows';

export interface WorkflowRunDetailPageProps {
  executionId: string;
}

export function WorkflowRunDetailPage({ executionId }: WorkflowRunDetailPageProps) {
  const runQuery = useQuery({
    queryKey: ['workflow-run', executionId],
    queryFn: () => getWorkflowRun(executionId)
  });
  const execution = runQuery.data;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Typography.Title level={4} style={{ margin: 0 }}>执行详情</Typography.Title>
      <Card loading={runQuery.isLoading}>
        {execution ? <ExecutionSummary execution={execution} /> : null}
      </Card>
      <Card title="节点执行">
        <Table
          rowKey="id"
          loading={runQuery.isLoading}
          pagination={false}
          dataSource={execution?.nodeExecutions ?? []}
          columns={[
            { title: '节点', dataIndex: 'nodeId' },
            { title: '类型', dataIndex: 'nodeType', width: 140 },
            { title: '状态', dataIndex: 'status', width: 120, render: (status) => <NodeStatusTag status={status} /> },
            { title: '错误', dataIndex: 'errorMessage', render: (value) => value ?? '-' },
            { title: '输出', dataIndex: 'output', render: (value) => <pre style={preStyle}>{JSON.stringify(value, null, 2)}</pre> }
          ]}
        />
      </Card>
    </Space>
  );
}

function ExecutionSummary({ execution }: { execution: WorkflowExecution }) {
  return (
    <Descriptions column={1} bordered size="small">
      <Descriptions.Item label="执行 ID">
        <Typography.Text copyable>{execution.id}</Typography.Text>
      </Descriptions.Item>
      <Descriptions.Item label="工作流 ID">{execution.workflowId}</Descriptions.Item>
      <Descriptions.Item label="状态"><StatusTag status={execution.status} /></Descriptions.Item>
      <Descriptions.Item label="输入"><pre style={preStyle}>{JSON.stringify(execution.input, null, 2)}</pre></Descriptions.Item>
      <Descriptions.Item label="输出"><pre style={preStyle}>{JSON.stringify(execution.output, null, 2)}</pre></Descriptions.Item>
      <Descriptions.Item label="错误">{execution.errorMessage ?? '-'}</Descriptions.Item>
    </Descriptions>
  );
}

function StatusTag({ status }: { status: WorkflowExecution['status'] }) {
  const color = status === 'SUCCEEDED' ? 'green' : status === 'FAILED' ? 'red' : 'blue';
  return <Tag color={color}>{status}</Tag>;
}

function NodeStatusTag({ status }: { status: NodeExecution['status'] }) {
  const color = status === 'SUCCEEDED' ? 'green' : status === 'FAILED' ? 'red' : status === 'SKIPPED' ? 'default' : 'blue';
  return <Tag color={color}>{status}</Tag>;
}

const preStyle = {
  background: '#f7f8fa',
  border: '1px solid #e4e8f0',
  borderRadius: 6,
  margin: 0,
  maxHeight: 180,
  overflow: 'auto',
  padding: 10
};
