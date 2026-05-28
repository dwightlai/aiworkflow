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
  updateNode(nodeId: string, patch: Partial<WorkflowNode>): void;
  removeNode(nodeId: string): void;
  connectNodes(edgeId: string, sourceNodeId: string, targetNodeId: string): void;
  selectNode(nodeId: string | null): void;
  getSelectedNode(): WorkflowNode | null;
}

export function createWorkflowDesignerCore(options: WorkflowDesignerCoreOptions): WorkflowDesignerCore {
  let currentValue = options.value;
  let selectedNodeId: string | null = null;

  function emit(nextValue: WorkflowDefinition) {
    currentValue = nextValue;
    options.onChange?.(nextValue);
  }

  function updateValue(updater: (value: WorkflowDefinition) => WorkflowDefinition) {
    if (options.readonly) {
      return;
    }
    emit(updater(currentValue));
  }

  return {
    mount() {
      options.container.dataset.workflowDesignerMounted = 'true';
    },
    destroy() {
      delete options.container.dataset.workflowDesignerMounted;
    },
    getValue() {
      return currentValue;
    },
    setValue(value: WorkflowDefinition) {
      const nodeStillExists = !selectedNodeId || value.nodes.some((node) => node.id === selectedNodeId);
      if (!nodeStillExists) {
        selectedNodeId = null;
      }
      emit(value);
    },
    addNode(node: WorkflowNode) {
      updateValue((value) => ({
        ...value,
        nodes: [...value.nodes.filter((item) => item.id !== node.id), node]
      }));
    },
    updateNode(nodeId: string, patch: Partial<WorkflowNode>) {
      updateValue((value) => ({
        ...value,
        nodes: value.nodes.map((node) => node.id === nodeId ? { ...node, ...patch, id: node.id } : node)
      }));
    },
    removeNode(nodeId: string) {
      updateValue((value) => ({
        ...value,
        nodes: value.nodes.filter((node) => node.id !== nodeId),
        edges: value.edges.filter((edge) => edge.sourceNodeId !== nodeId && edge.targetNodeId !== nodeId)
      }));
      if (selectedNodeId === nodeId) {
        selectedNodeId = null;
      }
    },
    connectNodes(edgeId: string, sourceNodeId: string, targetNodeId: string) {
      const edge: WorkflowEdge = {
        id: edgeId,
        sourceNodeId,
        targetNodeId,
        condition: null
      };
      updateValue((value) => ({
        ...value,
        edges: [...value.edges.filter((item) => item.id !== edgeId), edge]
      }));
    },
    selectNode(nodeId: string | null) {
      selectedNodeId = nodeId && currentValue.nodes.some((node) => node.id === nodeId) ? nodeId : null;
    },
    getSelectedNode() {
      return currentValue.nodes.find((node) => node.id === selectedNodeId) ?? null;
    }
  };
}
