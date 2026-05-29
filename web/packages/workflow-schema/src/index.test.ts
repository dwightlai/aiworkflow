import { describe, expect, it } from 'vitest';
import { createEmptyWorkflowDefinition } from './index';

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
