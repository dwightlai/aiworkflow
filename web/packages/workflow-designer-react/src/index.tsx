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
}

export interface WorkflowDesignerHandle {
  addNode(node: WorkflowNode): void;
  getValue(): WorkflowDefinition;
}

export const WorkflowDesignerReact = forwardRef<WorkflowDesignerHandle, WorkflowDesignerReactProps>(
function WorkflowDesignerReact(props, ref) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const designerRef = useRef<WorkflowDesignerCore | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const layout = useMemo(() => createWorkflowDesignerLayout(props.value), [props.value]);

  useImperativeHandle(ref, () => ({
    addNode(node: WorkflowNode) {
      designerRef.current?.addNode(node);
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
          return (
            <button
              key={node.id}
              type="button"
              style={{
                ...nodeStyle,
                ...nodeToneStyle(node.type),
                borderColor: selected ? '#1677ff' : '#d8e0ec',
                boxShadow: selected ? '0 0 0 3px rgba(22, 119, 255, 0.14)' : '0 8px 20px rgba(15, 23, 42, 0.08)',
                height: node.height,
                left: node.x,
                top: node.y,
                width: node.width
              }}
              onClick={() => {
                designerRef.current?.selectNode(node.id);
                setSelectedNodeId(node.id);
              }}
            >
              <span style={nodeTypeStyle}>{node.type}</span>
              <span style={nodeNameStyle}>{node.name}</span>
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
  cursor: 'pointer',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  padding: '12px 14px',
  position: 'absolute',
  textAlign: 'left'
};

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
