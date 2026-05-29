import { WorkflowDesignerReact, type WorkflowDesignerHandle } from '@aiworkflow/workflow-designer-react';
import { createEmptyWorkflowDefinition, type WorkflowDefinition, type WorkflowNode } from '@aiworkflow/workflow-schema';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Space, Tag, Typography, message } from 'antd';
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

  const workflowName = workflowQuery.data?.name ?? (isNewWorkflow ? '新建工作流' : '工作流设计器');

  return (
    <section style={pageStyle}>
      <div style={toolbarStyle}>
        <Space size={12} align="center">
          <Typography.Title level={4} style={{ margin: 0 }}>{workflowName}</Typography.Title>
          <Tag color={workflowQuery.data?.status === 'PUBLISHED' ? 'green' : 'blue'}>
            {workflowQuery.data?.status === 'PUBLISHED' ? '已发布' : '草稿'}
          </Tag>
        </Space>
        <Space>
          <Button onClick={() => saveMutation.mutate()} loading={saveMutation.isPending}>
            {isNewWorkflow ? '创建工作流' : '保存草稿'}
          </Button>
          <Button onClick={() => publishMutation.mutate()} loading={publishMutation.isPending} disabled={isNewWorkflow}>
            发布
          </Button>
          <Button type="primary" onClick={() => runMutation.mutate()} loading={runMutation.isPending} disabled={isNewWorkflow}>
            运行
          </Button>
        </Space>
      </div>

      {workflowQuery.isError ? (
        <Alert type="error" showIcon message="工作流加载失败" description={(workflowQuery.error as Error).message} />
      ) : null}

      <div style={designerShellStyle}>
        <NodePalette onAddNode={handleAddNode} />
        <main style={canvasColumnStyle}>
          <Card styles={{ body: { padding: 0, height: '100%' } }} style={canvasCardStyle}>
            <div style={canvasHeaderStyle}>
              <Typography.Text strong>流程画布</Typography.Text>
              <Typography.Text type="secondary">{definition.nodes.length} 个节点 · {definition.edges.length} 条连线</Typography.Text>
            </div>
            <div style={canvasBodyStyle}>
              <WorkflowDesignerReact ref={designerRef} value={definition} onChange={setDefinition} />
              <div style={nodeStripStyle}>
                {definition.nodes.map((node) => (
                  <button
                    key={node.id}
                    type="button"
                    style={nodeChipStyle(node.id === selectedNode?.id)}
                    onClick={() => setSelectedNodeId(node.id)}
                  >
                    {node.name}
                  </button>
                ))}
              </div>
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

function parseJson(value: string) {
  try {
    return JSON.parse(value) as Record<string, unknown>;
  } catch {
    return {};
  }
}

function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

const pageStyle: React.CSSProperties = {
  background: '#f5f7fb',
  display: 'flex',
  flexDirection: 'column',
  height: 'calc(100vh - 126px)',
  margin: '-16px -24px',
  minHeight: 680
};

const toolbarStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  borderBottom: '1px solid #e8edf5',
  display: 'flex',
  justifyContent: 'space-between',
  padding: '14px 18px'
};

const designerShellStyle: React.CSSProperties = {
  display: 'flex',
  flex: 1,
  minHeight: 0
};

const canvasColumnStyle: React.CSSProperties = {
  flex: 1,
  minWidth: 0,
  padding: 14
};

const canvasCardStyle: React.CSSProperties = {
  height: '100%'
};

const canvasHeaderStyle: React.CSSProperties = {
  alignItems: 'center',
  borderBottom: '1px solid #eef2f7',
  display: 'flex',
  justifyContent: 'space-between',
  padding: '12px 14px'
};

const canvasBodyStyle: React.CSSProperties = {
  height: 'calc(100% - 47px)',
  position: 'relative'
};

const nodeStripStyle: React.CSSProperties = {
  display: 'flex',
  gap: 10,
  left: 18,
  position: 'absolute',
  top: 18
};

function nodeChipStyle(active: boolean): React.CSSProperties {
  return {
    background: active ? '#e8f1ff' : '#fff',
    border: `1px solid ${active ? '#1677ff' : '#dbe3ef'}`,
    borderRadius: 6,
    color: active ? '#0958d9' : '#344054',
    cursor: 'pointer',
    padding: '7px 12px'
  };
}
