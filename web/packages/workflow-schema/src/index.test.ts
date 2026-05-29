import { describe, expect, it } from 'vitest';
import { createEmptyWorkflowDefinition, validateWorkflowDefinition } from './index';

describe('createEmptyWorkflowDefinition', () => {
  it('creates a starter canvas with only a START node', () => {
    const definition = createEmptyWorkflowDefinition();

    expect(definition.nodes.map((node) => node.type)).toEqual(['START']);
    expect(definition.edges).toEqual([]);
  });
});

describe('validateWorkflowDefinition', () => {
  it('reports that the default starter canvas is not publishable yet', () => {
    expect(validateWorkflowDefinition(createEmptyWorkflowDefinition()).map((issue) => issue.code)).toEqual([
      'AT_LEAST_ONE_END'
    ]);
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
