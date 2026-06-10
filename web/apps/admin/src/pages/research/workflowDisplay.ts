import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';
import type { WorkflowSnapshot, WorkflowSnapshotNode } from '../../api/generationTemplates';

export function workflowDefinitionToSnapshot(
  workflowId: string,
  workflowName: string,
  definition: WorkflowDefinition | null | undefined
): WorkflowSnapshot {
  const nodes: WorkflowSnapshotNode[] = (definition?.nodes ?? []).map((node, index) => ({
    id: node.id,
    name: node.name,
    type: node.type,
    order: index + 1
  }));
  return { workflowId, name: workflowName, nodes };
}

export function resolveWorkflowDisplay(
  workflowId: string | null | undefined,
  workflowName: string | null | undefined,
  definition: WorkflowDefinition | null | undefined,
  workflowSnapshot: WorkflowSnapshot | string | null | undefined,
  parseWorkflowSnapshot: (value: WorkflowSnapshot | string | null | undefined) => WorkflowSnapshot
): WorkflowSnapshot {
  if (workflowId && definition?.nodes?.length) {
    return workflowDefinitionToSnapshot(workflowId, workflowName ?? workflowId, definition);
  }
  return parseWorkflowSnapshot(workflowSnapshot);
}
