import type { WorkflowDefinition, WorkflowEdge, WorkflowNode } from '@aiworkflow/workflow-schema';
export interface WorkflowDesignerCoreOptions {
    container: HTMLElement;
    value: WorkflowDefinition;
    readonly?: boolean;
    onChange?: (value: WorkflowDefinition) => void;
}
export interface WorkflowDesignerCore {
    mount(): void;
    destroy(): void;
    getValue(): WorkflowDefinition;
    setValue(value: WorkflowDefinition): void;
    addNode(node: WorkflowNode): void;
    duplicateNode(nodeId: string): WorkflowNode | null;
    updateNode(nodeId: string, patch: Partial<WorkflowNode>): void;
    updateEdge(edgeId: string, patch: Partial<WorkflowEdge>): void;
    moveNode(nodeId: string, x: number, y: number): void;
    removeNode(nodeId: string): void;
    removeEdge(edgeId: string): void;
    connectNodes(edgeId: string, sourceNodeId: string, targetNodeId: string): void;
    autoLayout(): void;
    selectNode(nodeId: string | null): void;
    getSelectedNode(): WorkflowNode | null;
}
export interface WorkflowDesignerLayoutNode extends WorkflowNode {
    x: number;
    y: number;
    width: number;
    height: number;
}
export interface WorkflowDesignerLayoutEdge extends WorkflowEdge {
    path: string;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}
export interface WorkflowDesignerLayout {
    nodes: WorkflowDesignerLayoutNode[];
    edges: WorkflowDesignerLayoutEdge[];
    bounds: {
        width: number;
        height: number;
    };
}
export declare function createWorkflowDesignerLayout(definition: WorkflowDefinition): WorkflowDesignerLayout;
export declare function createWorkflowDesignerCore(options: WorkflowDesignerCoreOptions): WorkflowDesignerCore;
