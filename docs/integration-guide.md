# AI Workflow Integration Guide

本文档说明第一版 AI Studio 的本地启动、工作流使用方式，以及第三方系统如何集成设计器和运行 API。

## 启动 PostgreSQL

```powershell
docker compose up -d postgres
```

默认连接信息：

```text
url: jdbc:postgresql://localhost:5432/aiworkflow
username: aiworkflow
password: aiworkflow
```

后端启动时会通过 Flyway 执行 `server/src/main/resources/db/migration` 下的表结构迁移。

## 启动后端

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f server/pom.xml spring-boot:run
```

后端地址：

```text
http://127.0.0.1:8080
```

核心 API：

```http
GET  /api/workflows
POST /api/workflows
GET  /api/workflows/{workflowId}
PUT  /api/workflows/{workflowId}/draft
POST /api/workflows/{workflowId}/publish
POST /api/workflows/{workflowId}/runs
GET  /api/workflow-runs
GET  /api/workflow-runs/{executionId}
GET  /api/prompts
POST /api/prompts
GET  /api/model-providers
POST /api/model-providers
```

## 启动前端

```powershell
cd web
corepack pnpm --filter @aiworkflow/admin dev
```

前端地址：

```text
http://127.0.0.1:5173
```

当前 AI Studio 包含：

- 工作台
- 工作流卡片控制台
- 工作流设计器
- 运行历史
- 执行详情
- Prompt 与模型配置 API

## 创建并运行工作流

创建工作流：

```http
POST /api/workflows
Content-Type: application/json
```

```json
{
  "name": "Greeting workflow",
  "description": "Prompt and LLM demo",
  "definition": {
    "nodes": [
      { "id": "start", "type": "START", "name": "Start", "config": {} },
      {
        "id": "prompt",
        "type": "PROMPT",
        "name": "Prompt",
        "config": { "template": "Hello {{name}}", "outputKey": "prompt" }
      },
      {
        "id": "llm",
        "type": "LLM",
        "name": "LLM",
        "config": {
          "providerId": "dev",
          "model": "mock",
          "promptKey": "prompt",
          "outputKey": "answer"
        }
      },
      { "id": "end", "type": "END", "name": "End", "config": { "outputKeys": ["answer"] } }
    ],
    "edges": [
      { "id": "edge-1", "sourceNodeId": "start", "targetNodeId": "prompt", "condition": null },
      { "id": "edge-2", "sourceNodeId": "prompt", "targetNodeId": "llm", "condition": null },
      { "id": "edge-3", "sourceNodeId": "llm", "targetNodeId": "end", "condition": null }
    ],
    "variables": [{ "name": "name", "type": "STRING", "required": true }]
  }
}
```

发布工作流：

```http
POST /api/workflows/{workflowId}/publish
```

运行工作流：

```http
POST /api/workflows/{workflowId}/runs
Content-Type: application/json
```

```json
{
  "input": {
    "name": "Ada"
  }
}
```

运行结果会返回执行状态、最终输出、错误信息和节点执行明细。第一版内置 `StubChatModelClient`，LLM 节点默认返回 `"model response"`，后续可替换为真实模型网关。

## React 嵌入设计器

```tsx
import { useRef } from 'react';
import { WorkflowDesignerReact, type WorkflowDesignerHandle } from '@aiworkflow/workflow-designer-react';
import type { WorkflowDefinition } from '@aiworkflow/workflow-schema';

export function EmbeddedDesigner({
  definition,
  onChange
}: {
  definition: WorkflowDefinition;
  onChange: (value: WorkflowDefinition) => void;
}) {
  const designerRef = useRef<WorkflowDesignerHandle | null>(null);

  return (
    <div style={{ height: 520 }}>
      <WorkflowDesignerReact ref={designerRef} value={definition} onChange={onChange} />
    </div>
  );
}
```

React 系统可以直接使用 `@aiworkflow/workflow-designer-react`。Ant Design React 项目可以把它放入现有 ProLayout、Drawer、Modal 或业务表单中。

## Vue 或非 React 系统嵌入

Vue、原生 Web 或微前端场景可以优先使用 Web Component 包：

```ts
import '@aiworkflow/workflow-designer-wc';
```

```html
<ai-workflow-designer></ai-workflow-designer>
```

更深度的 Vue 集成可以基于 `@aiworkflow/workflow-designer-core` 封装 Vue adapter。核心设计器状态 API 与 React 无关，便于被第三方框架复用。

## TypeScript SDK

```ts
import { AiWorkflowClient } from '@aiworkflow/workflow-sdk';

const client = new AiWorkflowClient({
  baseUrl: 'https://workflow.example.com',
  apiKey: 'your-api-key'
});

const workflows = await client.listWorkflows();
const created = await client.createWorkflow({
  name: 'Greeting workflow',
  description: null,
  definition
});
await client.publishWorkflow(created.id);
const run = await client.runWorkflow(created.id, { name: 'Ada' });
const detail = await client.getWorkflowRun(run.id);
```

浏览器端接入可以把 `baseUrl` 留空，让网关、Vite 或 NGINX 代理 `/api`。
