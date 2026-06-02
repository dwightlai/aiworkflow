import { BugOutlined, CheckCircleOutlined, PlayCircleOutlined } from '@ant-design/icons';
import type { WorkflowExecution } from '../../../api/workflows';
import { Alert, Button, Input, Space, Tag, Timeline, Typography } from 'antd';
import type React from 'react';

export interface DebugPanelProps {
  input: string;
  loading?: boolean;
  execution: WorkflowExecution | null;
  onInputChange: (input: string) => void;
  onRun: () => void;
}

export function DebugPanel({ input, loading, execution, onInputChange, onRun }: DebugPanelProps) {
  return (
    <section style={panelStyle}>
      <div style={headerStyle}>
        <Space size={10}>
          <div style={iconStyle}><BugOutlined /></div>
          <div>
            <Typography.Title level={5} style={{ margin: 0 }}>调试控制台</Typography.Title>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>运行调试 · 输入样例、执行结果和节点轨迹</Typography.Text>
          </div>
        </Space>
        <Button type="primary" icon={<PlayCircleOutlined />} loading={loading} onClick={onRun}>
          运行调试
        </Button>
      </div>

      <div style={contentStyle}>
        <div style={inputColumnStyle}>
          <Typography.Text strong>请求输入 JSON</Typography.Text>
          <Input.TextArea
            value={input}
            onChange={(event) => onInputChange(event.target.value)}
            autoSize={{ minRows: 6, maxRows: 6 }}
            style={textareaStyle}
          />
        </div>
        <div style={outputColumnStyle}>
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%', marginBottom: 8 }}>
            <Typography.Text strong>执行输出</Typography.Text>
            {execution ? <StatusTag status={execution.status} /> : <Tag>未运行</Tag>}
          </Space>
          {execution ? (
            <pre style={preStyle}>{JSON.stringify(execution.output, null, 2)}</pre>
          ) : (
            <Alert type="info" showIcon message="保存并运行后，这里会展示最终输出。" />
          )}
        </div>
        <div style={traceColumnStyle}>
          <Typography.Text strong>节点轨迹</Typography.Text>
          {execution?.nodeExecutions?.length ? (
            <Timeline
              style={{ marginTop: 12 }}
              items={execution.nodeExecutions.map((node) => ({
                dot: <CheckCircleOutlined />,
                color: node.status === 'FAILED' ? 'red' : node.status === 'SUCCEEDED' ? 'green' : 'blue',
                children: (
                  <Space direction="vertical" size={0}>
                    <Typography.Text>{node.nodeId}</Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>{node.nodeType} · {node.status}</Typography.Text>
                  </Space>
                )
              }))}
            />
          ) : (
            <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
              运行后按节点展示耗时、状态和错误信息。
            </Typography.Paragraph>
          )}
        </div>
      </div>
    </section>
  );
}

function StatusTag({ status }: { status: WorkflowExecution['status'] }) {
  const color = status === 'SUCCEEDED' ? 'green' : status === 'FAILED' ? 'red' : 'blue';
  const label = status === 'SUCCEEDED' ? '成功' : status === 'FAILED' ? '失败' : '运行中';
  return <Tag color={color}>{label}</Tag>;
}

const panelStyle: React.CSSProperties = {
  background: '#fff',
  borderTop: '1px solid #e7ecf3',
  flex: '0 0 250px',
  padding: '14px 16px 16px'
};

const headerStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  justifyContent: 'space-between',
  marginBottom: 12
};

const iconStyle: React.CSSProperties = {
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

const contentStyle: React.CSSProperties = {
  display: 'grid',
  gap: 14,
  gridTemplateColumns: '360px minmax(260px, 1fr) 300px'
};

const inputColumnStyle: React.CSSProperties = {
  minWidth: 0
};

const outputColumnStyle: React.CSSProperties = {
  minWidth: 0
};

const traceColumnStyle: React.CSSProperties = {
  borderLeft: '1px solid #eef2f7',
  minWidth: 0,
  paddingLeft: 14
};

const textareaStyle: React.CSSProperties = {
  fontFamily: 'Consolas, monospace',
  marginTop: 8
};

const preStyle: React.CSSProperties = {
  background: '#101828',
  borderRadius: 8,
  color: '#e2e8f0',
  fontFamily: 'Consolas, monospace',
  margin: 0,
  maxHeight: 152,
  overflow: 'auto',
  padding: 12
};
