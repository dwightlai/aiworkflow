export type WorkflowNodeType =
  | 'START'
  | 'END'
  | 'LLM'
  | 'PROMPT'
  | 'KNOWLEDGE_RETRIEVAL'
  | 'HTTP_TOOL'
  | 'CONDITION'
  | 'TEXT_TRANSFORM';

export interface WorkflowNode {
  id: string;
  type: WorkflowNodeType;
  name: string;
  config: Record<string, unknown>;
}

export interface PromptNodeConfig {
  template: string;
  outputKey: string;
}

export interface LlmNodeConfig {
  providerId: string;
  model: string;
  promptKey: string;
  outputKey: string;
  temperature?: number;
  maxTokens?: number;
}

export interface ConditionNodeConfig {
  contextKey: string;
  operator: 'EQUALS' | 'NOT_EQUALS' | 'CONTAINS' | 'IS_EMPTY' | 'IS_NOT_EMPTY';
  compareValue?: string;
  trueTargetNodeId: string;
  falseTargetNodeId: string;
}

export interface WorkflowEdge {
  id: string;
  sourceNodeId: string;
  targetNodeId: string;
  condition?: string | null;
}

export interface WorkflowVariable {
  name: string;
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'OBJECT' | 'ARRAY';
  required: boolean;
}

export interface WorkflowDefinition {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  variables: WorkflowVariable[];
}

export type WorkflowValidationIssueCode =
  | 'EXACTLY_ONE_START'
  | 'AT_LEAST_ONE_END'
  | 'DUPLICATE_NODE_ID'
  | 'EDGE_SOURCE_MISSING'
  | 'EDGE_TARGET_MISSING'
  | 'NODE_UNREACHABLE';

export interface WorkflowValidationIssue {
  code: WorkflowValidationIssueCode;
  message: string;
  nodeId?: string;
  edgeId?: string;
}

export function createEmptyWorkflowDefinition(): WorkflowDefinition {
  return {
    nodes: [
      { id: 'start_1', type: 'START', name: 'Start', config: {} }
    ],
    edges: [],
    variables: []
  };
}

export function validateWorkflowDefinition(definition: WorkflowDefinition): WorkflowValidationIssue[] {
  const issues: WorkflowValidationIssue[] = [];
  const nodeIds = new Set<string>();
  const duplicateNodeIds = new Set<string>();

  for (const node of definition.nodes) {
    if (nodeIds.has(node.id)) {
      duplicateNodeIds.add(node.id);
    }
    nodeIds.add(node.id);
  }

  for (const nodeId of duplicateNodeIds) {
    issues.push({
      code: 'DUPLICATE_NODE_ID',
      message: `节点 ID 重复：${nodeId}`,
      nodeId
    });
  }

  const startNodes = definition.nodes.filter((node) => node.type === 'START');
  if (startNodes.length !== 1) {
    issues.push({
      code: 'EXACTLY_ONE_START',
      message: '流程必须且只能有一个开始节点'
    });
  }

  if (!definition.nodes.some((node) => node.type === 'END')) {
    issues.push({
      code: 'AT_LEAST_ONE_END',
      message: '流程至少需要一个结束节点'
    });
  }

  const adjacency = new Map<string, string[]>();
  for (const edge of definition.edges) {
    const sourceExists = nodeIds.has(edge.sourceNodeId);
    const targetExists = nodeIds.has(edge.targetNodeId);
    if (!sourceExists) {
      issues.push({
        code: 'EDGE_SOURCE_MISSING',
        message: `连线 ${edge.id} 的源节点不存在`,
        edgeId: edge.id
      });
    }
    if (!targetExists) {
      issues.push({
        code: 'EDGE_TARGET_MISSING',
        message: `连线 ${edge.id} 的目标节点不存在`,
        edgeId: edge.id
      });
    }
    if (sourceExists && targetExists) {
      adjacency.set(edge.sourceNodeId, [...(adjacency.get(edge.sourceNodeId) ?? []), edge.targetNodeId]);
    }
  }

  if (startNodes.length === 1) {
    const reachableNodeIds = resolveReachableNodeIds(startNodes[0].id, adjacency);
    for (const node of definition.nodes) {
      if (!reachableNodeIds.has(node.id)) {
        issues.push({
          code: 'NODE_UNREACHABLE',
          message: `节点「${node.name}」无法从开始节点到达`,
          nodeId: node.id
        });
      }
    }
  }

  return issues;
}

function resolveReachableNodeIds(startNodeId: string, adjacency: Map<string, string[]>) {
  const reachableNodeIds = new Set<string>();
  const queue = [startNodeId];

  while (queue.length) {
    const nodeId = queue.shift() as string;
    if (reachableNodeIds.has(nodeId)) {
      continue;
    }
    reachableNodeIds.add(nodeId);
    queue.push(...(adjacency.get(nodeId) ?? []));
  }

  return reachableNodeIds;
}
