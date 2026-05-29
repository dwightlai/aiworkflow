import {
  BranchesOutlined,
  CommentOutlined,
  PlayCircleOutlined,
  RobotOutlined,
  StopOutlined,
  ToolOutlined
} from '@ant-design/icons';
import type { WorkflowNode, WorkflowNodeType } from '@aiworkflow/workflow-schema';
import { Button, Tag, Typography } from 'antd';
import type React from 'react';

export interface NodePaletteProps {
  onAddNode: (node: WorkflowNode) => void;
}

export const WORKFLOW_NODE_TEMPLATE_MIME = 'application/x-aiworkflow-node-template';

export interface NodeTemplate {
  type: WorkflowNodeType;
  name: string;
  description: string;
  icon: React.ReactNode;
  config: Record<string, unknown>;
}

const nodeTemplates: NodeTemplate[] = [
  { type: 'START', name: '开始', description: '接收输入并初始化上下文', icon: <PlayCircleOutlined />, config: {} },
  { type: 'PROMPT', name: 'Prompt 模板', description: '把变量渲染成模型提示词', icon: <CommentOutlined />, config: { template: 'Hello {{name}}', outputKey: 'prompt' } },
  { type: 'LLM', name: '大模型调用', description: '调用模型并写入输出变量', icon: <RobotOutlined />, config: { providerId: 'default', model: 'mock', promptKey: 'prompt', outputKey: 'answer' } },
  { type: 'CONDITION', name: '条件分支', description: '根据上下文选择后续节点', icon: <BranchesOutlined />, config: { contextKey: 'answer', operator: 'CONTAINS', compareValue: '', trueTargetNodeId: '', falseTargetNodeId: '' } },
  { type: 'HTTP_TOOL', name: 'HTTP 工具', description: '调用外部业务系统接口', icon: <ToolOutlined />, config: { method: 'POST', url: '', outputKey: 'toolResult' } },
  { type: 'END', name: '结束', description: '整理最终输出返回调用方', icon: <StopOutlined />, config: { outputKeys: ['answer'] } }
];

export function NodePalette({ onAddNode }: NodePaletteProps) {
  return (
    <aside style={panelStyle}>
      <Typography.Text type="secondary" style={eyebrowStyle}>AI 节点资产</Typography.Text>
      <Typography.Title level={5} style={titleStyle}>节点库</Typography.Title>
      <div style={nodeListStyle}>
        {nodeTemplates.map((template) => (
          <button
            key={template.type}
            draggable
            type="button"
            style={nodeButtonStyle}
            onDragStart={(event) => {
              event.dataTransfer.effectAllowed = 'copy';
              event.dataTransfer.setData(WORKFLOW_NODE_TEMPLATE_MIME, serializeTemplate(template));
            }}
            onClick={() => onAddNode(createNode(template))}
          >
            <span style={iconStyle}>{template.icon}</span>
            <span style={{ minWidth: 0 }}>
              <span style={nodeNameStyle}>{template.name}</span>
              <span style={nodeDescriptionStyle}>{template.description}</span>
              <Tag color="blue" style={{ marginTop: 8 }}>{template.type}</Tag>
            </span>
          </button>
        ))}
      </div>
      <Button block type="dashed" style={{ marginTop: 14 }}>
        导入节点模板
      </Button>
    </aside>
  );
}

export function createNode(template: NodeTemplate, position?: { x: number; y: number }): WorkflowNode {
  const suffix = Date.now().toString(36);
  return {
    id: `${template.type.toLowerCase()}_${suffix}`,
    type: template.type,
    name: template.name,
    config: {
      ...template.config,
      ...(position ? { ui: { position } } : {})
    }
  };
}

export function parseNodeTemplate(value: string): NodeTemplate | null {
  try {
    const template = JSON.parse(value) as Omit<NodeTemplate, 'icon'>;
    if (!template.type || !template.name || !template.config) {
      return null;
    }
    return { ...template, icon: null };
  } catch {
    return null;
  }
}

function serializeTemplate(template: NodeTemplate) {
  const { icon: _icon, ...serializable } = template;
  return JSON.stringify(serializable);
}

const panelStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  flex: '0 0 260px',
  overflow: 'auto',
  padding: 16
};

const eyebrowStyle: React.CSSProperties = {
  display: 'block',
  fontSize: 12,
  marginBottom: 2
};

const titleStyle: React.CSSProperties = {
  marginBottom: 14,
  marginTop: 0
};

const nodeListStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 10
};

const nodeButtonStyle: React.CSSProperties = {
  alignItems: 'flex-start',
  background: '#fbfdff',
  border: '1px solid #e3eaf4',
  borderRadius: 8,
  color: '#1f2937',
  cursor: 'pointer',
  display: 'flex',
  gap: 12,
  padding: 12,
  textAlign: 'left',
  width: '100%'
};

const iconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#edf5ff',
  borderRadius: 8,
  color: '#1677ff',
  display: 'flex',
  flex: '0 0 34px',
  fontSize: 18,
  height: 34,
  justifyContent: 'center',
  width: 34
};

const nodeNameStyle: React.CSSProperties = {
  display: 'block',
  fontSize: 14,
  fontWeight: 700,
  lineHeight: '20px'
};

const nodeDescriptionStyle: React.CSSProperties = {
  color: '#667085',
  display: 'block',
  fontSize: 12,
  lineHeight: '18px',
  marginTop: 2
};
