import { createWorkflowDesignerCore, type WorkflowDesignerCore } from '@aiworkflow/workflow-designer-core';
import type { WorkflowDefinition, WorkflowNode } from '@aiworkflow/workflow-schema';
import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';

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

  return <div ref={containerRef} style={{ width: '100%', height: '100%' }} />;
});
