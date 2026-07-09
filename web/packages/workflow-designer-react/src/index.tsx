import {
  createWorkflowDesignerCore,
  createWorkflowDesignerLayout,
  type WorkflowDesignerCore
} from '@aiworkflow/workflow-designer-core';
import type { WorkflowDefinition, WorkflowEdge, WorkflowNode } from '@aiworkflow/workflow-schema';
import type React from 'react';
import { forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

export interface WorkflowNodeExecutionView {
  status: string;
  input?: Record<string, unknown>;
  output?: Record<string, unknown>;
  errorMessage?: string | null;
  durationMs?: number | null;
}

export interface WorkflowDesignerReactProps {
  value: WorkflowDefinition;
  readonly?: boolean;
  selectedNodeId?: string | null;
  nodeRunStates?: Record<string, string>;
  nodeExecutions?: Record<string, WorkflowNodeExecutionView>;
  onChange?: (value: WorkflowDefinition) => void;
  onNodeSelect?: (nodeId: string) => void;
  onEdgeSelect?: (edgeId: string) => void;
  onCanvasSelect?: () => void;
  hideToolbar?: boolean;
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
  zoomIn(): void;
  zoomOut(): void;
  fitView(): void;
}

export const WorkflowDesignerReact = forwardRef<WorkflowDesignerHandle, WorkflowDesignerReactProps>(
function WorkflowDesignerReact(props, ref) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const viewportRef = useRef<HTMLDivElement | null>(null);
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
  const [draggingEdge, setDraggingEdge] = useState<{
    sourceNodeId: string;
    startX: number;
    startY: number;
    currentX: number;
    currentY: number;
  } | null>(null);
  const [panningCanvas, setPanningCanvas] = useState<{
    startClientX: number;
    startClientY: number;
    startOffsetX: number;
    startOffsetY: number;
  } | null>(null);
  const [spacePressed, setSpacePressed] = useState(false);
  const [canvasOffset, setCanvasOffset] = useState({ x: 0, y: 0 });
  const [viewportMetrics, setViewportMetrics] = useState({
    clientWidth: 0,
    clientHeight: 0
  });
  const canvasOffsetRef = useRef(canvasOffset);
  canvasOffsetRef.current = canvasOffset;
  const zoomRef = useRef(zoom);
  zoomRef.current = zoom;
  const layout = useMemo(() => createWorkflowDesignerLayout(props.value), [props.value]);
  const layoutRef = useRef(layout);
  layoutRef.current = layout;
  const draggingEdgeRef = useRef(draggingEdge);
  draggingEdgeRef.current = draggingEdge;
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
    },
    zoomIn() {
      setZoom((value) => clampZoom(value + 0.1));
    },
    zoomOut() {
      setZoom((value) => clampZoom(value - 0.1));
    },
    fitView() {
      setZoom(0.85);
      setCanvasOffset({ x: 0, y: 0 });
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
        x: Math.max(current.startX + (event.clientX - current.startClientX) / zoom, 0),
        y: Math.max(current.startY + (event.clientY - current.startClientY) / zoom, 0)
      } : null);
    }

    function handleMouseUp(event: MouseEvent) {
      const nextX = Math.max(activeDrag.startX + (event.clientX - activeDrag.startClientX) / zoom, 0);
      const nextY = Math.max(activeDrag.startY + (event.clientY - activeDrag.startClientY) / zoom, 0);
      designerRef.current?.moveNode(activeDrag.nodeId, nextX, nextY);
      setDraggingNode(null);
    }

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingNode, zoom]);

  useEffect(() => {
    if (!draggingEdge) {
      return;
    }

    function handleMouseMove(event: MouseEvent) {
      const activeDrag = draggingEdgeRef.current;
      if (!activeDrag) {
        return;
      }
      const viewportRect = viewportRef.current?.getBoundingClientRect() ?? null;
      const snap = findConnectionSnapAtPoint(
        event.clientX,
        event.clientY,
        layoutRef.current,
        zoom,
        activeDrag.sourceNodeId,
        viewportRect,
        canvasOffsetRef.current
      );
      const point = snap ?? resolveCanvasPoint(event.clientX, event.clientY, zoom);
      setDraggingEdge((current) => current ? {
        ...current,
        currentX: point.x,
        currentY: point.y
      } : null);
    }

    function handleMouseUp(event: MouseEvent) {
      const activeDrag = draggingEdgeRef.current;
      if (activeDrag && !props.readonly) {
        const viewportRect = viewportRef.current?.getBoundingClientRect() ?? null;
        const snap = findConnectionSnapAtPoint(
          event.clientX,
          event.clientY,
          layoutRef.current,
          zoom,
          activeDrag.sourceNodeId,
          viewportRect,
          canvasOffsetRef.current
        );
        const targetNodeId = snap?.nodeId ?? resolveConnectionTargetNodeId(
          event.clientX,
          event.clientY,
          layoutRef.current,
          zoom,
          activeDrag.sourceNodeId,
          viewportRect,
          canvasOffsetRef.current
        );
        if (targetNodeId && targetNodeId !== activeDrag.sourceNodeId) {
          designerRef.current?.connectNodes(
            createEdgeId(activeDrag.sourceNodeId, targetNodeId),
            activeDrag.sourceNodeId,
            targetNodeId
          );
        }
      }
      setDraggingEdge(null);
      setConnectingFromNodeId(null);
      setSelectedEdgeId(null);
    }

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingEdge, zoom, props.readonly]);

  useEffect(() => {
    if (!panningCanvas) {
      return;
    }
    const activePan = panningCanvas;

    function handleMouseMove(event: MouseEvent) {
      setCanvasOffset({
        x: activePan.startOffsetX + (event.clientX - activePan.startClientX),
        y: activePan.startOffsetY + (event.clientY - activePan.startClientY)
      });
    }

    function handleMouseUp() {
      setPanningCanvas(null);
    }

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [panningCanvas]);

  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) {
      return;
    }
    const updateViewportMetrics = () => {
      setViewportMetrics({
        clientWidth: viewport.clientWidth,
        clientHeight: viewport.clientHeight
      });
    };
    updateViewportMetrics();
    window.addEventListener('resize', updateViewportMetrics);
    return () => {
      window.removeEventListener('resize', updateViewportMetrics);
    };
  }, [layout.bounds.width, layout.bounds.height, zoom, canvasOffset.x, canvasOffset.y]);

  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) {
      return;
    }

    const viewportElement = viewport;

    function handleWheel(event: WheelEvent) {
      if (isTypingTarget(event.target)) {
        return;
      }
      event.preventDefault();
      event.stopPropagation();

      const rect = viewportElement.getBoundingClientRect();
      const oldZoom = zoomRef.current;
      const zoomDelta = -event.deltaY * 0.0015;
      const newZoom = clampZoom(oldZoom + zoomDelta);
      if (newZoom === oldZoom) {
        return;
      }
      const offset = canvasOffsetRef.current;
      const canvasX = (event.clientX - rect.left - offset.x) / oldZoom;
      const canvasY = (event.clientY - rect.top - offset.y) / oldZoom;
      zoomRef.current = newZoom;
      setZoom(newZoom);
      setCanvasOffset({
        x: event.clientX - rect.left - canvasX * newZoom,
        y: event.clientY - rect.top - canvasY * newZoom
      });
    }

    viewport.addEventListener('wheel', handleWheel, { passive: false });
    return () => viewport.removeEventListener('wheel', handleWheel);
  }, []);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.code !== 'Space' || isTypingTarget(event.target)) {
        return;
      }
      event.preventDefault();
      setSpacePressed(true);
    }

    function handleKeyUp(event: KeyboardEvent) {
      if (event.code === 'Space') {
        setSpacePressed(false);
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, []);

  function handleInputHandleClick(targetNodeId: string) {
    if (!connectingFromNodeId || connectingFromNodeId === targetNodeId || props.readonly) {
      return;
    }
    connectNodes(connectingFromNodeId, targetNodeId);
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

  function handleOutputHandleMouseDown(event: React.MouseEvent, node: ReturnType<typeof createWorkflowDesignerLayout>['nodes'][number]) {
    event.preventDefault();
    event.stopPropagation();
    if (props.readonly) {
      return;
    }
    setConnectingFromNodeId(node.id);
    setSelectedEdgeId(null);
    setDraggingEdge({
      sourceNodeId: node.id,
      startX: node.x + node.width,
      startY: node.y + node.height / 2,
      currentX: node.x + node.width,
      currentY: node.y + node.height / 2
    });
  }

  function handleInputHandleMouseUp(event: React.MouseEvent, targetNodeId: string) {
    event.preventDefault();
    event.stopPropagation();
    if (!draggingEdge || draggingEdge.sourceNodeId === targetNodeId || props.readonly) {
      return;
    }
    connectNodes(draggingEdge.sourceNodeId, targetNodeId);
  }

  function handleNodeMouseUp(event: React.MouseEvent, targetNodeId: string) {
    if (!draggingEdge || draggingEdge.sourceNodeId === targetNodeId || props.readonly) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    connectNodes(draggingEdge.sourceNodeId, targetNodeId);
  }

  function connectNodes(sourceNodeId: string, targetNodeId: string) {
    designerRef.current?.connectNodes(createEdgeId(sourceNodeId, targetNodeId), sourceNodeId, targetNodeId);
    setDraggingEdge(null);
    setConnectingFromNodeId(null);
    setSelectedEdgeId(null);
  }

  function resolveCanvasPoint(clientX: number, clientY: number, currentZoom: number) {
    const rect = viewportRef.current?.getBoundingClientRect();
    const offset = canvasOffsetRef.current;
    if (!rect) {
      return { x: clientX / currentZoom, y: clientY / currentZoom };
    }
    return {
      x: (clientX - rect.left - offset.x) / currentZoom,
      y: (clientY - rect.top - offset.y) / currentZoom
    };
  }

  function startCanvasPan(event: React.MouseEvent | MouseEvent) {
    event.preventDefault();
    setSelectedNodeId(null);
    setSelectedEdgeId(null);
    props.onCanvasSelect?.();
    setPanningCanvas({
      startClientX: event.clientX,
      startClientY: event.clientY,
      startOffsetX: canvasOffset.x,
      startOffsetY: canvasOffset.y
    });
  }

  function shouldPanCanvas(event: React.MouseEvent, target: EventTarget) {
    if (draggingEdge || draggingNode) {
      return false;
    }
    if (event.button === 1) {
      return true;
    }
    if (spacePressed) {
      return true;
    }
    return event.button === 0 && !isInteractiveCanvasTarget(target);
  }

  function handleCanvasPanMouseDown(event: React.MouseEvent<HTMLDivElement>) {
    if (!shouldPanCanvas(event, event.target)) {
      return;
    }
    event.stopPropagation();
    startCanvasPan(event);
  }

  function fitView() {
    setZoom(0.85);
    setCanvasOffset({ x: 0, y: 0 });
  }

  function panCanvasToCanvasPoint(canvasX: number, canvasY: number) {
    const viewport = viewportRef.current;
    if (!viewport) {
      return;
    }
    setCanvasOffset({
      x: viewport.clientWidth / 2 - canvasX * zoom,
      y: viewport.clientHeight / 2 - canvasY * zoom
    });
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

  function selectEdge(edgeId: string) {
    if (props.readonly) {
      return;
    }
    setSelectedEdgeId(edgeId);
    setSelectedNodeId(null);
    props.onEdgeSelect?.(edgeId);
  }

  return (
    <div ref={containerRef} style={containerStyle} data-readonly={props.readonly ? 'true' : 'false'}>
      {!props.readonly && !props.hideToolbar ? (
        <div style={toolbarStyle} data-workflow-interactive="true">
          <button type="button" aria-label="放大" style={iconButtonStyle} onClick={() => setZoom((value) => clampZoom(value + 0.1))}>
            +
          </button>
          <button type="button" aria-label="缩小" style={iconButtonStyle} onClick={() => setZoom((value) => clampZoom(value - 0.1))}>
            -
          </button>
          <button type="button" aria-label="适配视图" style={toolbarButtonStyle} onClick={fitView}>
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
          {draggingEdge ? <span style={connectHintStyle}>拖到目标节点松开</span> : null}
        </div>
      ) : null}
      <div
        ref={viewportRef}
        aria-label="工作流画布视口"
        data-zoom={formatZoom(zoom)}
        onMouseDown={handleCanvasPanMouseDown}
        style={{
          ...viewportStyle,
          cursor: panningCanvas || spacePressed ? 'grabbing' : 'grab'
        }}
      >
        <div
          data-canvas-offset-x={Math.round(canvasOffset.x)}
          data-canvas-offset-y={Math.round(canvasOffset.y)}
          style={{
            position: 'absolute',
            left: 0,
            top: 0,
            transform: `translate(${canvasOffset.x}px, ${canvasOffset.y}px)`,
            transformOrigin: '0 0'
          }}
        >
          <div
            data-workflow-canvas="true"
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
            style={{
              ...edgeLayerStyle,
              pointerEvents: draggingEdge ? 'none' : 'auto'
            }}
          >
            <defs>
              <marker
                id="aiworkflow-arrow"
                viewBox="0 0 12 12"
                refX="10"
                refY="6"
                markerWidth="14"
                markerHeight="14"
                orient="auto"
                markerUnits="userSpaceOnUse"
                overflow="visible"
              >
                <path d="M1.5,2 L10,6 L1.5,10 Z" fill="#667085" />
              </marker>
              <marker
                id="aiworkflow-arrow-active"
                viewBox="0 0 12 12"
                refX="10"
                refY="6"
                markerWidth="14"
                markerHeight="14"
                orient="auto"
                markerUnits="userSpaceOnUse"
                overflow="visible"
              >
                <path d="M1.5,2 L10,6 L1.5,10 Z" fill="#1677ff" />
              </marker>
            </defs>
            {layout.edges.map((edge) => (
              <g key={edge.id}>
                <path
                  aria-label={`选择连线 ${edge.id}`}
                  data-workflow-interactive="true"
                  role="button"
                  d={edge.path}
                  fill="none"
                  stroke="transparent"
                  strokeWidth={22}
                  style={{ cursor: props.readonly ? 'default' : 'pointer', pointerEvents: 'stroke' }}
                  onClick={() => selectEdge(edge.id)}
                />
                <path
                  aria-hidden="true"
                  d={edge.path}
                  fill="none"
                  stroke={selectedEdgeId === edge.id ? '#1677ff' : '#667085'}
                  strokeWidth={2.5}
                  strokeLinecap="round"
                  markerEnd={selectedEdgeId === edge.id ? 'url(#aiworkflow-arrow-active)' : 'url(#aiworkflow-arrow)'}
                  style={{ pointerEvents: 'none' }}
                />
              </g>
            ))}
            {draggingEdge ? (
              <path
                aria-label="正在拖拽连线"
                d={createPreviewPath(draggingEdge.startX, draggingEdge.startY, draggingEdge.currentX, draggingEdge.currentY)}
                fill="none"
                markerEnd="url(#aiworkflow-arrow-active)"
                stroke="#1677ff"
                strokeDasharray="6 5"
                strokeLinecap="round"
                strokeWidth={2.5}
                style={{ pointerEvents: 'none' }}
              />
            ) : null}
          </svg>

          {layout.nodes.map((node) => {
            const selected = effectiveSelectedNodeId === node.id;
            const dragged = draggingNode?.nodeId === node.id ? draggingNode : null;
            const nodeX = dragged?.x ?? node.x;
            const nodeY = dragged?.y ?? node.y;
            const executionView = props.nodeExecutions?.[node.id];
            const runState = executionView?.status ?? props.nodeRunStates?.[node.id];
            return (
              <div
                key={node.id}
                data-workflow-node-id={node.id}
                style={{
                  left: nodeX,
                  position: 'absolute',
                  top: nodeY,
                  width: node.width,
                  zIndex: selected ? 3 : 2
                }}
              >
                <button
                  type="button"
                  data-workflow-interactive="true"
                  aria-label={`节点 ${node.name}`}
                  style={{
                    ...nodeStyle,
                    ...nodeToneStyle(node.type),
                    borderColor: selected ? '#1677ff' : '#d8e0ec',
                    boxShadow: selected ? '0 0 0 3px rgba(22, 119, 255, 0.14)' : '0 8px 20px rgba(15, 23, 42, 0.08)',
                    cursor: spacePressed ? 'grab' : 'grab',
                    height: node.height,
                    position: 'relative',
                    width: '100%'
                  }}
                  onMouseDown={(event) => {
                    if (draggingEdge) {
                      return;
                    }
                    if (shouldPanCanvas(event, event.target)) {
                      startCanvasPan(event);
                      return;
                    }
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
                  onMouseUp={(event) => handleNodeMouseUp(event, node.id)}
                  onClick={() => {
                    if (connectingFromNodeId && connectingFromNodeId !== node.id && !props.readonly) {
                      connectNodes(connectingFromNodeId, node.id);
                      setSelectedNodeId(node.id);
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
                    data-workflow-interactive="true"
                    data-workflow-port="input"
                    data-workflow-node-id={node.id}
                    aria-label={`连接到 ${node.name}`}
                    title="连接到此节点"
                    style={{ ...handleStyle, ...inputHandleStyle(connectingFromNodeId !== null) }}
                    onMouseDown={(event) => event.stopPropagation()}
                    onMouseUp={(event) => handleInputHandleMouseUp(event, node.id)}
                    onClick={(event) => {
                      event.stopPropagation();
                      handleInputHandleClick(node.id);
                    }}
                  />
                  {runState && !executionView ? <span style={{ ...statusBadgeStyle, ...statusToneStyle(runState) }}>{runState}</span> : null}
                  <span style={nodeTypeStyle}>{node.type}</span>
                  <span style={nodeNameStyle}>{node.name}</span>
                  <span
                    role="button"
                    data-workflow-interactive="true"
                    data-workflow-port="output"
                    data-workflow-node-id={node.id}
                    aria-label={`从 ${node.name} 连线`}
                    title="从此节点连线"
                    style={{ ...handleStyle, ...outputHandleStyle(connectingFromNodeId === node.id) }}
                    onMouseDown={(event) => handleOutputHandleMouseDown(event, node)}
                    onClick={(event) => {
                      event.stopPropagation();
                      if (!props.readonly) {
                        setConnectingFromNodeId((value) => value === node.id ? null : node.id);
                      }
                    }}
                  />
                </button>
                {executionView ? <NodeExecutionPanel execution={executionView} /> : null}
              </div>
            );
          })}
          </div>
        </div>
      </div>
      <CanvasNavigator
        layout={layout}
        zoom={zoom}
        canvasOffset={canvasOffset}
        viewport={viewportMetrics}
        onZoomIn={() => setZoom((value) => clampZoom(value + 0.1))}
        onZoomOut={() => setZoom((value) => clampZoom(value - 0.1))}
        onFitView={fitView}
        onPanTo={panCanvasToCanvasPoint}
      />
    </div>
  );
});

const containerStyle: React.CSSProperties = {
  background: '#f8fafc',
  borderRadius: 8,
  display: 'flex',
  flexDirection: 'column',
  height: '100%',
  minHeight: 320,
  overflow: 'hidden',
  position: 'relative',
  width: '100%'
};

const canvasStyle: React.CSSProperties = {
  minHeight: 320,
  position: 'relative'
};

const viewportStyle: React.CSSProperties = {
  backgroundColor: '#f8fafc',
  backgroundImage: 'radial-gradient(circle, #cbd5e1 1px, transparent 1px)',
  backgroundPosition: '0 0',
  backgroundSize: '20px 20px',
  flex: 1,
  minHeight: 0,
  overflow: 'hidden',
  overscrollBehavior: 'none',
  position: 'relative',
  touchAction: 'none',
  width: '100%'
};

const edgeLayerStyle: React.CSSProperties = {
  inset: 0,
  pointerEvents: 'auto',
  position: 'absolute'
};

const toolbarStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  borderBottom: '1px solid #e7ecf3',
  display: 'flex',
  flex: '0 0 auto',
  gap: 8,
  justifyContent: 'flex-end',
  padding: 10,
  position: 'relative',
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
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  padding: '12px 14px',
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
  zIndex: 4
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
    LOOP: '#f0f9ff',
    QUESTION_CLASSIFIER: '#fdf2f8',
    PROMPT: '#eef6ff',
    START: '#ecfdf3',
    CONTENT_TEMPLATE: '#f5f3ff',
    TEXT_TRANSFORM: '#f5f3ff'
  };
  return {
    background: `linear-gradient(90deg, ${colorMap[type]} 0, #fff 34%)`
  };
}

function createEdgeId(sourceNodeId: string, targetNodeId: string) {
  return `edge_${sourceNodeId}_${targetNodeId}`;
}

function createPreviewPath(startX: number, startY: number, endX: number, endY: number) {
  const controlOffset = Math.max(Math.abs(endX - startX) / 2, 60);
  return `M ${startX} ${startY} C ${startX + controlOffset} ${startY}, ${endX - controlOffset} ${endY}, ${endX} ${endY}`;
}

function clampZoom(value: number) {
  return Math.min(Math.max(Number(value.toFixed(2)), 0.5), 1.6);
}

function formatZoom(value: number) {
  return String(Number(value.toFixed(2)));
}

function isInteractiveCanvasTarget(target: EventTarget) {
  if (!(target instanceof Element)) {
    return false;
  }
  return Boolean(target.closest('[data-workflow-interactive="true"], input, label, textarea, select'));
}

function isTypingTarget(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) {
    return false;
  }
  const tag = target.tagName;
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || target.isContentEditable;
}

function NodeExecutionPanel({ execution }: { execution: WorkflowNodeExecutionView }) {
  const [panelOpen, setPanelOpen] = useState(true);
  const [detailOpen, setDetailOpen] = useState(false);
  const failed = execution.status === 'FAILED';
  const succeeded = execution.status === 'SUCCEEDED';
  const durationLabel = typeof execution.durationMs === 'number' ? `${execution.durationMs}ms` : '';
  const statusLabel = failed
    ? '结果异常'
    : succeeded
      ? '执行完成'
      : execution.status === 'RUNNING'
        ? '执行中'
        : execution.status;
  const statusText = failed ? '失败' : succeeded ? '成功' : execution.status === 'RUNNING' ? '运行中' : execution.status;
  const hasInput = Boolean(execution.input && Object.keys(execution.input).length > 0);
  const hasOutput = Boolean(execution.output && Object.keys(execution.output).length > 0);
  const hasError = Boolean(execution.errorMessage);

  return (
    <>
      <div style={executionPanelStyle} data-workflow-interactive="true">
        <button
          type="button"
          aria-label={panelOpen ? '折叠执行结果' : '展开执行结果'}
          data-workflow-interactive="true"
          style={{
            ...executionHeaderStyle,
            ...(failed ? executionHeaderFailedStyle : succeeded ? executionHeaderSuccessStyle : executionHeaderRunningStyle),
            border: 'none',
            cursor: 'pointer',
            width: '100%'
          }}
          onMouseDown={(event) => event.stopPropagation()}
          onClick={() => setPanelOpen((value) => !value)}
        >
          <span style={executionHeaderMainStyle}>
            <span>{statusLabel}</span>
            {durationLabel ? <span>{durationLabel}</span> : null}
          </span>
          <ChevronIcon open={panelOpen} />
        </button>
        {panelOpen ? (
          <div>
            {hasInput ? (
              <ExecutionCollapsibleSection
                title="输入数据"
                preview={formatJsonPreview(execution.input!)}
                onExpand={() => setDetailOpen(true)}
              />
            ) : null}
            {hasOutput ? (
              <ExecutionCollapsibleSection
                title="输出结果"
                preview={formatJsonPreview(execution.output!)}
                onExpand={() => setDetailOpen(true)}
              />
            ) : null}
            {hasError ? (
              <ExecutionCollapsibleSection
                title="错误详情"
                preview={execution.errorMessage!}
                tone="error"
                onExpand={() => setDetailOpen(true)}
              />
            ) : null}
          </div>
        ) : null}
      </div>
      {detailOpen ? createPortal(
        <ExecutionDetailModal
          execution={execution}
          durationLabel={durationLabel}
          statusText={statusText}
          failed={failed}
          succeeded={succeeded}
          onClose={() => setDetailOpen(false)}
        />,
        document.body
      ) : null}
    </>
  );
}

function ExecutionCollapsibleSection({
  title,
  preview,
  tone = 'default',
  onExpand
}: {
  title: string;
  preview: string;
  tone?: 'default' | 'error';
  onExpand: () => void;
}) {
  const [open, setOpen] = useState(true);

  return (
    <div style={tone === 'error' ? executionErrorSectionStyle : executionBlockStyle}>
      <div style={executionSectionHeaderStyle}>
        <button
          type="button"
          aria-label={open ? `折叠${title}` : `展开${title}`}
          data-workflow-interactive="true"
          style={executionSectionToggleStyle}
          onMouseDown={(event) => event.stopPropagation()}
          onClick={() => setOpen((value) => !value)}
        >
          <span style={executionBlockTitleStyle}>{title}</span>
          <ChevronIcon open={open} />
        </button>
        <button
          type="button"
          aria-label={`放大查看${title}`}
          data-workflow-interactive="true"
          style={executionExpandButtonStyle}
          onMouseDown={(event) => event.stopPropagation()}
          onClick={onExpand}
        >
          <ExpandIcon />
        </button>
      </div>
      {open ? (
        <pre style={tone === 'error' ? executionErrorPreStyle : executionPreStyle}>{preview}</pre>
      ) : null}
    </div>
  );
}

function ExecutionDetailModal({
  execution,
  durationLabel,
  statusText,
  failed,
  succeeded,
  onClose
}: {
  execution: WorkflowNodeExecutionView;
  durationLabel: string;
  statusText: string;
  failed: boolean;
  succeeded: boolean;
  onClose: () => void;
}) {
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div style={executionModalOverlayStyle} data-workflow-interactive="true" onMouseDown={onClose}>
      <div
        role="dialog"
        aria-label="执行详情"
        style={executionModalStyle}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div style={executionModalHeaderStyle}>
          <span style={executionModalTitleStyle}>执行详情</span>
          <button type="button" aria-label="关闭" data-workflow-interactive="true" style={executionModalCloseStyle} onClick={onClose}>
            ×
          </button>
        </div>
        <div style={executionModalMetaStyle}>
          <span>耗时 <strong>{durationLabel || '-'}</strong></span>
          <span>
            状态{' '}
            <strong style={{ color: failed ? '#b42318' : succeeded ? '#027a48' : '#175cd3' }}>
              {statusText}
            </strong>
          </span>
        </div>
        {execution.input && Object.keys(execution.input).length > 0 ? (
          <ExecutionDetailSection title="输入参数" value={formatJsonPreview(execution.input)} />
        ) : null}
        {execution.output && Object.keys(execution.output).length > 0 ? (
          <ExecutionDetailSection title="执行输出" value={formatJsonPreview(execution.output)} tone="success" />
        ) : null}
        {execution.errorMessage ? (
          <ExecutionDetailSection title="错误追踪" value={execution.errorMessage} tone="error" />
        ) : null}
      </div>
    </div>
  );
}

function ExecutionDetailSection({
  title,
  value,
  tone = 'default'
}: {
  title: string;
  value: string;
  tone?: 'default' | 'success' | 'error';
}) {
  return (
    <div style={executionModalSectionStyle}>
      <div style={executionModalSectionTitleStyle}>
        <span style={{
          ...executionModalDotStyle,
          ...(tone === 'success' ? executionModalDotSuccessStyle : tone === 'error' ? executionModalDotErrorStyle : executionModalDotDefaultStyle)
        }} />
        <span>{title}</span>
      </div>
      <pre style={{
        ...executionModalPreStyle,
        ...(tone === 'error' ? executionModalPreErrorStyle : {})
      }}>
        {value}
      </pre>
    </div>
  );
}

function ChevronIcon({ open }: { open: boolean }) {
  return (
    <span aria-hidden="true" style={chevronStyle}>
      {open ? '⌃' : '⌄'}
    </span>
  );
}

function ExpandIcon() {
  return (
    <span aria-hidden="true" style={expandIconStyle}>
      ⤢
    </span>
  );
}

function formatJsonPreview(value: Record<string, unknown>) {
  return JSON.stringify(value, null, 2);
}

const executionPanelStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #d8e0ec',
  borderRadius: 8,
  boxShadow: '0 8px 20px rgba(15, 23, 42, 0.06)',
  marginTop: 8,
  overflow: 'hidden',
  width: '100%'
};

const executionHeaderStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  fontSize: 12,
  fontWeight: 700,
  justifyContent: 'space-between',
  padding: '8px 10px'
};

const executionHeaderMainStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 10
};

const executionHeaderSuccessStyle: React.CSSProperties = {
  background: '#ecfdf3',
  color: '#027a48'
};

const executionHeaderFailedStyle: React.CSSProperties = {
  background: '#fef2f2',
  color: '#b42318'
};

const executionHeaderRunningStyle: React.CSSProperties = {
  background: '#eff6ff',
  color: '#175cd3'
};

const executionErrorStyle: React.CSSProperties = {
  background: '#fff5f5',
  borderTop: '1px solid #fecdca',
  color: '#b42318',
  fontSize: 12,
  lineHeight: '18px',
  padding: '8px 10px'
};

const executionBlockStyle: React.CSSProperties = {
  borderTop: '1px solid #eef2f7',
  padding: '8px 10px 10px'
};

const executionErrorSectionStyle: React.CSSProperties = {
  ...executionBlockStyle,
  background: '#fffafa'
};

const executionSectionHeaderStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 6,
  justifyContent: 'space-between'
};

const executionSectionToggleStyle: React.CSSProperties = {
  alignItems: 'center',
  background: 'transparent',
  border: 'none',
  cursor: 'pointer',
  display: 'flex',
  flex: 1,
  justifyContent: 'space-between',
  padding: 0
};

const executionExpandButtonStyle: React.CSSProperties = {
  alignItems: 'center',
  background: '#fff',
  border: '1px solid #d8e0ec',
  borderRadius: 6,
  color: '#667085',
  cursor: 'pointer',
  display: 'flex',
  height: 24,
  justifyContent: 'center',
  width: 24
};

const chevronStyle: React.CSSProperties = {
  color: '#667085',
  fontSize: 12,
  lineHeight: 1
};

const expandIconStyle: React.CSSProperties = {
  fontSize: 13,
  lineHeight: 1
};

const executionBlockTitleStyle: React.CSSProperties = {
  color: '#667085',
  fontSize: 12,
  fontWeight: 700
};

const executionPreStyle: React.CSSProperties = {
  background: '#f8fafc',
  border: '1px solid #eef2f7',
  borderRadius: 6,
  color: '#344054',
  fontFamily: 'Consolas, monospace',
  fontSize: 11,
  lineHeight: '16px',
  margin: '6px 0 0',
  maxHeight: 120,
  overflow: 'auto',
  padding: 8,
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-word'
};

const executionErrorPreStyle: React.CSSProperties = {
  ...executionPreStyle,
  background: '#fff5f5',
  borderColor: '#fecdca',
  color: '#b42318'
};

const executionModalOverlayStyle: React.CSSProperties = {
  alignItems: 'center',
  background: 'rgba(15, 23, 42, 0.45)',
  display: 'flex',
  inset: 0,
  justifyContent: 'center',
  padding: 24,
  position: 'fixed',
  zIndex: 2000
};

const executionModalStyle: React.CSSProperties = {
  background: '#fff',
  borderRadius: 12,
  boxShadow: '0 24px 48px rgba(15, 23, 42, 0.18)',
  maxHeight: '82vh',
  maxWidth: 760,
  overflow: 'auto',
  padding: '18px 20px 20px',
  width: '100%'
};

const executionModalHeaderStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  justifyContent: 'space-between',
  marginBottom: 12
};

const executionModalTitleStyle: React.CSSProperties = {
  color: '#101828',
  fontSize: 18,
  fontWeight: 700
};

const executionModalCloseStyle: React.CSSProperties = {
  background: 'transparent',
  border: 'none',
  color: '#667085',
  cursor: 'pointer',
  fontSize: 24,
  lineHeight: 1,
  padding: 0
};

const executionModalMetaStyle: React.CSSProperties = {
  color: '#475467',
  display: 'flex',
  fontSize: 14,
  gap: 24,
  marginBottom: 16
};

const executionModalSectionStyle: React.CSSProperties = {
  marginBottom: 14
};

const executionModalSectionTitleStyle: React.CSSProperties = {
  alignItems: 'center',
  color: '#344054',
  display: 'flex',
  fontSize: 14,
  fontWeight: 700,
  gap: 8,
  marginBottom: 8
};

const executionModalDotStyle: React.CSSProperties = {
  borderRadius: 999,
  display: 'inline-block',
  height: 8,
  width: 8
};

const executionModalDotDefaultStyle: React.CSSProperties = {
  background: '#98a2b3'
};

const executionModalDotSuccessStyle: React.CSSProperties = {
  background: '#12b76a'
};

const executionModalDotErrorStyle: React.CSSProperties = {
  background: '#f04438'
};

const executionModalPreStyle: React.CSSProperties = {
  background: '#f8fafc',
  border: '1px solid #eef2f7',
  borderRadius: 8,
  color: '#344054',
  fontFamily: 'Consolas, monospace',
  fontSize: 13,
  lineHeight: '20px',
  margin: 0,
  maxHeight: 280,
  overflow: 'auto',
  padding: 12,
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-word'
};

const executionModalPreErrorStyle: React.CSSProperties = {
  background: '#fff5f5',
  borderColor: '#fecdca',
  color: '#b42318'
};

function CanvasNavigator({
  layout,
  zoom,
  canvasOffset,
  viewport,
  onZoomIn,
  onZoomOut,
  onFitView,
  onPanTo
}: {
  layout: ReturnType<typeof createWorkflowDesignerLayout>;
  zoom: number;
  canvasOffset: { x: number; y: number };
  viewport: { clientWidth: number; clientHeight: number };
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitView: () => void;
  onPanTo: (canvasX: number, canvasY: number) => void;
}) {
  const minimapWidth = 148;
  const minimapHeight = 92;
  const viewportX = Math.max(-canvasOffset.x / zoom, 0);
  const viewportY = Math.max(-canvasOffset.y / zoom, 0);
  const viewportWidth = viewport.clientWidth / zoom;
  const viewportHeight = viewport.clientHeight / zoom;

  function handleMinimapPointer(clientX: number, clientY: number, rect: DOMRect) {
    const canvasX = ((clientX - rect.left) / rect.width) * layout.bounds.width;
    const canvasY = ((clientY - rect.top) / rect.height) * layout.bounds.height;
    onPanTo(canvasX, canvasY);
  }

  return (
    <div style={navigatorStyle} data-workflow-interactive="true" aria-label="画布导航">
      <div
        aria-label="画布缩略图"
        data-workflow-interactive="true"
        style={minimapStyle}
        onMouseDown={(event) => {
          event.preventDefault();
          event.stopPropagation();
          const rect = event.currentTarget.getBoundingClientRect();
          handleMinimapPointer(event.clientX, event.clientY, rect);

          function handleMouseMove(moveEvent: MouseEvent) {
            handleMinimapPointer(moveEvent.clientX, moveEvent.clientY, rect);
          }

          function handleMouseUp() {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
          }

          window.addEventListener('mousemove', handleMouseMove);
          window.addEventListener('mouseup', handleMouseUp);
        }}
      >
        <svg width={minimapWidth} height={minimapHeight} viewBox={`0 0 ${layout.bounds.width} ${layout.bounds.height}`}>
          {layout.edges.map((edge) => (
            <path key={edge.id} d={edge.path} fill="none" stroke="#98a2b3" strokeWidth={2} />
          ))}
          {layout.nodes.map((node) => (
            <rect
              key={node.id}
              x={node.x}
              y={node.y}
              width={node.width}
              height={node.height}
              rx={4}
              fill="#dbeafe"
              stroke="#60a5fa"
              strokeWidth={2}
            />
          ))}
          <rect
            x={viewportX}
            y={viewportY}
            width={Math.max(viewportWidth, 24)}
            height={Math.max(viewportHeight, 24)}
            fill="rgba(22, 119, 255, 0.08)"
            stroke="#1677ff"
            strokeWidth={3}
          />
        </svg>
      </div>
      <div style={navigatorControlsStyle}>
        <button type="button" aria-label="缩小" style={navigatorButtonStyle} onClick={onZoomOut}>−</button>
        <button type="button" aria-label="适配视图" style={navigatorFitButtonStyle} onClick={onFitView}>适配</button>
        <button type="button" aria-label="放大" style={navigatorButtonStyle} onClick={onZoomIn}>+</button>
        <span style={navigatorZoomStyle}>{Math.round(zoom * 100)}%</span>
      </div>
    </div>
  );
}

const navigatorStyle: React.CSSProperties = {
  background: 'rgba(255, 255, 255, 0.96)',
  border: '1px solid #d8e0ec',
  borderRadius: 10,
  bottom: 12,
  boxShadow: '0 10px 24px rgba(15, 23, 42, 0.08)',
  left: 12,
  padding: 8,
  position: 'absolute',
  zIndex: 6
};

const minimapStyle: React.CSSProperties = {
  background: '#f8fafc',
  border: '1px solid #e7ecf3',
  borderRadius: 8,
  cursor: 'grab',
  overflow: 'hidden'
};

const navigatorControlsStyle: React.CSSProperties = {
  alignItems: 'center',
  display: 'flex',
  gap: 6,
  marginTop: 8
};

const navigatorButtonStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #d8e0ec',
  borderRadius: 6,
  color: '#344054',
  cursor: 'pointer',
  fontSize: 14,
  fontWeight: 700,
  height: 28,
  width: 28
};

const navigatorFitButtonStyle: React.CSSProperties = {
  ...navigatorButtonStyle,
  fontSize: 12,
  padding: '0 8px',
  width: 'auto'
};

const navigatorZoomStyle: React.CSSProperties = {
  color: '#475467',
  fontSize: 12,
  fontWeight: 700,
  marginLeft: 4,
  minWidth: 42
};

const CONNECTION_SNAP_RADIUS = 28;
const PORT_OFFSET = 7;

function resolveCanvasClientPoint(
  clientX: number,
  clientY: number,
  currentZoom: number,
  viewportRect: DOMRect | null,
  offset: { x: number; y: number }
) {
  if (!viewportRect) {
    return null;
  }
  return {
    x: (clientX - viewportRect.left - offset.x) / currentZoom,
    y: (clientY - viewportRect.top - offset.y) / currentZoom
  };
}

function findConnectionSnapAtPoint(
  clientX: number,
  clientY: number,
  layout: ReturnType<typeof createWorkflowDesignerLayout>,
  currentZoom: number,
  excludeNodeId?: string | null,
  viewportRect: DOMRect | null = null,
  offset: { x: number; y: number } = { x: 0, y: 0 }
): { nodeId: string; x: number; y: number } | null {
  const canvasPoint = resolveCanvasClientPoint(clientX, clientY, currentZoom, viewportRect, offset);
  if (!canvasPoint) {
    return null;
  }

  let nearest: { nodeId: string; x: number; y: number; distance: number } | null = null;
  for (const node of layout.nodes) {
    if (node.id === excludeNodeId) {
      continue;
    }
    const inputX = node.x - PORT_OFFSET;
    const inputY = node.y + node.height / 2;
    const distance = Math.hypot(canvasPoint.x - inputX, canvasPoint.y - inputY);
    if (!nearest || distance < nearest.distance) {
      nearest = { nodeId: node.id, x: inputX, y: inputY, distance };
    }
  }
  if (!nearest || nearest.distance > CONNECTION_SNAP_RADIUS) {
    return null;
  }
  return { nodeId: nearest.nodeId, x: nearest.x, y: nearest.y };
}

function resolveConnectionTargetNodeId(
  clientX: number,
  clientY: number,
  layout: ReturnType<typeof createWorkflowDesignerLayout>,
  currentZoom: number,
  excludeNodeId?: string | null,
  viewportRect: DOMRect | null = null,
  offset: { x: number; y: number } = { x: 0, y: 0 }
): string | null {
  const snap = findConnectionSnapAtPoint(clientX, clientY, layout, currentZoom, excludeNodeId, viewportRect, offset);
  if (snap) {
    return snap.nodeId;
  }

  const hitTarget = document.elementFromPoint(clientX, clientY);
  const nodeElement = hitTarget?.closest('[data-workflow-node-id]');
  const nodeIdFromDom = nodeElement?.getAttribute('data-workflow-node-id');
  if (nodeIdFromDom && nodeIdFromDom !== excludeNodeId) {
    return nodeIdFromDom;
  }
  return null;
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
