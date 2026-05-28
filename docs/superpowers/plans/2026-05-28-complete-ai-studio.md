# Complete AI Studio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the current MVP into a complete AIFLowy-style AI Studio: full admin shell, workflow card list, visual workflow designer, node configuration, Prompt/LLM execution, run history, execution detail, and PostgreSQL persistence.

**Architecture:** Keep the project as a modular monolith. The backend owns workflow definitions, versions, execution, prompts, model configs, and persistence; the frontend owns the AIFLowy-style shell, routes, workflow design experience, and debugging surfaces. The designer remains framework-agnostic through `workflow-designer-core`, with React as the first complete adapter.

**Tech Stack:** Java 21, Spring Boot 3.3.5, PostgreSQL/Flyway/JdbcTemplate, Jackson, JUnit 5, MockMvc, React 18, Ant Design 5, TanStack Query 5, LogicFlow, TypeScript, Vitest, Vite.

---

## Implementation Rules

- Follow TDD for production behavior: write a failing test, run it red, implement, run green, then commit.
- Keep commits small and stage-complete.
- Do not remove the existing SDK, Web Component, Vue adapter, or REST contracts.
- Keep the first complete system focused on AI Studio. System management pages can be functional placeholders with real routing and layout.
- Use PostgreSQL/Flyway for runtime persistence, but keep tests able to run without local Docker by using service-level stores and controller tests that exclude datasource unless persistence is under test.

## File Structure

```text
web/apps/admin/src
|-- App.tsx
|-- routes.tsx
|-- layout
|   |-- AdminShell.tsx
|   |-- menu.ts
|   |-- PageHeader.tsx
|-- pages
|   |-- DashboardPage.tsx
|   |-- PlaceholderPage.tsx
|   |-- workflows
|       |-- WorkflowCardsPage.tsx
|       |-- WorkflowDesignerPage.tsx
|       |-- WorkflowRunsPage.tsx
|       |-- WorkflowRunDetailPage.tsx
|-- api
|   |-- workflows.ts
|   |-- prompts.ts
|   |-- modelConfigs.ts

web/packages/workflow-schema/src/index.ts
web/packages/workflow-designer-core/src/index.ts
web/packages/workflow-designer-react/src/index.tsx

server/src/main/java/com/aiworkflow/prompt
server/src/main/java/com/aiworkflow/model
server/src/main/java/com/aiworkflow/workflow
server/src/main/resources/db/migration
```

## Task 1: AIFLowy-Style Admin Shell and Routes

**Files:**
- Modify: `web/apps/admin/src/App.tsx`
- Create: `web/apps/admin/src/routes.tsx`
- Create: `web/apps/admin/src/layout/menu.ts`
- Create: `web/apps/admin/src/layout/AdminShell.tsx`
- Create: `web/apps/admin/src/layout/PageHeader.tsx`
- Create: `web/apps/admin/src/pages/DashboardPage.tsx`
- Create: `web/apps/admin/src/pages/PlaceholderPage.tsx`
- Test: `web/apps/admin/src/layout/AdminShell.test.tsx`

- [ ] **Step 1: Add React Testing Library dependencies**

Update `web/apps/admin/package.json`:

```json
"devDependencies": {
  "@testing-library/jest-dom": "^6.6.3",
  "@testing-library/react": "^16.0.1",
  "@testing-library/user-event": "^14.5.2"
}
```

- [ ] **Step 2: Write failing shell test**

Create `AdminShell.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AdminShell } from './AdminShell';

describe('AdminShell', () => {
  it('renders grouped AI and system menus with page content', () => {
    render(
      <AdminShell title="工作台" breadcrumb={['首页', 'AI 功能', '工作台']}>
        <div>dashboard content</div>
      </AdminShell>
    );

    expect(screen.getByText('AI 功能')).toBeInTheDocument();
    expect(screen.getByText('工作流')).toBeInTheDocument();
    expect(screen.getByText('系统管理')).toBeInTheDocument();
    expect(screen.getByText('用户管理')).toBeInTheDocument();
    expect(screen.getByText('首页 / AI 功能 / 工作台')).toBeInTheDocument();
    expect(screen.getByText('dashboard content')).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Run red test**

Run:

```powershell
corepack pnpm --filter @aiworkflow/admin test -- AdminShell.test.tsx
```

Expected: FAIL because `AdminShell` does not exist.

- [ ] **Step 4: Implement menu and shell**

Create `menu.ts` with groups:

```ts
export const menuGroups = [
  {
    title: 'AI 功能',
    items: [
      { key: 'dashboard', label: '工作台', path: '/' },
      { key: 'bots', label: '智能体 Bots', path: '/bots' },
      { key: 'workflows', label: '工作流', path: '/workflows' },
      { key: 'prompts', label: 'Prompt', path: '/prompts' },
      { key: 'knowledge', label: '知识库', path: '/knowledge' },
      { key: 'tools', label: '工具插件', path: '/tools' },
      { key: 'models', label: '模型配置', path: '/models' },
      { key: 'model-market', label: '模型市场', path: '/model-market' }
    ]
  },
  {
    title: '系统管理',
    items: [
      { key: 'users', label: '用户管理', path: '/system/users' },
      { key: 'roles', label: '角色管理', path: '/system/roles' },
      { key: 'menus', label: '菜单管理', path: '/system/menus' },
      { key: 'departments', label: '部门管理', path: '/system/departments' },
      { key: 'dictionary', label: '数据字典', path: '/system/dictionary' },
      { key: 'jobs', label: '定时任务', path: '/system/jobs' },
      { key: 'logs', label: '日志管理', path: '/system/logs' }
    ]
  }
];
```

`AdminShell` renders a fixed left sidebar, grouped menu labels, a top page header, and children.

- [ ] **Step 5: Add routes and pages**

`routes.tsx` should map:

- `/` -> `DashboardPage`
- `/workflows` -> `WorkflowCardsPage` from Task 2
- `/workflows/:workflowId/designer` -> `WorkflowDesignerPage` from Task 3
- `/workflow-runs` -> `WorkflowRunsPage` from Task 7
- `/workflow-runs/:executionId` -> `WorkflowRunDetailPage` from Task 7
- all other menu routes -> `PlaceholderPage`

For this task, route unavailable pages to `PlaceholderPage` until later tasks create them.

- [ ] **Step 6: Run green test**

Run:

```powershell
corepack pnpm --filter @aiworkflow/admin test -- AdminShell.test.tsx
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add web/apps/admin/src web/apps/admin/package.json web/pnpm-lock.yaml
git commit -m "feat: add AIFLowy style admin shell"
```

## Task 2: Workflow Card List and Dashboard

**Files:**
- Modify: `web/apps/admin/src/api/workflows.ts`
- Create: `web/apps/admin/src/pages/workflows/WorkflowCardsPage.tsx`
- Modify: `web/apps/admin/src/pages/DashboardPage.tsx`
- Test: `web/apps/admin/src/pages/workflows/WorkflowCardsPage.test.tsx`

- [ ] **Step 1: Write failing workflow cards test**

Create `WorkflowCardsPage.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { WorkflowCardsPage } from './WorkflowCardsPage';

vi.mock('../../api/workflows', () => ({
  listWorkflows: vi.fn(async () => ({
    items: [
      { id: 'workflow-1', name: '客服意图识别', description: '识别并路由用户问题', status: 'PUBLISHED', updatedAt: '2026-05-28T08:00:00Z', latestVersion: { version: 1 } }
    ],
    total: 1
  })),
  createWorkflow: vi.fn(),
  publishWorkflow: vi.fn(),
  runWorkflow: vi.fn()
}));

describe('WorkflowCardsPage', () => {
  it('renders search bar create card and workflow cards', async () => {
    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <WorkflowCardsPage />
      </QueryClientProvider>
    );

    expect(await screen.findByText('创建工作流')).toBeInTheDocument();
    expect(await screen.findByText('客服意图识别')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入工作流名称')).toBeInTheDocument();
    expect(screen.getByText('运行')).toBeInTheDocument();
    expect(screen.getByText('编辑')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run red test**

Run:

```powershell
corepack pnpm --filter @aiworkflow/admin test -- WorkflowCardsPage.test.tsx
```

Expected: FAIL because the page does not exist.

- [ ] **Step 3: Implement card page**

`WorkflowCardsPage` should:

- Use `listWorkflows`.
- Render search input with placeholder `请输入工作流名称`.
- Render a large bordered `创建工作流` card.
- Render workflow cards with title, description, status tag, latest version.
- Render footer actions: `设置`, `运行`, `编辑`, `更多`.
- Route edit action to `/workflows/{id}/designer`.

- [ ] **Step 4: Implement dashboard cards**

`DashboardPage` should render four stat cards:

- 工作流
- 今日执行
- 运行成功率
- 模型调用

Use current API data for workflow count and static `0` values for not-yet-backed metrics.

- [ ] **Step 5: Run green test and build**

Run:

```powershell
corepack pnpm --filter @aiworkflow/admin test -- WorkflowCardsPage.test.tsx
corepack pnpm build
```

Expected: PASS and build success.

- [ ] **Step 6: Commit**

```powershell
git add web/apps/admin/src
git commit -m "feat: add workflow card console"
```

## Task 3: Workflow Schema and Designer Core

**Files:**
- Modify: `web/packages/workflow-schema/src/index.ts`
- Modify: `web/packages/workflow-designer-core/src/index.ts`
- Modify: `web/packages/workflow-designer-react/src/index.tsx`
- Test: `web/packages/workflow-designer-core/src/index.test.ts`

- [ ] **Step 1: Write failing designer core test**

Create `index.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { createWorkflowDesignerCore } from './index';

describe('createWorkflowDesignerCore', () => {
  it('adds a node and emits updated workflow definition', () => {
    const container = document.createElement('div');
    const changes: unknown[] = [];
    const designer = createWorkflowDesignerCore({
      container,
      value: { nodes: [], edges: [], variables: [] },
      onChange: (value) => changes.push(value)
    });

    designer.mount();
    designer.addNode({ id: 'prompt-1', type: 'PROMPT', name: 'Prompt', config: { template: 'Hello {{name}}', outputKey: 'prompt' } });

    expect(designer.getValue().nodes).toHaveLength(1);
    expect(changes).toHaveLength(1);
  });

  it('connects nodes and exposes selected node state', () => {
    const container = document.createElement('div');
    const designer = createWorkflowDesignerCore({
      container,
      value: {
        nodes: [
          { id: 'start', type: 'START', name: 'Start', config: {} },
          { id: 'end', type: 'END', name: 'End', config: {} }
        ],
        edges: [],
        variables: []
      }
    });

    designer.mount();
    designer.connectNodes('edge-1', 'start', 'end');
    designer.selectNode('end');

    expect(designer.getValue().edges[0].targetNodeId).toBe('end');
    expect(designer.getSelectedNode()?.id).toBe('end');
  });
});
```

- [ ] **Step 2: Run red test**

Run:

```powershell
corepack pnpm --filter @aiworkflow/workflow-designer-core test
```

Expected: FAIL because `addNode`, `connectNodes`, `selectNode`, and `getSelectedNode` do not exist.

- [ ] **Step 3: Extend schema**

Add node config helper types:

```ts
export interface PromptNodeConfig { template: string; outputKey: string; }
export interface LlmNodeConfig { providerId: string; model: string; promptKey: string; outputKey: string; temperature?: number; maxTokens?: number; }
export interface ConditionNodeConfig { contextKey: string; operator: 'EQUALS' | 'NOT_EQUALS' | 'CONTAINS' | 'IS_EMPTY' | 'IS_NOT_EMPTY'; compareValue?: string; trueTargetNodeId: string; falseTargetNodeId: string; }
```

- [ ] **Step 4: Implement designer core methods**

Extend `WorkflowDesignerCore`:

```ts
addNode(node: WorkflowNode): void;
updateNode(nodeId: string, patch: Partial<WorkflowNode>): void;
removeNode(nodeId: string): void;
connectNodes(edgeId: string, sourceNodeId: string, targetNodeId: string): void;
selectNode(nodeId: string | null): void;
getSelectedNode(): WorkflowNode | null;
```

Keep LogicFlow initialization behind the core interface. In tests, allow DOM-free state transitions to pass without requiring canvas rendering.

- [ ] **Step 5: Update React adapter**

Expose:

```ts
export interface WorkflowDesignerHandle {
  addNode(node: WorkflowNode): void;
  getValue(): WorkflowDefinition;
}
```

Use `forwardRef` so `WorkflowDesignerPage` can add nodes from the palette.

- [ ] **Step 6: Run tests and build**

Run:

```powershell
corepack pnpm --filter @aiworkflow/workflow-designer-core test
corepack pnpm build
```

Expected: PASS and build success.

- [ ] **Step 7: Commit**

```powershell
git add web/packages/workflow-schema/src web/packages/workflow-designer-core/src web/packages/workflow-designer-react/src
git commit -m "feat: expand workflow designer core"
```

## Task 4: Designer Page, Node Palette, Config Panel, Debug Panel

**Files:**
- Create: `web/apps/admin/src/pages/workflows/WorkflowDesignerPage.tsx`
- Create: `web/apps/admin/src/pages/workflows/designer/NodePalette.tsx`
- Create: `web/apps/admin/src/pages/workflows/designer/NodeConfigPanel.tsx`
- Create: `web/apps/admin/src/pages/workflows/designer/DebugPanel.tsx`
- Modify: `web/apps/admin/src/api/workflows.ts`
- Test: `web/apps/admin/src/pages/workflows/WorkflowDesignerPage.test.tsx`

- [ ] **Step 1: Write failing designer page test**

Create test:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { WorkflowDesignerPage } from './WorkflowDesignerPage';

vi.mock('../../api/workflows', () => ({
  getWorkflow: vi.fn(async () => ({
    id: 'workflow-1',
    name: '客服意图识别',
    status: 'DRAFT',
    latestVersion: {
      definition: {
        nodes: [{ id: 'start', type: 'START', name: '开始', config: {} }],
        edges: [],
        variables: []
      }
    }
  })),
  updateWorkflowDraft: vi.fn(),
  publishWorkflow: vi.fn(),
  runWorkflow: vi.fn()
}));

describe('WorkflowDesignerPage', () => {
  it('renders palette canvas config panel and debug panel', async () => {
    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <WorkflowDesignerPage workflowId="workflow-1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('客服意图识别')).toBeInTheDocument();
    expect(screen.getByText('节点库')).toBeInTheDocument();
    expect(screen.getByText('PROMPT')).toBeInTheDocument();
    expect(screen.getByText('节点配置')).toBeInTheDocument();
    expect(screen.getByText('运行调试')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run red test**

Run:

```powershell
corepack pnpm --filter @aiworkflow/admin test -- WorkflowDesignerPage.test.tsx
```

Expected: FAIL because page/components do not exist.

- [ ] **Step 3: Implement API helpers**

Add:

```ts
export async function getWorkflow(workflowId: string): Promise<Workflow>;
export async function updateWorkflowDraft(workflowId: string, definition: WorkflowDefinition): Promise<Workflow>;
```

Use `/api/workflows/{workflowId}` and `/api/workflows/{workflowId}/draft`.

- [ ] **Step 4: Implement designer page**

Page layout:

- top toolbar with workflow name, `保存草稿`, `发布`, `运行`
- left `NodePalette`
- center `WorkflowDesignerReact`
- right `NodeConfigPanel`
- bottom `DebugPanel`

- [ ] **Step 5: Implement config panel**

Forms for:

- PROMPT: template, outputKey
- LLM: providerId, model, promptKey, outputKey, temperature, maxTokens
- CONDITION: contextKey, operator, compareValue, trueTargetNodeId, falseTargetNodeId
- TEXT_TRANSFORM: template, outputKey
- END: outputKeys as comma-separated text

- [ ] **Step 6: Run green test and build**

Run:

```powershell
corepack pnpm --filter @aiworkflow/admin test -- WorkflowDesignerPage.test.tsx
corepack pnpm build
```

Expected: PASS and build success.

- [ ] **Step 7: Commit**

```powershell
git add web/apps/admin/src
git commit -m "feat: add workflow designer page"
```

## Task 5: Prompt and Model Backend APIs

**Files:**
- Create: `server/src/main/java/com/aiworkflow/prompt/domain/PromptTemplate.java`
- Create: `server/src/main/java/com/aiworkflow/prompt/service/PromptTemplateService.java`
- Create: `server/src/main/java/com/aiworkflow/prompt/api/PromptTemplateController.java`
- Create: `server/src/main/java/com/aiworkflow/model/domain/ModelProvider.java`
- Create: `server/src/main/java/com/aiworkflow/model/service/ModelProviderService.java`
- Create: `server/src/main/java/com/aiworkflow/model/api/ModelProviderController.java`
- Test: `server/src/test/java/com/aiworkflow/prompt/api/PromptTemplateControllerIntegrationTest.java`
- Test: `server/src/test/java/com/aiworkflow/model/api/ModelProviderControllerIntegrationTest.java`

- [ ] **Step 1: Write failing Prompt API test**

Test creates a prompt and lists it:

```java
mockMvc.perform(post("/api/prompts")
    .contentType(MediaType.APPLICATION_JSON)
    .content("""
      {"name":"Greeting","template":"Hello {{name}}","description":"Greeting prompt"}
    """))
  .andExpect(status().isOk())
  .andExpect(jsonPath("$.data.name").value("Greeting"));

mockMvc.perform(get("/api/prompts"))
  .andExpect(status().isOk())
  .andExpect(jsonPath("$.data.items[0].template").value("Hello {{name}}"));
```

- [ ] **Step 2: Write failing Model API test**

Test creates a provider and lists it:

```java
mockMvc.perform(post("/api/model-providers")
    .contentType(MediaType.APPLICATION_JSON)
    .content("""
      {"name":"OpenAI Compatible","baseUrl":"https://api.example.com/v1","apiKeyRef":"dev-key","enabled":true}
    """))
  .andExpect(status().isOk())
  .andExpect(jsonPath("$.data.name").value("OpenAI Compatible"));
```

- [ ] **Step 3: Run red tests**

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml '-Dtest=PromptTemplateControllerIntegrationTest,ModelProviderControllerIntegrationTest' test
```

Expected: FAIL because controllers do not exist.

- [ ] **Step 4: Implement in-memory services and controllers**

Use in-memory maps first to keep API behavior independently testable. Records:

```java
public record PromptTemplate(String id, String name, String template, String description, Instant createdAt, Instant updatedAt) {}
public record ModelProvider(String id, String name, String baseUrl, String apiKeyRef, boolean enabled, Instant createdAt, Instant updatedAt) {}
```

Controllers:

- `GET /api/prompts`
- `POST /api/prompts`
- `GET /api/model-providers`
- `POST /api/model-providers`

- [ ] **Step 5: Run green tests**

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml '-Dtest=PromptTemplateControllerIntegrationTest,ModelProviderControllerIntegrationTest' test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add server/src/main/java/com/aiworkflow/prompt server/src/main/java/com/aiworkflow/model server/src/test/java/com/aiworkflow/prompt server/src/test/java/com/aiworkflow/model
git commit -m "feat: add prompt and model provider APIs"
```

## Task 6: PROMPT and LLM Node Execution

**Files:**
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/PromptNodeExecutor.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/LlmNodeExecutor.java`
- Create: `server/src/main/java/com/aiworkflow/model/service/ChatModelClient.java`
- Create: `server/src/main/java/com/aiworkflow/model/service/StubChatModelClient.java`
- Test: `server/src/test/java/com/aiworkflow/workflow/engine/AiNodeExecutorTest.java`

- [ ] **Step 1: Write failing AI node executor tests**

Test:

```java
@Test
void promptNodeRendersTemplateIntoContext()

@Test
void llmNodeCallsModelClientAndStoresOutput()
```

Expected behavior:

- PROMPT config `{ "template": "Hello {{name}}", "outputKey": "prompt" }` outputs `{ "prompt": "Hello Ada" }`
- LLM config `{ "promptKey": "prompt", "outputKey": "answer", "providerId": "dev", "model": "mock" }` calls `ChatModelClient.generate(...)` and outputs `{ "answer": "model response" }`

- [ ] **Step 2: Run red test**

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=AiNodeExecutorTest test
```

Expected: FAIL because executors do not exist.

- [ ] **Step 3: Implement PromptNodeExecutor**

Reuse template token replacement behavior from `TextTransformNodeExecutor`, but set `nodeType()` to `WorkflowNodeType.PROMPT`.

- [ ] **Step 4: Implement ChatModelClient and LlmNodeExecutor**

Contract:

```java
public interface ChatModelClient {
    String generate(String providerId, String model, String prompt, Map<String, Object> options);
}
```

`StubChatModelClient` returns `"model response"` for local development.

- [ ] **Step 5: Register executors**

Both executors are Spring `@Component`s so `WorkflowNodeExecutorRegistry` discovers them.

- [ ] **Step 6: Run green tests and full backend tests**

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add server/src/main/java/com/aiworkflow/workflow/engine server/src/main/java/com/aiworkflow/model/service server/src/test/java/com/aiworkflow/workflow/engine/AiNodeExecutorTest.java
git commit -m "feat: add prompt and LLM node execution"
```

## Task 7: Run History and Execution Detail

**Files:**
- Modify: `server/src/main/java/com/aiworkflow/workflow/engine/WorkflowExecutionStore.java`
- Modify: `server/src/main/java/com/aiworkflow/workflow/engine/InMemoryWorkflowExecutionStore.java`
- Modify: `server/src/main/java/com/aiworkflow/workflow/api/WorkflowRunController.java`
- Create: `web/apps/admin/src/pages/workflows/WorkflowRunsPage.tsx`
- Create: `web/apps/admin/src/pages/workflows/WorkflowRunDetailPage.tsx`
- Modify: `web/apps/admin/src/api/workflows.ts`
- Test: `server/src/test/java/com/aiworkflow/workflow/api/WorkflowRunHistoryControllerIntegrationTest.java`
- Test: `web/apps/admin/src/pages/workflows/WorkflowRunsPage.test.tsx`

- [ ] **Step 1: Write failing backend history test**

After creating and running a workflow, assert:

```java
mockMvc.perform(get("/api/workflow-runs"))
  .andExpect(status().isOk())
  .andExpect(jsonPath("$.data.items[0].status").value("SUCCEEDED"));
```

- [ ] **Step 2: Run red backend test**

Expected: FAIL because `GET /api/workflow-runs` is missing.

- [ ] **Step 3: Implement run listing**

Add `listWorkflowExecutions()` to store and service. Controller endpoint:

- `GET /api/workflow-runs`

Return `PageResponse<WorkflowExecutionResponse>`.

- [ ] **Step 4: Write failing frontend run history test**

Mock `listWorkflowRuns` and assert `运行历史`, status, and detail link render.

- [ ] **Step 5: Implement frontend pages**

`WorkflowRunsPage` renders filter toolbar and table.

`WorkflowRunDetailPage` renders:

- status
- input JSON
- output JSON
- node executions table

- [ ] **Step 6: Run green tests**

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=WorkflowRunHistoryControllerIntegrationTest test
corepack pnpm --filter @aiworkflow/admin test -- WorkflowRunsPage.test.tsx
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add server/src/main/java/com/aiworkflow/workflow server/src/test/java/com/aiworkflow/workflow/api web/apps/admin/src
git commit -m "feat: add workflow run history"
```

## Task 8: PostgreSQL Persistence Layer

**Files:**
- Modify: `server/pom.xml`
- Create: `server/src/main/java/com/aiworkflow/workflow/service/JdbcWorkflowStore.java`
- Create: `server/src/main/java/com/aiworkflow/workflow/engine/JdbcWorkflowExecutionStore.java`
- Create: `server/src/main/java/com/aiworkflow/config/StoreConfig.java`
- Modify: `server/src/main/resources/db/migration/V1__foundation_schema.sql`
- Create: `server/src/main/resources/db/migration/V2__ai_studio_schema.sql`
- Test: `server/src/test/java/com/aiworkflow/workflow/service/JdbcWorkflowStoreTest.java`

- [ ] **Step 1: Add JDBC dependency**

Add to `server/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

- [ ] **Step 2: Write failing JDBC store test**

Use `@JdbcTest` and embedded datasource when available. Test saves workflow and version then reads it back through `JdbcWorkflowStore`.

- [ ] **Step 3: Run red test**

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml -Dtest=JdbcWorkflowStoreTest test
```

Expected: FAIL because store does not exist.

- [ ] **Step 4: Implement JDBC stores**

Use `JdbcTemplate` and Jackson `ObjectMapper`.

`JdbcWorkflowStore` persists:

- `workflow`
- `workflow_version.definition_json`

`JdbcWorkflowExecutionStore` persists:

- `workflow_execution`
- `workflow_node_execution`

- [ ] **Step 5: Implement StoreConfig**

Use in-memory stores when no `JdbcTemplate` bean is available. Use JDBC stores when datasource exists.

- [ ] **Step 6: Add V2 migration**

Create `V2__ai_studio_schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS prompt_template (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    template TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS model_provider (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    base_url TEXT NOT NULL,
    api_key_ref VARCHAR(200) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

- [ ] **Step 7: Run backend tests**

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml test
```

Expected: PASS. If local Docker is unavailable, note that live PostgreSQL startup is not verified.

- [ ] **Step 8: Commit**

```powershell
git add server/pom.xml server/src/main/java/com/aiworkflow/config server/src/main/java/com/aiworkflow/workflow server/src/main/resources/db/migration server/src/test/java/com/aiworkflow/workflow/service
git commit -m "feat: add PostgreSQL workflow persistence"
```

## Task 9: Final Integration Verification

**Files:**
- Modify: `docs/integration-guide.md`
- Modify: `docs/superpowers/specs/2026-05-28-complete-ai-studio-design.md` only if implementation changes an approved detail.

- [ ] **Step 1: Update integration guide**

Document:

- how to start backend with PostgreSQL
- how to start frontend
- how to create a workflow
- how to run a workflow
- how to embed React designer
- how to use SDK

- [ ] **Step 2: Run full backend tests**

Run:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml test
```

Expected: PASS.

- [ ] **Step 3: Run frontend tests**

Run:

```powershell
corepack pnpm --filter @aiworkflow/admin test
corepack pnpm --filter @aiworkflow/workflow-designer-core test
corepack pnpm --filter @aiworkflow/workflow-sdk test
```

Expected: PASS.

- [ ] **Step 4: Run frontend build**

Run:

```powershell
corepack pnpm build
```

Expected: PASS. Vite chunk-size warnings are acceptable.

- [ ] **Step 5: Smoke test local app**

Start backend:

```powershell
$env:JAVA_HOME='D:\devtools\JetBrains\WebStorm 2024.3.1.1\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -f server/pom.xml spring-boot:run
```

Start frontend:

```powershell
corepack pnpm --filter @aiworkflow/admin dev
```

Verify:

- `http://127.0.0.1:5173` returns 200
- `http://127.0.0.1:8080/api/workflows` returns 200

- [ ] **Step 6: Commit docs**

```powershell
git add docs
git commit -m "docs: update AI Studio integration guide"
```

- [ ] **Step 7: Clean status**

Run:

```powershell
git status --short
```

Expected: no output.

