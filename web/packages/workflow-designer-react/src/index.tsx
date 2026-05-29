import {
  createWorkflowDesignerCore,
  createWorkflowDesignerLayout,
  type WorkflowDesignerCore
} from '@aiworkflow/workflow-designer-core';
import type { WorkflowDefinition, WorkflowEdge, WorkflowNode } from '@aiworkflow/workflow-schema';
import type React from 'react';
import { forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react';

export interface WorkflowDesignerReactProps {
  value: WorkflowDefinition;
  readonly?: boolean;
  selectedNodeId?: string | null;
  nodeRunStates?: Record<string, string>;
  onChange?: (value: WorkflowDefinition) => void;
  onNodeSelect?: (nodeId: string) => void;
}

export interface WorkflowDesignerHandle {
  addNode(node: WorkflowNode): void;
  duplicateNode(nodeId: string): WorkflowNode | null;
  connectNodes(sourceNodeId: string, targetNodeId: string): void;
  updateEdge(edgeId: string, patch: Partial<WorkflowEdge>): void;
  autoLayout(): void;
  removeNode(nodeId: string): void;
  removeEdge(edgeId: string): void;
  getValue(): WorkflowDefinition;
}

export const WorkflowDesignerReact = forwardRef<WorkflowDesignerHandle, WorkflowDesignerReactProps>(
function WorkflowDesignerReact(props, ref) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const designerRef = useRef<WorkflowDesignerCore | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);
  const [connectingFromNodeId, setConnectingFromNodeId] = useState<string | null>(null);
  const [zoom, setZoom] = useState(1);
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
  const effectiveSelectedNodeId = props.selectedNodeId === undefined ? selectedNodeId : props.selectedNodeId;
  const selectedEdge = layout.edges.find((edge) => edge.id === selectedEdgeId) ?? null;

  useImperativeHandle(ref, () => ({
    addNode(node: WorkflowNode) {
      designerRef.current?.addNode(node);
    },
    duplicateNode(nodeId: string) {
      return designerRef.current?.duplicateNode(nodeId) ?? null;
    },
    connectNodes(sourceNodeId: string, targetNodeId: string) {
      designerRef.current?.connectNodes(createEdgeId(sourceNodeId, targetNodeId), sourceNodeId, targetNodeId);
    },
    updateEdge(edgeId: string, patch: Partial<WorkflowEdge>) {
      designerRef.current?.updateEdge(edgeId, patch);
    },
    autoLayout() {
      designerRef.current?.autoLayout();
    },
    removeNode(nodeId: string) {
      designerRef.current?.removeNode(nodeId);
    },
    removeEdge(edgeId: string) {
      designerRef.current?.removeEdge(edgeId);
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

  function handleAutoLayout() {
    designerRef.current?.autoLayout();
    setSelectedEdgeId(null);
  }

  function handleDuplicateSelectedNode() {
    const nodeId = effectiveSelectedNodeId;
    if (!nodeId) {
      return;
    }
    const copy = designerRef.current?.duplicateNode(nodeId);
    if (copy) {
      setSelectedNodeId(copy.id);
      setSelectedEdgeId(null);
      props.onNodeSelect?.(copy.id);
    }
  }

  function handleStartConnectMode() {
    const nodeId = effectiveSelectedNodeId;
    if (!nodeId || props.readonly) {
      return;
    }
    setConnectingFromNodeId((current) => current === nodeId ? null : nodeId);
    setSelectedEdgeId(null);
  }

  function handleRemoveSelectedNode() {
    const nodeId = effectiveSelectedNodeId;
    if (!nodeId) {
      return;
    }
    designerRef.current?.removeNode(nodeId);
    setSelectedNodeId(null);
    setSelectedEdgeId(null);
  }

  function handleRemoveSelectedEdge() {
    if (!selectedEdgeId) {
      return;
    }
    designerRef.current?.removeEdge(selectedEdgeId);
    setSelectedEdgeId(null);
  }

  function handleEdgeConditionChange(value: string) {
    if (!selectedEdgeId) {
      return;
    }
    designerRef.current?.updateEdge(selectedEdgeId, { condition: value || null });
  }

  return (
    <div ref={containerRef} style={containerStyle} data-readonly={props.readonly ? 'true' : 'false'}>
      {!props.readonly ? (
        <div style={toolbarStyle}>
          <button type="button" aria-label="放大" style={iconButtonStyle} onClick={() => setZoom((value) => clampZoom(value + 0.1))}>
            +
          </button>
          <button type="button" aria-label="缩小" style={iconButtonStyle} onClick={() => setZoom((value) => clampZoom(value - 0.1))}>
            -
          </button>
          <button type="button" aria-label="适配视图" style={toolbarButtonStyle} onClick={() => setZoom(0.85)}>
            适配视图
          </button>
          <button type="button" aria-label="自动布局" style={toolbarButtonStyle} onClick={handleAutoLayout}>
            自动布局
          </button>
          <button
            type="button"
            aria-label="复制节点"
            style={{ ...toolbarButtonStyle, opacity: effectiveSelectedNodeId ? 1 : 0.46 }}
            onClick={handleDuplicateSelectedNode}
            disabled={!effectiveSelectedNodeId}
          >
            复制节点
          </button>
          <button
            type="button"
            aria-label="删除节点"
            style={{ ...toolbarButtonStyle, opacity: effectiveSelectedNodeId ? 1 : 0.46 }}
            onClick={handleRemoveSelectedNode}
            disabled={!effectiveSelectedNodeId}
          >
            删除节点
          </button>
          <button
            type="button"
            aria-label="删除连线"
            style={{ ...toolbarButtonStyle, opacity: selectedEdgeId ? 1 : 0.46 }}
            onClick={handleRemoveSelectedEdge}
            disabled={!selectedEdgeId}
          >
            删除连线
          </button>
          <button
            type="button"
            aria-label="连接节点"
            style={{
              ...toolbarButtonStyle,
              background: connectingFromNodeId ? '#e9f2ff' : '#fff',
              borderColor: connectingFromNodeId ? '#1677ff' : '#d8e0ec',
              color: connectingFromNodeId ? '#175cd3' : '#344054',
              opacity: effectiveSelectedNodeId ? 1 : 0.46
            }}
            onClick={handleStartConnectMode}
            disabled={!effectiveSelectedNodeId}
          >
            连接节点
          </button>
          {selectedEdge ? (
            <label style={edgeConditionStyle}>
              <span style={edgeConditionLabelStyle}>连线条件</span>
              <input
                aria-label="连线条件"
                value={String(selectedEdge.condition ?? '')}
                onChange={(event) => handleEdgeConditionChange(event.target.value)}
                placeholder="例如 intent == refund"
                style={edgeConditionInputStyle}
              />
            </label>
          ) : null}
          {connectingFromNodeId ? <span style={connectHintStyle}>选择目标节点</span> : null}
        </div>
      ) : null}
      <div
        aria-label="工作流画布视口"
        data-zoom={formatZoom(zoom)}
        style={{ ...viewportStyle, minWidth: layout.bounds.width * zoom, minHeight: layout.bounds.height * zoom }}
      >
        <div
          style={{
            ...canvasStyle,
            minWidth: layout.bounds.width,
            minHeight: layout.bounds.height,
            transform: `scale(${zoom})`,
            transformOrigin: '0 0'
          }}
        >
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
                aria-label={`选择连线 ${edge.id}`}
                role="button"
                d={edge.path}
                fill="none"
                stroke={selectedEdgeId === edge.id ? '#1677ff' : '#7f90a8'}
                strokeWidth={selectedEdgeId === edge.id ? 4 : 2.5}
                markerEnd="url(#aiworkflow-arrow)"
                style={{ cursor: props.readonly ? 'default' : 'pointer', pointerEvents: 'stroke' }}
                onClick={() => {
                  if (!props.readonly) {
                    setSelectedEdgeId(edge.id);
                    setSelectedNodeId(null);
                  }
                }}
              />
            ))}
          </svg>

          {layout.nodes.map((node) => {
            const selected = effectiveSelectedNodeId === node.id;
            const dragged = draggingNode?.nodeId === node.id ? draggingNode : null;
            const nodeX = dragged?.x ?? node.x;
            const nodeY = dragged?.y ?? node.y;
            const runState = props.nodeRunStates?.[node.id];
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
                  width: node.width,
                  zIndex: selected ? 3 : 2
                }}
                onMouseDown={(event) => {
                  if (props.readonly) {
                    return;
                  }
                  setSelectedNodeId(node.id);
                  setSelectedEdgeId(null);
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
                  if (connectingFromNodeId && connectingFromNodeId !== node.id && !props.readonly) {
                    designerRef.current?.connectNodes(createEdgeId(connectingFromNodeId, node.id), connectingFromNodeId, node.id);
                    setConnectingFromNodeId(null);
                    setSelectedNodeId(node.id);
                    setSelectedEdgeId(null);
                    designerRef.current?.selectNode(node.id);
                    props.onNodeSelect?.(node.id);
                    return;
                  }
                  designerRef.current?.selectNode(node.id);
                  props.onNodeSelect?.(node.id);
                  setSelectedNodeId(node.id);
                  setSelectedEdgeId(null);
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
                {runState ? <span style={{ ...statusBadgeStyle, ...statusToneStyle(runState) }}>{runState}</span> : null}
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
  minHeight: 320,
  position: 'relative'
};

const viewportStyle: React.CSSProperties = {
  position: 'relative'
};

const edgeLayerStyle: React.CSSProperties = {
  inset: 0,
  pointerEvents: 'auto',
  position: 'absolute'
};

const toolbarStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 8,
  justifyContent: 'flex-end',
  padding: 10,
  position: 'sticky',
  top: 0,
  zIndex: 5
};

const toolbarButtonStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #d8e0ec',
  borderRadius: 6,
  color: '#344054',
  cursor: 'pointer',
  fontSize: 12,
  fontWeight: 700,
  height: 30,
  padding: '0 10px'
};

const iconButtonStyle: React.CSSProperties = {
  ...toolbarButtonStyle,
  fontSize: 16,
  padding: 0,
  width: 30
};

const edgeConditionStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 6,
  marginLeft: 4
};

const edgeConditionLabelStyle: React.CSSProperties = {
  color: '#667085',
  fontSize: 12,
  fontWeight: 700
};

const edgeConditionInputStyle: React.CSSProperties = {
  border: '1px solid #d8e0ec',
  borderRadius: 6,
  height: 30,
  padding: '0 8px',
  width: 180
};

const connectHintStyle: React.CSSProperties = {
  background: '#ecfdf3',
  border: '1px solid #abefc6',
  borderRadius: 999,
  color: '#027a48',
  fontSize: 12,
  fontWeight: 800,
  lineHeight: '24px',
  padding: '0 10px'
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

const statusBadgeStyle: React.CSSProperties = {
  borderRadius: 999,
  fontSize: 10,
  fontWeight: 800,
  lineHeight: '16px',
  padding: '0 7px',
  position: 'absolute',
  right: 10,
  top: 8
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

function clampZoom(value: number) {
  return Math.min(Math.max(Number(value.toFixed(2)), 0.5), 1.6);
}

function formatZoom(value: number) {
  return String(Number(value.toFixed(2)));
}

function statusToneStyle(status: string): React.CSSProperties {
  if (status === 'FAILED') {
    return { background: '#fef2f2', color: '#b42318' };
  }
  if (status === 'SUCCEEDED') {
    return { background: '#ecfdf3', color: '#027a48' };
  }
  if (status === 'RUNNING') {
    return { background: '#eff6ff', color: '#175cd3' };
  }
  return { background: '#f2f4f7', color: '#475467' };
}
