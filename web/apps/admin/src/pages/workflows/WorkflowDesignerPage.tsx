import {
  ArrowLeftOutlined,
  BugOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DeploymentUnitOutlined,
  PlayCircleOutlined,
  SaveOutlined,
  SendOutlined,
  SettingOutlined
} from '@ant-design/icons';
import { WorkflowDesignerReact, type WorkflowDesignerHandle } from '@aiworkflow/workflow-designer-react';
import {
  createEmptyWorkflowDefinition,
  validateWorkflowDefinition,
  type WorkflowDefinition,
  type WorkflowNode,
  type WorkflowValidationIssue
} from '@aiworkflow/workflow-schema';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Drawer, Space, Tag, Typography, message } from 'antd';
import type React from 'react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { listKnowledgeBases } from '../../api/knowledge';
import { listModelProviders } from '../../api/models';
import { listPromptTemplates } from '../../api/prompts';
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
import { NodePalette, WORKFLOW_NODE_TEMPLATE_MIME, createNode, parseNodeTemplate } from './designer/NodePalette';

export interface WorkflowDesignerPageProps {
  workflowId: string;
}

export function WorkflowDesignerPage({ workflowId }: WorkflowDesignerPageProps) {
  const isNewWorkflow = workflowId === 'new';
  const queryClient = useQueryClient();
  const designerRef = useRef<WorkflowDesignerHandle | null>(null);
  const canvasDropRef = useRef<HTMLDivElement | null>(null);
  const [definition, setDefinition] = useState<WorkflowDefinition>(() => createEmptyWorkflowDefinition());
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [debugInput, setDebugInput] = useState('{\n  "name": "Ada"\n}');
  const [execution, setExecution] = useState<WorkflowExecution | null>(null);
  const [validationIssues, setValidationIssues] = useState<WorkflowValidationIssue[]>([]);
  const [configOpen, setConfigOpen] = useState(false);
  const [debugOpen, setDebugOpen] = useState(false);

  const workflowQuery = useQuery({
    queryKey: ['workflow', workflowId],
    queryFn: () => getWorkflow(workflowId),
    enabled: !isNewWorkflow
  });
  const modelProvidersQuery = useQuery({
    queryKey: ['model-providers'],
    queryFn: listModelProviders
  });
  const promptTemplatesQuery = useQuery({
    queryKey: ['prompt-templates'],
    queryFn: listPromptTemplates
  });
  const knowledgeBasesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases
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
    () => definition.nodes.find((node) => node.id === selectedNodeId) ?? null,
    [definition.nodes, selectedNodeId]
  );
  const workflowName = workflowQuery.data?.name ?? (isNewWorkflow ? '新建工作流' : '工作流设计器');
  const configCompleteness = useMemo(() => calculateConfigCompleteness(definition), [definition]);
  const nodeRunStates = useMemo(() => {
    return Object.fromEntries((execution?.nodeExecutions ?? []).map((node) => [node.nodeId, node.status]));
  }, [execution]);

  function handleAddNode(node: WorkflowNode) {
    designerRef.current?.addNode(node);
    setDefinition((current) => ({
      ...current,
      nodes: [...current.nodes.filter((item) => item.id !== node.id), node]
    }));
    setSelectedNodeId(node.id);
    setConfigOpen(true);
    setValidationIssues([]);
  }

  function handleCanvasDrop(event: React.DragEvent<HTMLDivElement>) {
    const template = parseNodeTemplate(event.dataTransfer.getData(WORKFLOW_NODE_TEMPLATE_MIME));
    if (!template) {
      return;
    }
    event.preventDefault();
    const rect = canvasDropRef.current?.getBoundingClientRect();
    const clientX = readDragCoordinate(event, 'clientX', 'pageX', 32);
    const clientY = readDragCoordinate(event, 'clientY', 'pageY', 32);
    const position = rect ? {
      x: Math.max(Math.round(clientX - rect.left), 0),
      y: Math.max(Math.round(clientY - rect.top), 0)
    } : { x: 32, y: 32 };
    handleAddNode(createNode(template, position));
  }

  function handleUpdateNode(nodeId: string, patch: Partial<WorkflowNode>) {
    setDefinition((current) => ({
      ...current,
      nodes: current.nodes.map((node) => node.id === nodeId ? { ...node, ...patch, id: node.id } : node)
    }));
    setValidationIssues([]);
  }

  function validateCurrentDefinition() {
    const nextDefinition = designerRef.current?.getValue() ?? definition;
    const nextIssues = validateWorkflowDefinition(nextDefinition);
    setDefinition(nextDefinition);
    setValidationIssues(nextIssues);
    return { nextDefinition, nextIssues };
  }

  function handleSave() {
    validateCurrentDefinition();
    saveMutation.mutate();
  }

  function handlePublish() {
    const { nextIssues } = validateCurrentDefinition();
    if (nextIssues.length > 0) {
      message.warning('流程结构校验未通过');
      return;
    }
    publishMutation.mutate();
  }

  return (
    <section style={pageStyle}>

      <div style={toolbarStyle}>
        <Space size={10} align="center">
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigateTo('/workflows')}>返回</Button>
          <div style={titleIconStyle}><DeploymentUnitOutlined /></div>
          <Typography.Text strong>{workflowName}</Typography.Text>
          <Tag color={workflowQuery.data?.status === 'PUBLISHED' ? 'green' : 'blue'}>
            {workflowQuery.data?.status === 'PUBLISHED' ? '已发布' : '草稿'}
          </Tag>
          <div style={modeTabsStyle}>
            <Button type="primary" ghost icon={<SettingOutlined />}>编排</Button>
            <Button type="text">API</Button>
          </div>
        </Space>
        <Space size={8}>
          <Tag icon={<DeploymentUnitOutlined />}>{definition.nodes.length} 节点</Tag>
          <Tag>{definition.edges.length} 连线</Tag>
          <Tag icon={<CheckCircleOutlined />}>{configCompleteness}% 配置</Tag>
          <Tag icon={<ClockCircleOutlined />}>{execution?.status ?? '待调试'}</Tag>
          <Button icon={<SettingOutlined />} disabled={!selectedNode} onClick={() => setConfigOpen(true)}>
            属性
          </Button>
          <Button icon={<BugOutlined />} onClick={() => setDebugOpen(true)}>
            调试
          </Button>
          <Button
            aria-label={isNewWorkflow ? '创建工作流' : '保存草稿'}
            icon={<SaveOutlined />}
            onClick={handleSave}
            loading={saveMutation.isPending}
          >
            {isNewWorkflow ? '创建工作流' : '保存草稿'}
          </Button>
          <Button
            aria-label="发布"
            icon={<SendOutlined />}
            onClick={handlePublish}
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

      {validationIssues.length > 0 ? (
        <Alert
          type="warning"
          showIcon
          message="流程结构校验未通过"
          description={(
            <ul style={validationListStyle}>
              {validationIssues.slice(0, 5).map((issue) => (
                <li key={`${issue.code}-${issue.nodeId ?? issue.edgeId ?? issue.message}`}>{issue.message}</li>
              ))}
            </ul>
          )}
          style={validationAlertStyle}
        />
      ) : null}

      <div style={designerShellStyle}>
        <NodePalette onAddNode={handleAddNode} />
        <main style={canvasColumnStyle}>
          <div style={canvasHeaderStyle}>
            <Space size={8}>
              <Typography.Text strong>节点编排</Typography.Text>
              <Tag color="geekblue">拖拽节点</Tag>
              <Tag color="blue">端口连线</Tag>
              <Tag>{definition.variables.length} 变量</Tag>
            </Space>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              画布区域已最大化，点击节点打开属性，调试从右上角进入
            </Typography.Text>
          </div>
          <div
            ref={canvasDropRef}
            aria-label="工作流画布投放区"
            style={canvasBodyStyle}
            onDragOver={(event) => {
              if (event.dataTransfer.types.includes(WORKFLOW_NODE_TEMPLATE_MIME)) {
                event.preventDefault();
                event.dataTransfer.dropEffect = 'copy';
              }
            }}
            onDrop={handleCanvasDrop}
          >
            <WorkflowDesignerReact
              ref={designerRef}
              value={definition}
              selectedNodeId={selectedNodeId}
              nodeRunStates={nodeRunStates}
              onChange={setDefinition}
              onNodeSelect={(nodeId) => {
                setSelectedNodeId(nodeId);
                setConfigOpen(true);
              }}
            />
          </div>
        </main>
      </div>

      <Drawer
        title={selectedNode ? `节点属性：${selectedNode.name}` : '节点属性'}
        open={configOpen}
        onClose={() => setConfigOpen(false)}
        keyboard={false}
        mask={false}
        maskClosable={false}
        width={380}
        styles={{ body: { padding: 0 } }}
      >
        <NodeConfigPanel
          node={selectedNode}
          onChange={handleUpdateNode}
          modelProviders={modelProvidersQuery.data?.items ?? []}
          promptTemplates={promptTemplatesQuery.data?.items ?? []}
          knowledgeBases={knowledgeBasesQuery.data?.items ?? []}
        />
      </Drawer>

      <Drawer
        title="调试控制台"
        open={debugOpen}
        onClose={() => setDebugOpen(false)}
        placement="bottom"
        height={430}
        styles={{ body: { padding: 0 } }}
      >
        <DebugPanel
          input={debugInput}
          loading={runMutation.isPending}
          execution={execution}
          onInputChange={setDebugInput}
          onRun={() => runMutation.mutate()}
        />
      </Drawer>
    </section>
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

function readDragCoordinate(
  event: React.DragEvent<HTMLDivElement>,
  primaryKey: 'clientX' | 'clientY',
  fallbackKey: 'pageX' | 'pageY',
  defaultValue: number
) {
  const nativeEvent = event.nativeEvent as DragEvent & Record<string, unknown>;
  const values = [
    event[primaryKey],
    nativeEvent[primaryKey],
    nativeEvent[fallbackKey]
  ];
  const value = values.find((item) => typeof item === 'number' && Number.isFinite(item));
  return typeof value === 'number' ? value : defaultValue;
}

const pageStyle: React.CSSProperties = {
  background: '#f4f7fb',
  display: 'flex',
  flexDirection: 'column',
  height: 'calc(100vh - 40px)',
  margin: '-16px -24px',
  minHeight: 720,
  overflow: 'hidden'
};

const toolbarStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  borderBottom: '1px solid #e7ecf3',
  display: 'flex',
  flex: '0 0 56px',
  justifyContent: 'space-between',
  padding: '8px 16px'
};

const modeTabsStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 8,
  marginLeft: 18
};

const validationAlertStyle: React.CSSProperties = {
  borderRadius: 0,
  flex: '0 0 auto',
  margin: 0
};

const validationListStyle: React.CSSProperties = {
  margin: '4px 0 0',
  paddingLeft: 18
};

const titleIconStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#e9f2ff',
  borderRadius: 8,
  color: '#1677ff',
  display: 'flex',
  fontSize: 18,
  height: 34,
  justifyContent: 'center',
  width: 34
};

const designerShellStyle: React.CSSProperties = {
  display: 'flex',
  flex: 1,
  gap: 0,
  minHeight: 0
};

const canvasColumnStyle: React.CSSProperties = {
  background: '#f8fbff',
  borderLeft: '1px solid #e7ecf3',
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  minWidth: 0
};

const canvasHeaderStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  borderBottom: '1px solid #eef2f7',
  display: 'flex',
  flex: '0 0 42px',
  justifyContent: 'space-between',
  padding: '8px 14px'
};

const canvasBodyStyle: React.CSSProperties = {
  flex: 1,
  minHeight: 0,
  position: 'relative'
};
