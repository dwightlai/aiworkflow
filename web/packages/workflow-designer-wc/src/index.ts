import { createWorkflowDesignerCore, type WorkflowDesignerCore } from '@aiworkflow/workflow-designer-core';
import { createEmptyWorkflowDefinition } from '@aiworkflow/workflow-schema';

export class AiWorkflowDesignerElement extends HTMLElement {
  private designer?: WorkflowDesignerCore;

  connectedCallback() {
    this.designer = createWorkflowDesignerCore({
      container: this,
      value: createEmptyWorkflowDefinition()
    });
    this.designer.mount();
  }

  disconnectedCallback() {
    this.designer?.destroy();
    this.designer = undefined;
  }
}

if (!customElements.get('ai-workflow-designer')) {
  customElements.define('ai-workflow-designer', AiWorkflowDesignerElement);
}
