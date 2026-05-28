import { createWorkflowDesignerCore } from '@aiworkflow/workflow-designer-core';
import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';
import { useEffect, useRef } from 'react';

export interface WorkflowDesignerReactProps {
  value: WorkflowDefinition;
  readonly?: boolean;
  onChange?: (value: WorkflowDefinition) => void;
}

export function WorkflowDesignerReact(props: WorkflowDesignerReactProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);

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

    designer.mount();
    return () => designer.destroy();
  }, [props.value, props.readonly, props.onChange]);

  return <div ref={containerRef} style={{ width: '100%', height: '100%' }} />;
}
