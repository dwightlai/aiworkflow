# AI Workflow Platform — Architecture Document

## 1. Overview

AI Workflow Platform is a full-stack, extensible AI workflow automation platform. Users can visually design DAG-based workflows, connect to LLM providers, build knowledge bases with vector search, create AI chatbots, and execute workflows with execution tracing — all from a unified admin interface.

### 1.1 Technology Stack

| Layer | Technology |
|-------|-----------|
| **Backend Framework** | Spring Boot 3.3.5 (Java 17) |
| **API Documentation** | SpringDoc OpenAPI 3.0 (Swagger UI) |
| **Database** | PostgreSQL (default) / DamengDB (China) |
| **ORM** | MyBatis-Plus 3.5.9 |
| **Database Migration** | Flyway |
| **Service Discovery** | Nacos (optional, disabled by default) |
| **Inter-service RPC** | OpenFeign + Spring Cloud LoadBalancer |
| **Security** | Spring Security (permit-all; auth delegated to external service) |
| **Document Parsing** | Apache Tika 2.9.2 |
| **Build** | Maven |
| **Frontend Framework** | React 18.3 + TypeScript 5.6 |
| **UI Library** | Ant Design 5.21 + @ant-design/pro-components |
| **Build Tool** | Vite 5.4 |
| **Server State** | @tanstack/react-query 5.59 |
| **Charts** | ECharts 5.5 |
| **Testing** | Vitest 2.1 + Testing Library + JUnit 5 |
| **Package Manager** | pnpm (workspaces monorepo) |

### 1.2 Repository Structure

```
aiworkflow/
├── server/                          # Spring Boot backend
│   ├── src/main/java/com/aiworkflow/
│   │   ├── AiWorkflowApplication.java
│   │   ├── common/                  # Shared: ApiResponse, GlobalExceptionHandler
│   │   ├── config/                  # Security, OpenAPI, StoreConfig
│   │   ├── persistence/             # JsonSupport (JSON serialization)
│   │   ├── bot/                     # AI Chat Bot module
│   │   ├── integration/             # Feign clients for external services
│   │   ├── knowledge/               # Knowledge Base + Vector Store
│   │   ├── model/                   # Model Provider management
│   │   ├── prompt/                  # Prompt Template management
│   │   └── workflow/                # Workflow engine (DAG + execution)
│   ├── src/main/resources/
│   │   ├── application.yml          # Main config
│   │   ├── application-local.yml    # Profile: no DB, in-memory stores
│   │   ├── application-nacos.yml    # Profile: Nacos enabled
│   │   └── db/migration/            # Flyway migrations
│   │       ├── postgresql/          # V1-V13 PostgreSQL migrations
│   │       └── dameng/              # V1 Dameng migration
│   └── pom.xml
│
└── web/                             # Frontend monorepo (pnpm workspaces)
    ├── apps/
    │   └── admin/                   # Admin SPA (@aiworkflow/admin)
    │       ├── src/
    │       │   ├── main.tsx          # React entry
    │       │   ├── App.tsx           # Root component + routing
    │       │   ├── routes.tsx        # Path-based router
    │       │   ├── navigation.ts     # Navigation helper
    │       │   ├── api/              # API client modules (fetch-based)
    │       │   ├── layout/           # AdminShell, PageHeader, menu
    │       │   └── pages/            # Page components
    │       └── vite.config.ts
    └── packages/
        ├── workflow-schema/          # Shared types + validation
        ├── workflow-designer-core/   # Framework-agnostic designer logic
        ├── workflow-designer-react/  # React designer component
        ├── workflow-designer-vue/    # Vue 3 adapter (stub)
        ├── workflow-designer-wc/     # Web Component designer
        └── workflow-sdk/            # API client SDK
```

---

## 2. Backend Architecture

### 2.1 Layered Architecture

Every domain module follows the same layered structure:

```
api/          -- REST Controllers (Spring MVC)
  ├── XxxController.java
  └── XxxRequest/Response DTOs (Java records)
domain/       -- Domain Models (Java records)
  ├── Xxx.java
  └── Enums
service/      -- Business Logic
  ├── XxxService.java
  ├── XxxStore.java (interface)
  ├── InMemoryXxxStore.java
  └── MybatisXxxStore.java
persistence/  -- MyBatis-Plus Entities + Mappers
  ├── XxxEntity.java
  └── XxxMapper.java
```

### 2.2 Store Pattern (InMemory vs MyBatis)

Each domain has a **Store interface** with two implementations, selected at startup via `StoreConfig`:

```
                     StoreConfig
                         |
            +------------+------------+
            |                         |
    ObjectProvider<Mapper>        (no Mapper bean)
    available?                    available?
            |                         |
     MybatisXxxStore            InMemoryXxxStore
     (PostgreSQL/​Dameng)        (ConcurrentHashMap)
```

- **Production**: PostgreSQL (or Dameng) with MyBatis-Plus
- **Local/Test**: `application-local.yml` excludes DataSource auto-config, in-memory stores used
- **Profiles**: `default`, `local`, `nacos`, `dameng`

### 2.3 Domain Modules

#### 2.3.1 Workflow Engine (`com.aiworkflow.workflow`)

The core execution engine. Users create DAG workflows composed of 10 node types.

**Workflow Definition Model:**

```
Workflow (1) --- (*) WorkflowVersion (1) --- (1) WorkflowDefinition
                                                   ├── WorkflowNode[]
                                                   ├── WorkflowEdge[]
                                                   └── WorkflowVariable[]
```

**Node Types (10):**

| Node Type | Class | Description |
|-----------|-------|-------------|
| START | `StartNodeExecutor` | Entry point, passes input through |
| END | `EndNodeExecutor` | Selects output keys |
| LLM | `LlmNodeExecutor` | Calls LLM via `ChatModelClient` |
| PROMPT | `PromptNodeExecutor` | Template substitution `{{ token }}` |
| KNOWLEDGE_RETRIEVAL | `KnowledgeRetrievalNodeExecutor` | Queries knowledge base |
| HTTP_TOOL | `HttpToolNodeExecutor` | HTTP request (GET/POST/PUT/PATCH) |
| CONDITION | `ConditionNodeExecutor` | Branching (equals, contains, etc.) |
| TEXT_TRANSFORM | `TextTransformNodeExecutor` | Text transformation |
| CONTENT_TEMPLATE | `ContentTemplateNodeExecutor` | Structured template rendering |
| LOOP | `LoopNodeExecutor` | Loops over array items |

**Execution Flow** (`WorkflowExecutionService`):

```
1. Load published WorkflowVersion → WorkflowDefinition
2. Find START node
3. While not at END:
   a. Execute current node via WorkflowNodeExecutor
   b. Merge node output into execution context
   c. Follow edge or explicit nextNodeId (from condition)
   d. Persist NodeExecution
4. Return WorkflowExecutionResult (execution + node traces)
```

**DAG Validation** (`DagValidator`):
- Exactly 1 START node required
- At least 1 END node required
- All node IDs unique
- All edges reference valid nodes
- No cycles (DFS-based detection)

**Template Rendering** (`TemplateRenderer`):
- Syntax: `{{ variable.path.to.value }}`
- Dot-path resolution with nested object support
- Used by LLM, Prompt, ContentTemplate, TextTransform nodes

#### 2.3.2 Knowledge Base (`com.aiworkflow.knowledge`)

**Data Model:**

```
KnowledgeBase
  ├── embeddingModelId → ModelProvider (embeddings)
  ├── vectorStoreConfigId → VectorStoreConfig (Elasticsearch)
  ├── splitterType: FIXED_LENGTH | PARAGRAPH | SEMANTIC | SYMBOL | STRUCTURED_TABLE
  ├── retrievalMode: keyword | vector | hybrid
  └── (1) --- (*) KnowledgeDocument
                     ├── datasetType: MANUAL | FILE_UPLOAD | TEXT_DATASET | TABLE_DATASET
                     ├── processingStatus
                     ├── tags, category, source
                     └── (1) --- (*) KnowledgeChunk
                                  └── (1) --- (1) KnowledgeChunkVector (embedding JSON)
```

**Document Splitting** (5 strategies):

| Strategy | Description |
|----------|-------------|
| FIXED_LENGTH | Split by character count |
| PARAGRAPH | Split by paragraph boundaries |
| SEMANTIC | Semantic boundary detection |
| SYMBOL | Split by custom separators |
| STRUCTURED_TABLE | Row-based table splitting |

**Embedding Pipeline:**
1. `DocumentTextExtractor` (Apache Tika) → raw text
2. `KnowledgeDocumentSplitter` → chunks
3. `EmbeddingClient` → vectors
   - `LocalEmbeddingClient` (64-dim character hash, fallback)
   - `ModelProviderEmbeddingClient` (Ollama HTTP API)

**Search Modes:**
- **Keyword**: Text matching
- **Vector**: Cosine similarity (in-memory) or Elasticsearch KNN
- **Hybrid**: Combined keyword + vector

**Vector Store:**
- `ElasticsearchVectorStoreClient` — HTTP-based ES client
- `VectorStoreConfig` — ES connection (endpoint, index, auth, timeouts)

#### 2.3.3 Bot (`com.aiworkflow.bot`)

AI chatbot with multi-turn conversation support:

```
AiBot
  ├── workflowId → Workflow (optional, bot can work without workflow)
  ├── modelProviderId → ModelProvider (for direct LLM chat)
  ├── knowledgeBaseId → KnowledgeBase (for RAG)
  ├── systemPrompt, openingMessage
  └── status: ENABLED | DISABLED

BotSession (1) --- (*) BotMessage
                        ├── role: USER | ASSISTANT
                        └── content
```

**Bot Execution Modes:**
- **With Workflow**: Triggers a workflow run, returns execution result
- **Without Workflow**: Direct LLM chat via `ChatModelClient`

**Chat API** (`/api/bots/{id}/chat`):
- Creates or continues a session
- Maintains message history
- Returns session + messages + reply + optional workflow execution

#### 2.3.4 Model Provider (`com.aiworkflow.model`)

Manages LLM and embedding model configurations:

```
ModelProvider
  ├── modelType: model type identifier
  ├── modelUsage: CHAT | EMBEDDING
  ├── baseUrl, model, apiKeyRef
  ├── visionSupport, pricePerMillionTokens
  └── enabled: boolean
```

**Clients:**
- `ChatModelClient` (interface) → `StubChatModelClient` (stub implementation)
- `EmbeddingClient` (interface) → `LocalEmbeddingClient` | `ModelProviderEmbeddingClient`

#### 2.3.5 Prompt Template (`com.aiworkflow.prompt`)

Simple prompt template CRUD with name, template string, and description.

#### 2.3.6 Integration (`com.aiworkflow.integration`)

External service integration via Feign clients:

```
AuthenticationClient → auth-service
  ├── introspectToken(TokenIntrospectionRequest)
  └── getUser(userId)

OrganizationClient → organization-service
  ├── getUser(userId)
  ├── getDepartment(departmentId)
  └── listUserDepartments(userId)
```

### 2.4 Common Infrastructure

#### Cross-cutting Concerns:

| Concern | Implementation |
|---------|---------------|
| **Error Handling** | `GlobalExceptionHandler` (@RestControllerAdvice) — maps exceptions to structured `ApiResponse<Void>` |
| **API Envelope** | `ApiResponse<T>` — all responses wrapped with `success`, `data`, `error` |
| **Response Format** | `{ success: boolean, data: T, error: { code, message, requestId, details } }` |
| **Validation** | Jakarta Bean Validation (`@Valid`, `@NotBlank`) |
| **Auth** | Spring Security (CSRF disabled, all requests permitted; auth delegated to external auth-service) |

#### Configuration Profiles:

| Profile | Database | Store | Nacos |
|---------|----------|-------|-------|
| default | PostgreSQL | MyBatis | Disabled |
| local | None | InMemory | Disabled |
| nacos | PostgreSQL | MyBatis | Enabled |
| dameng | DamengDB | MyBatis | Disabled |

### 2.5 Database Schema

16 tables (all `agi_` prefixed), managed by Flyway migrations:

| Table | Module | Key Columns |
|-------|--------|-------------|
| `agi_workflow` | Workflow | id, tenant_id, name, status (DRAFT/PUBLISHED/ARCHIVED), current_version_id |
| `agi_workflow_version` | Workflow | id, workflow_id, version, definition (JSON), status |
| `agi_workflow_execution` | Workflow | id, workflow_id, status, input, output, error_message |
| `agi_workflow_node_execution` | Workflow | id, workflow_execution_id, node_id, node_type, status, input, output |
| `agi_integration_app` | Integration | id, name, status |
| `agi_webhook_subscription` | Integration | id, event, url |
| `agi_prompt_template` | Prompt | id, name, template, description |
| `agi_model_provider` | Model | id, name, model_type, model_usage, base_url, model, api_key_ref |
| `agi_vector_store_config` | Knowledge | id, name, store_type, endpoint, index_name, auth fields |
| `agi_knowledge_base` | Knowledge | id, name, embedding_model_id, vector_store_config_id, retrieval_mode, top_k |
| `agi_knowledge_document` | Knowledge | id, knowledge_base_id, name, dataset_type, processing_status, raw_content |
| `agi_knowledge_chunk` | Knowledge | id, knowledge_base_id, document_id, content, index, enabled |
| `agi_knowledge_chunk_vector` | Knowledge | id, chunk_id, embedding (JSON TEXT) |
| `agi_ai_bot` | Bot | id, name, workflow_id, model_provider_id, knowledge_base_id, system_prompt |
| `agi_bot_session` | Bot | id, bot_id, title |
| `agi_bot_message` | Bot | id, session_id, bot_id, role (USER/ASSISTANT), content |

---

## 3. Frontend Architecture

### 3.1 Monorepo Structure

```
pnpm workspaces
├── apps/admin          (@aiworkflow/admin)
└── packages/
    ├── workflow-schema         (@aiworkflow/workflow-schema)
    ├── workflow-designer-core  (@aiworkflow/workflow-designer-core)
    ├── workflow-designer-react (@aiworkflow/workflow-designer-react)
    ├── workflow-designer-vue   (@aiworkflow/workflow-designer-vue)
    ├── workflow-designer-wc    (@aiworkflow/workflow-designer-wc)
    └── workflow-sdk            (@aiworkflow/workflow-sdk)
```

### 3.2 Admin App (@aiworkflow/admin)

#### Routing

Custom path-based router (`routes.tsx`). No React Router. Navigation via `history.pushState` + synthetic `popstate` event.

**Implemented Pages (12):**

| Path | Page | Description |
|------|------|-------------|
| `/` | DashboardPage | Metrics, recent workflows, run health |
| `/workflows` | WorkflowCardsPage | Workflow card grid with filtering |
| `/workflows/:id/designer` | WorkflowDesignerPage | Visual DAG designer |
| `/workflow-runs` | WorkflowRunsPage | Execution history |
| `/workflow-runs/:id` | WorkflowRunDetailPage | Single execution detail |
| `/bots` | BotsPage | Bot CRUD + chat |
| `/models` | ModelProvidersPage | Model provider CRUD |
| `/prompts` | PromptTemplatesPage | Prompt template CRUD |
| `/knowledge` | KnowledgeBasesPage | KB + vector store management |
| `/knowledge/:id/documents` | KnowledgeDocumentsPage | Document list |
| `/knowledge/:id/documents/new` | KnowledgeDocumentCreatePage | Create document |
| `/knowledge/:id/documents/:docId/edit` | KnowledgeDocumentEditPage | Edit document |

#### API Layer

Per-domain API modules using raw `fetch` with a private `requestJson<T>` wrapper:

| Module | Endpoints Covered |
|--------|------------------|
| `api/workflows.ts` | Workflow CRUD, publish, archive, draft update, runs |
| `api/bots.ts` | Bot CRUD, run, chat, sessions, messages |
| `api/models.ts` | Model provider CRUD |
| `api/knowledge.ts` | KB CRUD, documents, chunks, vector store, upload, search |
| `api/prompts.ts` | Prompt template CRUD |

Vite dev proxy: `/api` → `http://localhost:8080`, `/openapi` → `http://localhost:8080`

#### State Management

- **Server state**: `@tanstack/react-query` (`useQuery` / `useMutation` / `useQueryClient`)
- **UI state**: Component-local `useState`
- **No global state management** library in use

#### Layout

```
AdminShell
├── Sidebar (collapsible, Ant Design Menu)
│   ├── Group: AI Function (Dashboard, Bots, Workflows, etc.)
│   └── Group: System Management (Users, Roles, etc.)
└── Main Content Area
    ├── PageHeader (toggle, breadcrumb, title)
    └── Page Content
```

### 3.3 Shared Packages

#### Workflow Designer Architecture

```
┌──────────────────────────────────────────┐
│  @aiworkflow/workflow-schema             │  ← Pure types + validation
│  (WorkflowNode, WorkflowEdge, etc.)      │
├──────────────────────────────────────────┤
│  @aiworkflow/workflow-designer-core      │  ← Framework-agnostic logic
│  (layout, mutation, value management)    │
├────────────┬────────────┬────────────────┤
│  React     │  Vue       │  Web Component │  ← Rendering layers
│  (full)    │  (stub)    │  (read-only)   │
└────────────┴────────────┴────────────────┘
```

- **workflow-schema**: TypeScript type definitions for all 10 node types, `validateWorkflowDefinition()`, `createEmptyWorkflowDefinition()`
- **workflow-designer-core**: Topological layout engine, create/move/delete/connect operations, auto-layout
- **workflow-designer-react**: Full interactive canvas with pan/zoom, drag nodes, connect handles, SVG bezier edges, node selection, toolbar
- **workflow-designer-vue**: Vue 3 adapter (stub, no Vue component rendering)
- **workflow-designer-wc**: Web Component `<ai-workflow-designer>` with Shadow DOM
- **workflow-sdk**: `AiWorkflowClient` class for external API consumers

### 3.4 Data Flow

```
Browser                    Vite Dev Proxy           Backend
┌──────────┐               ┌──────────┐            ┌───────────┐
│ React    │──fetch()────→│ /api/*   │──proxy───→│ Spring    │
│ Query    │              │ :8080    │            │ Boot      │
│ Cache    │←──JSON──────│          │←──────────│ :8080     │
└──────────┘               └──────────┘            └───────────┘
                                                         │
                                                    ┌────┴────┐
                                                    │ PostgreSQL│
                                                    │ / Dameng │
                                                    └─────────┘
```

---

## 4. API Design

### 4.1 Total Endpoints: 45+

| Controller | Base Path | Endpoints |
|------------|-----------|-----------|
| KnowledgeBase | `/api/knowledge-bases` | 22 (CRUD, documents, chunks, upload, preview, search, backfill, reparse) |
| Workflow | `/api/workflows` | 6 (list, create, get, update draft, publish, archive) |
| WorkflowRun | `/api` | 3 (run, get execution, list executions) |
| Bot | `/api/bots` | 8 (CRUD, run, chat, sessions, messages) |
| VectorStoreConfig | `/api/vector-store-configs` | 4 (CRUD) |
| ModelProvider | `/api/model-providers` | 4 (CRUD) |
| PromptTemplate | `/api/prompts` | 4 (CRUD) |
| OpenWorkflowRun | `/openapi/v1` | 2 (create run, get run status) |
| IntegrationApp | `/api/integration-apps` | 1 (list) |

Full API documentation: [API.md](./API.md)

### 4.2 Response Conventions

- Success: `{ success: true, data: T, error: null }`
- Failure: `{ success: false, data: null, error: { code, message, requestId, details } }`
- Pagination: `{ items: T[], total: number }` wrapped in ApiResponse

---

## 5. Deployment

### 5.1 Backend

```bash
cd server
mvn clean package -DskipTests
java -jar target/aiworkflow-server-*.jar
```

Environment variables:
- `POSTGRES_JDBC_URL`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`
- `NACOS_DISCOVERY_ENABLED`, `NACOS_SERVER_ADDR`
- `ORGANIZATION_SERVICE_NAME`, `AUTH_SERVICE_NAME`
- `FLYWAY_LOCATIONS` (default: `classpath:db/migration/postgresql`)

### 5.2 Frontend

```bash
cd web
pnpm install
pnpm --filter @aiworkflow/admin dev    # Development (port 5173)
pnpm --filter @aiworkflow/admin build  # Production build
```

---

## 6. Key Architectural Decisions

1. **Interface/Implementation pattern (Store)** — enables swapping between in-memory and database persistence, supporting local development without a database

2. **Strategy pattern for workflow nodes** — each node type has a dedicated executor, auto-registered via `WorkflowNodeExecutorRegistry`

3. **Workflow versioning** — DAG definitions are immutable snapshots; publish creates a new version

4. **Multi-database support** — Flyway migrations for both PostgreSQL and DamengDB

5. **Monorepo with layered packages** — shared schema, core logic, then framework-specific rendering (React / Vue / Web Component)

6. **No frontend router library** — lightweight path-based routing suffices for the admin SPA

7. **React Query for server state** — no global state management; all data flows through query cache

8. **API key-based SDK** — external consumers can use `@aiworkflow/workflow-sdk` to integrate

9. **Feign for inter-service communication** — declarative HTTP clients with Nacos service discovery (optional)

10. **Template rendering with `{{ path.syntax }}`** — consistently used across LLM, Prompt, ContentTemplate, and HTTP nodes
