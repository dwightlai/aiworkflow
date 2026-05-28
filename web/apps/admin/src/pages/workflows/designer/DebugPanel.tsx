import type { WorkflowExecution } from '../../../api/workflows';
import { Alert, Button, Input, Space, Typography } from 'antd';
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
      <Space align="start" size={16} style={{ width: '100%' }}>
        <div style={{ flex: '0 0 360px' }}>
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%', marginBottom: 8 }}>
            <Typography.Title level={5} style={{ margin: 0 }}>运行调试</Typography.Title>
            <Button type="primary" size="small" loading={loading} onClick={onRun}>运行</Button>
          </Space>
          <Input.TextArea
            value={input}
            onChange={(event) => onInputChange(event.target.value)}
            autoSize={{ minRows: 5, maxRows: 5 }}
          />
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          {execution ? (
            <pre style={preStyle}>{JSON.stringify(execution.output, null, 2)}</pre>
          ) : (
            <Alert type="info" showIcon message="运行后将在这里展示输出和节点执行结果" />
          )}
        </div>
      </Space>
    </section>
  );
}

const panelStyle: React.CSSProperties = {
  background: '#fff',
  borderTop: '1px solid #e8edf5',
  padding: 14
};

const preStyle: React.CSSProperties = {
  background: '#0f172a',
  borderRadius: 6,
  color: '#e2e8f0',
  margin: 0,
  maxHeight: 146,
  overflow: 'auto',
  padding: 12
};
