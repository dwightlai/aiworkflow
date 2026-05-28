import { describe, expect, it } from 'vitest';
import { createWorkflowDesignerCore } from './index';

describe('createWorkflowDesignerCore', () => {
  it('adds a node and emits updated workflow definition', () => {
    const container = { dataset: {} } as HTMLElement;
    const changes: unknown[] = [];
    const designer = createWorkflowDesignerCore({
      container,
      value: { nodes: [], edges: [], variables: [] },
      onChange: (value) => changes.push(value)
    });

    designer.mount();
    designer.addNode({
      id: 'prompt-1',
      type: 'PROMPT',
      name: 'Prompt',
      config: { template: 'Hello {{name}}', outputKey: 'prompt' }
    });

    expect(designer.getValue().nodes).toHaveLength(1);
    expect(changes).toHaveLength(1);
  });

  it('connects nodes and exposes selected node state', () => {
    const container = { dataset: {} } as HTMLElement;
    const designer = createWorkflowDesignerCore({
      container,
      value: {
        nodes: [
          { id: 'start', type: 'START', name: 'Start', config: {} },
          { id: 'end', type: 'END', name: 'End', config: {} }
        ],
        edges: [],
        variables: []
      }
    });

    designer.mount();
    designer.connectNodes('edge-1', 'start', 'end');
    designer.selectNode('end');

    expect(designer.getValue().edges[0].targetNodeId).toBe('end');
    expect(designer.getSelectedNode()?.id).toBe('end');
  });
});
