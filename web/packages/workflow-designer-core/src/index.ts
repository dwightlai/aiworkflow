import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';

export interface WorkflowDesignerCoreOptions {
  container: HTMLElement;
  value: WorkflowDefinition;
  readonly?: boolean;
  onChange?: (value: WorkflowDefinition) => void;
}

export interface WorkflowDesignerCore {
  mount(): void;
  destroy(): void;
  getValue(): WorkflowDefinition;
  setValue(value: WorkflowDefinition): void;
}

export function createWorkflowDesignerCore(options: WorkflowDesignerCoreOptions): WorkflowDesignerCore {
  let currentValue = options.value;

  return {
    mount() {
      options.container.dataset.workflowDesignerMounted = 'true';
    },
    destroy() {
      delete options.container.dataset.workflowDesignerMounted;
    },
    getValue() {
      return currentValue;
    },
    setValue(value: WorkflowDefinition) {
      currentValue = value;
      options.onChange?.(value);
    }
  };
}
