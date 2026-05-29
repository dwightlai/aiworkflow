import {
  ApiOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CodeOutlined,
  DeploymentUnitOutlined,
  PlayCircleOutlined,
  SaveOutlined,
  SendOutlined
} from '@ant-design/icons';
import { WorkflowDesignerReact, type WorkflowDesignerHandle } from '@aiworkflow/workflow-designer-react';
import { createEmptyWorkflowDefinition, type WorkflowDefinition, type WorkflowNode } from '@aiworkflow/workflow-schema';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Progress, Space, Tag, Typography, message } from 'antd';
import type React from 'react';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  createWorkflow,
  getWorkflow,
  publishWorkflow,
  runWorkflow,
  updateWorkflowDraft,
  type Workflow,
  type WorkflowExecution
} from '../../api/workflows';
import { DebugPanel } from './designer/DebugPanel';
import { NodeConfigPanel } from './designer/NodeConfigPanel';
import { NodePalette } from './designer/NodePalette';

export interface WorkflowDesignerPageProps {
  workflowId: string;
}

export function WorkflowDesignerPage({ workflowId }: WorkflowDesignerPageProps) {
  const isNewWorkflow = workflowId === 'new';
  const queryClient = useQueryClient();
  const designerRef = useRef<WorkflowDesignerHandle | null>(null);
  const [definition, setDefinition] = useState<WorkflowDefinition>(() => createEmptyWorkflowDefinition());
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [debugInput, setDebugInput] = useState('{\n  "name": "Ada"\n}');
  const [execution, setExecution] = useState<WorkflowExecution | null>(null);

  const workflowQuery = useQuery({
    queryKey: ['workflow', workflowId],
    queryFn: () => getWorkflow(workflowId),
    enabled: !isNewWorkflow
  });

  useEffect(() => {
    const nextDefinition = workflowQuery.data?.latestVersion?.definition;
    if (nextDefinition) {
      setDefinition(nextDefinition);
      setSelectedNodeId(nextDefinition.nodes[0]?.id ?? null);
    }
  }, [workflowQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () => {
      const nextDefinition = designerRef.current?.getValue() ?? definition;
      if (isNewWorkflow) {
        return createWorkflow({
          name: '新建工作流',
          description: null,
          definition: nextDefinition
        });
      }
      return updateWorkflowDraft(workflowId, nextDefinition);
    },
    onSuccess: async (workflow: Workflow) => {
      if (isNewWorkflow) {
        message.success('工作流已创建');
        await queryClient.invalidateQueries({ queryKey: ['workflows'] });
        navigateTo(`/workflows/${workflow.id}/designer`);
        return;
      }
      message.success('草稿已保存');
      await queryClient.invalidateQueries({ queryKey: ['workflow', workflowId] });
    }
  });

  const publishMutation = useMutation({
    mutationFn: () => publishWorkflow(workflowId),
    onSuccess: async () => {
      message.success('工作流已发布');
      await queryClient.invalidateQueries({ queryKey: ['workflow', workflowId] });
    }
  });

  const runMutation = useMutation({
    mutationFn: () => runWorkflow(workflowId, parseJson(debugInput)),
    onSuccess: (result) => {
      setExecution(result);
      message.success('运行完成');
    }
  });

  const selectedNode = useMemo(
    () => definition.nodes.find((node) => node.id === selectedNodeId) ?? definition.nodes[0] ?? null,
    [definition.nodes, selectedNodeId]
  );
  const workflowName = workflowQuery.data?.name ?? (isNewWorkflow ? '新建工作流' : '工作流设计器');
  const configCompleteness = useMemo(() => calculateConfigCompleteness(definition), [definition]);

  function handleAddNode(node: WorkflowNode) {
    designerRef.current?.addNode(node);
    setDefinition((current) => ({
      ...current,
      nodes: [...current.nodes.filter((item) => item.id !== node.id), node]
    }));
    setSelectedNodeId(node.id);
  }

  function handleUpdateNode(nodeId: string, patch: Partial<WorkflowNode>) {
    setDefinition((current) => ({
      ...current,
      nodes: current.nodes.map((node) => node.id === nodeId ? { ...node, ...patch, id: node.id } : node)
    }));
  }

  return (
    <section style={pageStyle}>
      <div style={toolbarStyle}>
        <Space size={12} align="center">
          <div style={titleIconStyle}><DeploymentUnitOutlined /></div>
          <div>
            <Typography.Title level={4} style={{ margin: 0 }}>{workflowName}</Typography.Title>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              轻量 DAG 编排 · Prompt / LLM / 条件 / 工具节点
            </Typography.Text>
          </div>
          <Tag color={workflowQuery.data?.status === 'PUBLISHED' ? 'green' : 'blue'}>
            {workflowQuery.data?.status === 'PUBLISHED' ? '已发布' : '草稿'}
          </Tag>
        </Space>
        <Space>
          <Button
            aria-label={isNewWorkflow ? '创建工作流' : '保存草稿'}
            icon={<SaveOutlined />}
            onClick={() => saveMutation.mutate()}
            loading={saveMutation.isPending}
          >
            {isNewWorkflow ? '创建工作流' : '保存草稿'}
          </Button>
          <Button
            aria-label="发布"
            icon={<SendOutlined />}
            onClick={() => publishMutation.mutate()}
            loading={publishMutation.isPending}
            disabled={isNewWorkflow}
          >
            发布
          </Button>
          <Button
            aria-label="运行"
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={() => runMutation.mutate()}
            loading={runMutation.isPending}
            disabled={isNewWorkflow}
          >
            运行
          </Button>
        </Space>
      </div>

      {workflowQuery.isError ? (
        <Alert type="error" showIcon message="工作流加载失败" description={(workflowQuery.error as Error).message} />
      ) : null}

      <section style={overviewStyle}>
        <OverviewItem title="工作流概览" value={`${definition.nodes.length} 个节点`} detail={`${definition.edges.length} 条连线`} icon={<DeploymentUnitOutlined />} />
        <OverviewItem title="配置完整度" value={`${configCompleteness}%`} detail="节点关键参数" icon={<CheckCircleOutlined />} progress={configCompleteness} />
        <OverviewItem title="运行状态" value={execution?.status ?? '待调试'} detail={execution ? formatDate(execution.startedAt) : '保存后可运行'} icon={<ClockCircleOutlined />} />
        <OverviewItem title="集成方式" value="React / Vue / WebComponent" detail="可嵌入第三方系统" icon={<ApiOutlined />} />
      </section>

      <div style={designerShellStyle}>
        <NodePalette onAddNode={handleAddNode} />
        <main style={canvasColumnStyle}>
          <Card styles={{ body: { padding: 0, height: '100%' } }} style={canvasCardStyle}>
            <div style={canvasHeaderStyle}>
              <div>
                <Typography.Text strong>节点编排</Typography.Text>
                <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12 }}>
                  拖入或点击节点，按业务意图组织 AI 执行链路
                </Typography.Text>
              </div>
              <Space size={8}>
                <Tag color="geekblue">执行链路</Tag>
                <Tag>{definition.variables.length} 个变量</Tag>
              </Space>
            </div>
            <div style={canvasBodyStyle}>
              <WorkflowDesignerReact ref={designerRef} value={definition} onChange={setDefinition} />
              <ExecutionMap nodes={definition.nodes} selectedNodeId={selectedNode?.id ?? null} onSelect={setSelectedNodeId} />
            </div>
          </Card>
        </main>
        <NodeConfigPanel node={selectedNode} onChange={handleUpdateNode} />
      </div>

      <DebugPanel
        input={debugInput}
        loading={runMutation.isPending}
        execution={execution}
        onInputChange={setDebugInput}
        onRun={() => runMutation.mutate()}
      />
    </section>
  );
}

function OverviewItem({
  title,
  value,
  detail,
  icon,
  progress
}: {
  title: string;
  value: string;
  detail: string;
  icon: React.ReactNode;
  progress?: number;
}) {
  return (
    <Card styles={{ body: { padding: 16 } }} style={overviewCardStyle}>
      <Space align="start" size={12} style={{ width: '100%' }}>
        <div style={overviewIconStyle}>{icon}</div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{title}</Typography.Text>
          <Typography.Title level={5} style={{ margin: '2px 0 3px' }}>{value}</Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{detail}</Typography.Text>
          {typeof progress === 'number' ? <Progress percent={progress} showInfo={false} size="small" style={{ marginTop: 8 }} /> : null}
        </div>
      </Space>
    </Card>
  );
}

function ExecutionMap({
  nodes,
  selectedNodeId,
  onSelect
}: {
  nodes: WorkflowNode[];
  selectedNodeId: string | null;
  onSelect: (nodeId: string) => void;
}) {
  return (
    <div style={executionMapStyle}>
      <div style={mapGridStyle} />
      <div style={mapHeaderStyle}>
        <Space size={8}>
          <CodeOutlined />
          <Typography.Text strong>执行链路</Typography.Text>
        </Space>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>从 Start 到 End 的轻量 DAG 预览</Typography.Text>
      </div>
      <div style={nodeRailStyle}>
        {nodes.map((node, index) => (
          <div key={node.id} style={railItemStyle}>
            {index > 0 ? <div style={connectorStyle} /> : null}
            <button
              type="button"
              style={flowNodeStyle(node.id === selectedNodeId, node.type)}
              onClick={() => onSelect(node.id)}
            >
              <span style={nodeTypeStyle}>{node.type}</span>
              <span style={nodeNameStyle}>{node.name}</span>
              <span style={nodeConfigStyle}>{summarizeNode(node)}</span>
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

function calculateConfigCompleteness(definition: WorkflowDefinition) {
  if (definition.nodes.length === 0) {
    return 0;
  }
  const completeNodes = definition.nodes.filter((node) => {
    if (node.type === 'PROMPT') {
      return Boolean(node.config.template && node.config.outputKey);
    }
    if (node.type === 'LLM') {
      return Boolean(node.config.providerId && node.config.model && node.config.promptKey && node.config.outputKey);
    }
    if (node.type === 'CONDITION') {
      return Boolean(node.config.contextKey && node.config.operator);
    }
    return true;
  }).length;
  return Math.round((completeNodes / definition.nodes.length) * 100);
}

function summarizeNode(node: WorkflowNode) {
  if (node.type === 'PROMPT') {
    return `输出 ${String(node.config.outputKey ?? '-')}`;
  }
  if (node.type === 'LLM') {
    return `${String(node.config.providerId ?? 'default')} / ${String(node.config.model ?? '-')}`;
  }
  if (node.type === 'CONDITION') {
    return `${String(node.config.contextKey ?? '-')} ${String(node.config.operator ?? '')}`;
  }
  if (node.type === 'END') {
    return '汇总输出';
  }
  return '系统节点';
}

function parseJson(value: string) {
  try {
    return JSON.parse(value) as Record<string, unknown>;
  } catch {
    return {};
  }
}

function formatDate(value?: string | null) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

const pageStyle: React.CSSProperties = {
  background: '#f4f6fa',
  display: 'flex',
  flexDirection: 'column',
  height: 'calc(100vh - 126px)',
  margin: '-16px -24px',
  minHeight: 760
};

const toolbarStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  borderBottom: '1px solid #e7ecf3',
  display: 'flex',
  justifyContent: 'space-between',
  padding: '14px 20px'
};

const titleIconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#e9f2ff',
  borderRadius: 8,
  color: '#1677ff',
  display: 'flex',
  fontSize: 22,
  height: 42,
  justifyContent: 'center',
  width: 42
};

const overviewStyle: React.CSSProperties = {
  display: 'grid',
  gap: 12,
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
  padding: '14px 16px 0'
};

const overviewCardStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3',
  borderRadius: 8
};

const overviewIconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#f1f5f9',
  borderRadius: 8,
  color: '#31516f',
  display: 'flex',
  fontSize: 18,
  height: 36,
  justifyContent: 'center',
  width: 36
};

const designerShellStyle: React.CSSProperties = {
  display: 'flex',
  flex: 1,
  gap: 14,
  minHeight: 0,
  padding: 16
};

const canvasColumnStyle: React.CSSProperties = {
  flex: 1,
  minWidth: 0
};

const canvasCardStyle: React.CSSProperties = {
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  height: '100%',
  overflow: 'hidden'
};

const canvasHeaderStyle: React.CSSProperties = {
  alignItems: 'center',
  borderBottom: '1px solid #eef2f7',
  display: 'flex',
  justifyContent: 'space-between',
  padding: '13px 16px'
};

const canvasBodyStyle: React.CSSProperties = {
  height: 'calc(100% - 58px)',
  position: 'relative'
};

const executionMapStyle: React.CSSProperties = {
  background: '#f8fafc',
  inset: 0,
  overflow: 'auto',
  position: 'absolute'
};

const mapGridStyle: React.CSSProperties = {
  backgroundImage: 'linear-gradient(#e8eef6 1px, transparent 1px), linear-gradient(90deg, #e8eef6 1px, transparent 1px)',
  backgroundSize: '28px 28px',
  inset: 0,
  opacity: 0.8,
  position: 'absolute'
};

const mapHeaderStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  justifyContent: 'space-between',
  left: 18,
  position: 'absolute',
  right: 18,
  top: 16
};

const nodeRailStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 26,
  minHeight: '100%',
  minWidth: 760,
  padding: '84px 54px 42px',
  position: 'relative'
};

const railItemStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  position: 'relative'
};

const connectorStyle: React.CSSProperties = {
  background: '#b8c7d9',
  height: 2,
  left: -26,
  position: 'absolute',
  width: 26
};

function flowNodeStyle(active: boolean, type: WorkflowNode['type']): React.CSSProperties {
  return {
    background: active ? '#ffffff' : '#fcfdff',
    border: `1px solid ${active ? '#1677ff' : '#d8e2ef'}`,
    borderLeft: `4px solid ${nodeColor(type)}`,
    borderRadius: 8,
    boxShadow: active ? '0 14px 32px rgba(22, 119, 255, 0.18)' : '0 10px 24px rgba(15, 23, 42, 0.07)',
    color: '#1f2937',
    cursor: 'pointer',
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
    minHeight: 118,
    padding: '14px 16px',
    textAlign: 'left',
    width: 190
  };
}

const nodeTypeStyle: React.CSSProperties = {
  color: '#64748b',
  fontSize: 11,
  fontWeight: 700,
  letterSpacing: 0
};

const nodeNameStyle: React.CSSProperties = {
  color: '#0f172a',
  fontSize: 16,
  fontWeight: 700
};

const nodeConfigStyle: React.CSSProperties = {
  color: '#64748b',
  fontSize: 12,
  lineHeight: '18px'
};

function nodeColor(type: WorkflowNode['type']) {
  if (type === 'LLM') {
    return '#7c3aed';
  }
  if (type === 'PROMPT') {
    return '#1677ff';
  }
  if (type === 'CONDITION') {
    return '#f59e0b';
  }
  if (type === 'END') {
    return '#10b981';
  }
  return '#64748b';
}
