# 企业级 AI 工作流编排平台设计文档

## 1. 背景

本项目目标是使用 Java Spring Boot 构建一个企业级 AI 工作流编排平台。

第一版采用自研轻量 DAG 引擎，不引入 Flowable、Camunda、Activiti 等传统 BPMN 流程引擎。平台聚焦 AI 工作流最需要的能力：模型调用、Prompt 编排、知识库检索、工具调用、条件分支、上下文传递、执行日志、权限控制和基础审计。

第一版不做强审批、强 BPMN、复杂人工任务、OA 式流程表单和人工流程 SLA。

## 2. 产品目标

构建一个支持私有化部署的 AI 工作流平台，让企业可以把大模型、知识库、Prompt、业务 API 和条件逻辑组合成可运行、可观测、可审计的自动化 AI 流程。

核心目标：

- 业务人员可以通过可视化方式创建和运行 AI 工作流。
- 开发人员可以把模型、工具、Prompt、知识库注册为可复用能力。
- 第三方系统可以通过标准 API、SDK、Webhook 和嵌入式组件集成工作流能力。
- 运维人员可以查看执行链路、失败原因、Token 消耗、调用成本和系统健康状态。
- 管理员可以控制用户权限、模型密钥、知识库权限和操作审计。
- 后端先采用模块化单体，后续根据规模演进为微服务。

## 3. 第一版范围

### 3.1 包含范围

- 用户登录。
- 基础角色权限控制。
- 工作流创建、编辑、发布、归档。
- 工作流版本管理。
- 基于 JSON 的 DAG 流程定义。
- DAG 合法性校验。
- 工作流执行记录。
- 节点执行记录。
- 工作流执行状态追踪。
- 节点输入输出上下文传递。
- 节点失败重试。
- 节点超时控制。
- 条件分支。
- LLM 调用节点。
- Prompt 模板节点。
- 知识库检索节点。
- HTTP 工具调用节点。
- 文本处理节点。
- 开始节点和结束节点。
- 模型供应商配置。
- API Key 加密存储。
- Prompt 模板管理。
- 知识库文档上传、解析、切分、向量化和检索。
- 操作审计。
- 执行审计。
- Token 用量和模型调用成本统计。
- 基础监控看板。
- 面向第三方的 OpenAPI 文档。
- 第三方应用 API Key 管理。
- 工作流运行 API。
- 工作流执行结果查询 API。
- Webhook 回调。
- TypeScript SDK 和 Java SDK 的生成基础。

### 3.2 不包含范围

- BPMN 导入、导出或兼容。
- Flowable、Camunda、Activiti 集成。
- 复杂审批链。
- 人工任务领取、转派、委托、升级。
- 人工流程 SLA。
- 审批表单设计器。
- 完整多 Agent 协作。
- 插件市场。
- 低代码页面搭建器。

## 4. 用户角色

### 4.1 平台管理员

管理用户、角色、权限、租户、模型供应商、系统配置、API Key 和审计策略。

### 4.2 AI 应用构建者

创建工作流，配置 Prompt，接入知识库，注册工具，调试节点，发布工作流版本。

### 4.3 业务使用者

运行已发布的工作流，查看自己有权限访问的执行结果。

### 4.4 运维人员

查看工作流运行状态、节点耗时、失败率、模型调用成本、Token 消耗和系统告警。

### 4.5 第三方开发者

通过 API、SDK、Webhook 或嵌入式前端组件，把工作流创建、运行、查询和执行结果通知能力集成到其他企业系统中。

## 5. 推荐技术栈

### 5.1 后端

- Java 21
- Spring Boot 3.x
- Spring Web MVC
- Spring Security
- Spring Authorization Server 或 Keycloak
- Spring AI
- MyBatis Plus 或 Spring Data JPA
- springdoc-openapi
- PostgreSQL
- Redis
- RabbitMQ
- MinIO
- pgvector
- OpenTelemetry
- Prometheus
- Grafana
- Resilience4j
- Flyway 或 Liquibase

### 5.2 前端

- React
- TypeScript
- Ant Design React
- Ant Design Pro Components
- LogicFlow 或 AntV X6 作为框架无关的工作流画布引擎
- Monaco Editor
- ECharts
- TanStack Query
- Zustand
- Vite

平台管理后台明确选择 Ant Design React 生态，便于后期集成到已有 Ant Design React 前端框架中。

工作流设计器不直接绑定 React Flow。设计器核心应封装为框架无关的 TypeScript 包，底层优先评估 LogicFlow 或 AntV X6。React 项目通过 React Adapter 集成，Vue 项目通过 Vue Adapter 集成，其他系统可以通过 Web Component 或原生 DOM API 集成。

### 5.2.1 工作流设计器前端包

建议将工作流设计器拆成独立包：

```text
packages
├── workflow-schema           DAG 类型、节点类型、校验规则
├── workflow-designer-core    画布核心，基于 LogicFlow 或 X6
├── workflow-designer-react   React 适配层
├── workflow-designer-vue     Vue 适配层
├── workflow-designer-wc      Web Component 适配层
└── workflow-sdk              TypeScript SDK
```

`workflow-designer-core` 不能依赖 React、Vue、Ant Design 或 Ant Design Vue。它只负责画布渲染、节点拖拽、连线、选择、缩放、撤销重做、导入导出、事件派发和 DAG JSON 转换。

React 和 Vue 适配层只负责把宿主系统的属性、事件、权限、主题和右侧配置面板接入核心设计器。

### 5.3 AI 模型供应商

- OpenAI 兼容 API
- Azure OpenAI
- DeepSeek
- 通义千问
- 智谱 AI
- Moonshot
- Ollama

### 5.4 部署

- Docker Compose
- Nginx
- GitHub Actions 或 GitLab CI
- Kubernetes 作为后续生产扩展方案

### 5.5 第三方集成

- REST API 作为主要集成协议。
- OpenAPI 3 文档作为 API 契约。
- springdoc-openapi 自动生成后端接口文档。
- OpenAPI Generator 生成 TypeScript SDK 和 Java SDK。
- OAuth2 Client Credentials 用于企业级服务间集成。
- API Key 用于轻量第三方调用。
- HMAC 签名用于 Webhook 安全校验。
- SSE 或 WebSocket 用于执行过程实时推送。
- Webhook 用于执行完成、执行失败、节点失败等事件通知。
- Web Component 用于低成本嵌入第三方前端系统。

## 6. 总体架构

第一版建议采用模块化单体。所有模块运行在一个 Spring Boot 应用中，但代码边界清晰，便于后续拆分。

```text
aiworkflow-server
├── auth              用户、角色、权限、租户
├── workflow-core     工作流定义、节点、边、版本
├── workflow-engine   DAG 执行、调度、上下文、状态机
├── ai-model          模型供应商、模型调用、Token 统计
├── prompt            Prompt 模板、变量渲染、版本管理
├── rag               知识库、文档解析、向量检索
├── tool              HTTP 工具、企业 API 调用
├── integration       OpenAPI、SDK、Webhook、第三方应用
├── audit             操作日志、执行审计
├── monitor           指标、成本、运行状态
└── common            公共类型、异常、工具类
```

## 7. 模块职责

### 7.1 auth

负责登录认证、用户管理、角色管理、权限控制和租户隔离。

### 7.2 workflow-core

负责工作流基础信息、DAG 定义、节点定义、边定义、版本管理和发布状态。

### 7.3 workflow-engine

负责 DAG 校验、执行计划生成、节点调度、上下文传递、失败重试、超时控制和执行状态持久化。

### 7.4 ai-model

负责模型供应商配置、模型路由、模型调用、Token 统计、成本估算和模型错误归一化。

### 7.5 prompt

负责 Prompt 模板、模板变量、模板版本、Prompt 渲染和调试。

### 7.6 rag

负责知识库、文档上传、文档解析、文本切分、Embedding、向量存储、向量检索和引用来源返回。

### 7.7 tool

负责 HTTP 工具注册、参数 Schema、认证配置、请求发送、响应映射和调用审计。

### 7.8 integration

负责第三方应用管理、API Key、OAuth2 客户端、OpenAPI 文档、SDK 生成契约、Webhook 订阅、Webhook 投递记录、幂等控制和外部调用限流。

### 7.9 audit

负责操作日志、执行审计、安全敏感事件记录。

### 7.10 monitor

负责工作流运行指标、节点耗时、模型调用成本、Token 消耗、错误率和运行趋势。

## 8. 工作流数据模型

### 8.1 Workflow

工作流是业务上的流程实体。

核心字段：

- `id`
- `tenantId`
- `name`
- `description`
- `status`：`DRAFT`、`PUBLISHED`、`ARCHIVED`
- `currentVersionId`
- `createdBy`
- `createdAt`
- `updatedAt`

### 8.2 WorkflowVersion

工作流版本是可执行的不可变快照。发布后的版本不允许直接修改。

核心字段：

- `id`
- `workflowId`
- `version`
- `definitionJson`
- `status`：`DRAFT`、`PUBLISHED`、`DISABLED`
- `publishedBy`
- `publishedAt`

### 8.3 DAG Definition

流程定义使用 JSON 存储，前端设计器和后端执行引擎共享同一份结构。

示例：

```json
{
  "nodes": [
    {
      "id": "start_1",
      "type": "START",
      "name": "开始",
      "config": {}
    },
    {
      "id": "llm_1",
      "type": "LLM",
      "name": "生成回答",
      "config": {
        "modelId": "deepseek-chat",
        "promptTemplateId": "customer_reply_v1",
        "temperature": 0.3,
        "timeoutSeconds": 60
      }
    }
  ],
  "edges": [
    {
      "id": "edge_1",
      "sourceNodeId": "start_1",
      "targetNodeId": "llm_1",
      "condition": null
    }
  ],
  "variables": [
    {
      "name": "question",
      "type": "STRING",
      "required": true
    }
  ]
}
```

## 9. 节点类型设计

### 9.1 START

接收工作流输入，初始化执行上下文。

### 9.2 END

收集最终输出，结束工作流执行。

### 9.3 LLM

调用指定模型，输入为渲染后的 Prompt 或上下文变量，输出为文本或结构化 JSON。

### 9.4 PROMPT

根据模板和上下文变量渲染 Prompt，并把渲染结果写入上下文。

### 9.5 KNOWLEDGE_RETRIEVAL

从一个或多个授权知识库中检索相关片段，返回片段内容和引用来源。

### 9.6 HTTP_TOOL

调用已注册的 HTTP API，把上下文变量映射为请求参数，并把响应字段写回上下文。

### 9.7 CONDITION

根据上下文表达式判断执行分支。

### 9.8 TEXT_TRANSFORM

执行确定性的文本处理，例如拼接、提取、JSON Path 选择、格式转换。

## 10. 执行引擎设计

执行引擎读取已发布的工作流版本，并基于版本中的 DAG 定义执行。

执行流程：

1. 校验工作流版本状态。
2. 校验输入变量。
3. 创建工作流执行记录。
4. 解析 DAG JSON。
5. 校验 DAG 是否存在唯一开始节点、至少一个结束节点、无环、无孤立必要节点。
6. 构建节点依赖图。
7. 从 START 节点开始执行。
8. 根据依赖完成情况和条件分支选择下游节点。
9. 执行节点并记录输入、输出、耗时和状态。
10. 将成功节点输出合并到执行上下文。
11. 根据最终状态将工作流标记为 `COMPLETED`、`FAILED`、`CANCELED` 或 `TIMEOUT`。

第一版优先实现顺序执行和条件分支。DAG 数据结构保留并行扩展空间，但并行调度不是 MVP 必需能力。

## 11. 执行状态模型

### 11.1 工作流执行状态

- `PENDING`
- `RUNNING`
- `COMPLETED`
- `FAILED`
- `CANCELED`
- `TIMEOUT`

### 11.2 节点执行状态

- `PENDING`
- `READY`
- `RUNNING`
- `SKIPPED`
- `COMPLETED`
- `FAILED`
- `TIMEOUT`

### 11.3 执行上下文

执行上下文使用 JSON 对象表示，在节点之间传递。

示例：

```json
{
  "input": {
    "question": "如何重置密码？"
  },
  "nodes": {
    "retrieval_1": {
      "chunks": []
    },
    "llm_1": {
      "answer": "..."
    }
  },
  "output": {
    "answer": "..."
  },
  "metadata": {
    "tenantId": "tenant_001",
    "workflowExecutionId": "exec_001"
  }
}
```

## 12. 错误处理

系统需要统一错误分类，便于前端展示、审计和监控。

错误类型：

- `VALIDATION_ERROR`
- `CONFIG_ERROR`
- `MODEL_CALL_ERROR`
- `TOOL_CALL_ERROR`
- `RAG_ERROR`
- `TIMEOUT_ERROR`
- `RATE_LIMIT_ERROR`
- `INTERNAL_ERROR`

每个节点支持以下执行策略：

- `timeoutSeconds`
- `maxRetries`
- `retryIntervalSeconds`
- `continueOnError`

当 `continueOnError` 为 `false` 时，节点失败会导致工作流失败，除非该节点处于未命中的条件分支。

## 13. RAG 设计

第一版 RAG 模块支持：

- 创建知识库。
- 上传文档到 MinIO。
- 在 PostgreSQL 保存文档元数据。
- 文档文本解析。
- 文本切分。
- Embedding 生成。
- 向量写入 pgvector。
- 相似度检索。
- 基于元数据的简单过滤。
- 返回文档名称、片段 ID、来源位置等引用信息。

检索流程：

1. 从工作流上下文获取查询文本。
2. 校验当前用户和工作流是否有目标知识库权限。
3. 调用 Embedding 模型生成查询向量。
4. 从 pgvector 检索 Top K 片段。
5. 将片段内容和引用信息写入工作流上下文。

## 14. 模型供应商设计

模型调用通过统一接口抽象，不让业务逻辑直接依赖具体供应商。

统一接口能力：

- Chat Completion。
- Embedding。
- Token 估算。
- 响应格式归一化。
- 错误格式归一化。

模型供应商配置字段：

- 供应商类型。
- Base URL。
- API Key 引用。
- 默认模型。
- 超时时间。
- 限流配置。
- 启用状态。

API Key 必须加密存储，前端不展示明文。

## 15. 工具调用设计

第一版只支持 HTTP 工具。

工具定义字段：

- 名称。
- 描述。
- HTTP Method。
- URL。
- Headers。
- 认证方式。
- 输入 Schema。
- 输出映射。
- 超时时间。
- 重试策略。

HTTP_TOOL 节点负责把上下文变量映射为请求参数，并把响应数据映射回执行上下文。

## 16. 安全与治理

第一版安全要求：

- 登录认证。
- 基于角色的权限控制。
- 租户级数据隔离。
- 工作流级权限。
- 知识库级权限。
- API Key 加密存储。
- 第三方应用 Client ID 和 Client Secret 管理。
- OAuth2 Client Credentials 授权。
- API Key 调用权限范围控制。
- Webhook HMAC 签名。
- 第三方调用幂等键。
- 操作审计日志。
- 工作流执行审计日志。
- 基础接口限流。
- 前端错误信息脱敏。

## 17. 第三方集成设计

平台需要从第一版开始把“可集成”作为核心能力，而不是后期补丁。

### 17.1 集成方式

第一版支持以下集成方式：

- REST API：第三方系统调用工作流运行、查询、取消等接口。
- TypeScript SDK：供 Ant Design React 前端或 Node.js 服务调用。
- Java SDK：供企业 Java 后端系统调用。
- Webhook：工作流完成、失败、节点异常时主动通知外部系统。
- React 适配组件：供 Ant Design React 或其他 React 系统嵌入。
- Vue 适配组件：供 Vue 系统嵌入。
- Web Component：供任意前端系统以原生标签方式嵌入。

### 17.2 API 契约

所有对外接口必须满足：

- 使用 `/openapi/v1/**` 作为第三方稳定 API 前缀。
- 使用 `/api/**` 作为平台自身管理后台 API 前缀。
- 对外 API 保持版本化，避免破坏第三方集成。
- 请求和响应使用稳定 DTO，不直接暴露数据库实体。
- 错误响应使用统一结构。
- 关键写操作支持 `Idempotency-Key`。
- 所有外部 API 进入审计日志。

统一错误响应示例：

```json
{
  "code": "WORKFLOW_NOT_FOUND",
  "message": "Workflow does not exist or is not accessible.",
  "requestId": "req_001",
  "details": {}
}
```

### 17.3 运行接口

核心开放接口：

- 创建工作流运行。
- 查询工作流运行状态。
- 查询工作流运行结果。
- 取消工作流运行。
- 查询节点执行详情。
- 查询可用工作流列表。

建议路径：

- `POST /openapi/v1/workflows/{workflowId}/runs`
- `GET /openapi/v1/workflow-runs/{runId}`
- `GET /openapi/v1/workflow-runs/{runId}/result`
- `POST /openapi/v1/workflow-runs/{runId}/cancel`
- `GET /openapi/v1/workflow-runs/{runId}/nodes`

### 17.4 Webhook

Webhook 事件类型：

- `workflow.run.completed`
- `workflow.run.failed`
- `workflow.run.canceled`
- `workflow.run.timeout`
- `workflow.node.failed`

Webhook 投递要求：

- 每次投递包含事件 ID。
- 每次投递包含时间戳。
- 每次投递包含 HMAC 签名。
- 投递失败后按退避策略重试。
- 保存投递记录和响应状态。
- 第三方系统可以用事件 ID 去重。

### 17.5 SDK

SDK 不手写第一版完整实现，优先通过 OpenAPI Generator 从 OpenAPI 契约生成。

第一版至少保证：

- TypeScript SDK 可用于浏览器前端和 Node.js。
- Java SDK 可用于 Spring Boot 服务。
- SDK 支持 API Key 和 OAuth2 Token。
- SDK 暴露工作流运行、查询、取消和结果获取能力。
- SDK 错误类型与后端错误响应保持一致。

## 18. 可观测性

平台需要采集以下指标：

- 工作流执行次数。
- 工作流成功率。
- 工作流失败率。
- 节点执行耗时。
- 模型调用耗时。
- Token 用量。
- 模型调用成本。
- 工具调用失败率。
- 知识库检索耗时。

使用 OpenTelemetry 串联工作流执行、节点执行、模型调用、知识库检索和工具调用链路。

## 19. 数据库表建议

第一版建议包含以下表：

- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `workflow`
- `workflow_version`
- `workflow_execution`
- `workflow_node_execution`
- `model_provider`
- `model_config`
- `prompt_template`
- `prompt_template_version`
- `knowledge_base`
- `knowledge_document`
- `knowledge_chunk`
- `tool_definition`
- `integration_app`
- `integration_app_secret`
- `webhook_subscription`
- `webhook_delivery`
- `audit_log`
- `api_key_secret`

向量数据可以存储在 `knowledge_chunk` 的 pgvector 字段中，也可以拆分成独立向量表。

## 20. API 分组

第一版 API 分为管理后台 API 和第三方开放 API。

管理后台 API 建议按以下路径分组：

- `/api/auth/**`
- `/api/users/**`
- `/api/workflows/**`
- `/api/workflow-versions/**`
- `/api/workflow-executions/**`
- `/api/model-providers/**`
- `/api/prompts/**`
- `/api/knowledge-bases/**`
- `/api/tools/**`
- `/api/audit-logs/**`
- `/api/metrics/**`
- `/api/integration-apps/**`
- `/api/webhooks/**`

第三方开放 API 建议按以下路径分组：

- `/openapi/v1/workflows/**`
- `/openapi/v1/workflow-runs/**`
- `/openapi/v1/knowledge-bases/**`
- `/openapi/v1/tools/**`

## 21. 前端页面

第一版建议实现以下页面：

- 登录页。
- 工作流列表页。
- 工作流可视化设计器。
- 工作流版本历史页。
- 工作流执行详情页。
- 模型供应商管理页。
- Prompt 模板管理页。
- 知识库管理页。
- HTTP 工具管理页。
- 第三方应用管理页。
- Webhook 管理页。
- 审计日志页。
- 基础监控看板。

## 22. MVP 里程碑

### 22.1 基础工程

- Spring Boot 工程。
- Ant Design React 前端工程。
- 登录认证。
- RBAC。
- PostgreSQL 迁移脚本。
- Docker Compose 基础环境。

### 22.2 工作流定义

- 工作流 CRUD。
- 工作流版本管理。
- DAG JSON 模型。
- DAG 校验。
- 基于设计器核心的可视化编辑器原型。

### 22.3 执行引擎

- 执行记录创建。
- 节点执行生命周期。
- 上下文传递。
- START、END、CONDITION、TEXT_TRANSFORM 节点。
- 执行详情 API。

### 22.4 AI 节点

- 模型供应商管理。
- Prompt 模板管理。
- PROMPT 节点。
- LLM 节点。
- Token 用量记录。

### 22.5 RAG

- 知识库 CRUD。
- 文档上传。
- 文档切分。
- Embedding。
- pgvector 检索。
- KNOWLEDGE_RETRIEVAL 节点。

### 22.6 第三方集成

- OpenAPI 文档。
- 第三方应用管理。
- API Key 调用。
- OAuth2 Client Credentials 调用。
- 工作流运行开放 API。
- Webhook 订阅和投递。
- TypeScript SDK 生成。
- Java SDK 生成。

### 22.7 工具与运维

- HTTP 工具注册。
- HTTP_TOOL 节点。
- 操作审计。
- 执行审计。
- 指标看板。
- Docker Compose 部署。

## 23. 关键设计决策

- 第一版采用模块化单体，降低开发、部署和调试复杂度。
- 第一版采用自研轻量 DAG 引擎，因为 AI 工作流更需要灵活节点、上下文传递和模型工具编排，而不是 BPMN 兼容。
- 前端明确选择 Ant Design React 生态，不再使用 Vue 方案。
- 工作流设计器采用框架无关核心，降低后期集成到 React、Vue 和其他前端系统的成本。
- 工作流定义使用 JSON，保证前端设计器和后端执行引擎的数据结构一致。
- 第三方集成使用 REST API、OpenAPI、SDK 和 Webhook，不把管理后台内部接口直接暴露为开放接口。
- 已发布的工作流版本不可变，保证执行审计和问题追溯可靠。
- 第一版使用 pgvector，减少基础设施复杂度；当向量规模扩大后，再评估 Milvus 或 Qdrant。
- RabbitMQ 用于异步执行事件；简单调试场景保留同步执行入口。

## 24. 待确认决策

- 持久化框架选择 MyBatis Plus 还是 Spring Data JPA。
- MVP 是否必须完整支持多租户，还是先在表结构中预留 `tenantId`。
- 本地开发默认模型供应商选择 DeepSeek、OpenAI 兼容 API 还是 Ollama。
- 第一版是否需要并行分支执行，还是只支持顺序执行加条件分支。
- 第三方集成第一版是否必须支持 OAuth2，还是先支持 API Key，OAuth2 放在第二阶段。
- 设计器底层最终选择 LogicFlow 还是 AntV X6。
- Vue 适配层和 Web Component 第一版是否交付，还是先完成设计器核心和 React 适配层。

## 25. 验收标准

第一版满足以下条件即可认为 MVP 成功：

- 用户可以登录系统。
- 用户可以创建工作流。
- 用户可以通过可视化设计器配置 DAG。
- 用户可以发布不可变工作流版本。
- 用户可以输入参数并运行工作流。
- 执行引擎可以运行 START、END、CONDITION、TEXT_TRANSFORM、PROMPT、LLM、KNOWLEDGE_RETRIEVAL、HTTP_TOOL 节点。
- 用户可以查看工作流执行记录和节点执行详情。
- 系统可以记录模型调用、Token 用量和失败原因。
- 知识库检索可以返回文本片段和来源信息。
- HTTP 工具可以被注册并在工作流中调用。
- 管理员可以管理模型供应商、Prompt、知识库和工具。
- 第三方系统可以通过开放 API 创建工作流运行并查询结果。
- 第三方系统可以配置 Webhook 并收到工作流完成或失败事件。
- 可以基于 OpenAPI 契约生成 TypeScript SDK 和 Java SDK。
- 系统具备基础权限控制和审计日志。
