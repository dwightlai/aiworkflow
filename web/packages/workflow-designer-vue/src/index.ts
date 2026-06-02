import {
  createWorkflowDesignerCore,
  createWorkflowDesignerLayout,
  type WorkflowDesignerCore,
  type WorkflowDesignerCoreOptions,
  type WorkflowDesignerLayout
} from '@aiworkflow/workflow-designer-core';
import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';

export { createWorkflowDesignerCore, createWorkflowDesignerLayout };
export type { WorkflowDefinition, WorkflowDesignerCore, WorkflowDesignerCoreOptions, WorkflowDesignerLayout };

export interface WorkflowDesignerVueAdapter {
  designer: WorkflowDesignerCore;
  getLayout(): WorkflowDesignerLayout;
  setValue(value: WorkflowDefinition): WorkflowDesignerLayout;
}

export function createWorkflowDesignerVueAdapter(options: WorkflowDesignerCoreOptions): WorkflowDesignerVueAdapter {
  const designer = createWorkflowDesignerCore(options);
  let layout = createWorkflowDesignerLayout(options.value);

  return {
    designer,
    getLayout() {
      return layout;
    },
    setValue(value: WorkflowDefinition) {
      designer.setValue(value);
      layout = createWorkflowDesignerLayout(value);
      return layout;
    }
  };
}
