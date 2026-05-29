import {
  createWorkflowDesignerCore,
  createWorkflowDesignerLayout,
  type WorkflowDesignerCore
} from '@aiworkflow/workflow-designer-core';
import type { WorkflowDefinition, WorkflowNode } from '@aiworkflow/workflow-schema';
import type React from 'react';
import { forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react';

export interface WorkflowDesignerReactProps {
  value: WorkflowDefinition;
  readonly?: boolean;
  onChange?: (value: WorkflowDefinition) => void;
  onNodeSelect?: (nodeId: string) => void;
}

export interface WorkflowDesignerHandle {
  addNode(node: WorkflowNode): void;
  connectNodes(sourceNodeId: string, targetNodeId: string): void;
  getValue(): WorkflowDefinition;
}

export const WorkflowDesignerReact = forwardRef<WorkflowDesignerHandle, WorkflowDesignerReactProps>(
function WorkflowDesignerReact(props, ref) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const designerRef = useRef<WorkflowDesignerCore | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [connectingFromNodeId, setConnectingFromNodeId] = useState<string | null>(null);
  const [draggingNode, setDraggingNode] = useState<{
    nodeId: string;
    startClientX: number;
    startClientY: number;
    startX: number;
    startY: number;
    x: number;
    y: number;
  } | null>(null);
  const layout = useMemo(() => createWorkflowDesignerLayout(props.value), [props.value]);

  useImperativeHandle(ref, () => ({
    addNode(node: WorkflowNode) {
      designerRef.current?.addNode(node);
    },
    connectNodes(sourceNodeId: string, targetNodeId: string) {
      designerRef.current?.connectNodes(createEdgeId(sourceNodeId, targetNodeId), sourceNodeId, targetNodeId);
    },
    getValue() {
      return designerRef.current?.getValue() ?? props.value;
    }
  }), [props.value]);

  useEffect(() => {
    if (!containerRef.current) {
      return;
    }

    const designer = createWorkflowDesignerCore({
      container: containerRef.current,
      value: props.value,
      readonly: props.readonly,
      onChange: props.onChange
    });

    designerRef.current = designer;
    designer.mount();
    return () => {
      designer.destroy();
      if (designerRef.current === designer) {
        designerRef.current = null;
      }
    };
  }, [props.value, props.readonly, props.onChange]);

  useEffect(() => {
    if (!draggingNode) {
      return;
    }
    const activeDrag = draggingNode;

    function handleMouseMove(event: MouseEvent) {
      setDraggingNode((current) => current ? {
        ...current,
        x: Math.max(current.startX + event.clientX - current.startClientX, 0),
        y: Math.max(current.startY + event.clientY - current.startClientY, 0)
      } : null);
    }

    function handleMouseUp(event: MouseEvent) {
      const nextX = Math.max(activeDrag.startX + event.clientX - activeDrag.startClientX, 0);
      const nextY = Math.max(activeDrag.startY + event.clientY - activeDrag.startClientY, 0);
      designerRef.current?.moveNode(activeDrag.nodeId, nextX, nextY);
      setDraggingNode(null);
    }

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingNode]);

  function handleInputHandleClick(targetNodeId: string) {
    if (!connectingFromNodeId || connectingFromNodeId === targetNodeId || props.readonly) {
      return;
    }
    designerRef.current?.connectNodes(createEdgeId(connectingFromNodeId, targetNodeId), connectingFromNodeId, targetNodeId);
    setConnectingFromNodeId(null);
  }

  return (
    <div ref={containerRef} style={containerStyle} data-readonly={props.readonly ? 'true' : 'false'}>
      <div style={{ ...canvasStyle, minWidth: layout.bounds.width, minHeight: layout.bounds.height }}>
        <svg
          width={layout.bounds.width}
          height={layout.bounds.height}
          viewBox={`0 0 ${layout.bounds.width} ${layout.bounds.height}`}
          style={edgeLayerStyle}
        >
          <defs>
            <marker id="aiworkflow-arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto" markerUnits="strokeWidth">
              <path d="M0,0 L0,6 L9,3 z" fill="#98a2b3" />
            </marker>
          </defs>
          {layout.edges.map((edge) => (
            <path
              key={edge.id}
              d={edge.path}
              fill="none"
              stroke="#b8c2d2"
              strokeWidth={2}
              markerEnd="url(#aiworkflow-arrow)"
            />
          ))}
        </svg>

        {layout.nodes.map((node) => {
          const selected = selectedNodeId === node.id;
          const dragged = draggingNode?.nodeId === node.id ? draggingNode : null;
          const nodeX = dragged?.x ?? node.x;
          const nodeY = dragged?.y ?? node.y;
          return (
            <button
              key={node.id}
              type="button"
              aria-label={`节点 ${node.name}`}
              style={{
                ...nodeStyle,
                ...nodeToneStyle(node.type),
                borderColor: selected ? '#1677ff' : '#d8e0ec',
                boxShadow: selected ? '0 0 0 3px rgba(22, 119, 255, 0.14)' : '0 8px 20px rgba(15, 23, 42, 0.08)',
                height: node.height,
                left: nodeX,
                top: nodeY,
                width: node.width
              }}
              onMouseDown={(event) => {
                if (props.readonly) {
                  return;
                }
                setSelectedNodeId(node.id);
                designerRef.current?.selectNode(node.id);
                props.onNodeSelect?.(node.id);
                setDraggingNode({
                  nodeId: node.id,
                  startClientX: event.clientX,
                  startClientY: event.clientY,
                  startX: node.x,
                  startY: node.y,
                  x: node.x,
                  y: node.y
                });
              }}
              onClick={() => {
                designerRef.current?.selectNode(node.id);
                props.onNodeSelect?.(node.id);
                setSelectedNodeId(node.id);
              }}
            >
              <span
                role="button"
                aria-label={`连接到 ${node.name}`}
                title="连接到此节点"
                style={{ ...handleStyle, ...inputHandleStyle(connectingFromNodeId !== null) }}
                onMouseDown={(event) => event.stopPropagation()}
                onClick={(event) => {
                  event.stopPropagation();
                  handleInputHandleClick(node.id);
                }}
              />
              <span style={nodeTypeStyle}>{node.type}</span>
              <span style={nodeNameStyle}>{node.name}</span>
              <span
                role="button"
                aria-label={`从 ${node.name} 连线`}
                title="从此节点连线"
                style={{ ...handleStyle, ...outputHandleStyle(connectingFromNodeId === node.id) }}
                onMouseDown={(event) => event.stopPropagation()}
                onClick={(event) => {
                  event.stopPropagation();
                  if (!props.readonly) {
                    setConnectingFromNodeId((value) => value === node.id ? null : node.id);
                  }
                }}
              />
            </button>
          );
        })}
      </div>
    </div>
  );
});

const containerStyle: React.CSSProperties = {
  background: '#f8fafc',
  borderRadius: 8,
  height: '100%',
  minHeight: 320,
  overflow: 'auto',
  position: 'relative',
  width: '100%'
};

const canvasStyle: React.CSSProperties = {
  position: 'relative'
};

const edgeLayerStyle: React.CSSProperties = {
  inset: 0,
  pointerEvents: 'none',
  position: 'absolute'
};

const nodeStyle: React.CSSProperties = {
  alignItems: 'flex-start',
  background: '#fff',
  border: '1px solid #d8e0ec',
  borderRadius: 8,
  cursor: 'grab',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  padding: '12px 14px',
  position: 'absolute',
  textAlign: 'left'
};

const handleStyle: React.CSSProperties = {
  border: '2px solid #fff',
  borderRadius: 8,
  boxShadow: '0 0 0 1px #9aa7bb',
  height: 14,
  position: 'absolute',
  top: '50%',
  transform: 'translateY(-50%)',
  width: 14,
  zIndex: 2
};

function inputHandleStyle(active: boolean): React.CSSProperties {
  return {
    background: active ? '#1677ff' : '#d8e0ec',
    left: -7
  };
}

function outputHandleStyle(active: boolean): React.CSSProperties {
  return {
    background: active ? '#16a34a' : '#1677ff',
    right: -7
  };
}

const nodeTypeStyle: React.CSSProperties = {
  color: '#667085',
  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
  fontSize: 11,
  lineHeight: '16px'
};

const nodeNameStyle: React.CSSProperties = {
  color: '#101828',
  fontSize: 14,
  fontWeight: 700,
  lineHeight: '20px',
  marginTop: 4,
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  width: '100%'
};

function nodeToneStyle(type: WorkflowNode['type']): React.CSSProperties {
  const colorMap: Record<WorkflowNode['type'], string> = {
    CONDITION: '#fdf2f8',
    END: '#f1f5f9',
    HTTP_TOOL: '#fefce8',
    KNOWLEDGE_RETRIEVAL: '#f0fdf4',
    LLM: '#fff7ed',
    PROMPT: '#eef6ff',
    START: '#ecfdf3',
    TEXT_TRANSFORM: '#f5f3ff'
  };
  return {
    background: `linear-gradient(90deg, ${colorMap[type]} 0, #fff 34%)`
  };
}

function createEdgeId(sourceNodeId: string, targetNodeId: string) {
  return `edge_${sourceNodeId}_${targetNodeId}`;
}
