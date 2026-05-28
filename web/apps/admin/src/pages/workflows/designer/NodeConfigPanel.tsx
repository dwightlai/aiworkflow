import type { WorkflowNode } from '@aiworkflow/workflow-schema';
import { Empty, Form, Input, InputNumber, Select, Space, Typography } from 'antd';
import type React from 'react';

export interface NodeConfigPanelProps {
  node: WorkflowNode | null;
  onChange: (nodeId: string, patch: Partial<WorkflowNode>) => void;
}

export function NodeConfigPanel({ node, onChange }: NodeConfigPanelProps) {
  if (!node) {
    return (
      <aside style={panelStyle}>
        <Typography.Title level={5} style={titleStyle}>节点配置</Typography.Title>
        <Empty description="请选择一个节点" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </aside>
    );
  }

  return (
    <aside style={panelStyle}>
      <Typography.Title level={5} style={titleStyle}>节点配置</Typography.Title>
      <Form layout="vertical" size="small">
        <Form.Item label="节点名称">
          <Input value={node.name} onChange={(event) => onChange(node.id, { name: event.target.value })} />
        </Form.Item>
        <Form.Item label="节点类型">
          <Input value={node.type} readOnly />
        </Form.Item>
        <ConfigFields node={node} onChange={onChange} />
      </Form>
    </aside>
  );
}

function ConfigFields({ node, onChange }: NodeConfigPanelProps & { node: WorkflowNode }) {
  const config = node.config;
  const setConfig = (patch: Record<string, unknown>) => onChange(node.id, { config: { ...config, ...patch } });

  if (node.type === 'PROMPT') {
    return (
      <>
        <Form.Item label="Prompt 模板">
          <Input.TextArea
            autoSize={{ minRows: 5, maxRows: 9 }}
            value={String(config.template ?? '')}
            onChange={(event) => setConfig({ template: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? '')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'LLM') {
    return (
      <>
        <Form.Item label="模型供应商">
          <Input value={String(config.providerId ?? '')} onChange={(event) => setConfig({ providerId: event.target.value })} />
        </Form.Item>
        <Form.Item label="模型">
          <Input value={String(config.model ?? '')} onChange={(event) => setConfig({ model: event.target.value })} />
        </Form.Item>
        <Form.Item label="Prompt 变量">
          <Input value={String(config.promptKey ?? '')} onChange={(event) => setConfig({ promptKey: event.target.value })} />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? '')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
        <Space size={8}>
          <Form.Item label="温度">
            <InputNumber min={0} max={2} step={0.1} value={Number(config.temperature ?? 0.7)} onChange={(value) => setConfig({ temperature: value ?? 0 })} />
          </Form.Item>
          <Form.Item label="最大 Token">
            <InputNumber min={1} max={32000} value={Number(config.maxTokens ?? 1024)} onChange={(value) => setConfig({ maxTokens: value ?? 1024 })} />
          </Form.Item>
        </Space>
      </>
    );
  }

  if (node.type === 'CONDITION') {
    return (
      <>
        <Form.Item label="判断变量">
          <Input value={String(config.contextKey ?? '')} onChange={(event) => setConfig({ contextKey: event.target.value })} />
        </Form.Item>
        <Form.Item label="操作符">
          <Select
            value={String(config.operator ?? 'EQUALS')}
            options={['EQUALS', 'NOT_EQUALS', 'CONTAINS', 'IS_EMPTY', 'IS_NOT_EMPTY'].map((value) => ({ value, label: value }))}
            onChange={(value) => setConfig({ operator: value })}
          />
        </Form.Item>
        <Form.Item label="比较值">
          <Input value={String(config.compareValue ?? '')} onChange={(event) => setConfig({ compareValue: event.target.value })} />
        </Form.Item>
        <Form.Item label="True 目标节点">
          <Input value={String(config.trueTargetNodeId ?? '')} onChange={(event) => setConfig({ trueTargetNodeId: event.target.value })} />
        </Form.Item>
        <Form.Item label="False 目标节点">
          <Input value={String(config.falseTargetNodeId ?? '')} onChange={(event) => setConfig({ falseTargetNodeId: event.target.value })} />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'TEXT_TRANSFORM') {
    return (
      <>
        <Form.Item label="文本模板">
          <Input.TextArea
            autoSize={{ minRows: 5, maxRows: 9 }}
            value={String(config.template ?? '')}
            onChange={(event) => setConfig({ template: event.target.value })}
          />
        </Form.Item>
        <Form.Item label="输出变量">
          <Input value={String(config.outputKey ?? '')} onChange={(event) => setConfig({ outputKey: event.target.value })} />
        </Form.Item>
      </>
    );
  }

  if (node.type === 'END') {
    return (
      <Form.Item label="输出变量列表">
        <Input
          value={Array.isArray(config.outputKeys) ? config.outputKeys.join(',') : ''}
          onChange={(event) => setConfig({ outputKeys: event.target.value.split(',').map((item) => item.trim()).filter(Boolean) })}
        />
      </Form.Item>
    );
  }

  return <Typography.Text type="secondary">该节点暂无额外配置。</Typography.Text>;
}

const panelStyle: React.CSSProperties = {
  background: '#fff',
  borderLeft: '1px solid #e8edf5',
  padding: 16,
  width: 300
};

const titleStyle: React.CSSProperties = {
  marginBottom: 14
};
