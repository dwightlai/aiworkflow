# AI Workflow Integration Guide

本文档说明第一版 AI 工作流平台的第三方集成方式。当前版本提供三类集成面：

- REST API：适合任何后端服务、低代码平台或企业系统直接调用。
- TypeScript SDK：适合 React、Vue、Node.js、BFF 服务调用。
- 嵌入式设计器包：适合把工作流设计能力嵌入 React、Vue 或原生 Web Component 系统。

## REST API

```http
GET /api/workflows
POST /api/workflows
POST /api/workflows/{workflowId}/publish
POST /api/workflows/{workflowId}/runs
GET /api/workflow-runs/{executionId}
```

运行请求：

```json
{
  "input": {
    "name": "Ada"
  }
}
```

运行响应会返回工作流执行状态、输出、错误信息和节点执行明细。

## TypeScript SDK

```ts
import { AiWorkflowClient } from '@aiworkflow/workflow-sdk';

const client = new AiWorkflowClient({
  baseUrl: 'https://workflow.example.com',
  apiKey: 'your-api-key'
});

const workflows = await client.listWorkflows();
const run = await client.runWorkflow(workflows.items[0].id, { name: 'Ada' });
const detail = await client.getWorkflowRun(run.id);
```

浏览器端接入可以把 `baseUrl` 留空，交给前端网关或 Vite/NGINX 代理 `/api`。

## React 嵌入

```tsx
import { WorkflowDesignerReact } from '@aiworkflow/workflow-designer-react';

export function Designer({ definition, onChange }) {
  return (
    <div style={{ height: 520 }}>
      <WorkflowDesignerReact value={definition} onChange={onChange} />
    </div>
  );
}
```

## Vue 或非 React 系统嵌入

Vue 系统可以优先使用 Web Component 包：

```ts
import '@aiworkflow/workflow-designer-wc';
```

```html
<ai-workflow-designer></ai-workflow-designer>
```

后续如果需要更深的 Vue 插槽和属性绑定，可以基于 `@aiworkflow/workflow-designer-core` 封装专属 Vue adapter。

## 数据契约

前端设计器、SDK 和后端共享同一类 DAG JSON：

```json
{
  "nodes": [
    { "id": "start", "type": "START", "name": "开始", "config": {} },
    {
      "id": "transform",
      "type": "TEXT_TRANSFORM",
      "name": "文本处理",
      "config": {
        "outputKey": "message",
        "template": "Hello {{name}}"
      }
    },
    {
      "id": "end",
      "type": "END",
      "name": "结束",
      "config": {
        "outputKeys": ["message"]
      }
    }
  ],
  "edges": [
    { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "transform", "condition": null },
    { "id": "edge-2", "sourceNodeId": "transform", "targetNodeId": "end", "condition": null }
  ],
  "variables": [
    { "name": "name", "type": "STRING", "required": true }
  ]
}
```

