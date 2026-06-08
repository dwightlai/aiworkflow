export type WorkflowNodeType = 'START' | 'END' | 'LLM' | 'QUESTION_CLASSIFIER' | 'PROMPT' | 'KNOWLEDGE_RETRIEVAL' | 'HTTP_TOOL' | 'CONDITION' | 'TEXT_TRANSFORM' | 'CONTENT_TEMPLATE' | 'LOOP';
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
export type WorkflowValidationIssueCode = 'EXACTLY_ONE_START' | 'AT_LEAST_ONE_END' | 'DUPLICATE_NODE_ID' | 'EDGE_SOURCE_MISSING' | 'EDGE_TARGET_MISSING' | 'NODE_UNREACHABLE';
export interface WorkflowValidationIssue {
    code: WorkflowValidationIssueCode;
    message: string;
    nodeId?: string;
    edgeId?: string;
}
export declare function createEmptyWorkflowDefinition(): WorkflowDefinition;
export declare function validateWorkflowDefinition(definition: WorkflowDefinition): WorkflowValidationIssue[];
