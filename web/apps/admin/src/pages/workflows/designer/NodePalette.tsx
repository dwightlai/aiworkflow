import { BranchesOutlined, CommentOutlined, PlayCircleOutlined, RobotOutlined, StopOutlined } from '@ant-design/icons';
import type { WorkflowNode, WorkflowNodeType } from '@aiworkflow/workflow-schema';
import { Button, Space, Typography } from 'antd';
import type React from 'react';

export interface NodePaletteProps {
  onAddNode: (node: WorkflowNode) => void;
}

const nodeTemplates: Array<{
  type: WorkflowNodeType;
  name: string;
  icon: React.ReactNode;
  config: Record<string, unknown>;
}> = [
  { type: 'START', name: '开始', icon: <PlayCircleOutlined />, config: {} },
  { type: 'PROMPT', name: 'Prompt', icon: <CommentOutlined />, config: { template: 'Hello {{name}}', outputKey: 'prompt' } },
  { type: 'LLM', name: 'LLM', icon: <RobotOutlined />, config: { providerId: 'default', model: 'mock', promptKey: 'prompt', outputKey: 'answer' } },
  { type: 'CONDITION', name: '条件', icon: <BranchesOutlined />, config: { contextKey: 'answer', operator: 'CONTAINS', compareValue: '', trueTargetNodeId: '', falseTargetNodeId: '' } },
  { type: 'END', name: '结束', icon: <StopOutlined />, config: { outputKeys: ['answer'] } }
];

export function NodePalette({ onAddNode }: NodePaletteProps) {
  return (
    <aside style={panelStyle}>
      <Typography.Title level={5} style={titleStyle}>节点库</Typography.Title>
      <Space direction="vertical" size={8} style={{ width: '100%' }}>
        {nodeTemplates.map((template) => (
          <Button
            key={template.type}
            block
            icon={template.icon}
            style={nodeButtonStyle}
            onClick={() => onAddNode(createNode(template))}
          >
            {template.type}
          </Button>
        ))}
      </Space>
    </aside>
  );
}

function createNode(template: typeof nodeTemplates[number]): WorkflowNode {
  const suffix = Date.now().toString(36);
  return {
    id: `${template.type.toLowerCase()}_${suffix}`,
    type: template.type,
    name: template.name,
    config: { ...template.config }
  };
}

const panelStyle: React.CSSProperties = {
  background: '#fff',
  borderRight: '1px solid #e8edf5',
  padding: 16,
  width: 212
};

const titleStyle: React.CSSProperties = {
  marginBottom: 14
};

const nodeButtonStyle: React.CSSProperties = {
  justifyContent: 'flex-start',
  height: 38
};
