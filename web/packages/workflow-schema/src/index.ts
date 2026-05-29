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

export function createEmptyWorkflowDefinition(): WorkflowDefinition {
  return {
    nodes: [
      { id: 'start_1', type: 'START', name: 'Start', config: {} },
      { id: 'end_1', type: 'END', name: 'End', config: {} }
    ],
    edges: [
      {
        id: 'edge_start_end',
        sourceNodeId: 'start_1',
        targetNodeId: 'end_1',
        condition: null
      }
    ],
    variables: []
  };
}
