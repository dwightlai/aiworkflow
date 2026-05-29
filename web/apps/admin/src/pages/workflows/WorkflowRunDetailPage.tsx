import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CodeOutlined,
  DeploymentUnitOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Alert, Card, Col, Row, Space, Tag, Timeline, Typography } from 'antd';
import type React from 'react';
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
    <div style={pageStyle}>
      <section style={heroStyle}>
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>AI 功能 / 运行历史 / 执行详情</Typography.Text>
          <Typography.Title level={3} style={{ margin: '4px 0 6px' }}>执行详情</Typography.Title>
          <Typography.Text type="secondary">定位一次运行的输入、输出、耗时、节点状态和错误原因。</Typography.Text>
        </div>
        {execution ? <StatusTag status={execution.status} /> : <Tag>加载中</Tag>}
      </section>

      <Card loading={runQuery.isLoading} styles={{ body: { padding: 0 } }} style={cardStyle}>
        {execution ? <ExecutionOverview execution={execution} /> : null}
      </Card>

      {execution ? (
        <section style={contentGridStyle}>
          <div style={{ minWidth: 0 }}>
            <Card title="输入 / 输出" style={cardStyle}>
              <Row gutter={14}>
                <Col span={12}>
                  <Typography.Text strong>输入</Typography.Text>
                  <pre style={preStyle}>{JSON.stringify(execution.input, null, 2)}</pre>
                </Col>
                <Col span={12}>
                  <Typography.Text strong>输出</Typography.Text>
                  <pre style={preStyle}>{JSON.stringify(execution.output, null, 2)}</pre>
                </Col>
              </Row>
            </Card>

            <Card title="节点时间线" style={{ ...cardStyle, marginTop: 16 }}>
              <Timeline
                items={execution.nodeExecutions.map((node) => ({
                  color: statusColor(node.status),
                  dot: node.status === 'FAILED' ? <CloseCircleOutlined /> : node.status === 'SUCCEEDED' ? <CheckCircleOutlined /> : <ClockCircleOutlined />,
                  children: <NodeTimelineItem node={node} />
                }))}
              />
            </Card>
          </div>

          <aside style={sideColumnStyle}>
            <Card title="运行诊断" style={cardStyle}>
              {execution.errorMessage ? (
                <Alert
                  type="error"
                  showIcon
                  message={execution.errorMessage}
                  description="建议检查失败节点配置、模型供应商状态、请求输入和超时参数。"
                />
              ) : (
                <Alert type="success" showIcon message="本次运行未发现错误" />
              )}
              <div style={diagnosisListStyle}>
                <DiagnosisItem label="失败节点" value={String(execution.nodeExecutions.filter((node) => node.status === 'FAILED').length)} />
                <DiagnosisItem label="总节点" value={String(execution.nodeExecutions.length)} />
                <DiagnosisItem label="耗时" value={formatDuration(execution.startedAt, execution.finishedAt)} />
              </div>
            </Card>
          </aside>
        </section>
      ) : null}
    </div>
  );
}

function ExecutionOverview({ execution }: { execution: WorkflowExecution }) {
  const metrics = [
    { label: '执行 ID', value: execution.id, icon: <CodeOutlined /> },
    { label: '工作流 ID', value: execution.workflowId, icon: <DeploymentUnitOutlined /> },
    { label: '耗时', value: formatDuration(execution.startedAt, execution.finishedAt), icon: <ClockCircleOutlined /> },
    { label: '节点数', value: `${execution.nodeExecutions.length}`, icon: <CheckCircleOutlined /> }
  ];

  return (
    <div style={overviewGridStyle}>
      {metrics.map((metric) => (
        <div key={metric.label} style={overviewItemStyle}>
          <div style={overviewIconStyle}>{metric.icon}</div>
          <div style={{ minWidth: 0 }}>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>{metric.label}</Typography.Text>
            <Typography.Paragraph copyable={metric.label.endsWith('ID')} ellipsis style={overviewValueStyle}>
              {metric.value}
            </Typography.Paragraph>
          </div>
        </div>
      ))}
    </div>
  );
}

function NodeTimelineItem({ node }: { node: NodeExecution }) {
  return (
    <div style={timelineItemStyle}>
      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
        <Space>
          <Typography.Text strong>{node.nodeId}</Typography.Text>
          <Tag>{node.nodeType}</Tag>
          <NodeStatusTag status={node.status} />
        </Space>
        <Typography.Text type="secondary">{formatDuration(node.startedAt, node.finishedAt)}</Typography.Text>
      </Space>
      {node.errorMessage ? (
        <Typography.Text type="danger" style={{ display: 'block', marginTop: 6 }}>
          <ExclamationCircleOutlined /> {node.errorMessage}
        </Typography.Text>
      ) : null}
      <Row gutter={12} style={{ marginTop: 10 }}>
        <Col span={12}>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>输入</Typography.Text>
          <pre style={smallPreStyle}>{JSON.stringify(node.input, null, 2)}</pre>
        </Col>
        <Col span={12}>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>输出</Typography.Text>
          <pre style={smallPreStyle}>{JSON.stringify(node.output, null, 2)}</pre>
        </Col>
      </Row>
    </div>
  );
}

function DiagnosisItem({ label, value }: { label: string; value: string }) {
  return (
    <div style={diagnosisItemStyle}>
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>{label}</Typography.Text>
      <Typography.Text strong>{value}</Typography.Text>
    </div>
  );
}

function StatusTag({ status }: { status: WorkflowExecution['status'] }) {
  const color = status === 'SUCCEEDED' ? 'green' : status === 'FAILED' ? 'red' : 'blue';
  return <Tag color={color}>{status}</Tag>;
}

function NodeStatusTag({ status }: { status: NodeExecution['status'] }) {
  return <Tag color={statusColor(status)}>{status}</Tag>;
}

function statusColor(status: WorkflowExecution['status'] | NodeExecution['status']) {
  if (status === 'SUCCEEDED') {
    return 'green';
  }
  if (status === 'FAILED') {
    return 'red';
  }
  if (status === 'SKIPPED') {
    return 'default';
  }
  return 'blue';
}

function formatDuration(startedAt: string, finishedAt: string | null) {
  if (!finishedAt) {
    return '-';
  }
  const ms = Math.max(new Date(finishedAt).getTime() - new Date(startedAt).getTime(), 0);
  return `${ms}ms`;
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

const cardStyle: React.CSSProperties = {
  borderRadius: 8
};

const overviewGridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))'
};

const overviewItemStyle: React.CSSProperties = {
  alignItems: 'center',
  borderRight: '1px solid #edf0f5',
  display: 'flex',
  gap: 12,
  minWidth: 0,
  padding: 16
};

const overviewIconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#eef6ff',
  borderRadius: 8,
  color: '#1677ff',
  display: 'flex',
  fontSize: 18,
  height: 36,
  justifyContent: 'center',
  width: 36
};

const overviewValueStyle: React.CSSProperties = {
  fontWeight: 700,
  margin: 0
};

const contentGridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
  gridTemplateColumns: 'minmax(0, 1fr) 330px'
};

const sideColumnStyle: React.CSSProperties = {
  minWidth: 0
};

const timelineItemStyle: React.CSSProperties = {
  background: '#fbfdff',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  padding: 12
};

const diagnosisListStyle: React.CSSProperties = {
  display: 'grid',
  gap: 10,
  marginTop: 14
};

const diagnosisItemStyle: React.CSSProperties = {
  background: '#f8fafc',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  display: 'flex',
  justifyContent: 'space-between',
  padding: '10px 12px'
};

const preStyle: React.CSSProperties = {
  background: '#101828',
  borderRadius: 8,
  color: '#e2e8f0',
  margin: '8px 0 0',
  maxHeight: 260,
  overflow: 'auto',
  padding: 12
};

const smallPreStyle: React.CSSProperties = {
  background: '#f7f8fa',
  border: '1px solid #e4e8f0',
  borderRadius: 6,
  margin: '4px 0 0',
  maxHeight: 120,
  overflow: 'auto',
  padding: 8
};
