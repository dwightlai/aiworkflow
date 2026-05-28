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

export function createEmptyWorkflowDefinition(): WorkflowDefinition {
  return {
    nodes: [{ id: 'start_1', type: 'START', name: '开始', config: {} }],
    edges: [],
    variables: []
  };
}
