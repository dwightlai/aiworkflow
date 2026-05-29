import { describe, expect, it } from 'vitest';
import { createEmptyWorkflowDefinition, validateWorkflowDefinition } from './index';

describe('createEmptyWorkflowDefinition', () => {
  it('creates a backend-valid runnable skeleton with START and END', () => {
    const definition = createEmptyWorkflowDefinition();

    expect(definition.nodes.map((node) => node.type)).toEqual(['START', 'END']);
    expect(definition.edges).toEqual([
      {
        id: 'edge_start_end',
        sourceNodeId: 'start_1',
        targetNodeId: 'end_1',
        condition: null
      }
    ]);
  });
});

describe('validateWorkflowDefinition', () => {
  it('accepts the default runnable skeleton', () => {
    expect(validateWorkflowDefinition(createEmptyWorkflowDefinition())).toEqual([]);
  });

  it('reports broken graph structure before saving or publishing', () => {
    const issues = validateWorkflowDefinition({
      nodes: [
        { id: 'start', type: 'START', name: 'Start', config: {} },
        { id: 'end', type: 'END', name: 'End', config: {} },
        { id: 'orphan', type: 'PROMPT', name: 'Orphan', config: {} }
      ],
      edges: [
        { id: 'bad-edge', sourceNodeId: 'missing', targetNodeId: 'end', condition: null }
      ],
      variables: []
    });

    expect(issues.map((issue) => issue.code)).toEqual([
      'EDGE_SOURCE_MISSING',
      'NODE_UNREACHABLE',
      'NODE_UNREACHABLE'
    ]);
  });
});
