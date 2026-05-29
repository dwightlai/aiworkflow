import { describe, expect, it } from 'vitest';
import { createWorkflowDesignerCore, createWorkflowDesignerLayout } from './index';

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

  it('creates deterministic node and edge layout for framework adapters', () => {
    const layout = createWorkflowDesignerLayout({
      nodes: [
        { id: 'start', type: 'START', name: 'Start', config: {} },
        { id: 'prompt', type: 'PROMPT', name: 'Prompt', config: {} },
        { id: 'llm', type: 'LLM', name: 'LLM', config: {} },
        { id: 'end', type: 'END', name: 'End', config: {} }
      ],
      edges: [
        { id: 'edge-1', sourceNodeId: 'start', targetNodeId: 'prompt' },
        { id: 'edge-2', sourceNodeId: 'prompt', targetNodeId: 'llm' },
        { id: 'edge-3', sourceNodeId: 'llm', targetNodeId: 'end' }
      ],
      variables: []
    });

    expect(layout.nodes.map((node) => node.id)).toEqual(['start', 'prompt', 'llm', 'end']);
    expect(layout.nodes[0].x).toBeLessThan(layout.nodes[1].x);
    expect(layout.edges[0].path).toContain('C');
    expect(layout.bounds.width).toBeGreaterThan(500);
  });

  it('moves nodes by storing visual position in node config', () => {
    const container = { dataset: {} } as HTMLElement;
    const changes: unknown[] = [];
    const designer = createWorkflowDesignerCore({
      container,
      value: {
        nodes: [
          { id: 'start', type: 'START', name: 'Start', config: {} },
          { id: 'end', type: 'END', name: 'End', config: {} }
        ],
        edges: [{ id: 'edge-1', sourceNodeId: 'start', targetNodeId: 'end', condition: null }],
        variables: []
      },
      onChange: (value) => changes.push(value)
    });

    designer.moveNode('end', 360, 160);

    expect(designer.getValue().nodes[1].config).toMatchObject({
      ui: { position: { x: 360, y: 160 } }
    });
    expect(changes).toHaveLength(1);
  });

  it('uses stored visual positions when creating layout', () => {
    const layout = createWorkflowDesignerLayout({
      nodes: [
        { id: 'start', type: 'START', name: 'Start', config: { ui: { position: { x: 88, y: 44 } } } },
        { id: 'end', type: 'END', name: 'End', config: { ui: { position: { x: 420, y: 120 } } } }
      ],
      edges: [{ id: 'edge-1', sourceNodeId: 'start', targetNodeId: 'end', condition: null }],
      variables: []
    });

    expect(layout.nodes[0]).toMatchObject({ id: 'start', x: 88, y: 44 });
    expect(layout.nodes[1]).toMatchObject({ id: 'end', x: 420, y: 120 });
    expect(layout.edges[0]).toMatchObject({ sourceX: 268, targetX: 420 });
  });

  it('removes individual edges without deleting connected nodes', () => {
    const designer = createWorkflowDesignerCore({
      container: { dataset: {} } as HTMLElement,
      value: {
        nodes: [
          { id: 'start', type: 'START', name: 'Start', config: {} },
          { id: 'end', type: 'END', name: 'End', config: {} }
        ],
        edges: [{ id: 'edge-1', sourceNodeId: 'start', targetNodeId: 'end', condition: null }],
        variables: []
      }
    });

    designer.removeEdge('edge-1');

    expect(designer.getValue().nodes).toHaveLength(2);
    expect(designer.getValue().edges).toHaveLength(0);
  });

  it('auto layouts nodes by storing deterministic positions in node config', () => {
    const designer = createWorkflowDesignerCore({
      container: { dataset: {} } as HTMLElement,
      value: {
        nodes: [
          { id: 'start', type: 'START', name: 'Start', config: { ui: { position: { x: 900, y: 900 } } } },
          { id: 'prompt', type: 'PROMPT', name: 'Prompt', config: {} },
          { id: 'end', type: 'END', name: 'End', config: {} }
        ],
        edges: [
          { id: 'edge-1', sourceNodeId: 'start', targetNodeId: 'prompt', condition: null },
          { id: 'edge-2', sourceNodeId: 'prompt', targetNodeId: 'end', condition: null }
        ],
        variables: []
      }
    });

    designer.autoLayout();

    expect(designer.getValue().nodes).toEqual([
      expect.objectContaining({ id: 'start', config: expect.objectContaining({ ui: { position: { x: 32, y: 32 } } }) }),
      expect.objectContaining({ id: 'prompt', config: expect.objectContaining({ ui: { position: { x: 262, y: 32 } } }) }),
      expect.objectContaining({ id: 'end', config: expect.objectContaining({ ui: { position: { x: 492, y: 32 } } }) })
    ]);
  });
});
