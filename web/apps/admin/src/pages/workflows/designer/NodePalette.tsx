import {
  BranchesOutlined,
  CommentOutlined,
  DatabaseOutlined,
  PlayCircleOutlined,
  RedoOutlined,
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
  {
    type: 'START',
    name: '开始',
    description: '接收 API、调试或会话输入',
    icon: <PlayCircleOutlined />,
    config: {
      inputParams: [],
      inputKeys: [],
      defaultInputJson: '{}'
    }
  },
  {
    type: 'KNOWLEDGE_RETRIEVAL',
    name: '知识库',
    description: '通过知识库获取内容',
    icon: <DatabaseOutlined />,
    config: {
      inputParams: [],
      knowledgeBaseId: '',
      knowledgeBaseIds: [],
      queryText: '{input}',
      keywordTemplate: '{input}',
      queryKey: 'input',
      fetchCount: 5,
      topK: 5,
      timeoutSeconds: 60,
      retryCount: 0,
      errorStrategy: 'INTERRUPT_NODE',
      outputKey: 'content',
      outputFormat: 'TEXT',
      outputParams: [
        { name: 'content', type: 'String' },
        { name: 'sources', type: 'Array[String]' },
        { name: 'query', type: 'String' }
      ]
    }
  },
  {
    type: 'CONTENT_TEMPLATE',
    name: '内容模板',
    description: '使用模板生成文本或 JSON 内容',
    icon: <CommentOutlined />,
    config: {
      template: '请结合以下知识片段回答用户问题。\n\n知识片段：{{documents}}\n\n用户问题：{{question}}',
      outputKey: 'content',
      outputFormat: 'TEXT',
      outputParams: [{ name: 'content', type: 'String' }]
    }
  },
  {
    type: 'LLM',
    name: '大模型',
    description: '使用大模型处理问题',
    icon: <RobotOutlined />,
    config: {
      inputParams: [],
      providerId: '',
      model: '',
      temperature: 0,
      topP: 0.9,
      topK: 50,
      maxTokens: 60,
      timeoutSeconds: 60,
      retryCount: 0,
      errorStrategy: 'INTERRUPT_NODE',
      systemPrompt: '',
      userPrompt: '${input}',
      promptKey: 'input',
      outputKey: 'content',
      outputFormat: 'TEXT',
      outputParams: [{ name: 'content', type: 'String' }]
    }
  },
  {
    type: 'QUESTION_CLASSIFIER',
    name: '问题分类',
    description: '按关键词把问题路由到不同分支',
    icon: <BranchesOutlined />,
    config: {
      inputKey: '开始.input',
      outputKey: 'questionCategory',
      categories: [
        { id: 'consult', name: '咨询类', keywords: ['咨询', '介绍', '怎么'], matchMode: 'CONTAINS' },
        { id: 'after_sales', name: '售后类', keywords: ['退款', '退货', '售后'], matchMode: 'CONTAINS' }
      ],
      outputParams: [
        { name: 'questionCategory', type: 'String' },
        { name: 'categoryName', type: 'String' },
        { name: 'categoryMatched', type: 'Boolean' }
      ]
    }
  },
  {
    type: 'LOOP',
    name: '循环',
    description: '遍历数组并执行循环体步骤',
    icon: <RedoOutlined />,
    config: {
      loopVar: 'items',
      itemVar: 'loopItem',
      indexVar: 'index',
      maxIterations: 100,
      outputKey: 'loopResults',
      loopSteps: [
        {
          type: 'CONTENT_TEMPLATE',
          name: '模板处理',
          template: '第 {{index}} 项：{{loopItem}}',
          outputKey: 'text'
        }
      ],
      outputParams: [{ name: 'loopResults', type: 'Array' }]
    }
  },
  {
    type: 'HTTP_TOOL',
    name: 'HTTP 请求',
    description: '调用外部 API 并输出响应结构',
    icon: <ToolOutlined />,
    config: {
      method: 'POST',
      url: '',
      params: [],
      headers: [{ key: 'Content-Type', value: 'application/json' }],
      bodyType: 'JSON',
      bodyTemplate: '{\n  "question": "{{question}}"\n}',
      responseBodyType: 'TEXT',
      outputKey: 'httpResult',
      outputParams: [
        { name: 'headers', type: 'Object' },
        { name: 'statusCode', type: 'Number' },
        { name: 'body', type: 'String' }
      ],
      timeoutMs: 30000
    }
  },
  {
    type: 'PROMPT',
    name: 'Prompt 模板',
    description: '引用已保存 Prompt 或渲染提示词',
    icon: <CommentOutlined />,
    config: {
      template: '请回答：{{question}}',
      outputKey: 'prompt'
    }
  },
  {
    type: 'CONDITION',
    name: '条件分支',
    description: '根据上下文变量选择后续节点',
    icon: <BranchesOutlined />,
    config: {
      contextKey: 'output',
      operator: 'CONTAINS',
      compareValue: '',
      equals: '',
      trueTargetNodeId: '',
      falseTargetNodeId: ''
    }
  },
  {
    type: 'TEXT_TRANSFORM',
    name: '文本处理',
    description: '兼容旧版模板渲染节点',
    icon: <CommentOutlined />,
    config: { template: '{{output}}', outputKey: 'text' }
  },
  {
    type: 'END',
    name: '结束',
    description: '整理最终输出返回调用方',
    icon: <StopOutlined />,
    config: {
      outputKeys: ['output'],
      outputParams: [{ name: 'result', value: 'content', type: 'String' }],
      outputFormat: 'JSON'
    }
  }
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
