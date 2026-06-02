import {
  createWorkflowDesignerCore,
  createWorkflowDesignerLayout,
  type WorkflowDesignerCore
} from '@aiworkflow/workflow-designer-core';
import { createEmptyWorkflowDefinition, type WorkflowDefinition } from '@aiworkflow/workflow-schema';

export class AiWorkflowDesignerElement extends HTMLElement {
  private designer?: WorkflowDesignerCore;
  private definition: WorkflowDefinition = createEmptyWorkflowDefinition();
  private root?: ShadowRoot;

  connectedCallback() {
    this.root = this.shadowRoot ?? this.attachShadow({ mode: 'open' });
    this.designer = createWorkflowDesignerCore({
      container: this,
      value: this.definition,
      readonly: this.hasAttribute('readonly'),
      onChange: (value) => {
        this.definition = value;
        this.render();
        this.dispatchEvent(new CustomEvent('workflow-change', { detail: value }));
      }
    });
    this.designer.mount();
    this.render();
  }

  disconnectedCallback() {
    this.designer?.destroy();
    this.designer = undefined;
  }

  set value(value: WorkflowDefinition) {
    this.definition = value;
    this.designer?.setValue(value);
    this.render();
  }

  get value() {
    return this.definition;
  }

  private render() {
    if (!this.root) {
      return;
    }
    const layout = createWorkflowDesignerLayout(this.definition);
    this.root.innerHTML = `
      <style>
        :host { display: block; min-height: 320px; }
        .canvas { background: #f8fafc; border-radius: 8px; min-height: 320px; overflow: auto; position: relative; width: 100%; height: 100%; }
        .inner { min-width: ${layout.bounds.width}px; min-height: ${layout.bounds.height}px; position: relative; }
        svg { inset: 0; pointer-events: none; position: absolute; }
        .node {
          background: #fff;
          border: 1px solid #d8e0ec;
          border-radius: 8px;
          box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
          box-sizing: border-box;
          display: flex;
          flex-direction: column;
          justify-content: center;
          padding: 12px 14px;
          position: absolute;
        }
        .type { color: #667085; font: 11px/16px ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
        .name { color: #101828; font: 700 14px/20px system-ui, sans-serif; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      </style>
      <div class="canvas">
        <div class="inner">
          <svg width="${layout.bounds.width}" height="${layout.bounds.height}" viewBox="0 0 ${layout.bounds.width} ${layout.bounds.height}">
            <defs>
              <marker id="aiworkflow-arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto" markerUnits="strokeWidth">
                <path d="M0,0 L0,6 L9,3 z" fill="#98a2b3"></path>
              </marker>
            </defs>
            ${layout.edges.map((edge) => `<path d="${edge.path}" fill="none" stroke="#b8c2d2" stroke-width="2" marker-end="url(#aiworkflow-arrow)"></path>`).join('')}
          </svg>
          ${layout.nodes.map((node) => `
            <div class="node" style="left:${node.x}px;top:${node.y}px;width:${node.width}px;height:${node.height}px;background:${nodeBackground(node.type)};">
              <span class="type">${node.type}</span>
              <span class="name">${escapeHtml(node.name)}</span>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }
}

if (!customElements.get('ai-workflow-designer')) {
  customElements.define('ai-workflow-designer', AiWorkflowDesignerElement);
}

function nodeBackground(type: string) {
  const colorMap: Record<string, string> = {
    CONDITION: '#fdf2f8',
    END: '#f1f5f9',
    HTTP_TOOL: '#fefce8',
    KNOWLEDGE_RETRIEVAL: '#f0fdf4',
    LLM: '#fff7ed',
    LOOP: '#f0f9ff',
    PROMPT: '#eef6ff',
    CONTENT_TEMPLATE: '#f5f3ff',
    START: '#ecfdf3',
    TEXT_TRANSFORM: '#f5f3ff'
  };
  return `linear-gradient(90deg, ${colorMap[type] ?? '#eef6ff'} 0, #fff 34%)`;
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
