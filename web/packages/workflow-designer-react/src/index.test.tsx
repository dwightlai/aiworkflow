// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { WorkflowDesignerReact } from './index';
import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';

afterEach(() => {
  cleanup();
});

describe('WorkflowDesignerReact', () => {
  it('emits node positions and edges when users drag nodes and connect ports', () => {
    const onChange = vi.fn();
    render(<WorkflowDesignerReact value={createDefinition()} onChange={onChange} />);

    fireEvent.mouseDown(screen.getByRole('button', { name: '节点 开始' }), { clientX: 32, clientY: 32 });
    fireEvent.mouseMove(window, { clientX: 112, clientY: 72 });
    fireEvent.mouseUp(window, { clientX: 112, clientY: 72 });

    fireEvent.click(screen.getByLabelText('从 开始 连线'));
    fireEvent.click(screen.getByLabelText('连接到 结束'));

    const latestDefinition = onChange.mock.calls.at(-1)?.[0] as WorkflowDefinition;
    expect(latestDefinition.nodes).toContainEqual(expect.objectContaining({
      id: 'start',
      config: expect.objectContaining({
        ui: expect.objectContaining({
          position: { x: 112, y: 72 }
        })
      })
    }));
    expect(latestDefinition.edges).toContainEqual(expect.objectContaining({
      id: 'edge_start_end',
      sourceNodeId: 'start',
      targetNodeId: 'end'
    }));
  });

  it('lets users select and remove edges and nodes from the canvas toolbar', () => {
    const onChange = vi.fn();
    render(<WorkflowDesignerReact value={createDefinition({
      edges: [{ id: 'edge_start_end', sourceNodeId: 'start', targetNodeId: 'end', condition: null }]
    })} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText('选择连线 edge_start_end'));
    fireEvent.click(screen.getByRole('button', { name: '删除连线' }));

    expect((onChange.mock.calls.at(-1)?.[0] as WorkflowDefinition).edges).toHaveLength(0);

    fireEvent.click(screen.getByRole('button', { name: '节点 结束' }));
    fireEvent.click(screen.getByRole('button', { name: '删除节点' }));

    const latestDefinition = onChange.mock.calls.at(-1)?.[0] as WorkflowDefinition;
    expect(latestDefinition.nodes.map((node) => node.id)).toEqual(['start']);
    expect(latestDefinition.edges).toHaveLength(0);
  });

  it('auto layouts the current graph from the canvas toolbar', () => {
    const onChange = vi.fn();
    render(<WorkflowDesignerReact value={createDefinition({
      nodes: [
        { id: 'start', type: 'START', name: '开始', config: { ui: { position: { x: 900, y: 900 } } } },
        { id: 'end', type: 'END', name: '结束', config: {} }
      ],
      edges: [{ id: 'edge_start_end', sourceNodeId: 'start', targetNodeId: 'end', condition: null }]
    })} onChange={onChange} />);

    fireEvent.click(screen.getByRole('button', { name: '自动布局' }));

    const latestDefinition = onChange.mock.calls.at(-1)?.[0] as WorkflowDefinition;
    expect(latestDefinition.nodes).toContainEqual(expect.objectContaining({
      id: 'start',
      config: expect.objectContaining({ ui: { position: { x: 32, y: 32 } } })
    }));
    expect(latestDefinition.nodes).toContainEqual(expect.objectContaining({
      id: 'end',
      config: expect.objectContaining({ ui: { position: { x: 262, y: 32 } } })
    }));
  });
});

function createDefinition(patch: Partial<WorkflowDefinition> = {}): WorkflowDefinition {
  return {
    nodes: [
      { id: 'start', type: 'START', name: '开始', config: {} },
      { id: 'end', type: 'END', name: '结束', config: {} }
    ],
    edges: [],
    variables: [],
    ...patch
  };
}
