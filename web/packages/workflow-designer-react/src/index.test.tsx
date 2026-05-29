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
});

function createDefinition(): WorkflowDefinition {
  return {
    nodes: [
      { id: 'start', type: 'START', name: '开始', config: {} },
      { id: 'end', type: 'END', name: '结束', config: {} }
    ],
    edges: [],
    variables: []
  };
}
