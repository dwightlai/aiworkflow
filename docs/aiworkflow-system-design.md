# AI Workflow 系统设计说明书

> 版本：v2.0  
> 日期：2026-06-17  
> 范围：AI Workflow 管理端、后端服务、Chat 端、OpenAPI Client、工作流设计器组件  
> 状态：基于当前代码实现重新整理

---

## 1. 概述

### 1.1 编写目的

本文描述 AI Workflow 当前系统设计，包括业务边界、模块划分、核心流程、接口分组、数据模型、权限与集成方式。本文用于需求评审、研发交接、测试设计、第三方系统集成和部署实施。

### 1.2 系统定位

AI Workflow 是企业 AI 应用平台，提供以下能力：

- 面向管理员：模型、知识库、工作流、智能体、连接器、编研模板、租户权限和系统配置。
- 面向业务用户：Chat 对话、智能体问答、智能编研。
- 面向第三方系统：OpenAPI、Java Feign Client、嵌入式 Chat、工作流设计器组件。

### 1.3 当前边界

| 范围内 | 范围外或后续增强 |
|---|---|
| 同步 DAG 工作流执行 | 分布式异步调度、队列化执行 |
| 知识库上传、解析、分段、检索 | 大规模向量集群运维 |
| Bot 多轮会话与 SSE | 完整 IM/客服坐席系统 |
| 智能编研 Markdown/content_json/DOCX | 在线协同编辑器 |
| 租户组织、第三方应用、资产授权 | 强制全局鉴权拦截 |
| Nacos/Feign 微服务适配 | 完整服务拆分治理 |

---

## 2. 总体架构

```text
┌────────────────────────────────────────────────────────────┐
│ 前端层                                                     │
│ Admin SPA / Chat SPA / Workflow Designer React-Vue-WC      │
└───────────────┬────────────────────────────────────────────┘
                │ HTTP JSON / SSE / OpenAPI
┌───────────────▼────────────────────────────────────────────┐
│ Spring Boot 后端                                           │
│ workflow knowledge model bot chat generation auth system   │
└───────────────┬────────────────────────────────────────────┘
                │
┌───────────────▼────────────────────────────────────────────┐
│ 持久化与外部能力                                           │
│ PostgreSQL / Flyway / MyBatis-Plus / 文件存储 / ES / LLM   │
└────────────────────────────────────────────────────────────┘
```

### 2.1 部署单元

当前代码以一个 Spring Boot 后端进程为主，前端为 Vite 构建产物。支持：

- 后端单 Jar 部署。
- 管理端和 Chat 端独立构建部署。
- 通过 Nacos 将后端注册为一个服务。
- 通过 OpenFeign 调用第三方认证服务和组织机构服务。

### 2.2 前端应用

| 应用/包 | 路径 | 说明 |
|---|---|---|
| Admin | `web/apps/admin` | 管理端，端口 5173 |
| Chat | `web/apps/chat` | 终端对话应用，端口 5174 |
| Schema | `web/packages/workflow-schema` | 工作流类型和校验 |
| Designer Core | `web/packages/workflow-designer-core` | 框架无关设计器核心 |
| Designer React | `web/packages/workflow-designer-react` | 管理端使用 |
| Designer Vue | `web/packages/workflow-designer-vue` | Vue 系统集成 |
| Designer WC | `web/packages/workflow-designer-wc` | Web Component 集成 |

---

## 3. 功能模块设计

### 3.1 工作台

展示平台概览、工作流数量、运行态势、常用入口和近期执行信息。对应前端 `DashboardPage`。

### 3.2 工作流

核心页面：

- 工作流列表：卡片化展示，支持新建、编辑、设置、归档、恢复、删除。
- 工作流设计器：节点库、画布、属性 Drawer、调试 Drawer、发布和运行。
- 运行监控：执行列表。
- 执行详情：输入输出、节点时间线、错误诊断。

业务规则：

- 新建工作流默认只有开始节点，允许保存不完整草稿。
- 发布时后端校验 DAG 完整性。
- 运行使用已发布版本。
- 发布版本快照不可变。

### 3.3 工作流节点配置

| 节点 | 属性设计 |
|---|---|
| 开始 | 输入参数、默认输入 JSON |
| 知识库 | 变量输入、知识库选择、查询文本、Top-K、相似度阈值、输出变量 |
| 大模型 | 变量输入、模型配置、系统消息、用户消息、回复格式、流式输出、输出变量 |
| 问题分类 | 输入问题、分类项、默认分类、分类结果变量 |
| HTTP 请求 | URL、Method、Headers、Body、输出映射、确认策略 |
| 条件 | 上下文变量、操作符、比较值、真假分支 |
| 内容模板 | 文本/JSON 模板、输出 Key |
| 循环 | 集合变量、子步骤、输出变量 |
| 结束 | 输出变量或最终响应模板 |

### 3.4 知识库

知识库模块承担资料接入和检索增强生成。

页面与能力：

- 知识库列表：创建、编辑、删除、基础统计。
- 文档列表：文档管理与切片入口。
- 新增文档：上传文件、选择分段策略、预览切片、确认上传。
- 编辑文档：查看和维护切片。
- 检索测试：关键词/向量/混合检索。
- 向量库配置：Elasticsearch 等外部向量库配置。
- 数据集：按专题或业务来源组织文档与来源索引。

### 3.5 模型配置

模型 Provider 支持 Chat 和 Embedding 两类主要用途。模型字段包括类型、描述、Base URL、API Key、模型名、价格、视觉支持和启用状态。

模型类型包括 DeepSeek、OpenAI、Anthropic、Zhipu、Moonshot、Qwen、MiniMax、VolcEngine、SiliconFlow、Ollama、自定义等。

### 3.6 智能体 Bot

Bot 是对话应用入口。一个 Bot 可选配置：

- 工作流 ID。
- 默认模型。
- 一个或多个知识库。
- 系统提示词。
- 开场白。
- 建议问题。
- 能力插件。

绑定工作流时优先执行工作流；未绑定工作流时走直连模型和知识库 RAG。

### 3.7 连接器

连接器用于维护外部系统能力，包括连接器和操作。HTTP 工作流节点可调用外部 API；Bot 能力和人工确认流程可围绕连接器扩展。

### 3.8 智能编研

智能编研面向档案专题汇编和报告生成：

- 编研模板维护变量、章节、模板类别、DOCX 母版和绑定工作流。
- 智能编研向导提交主题、主题库、知识库、模板和变量。
- 后端通过工作流生成输出，保存任务和成果。
- 支持 Markdown 展示、HTML 预览、content_json、DOCX 渲染下载。

### 3.9 系统管理

包括：

- 租户管理。
- 组织用户。
- 资产授权。
- 第三方应用。
- 开放 API 文档。
- 菜单管理。
- 数据字典。
- 存储路径配置。
- 日志管理。

---

## 4. 核心业务流程

### 4.1 知识库问答流程

```text
创建模型 Provider
  → 创建知识库并选择嵌入模型
  → 上传文档并生成切片
  → 检索测试
  → 编排工作流：开始 → 知识库 → 大模型 → 结束
  → 发布工作流
  → 创建 Bot 绑定工作流
  → Chat 或 OpenAPI 调用
```

### 4.2 工作流发布与执行

```text
保存草稿
  → 后端保存 WorkflowDefinition
  → 发布时 DagValidator 校验
  → 生成 WorkflowVersion
  → 运行时读取发布版本
  → WorkflowExecutionService 执行节点
  → 保存 WorkflowExecution 和 NodeExecution
```

### 4.3 Bot 多轮对话

```text
用户创建/选择会话
  → 发送消息
  → 保存 USER 消息
  → BotService 选择执行方式
     ├─ workflowId 存在：执行工作流
     └─ workflowId 不存在：检索知识库 + 调用模型
  → 保存 ASSISTANT 消息
  → 返回 reply / execution / metadata
```

### 4.4 智能编研流程

```text
维护编研模板并绑定工作流
  → 用户提交主题、受众、知识库、素材
  → ResearchGenerationService 构造 workflowInput
  → 执行工作流
  → 解析大纲、章节、素材、引用
  → 生成 Markdown 与 content_json
  → 按 DOCX 母版渲染文件
  → 保存 GenerationJob 和 GenerationOutput
```

### 4.5 数字档案馆集成流程

推荐两种方式：

| 方式 | 适用情况 |
|---|---|
| 同进程 Jar/模块集成 | 只需要知识库、模型、少量工作流能力，且希望减少独立安检流程 |
| 独立 AGI 服务 + OpenAPI | 需要管理端、Chat、工作流编排、独立扩缩容和升级 |

如果数字档案馆也是 Java Spring 系统，可优先采用 OpenAPI Client 或模块化 Jar 集成。知识库数据集可映射数字档案馆专题库、档案条目、题名、密级、保管期限和来源索引。

---

## 5. 接口设计

### 5.1 管理接口

| 分组 | 前缀 |
|---|---|
| 工作流 | `/api/workflows` |
| 工作流运行 | `/api/workflows/{workflowId}/runs`、`/api/workflow-runs` |
| 知识库 | `/api/knowledge-bases` |
| 知识库数据集 | `/api/knowledge-bases/{kbId}/datasets`、`/api/knowledge-datasets` |
| 检索服务 | `/api/knowledge/retrieval` |
| 向量库 | `/api/vector-store-configs` |
| 模型 | `/api/model-providers` |
| Bot | `/api/bots` |
| Chat | `/api/chat` |
| 连接器 | `/api/connectors` |
| 编研模板 | `/api/generation-templates` |
| 智能编研 | `/api/research` |
| 登录认证 | `/api/auth` |
| 租户组织 | `/api/auth/admin` |
| 资产授权 | `/api/asset-grants` |
| 菜单字典 | `/api/system/menus`、`/api/system/dictionaries` |
| 存储路径 | `/api/system/storage-settings` |
| 审计日志 | `/api/system/audit-logs` |

### 5.2 开放接口

| 分组 | 前缀 | 用途 |
|---|---|---|
| 工作流 | `/api/open/workflows` | 查询可用工作流和执行 |
| 运行记录 | `/api/open/workflow-runs` | 查询执行结果 |
| 知识库 | `/api/open/knowledge-bases` | 查询知识库和检索 |
| 模型 | `/api/open/model-providers` | 查询可用模型 |
| Bot | `/api/open/bots` | Bot 列表、会话、运行、聊天、流式消息 |
| 身份解析 | `/api/open/identity/resolve` | 第三方用户上下文解析 |
| Chat 嵌入 | `/api/open/chat/embed-tickets` | 生成嵌入会话票据 |

### 5.3 统一响应

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

错误响应：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误信息",
    "requestId": "trace-id"
  }
}
```

---

## 6. 数据设计

### 6.1 工作流表

主要实体：

- `agi_workflow`
- `agi_workflow_version`
- `agi_workflow_execution`
- `agi_workflow_node_execution`

设计要点：

- 草稿定义保存在工作流表。
- 发布版本保存 definition 快照。
- 执行记录关联 workflowId 和 workflowVersionId。
- 节点执行记录保存输入、输出、状态、耗时和错误。

### 6.2 知识库表

主要实体：

- `agi_knowledge_base`
- `agi_knowledge_document`
- `agi_knowledge_chunk`
- `agi_knowledge_chunk_vector`
- `agi_vector_store_config`
- `agi_knowledge_dataset`
- `agi_knowledge_source_index`

设计要点：

- 知识库保存默认分段、检索、向量维度和数据集模式。
- 文档保存解析状态、存储路径、来源元数据。
- 切片保存引用文本、来源页码/位置、安全等级。
- 数据集和来源索引用于数字档案馆专题资料同步。

### 6.3 Bot 表

主要实体：

- `agi_bot`
- `agi_bot_session`
- `agi_bot_message`
- `agi_bot_capability`

设计要点：

- Bot 可以不绑定工作流，只绑定模型或知识库。
- Session 支持用户 ID 绑定和置顶。
- Message 保存角色、内容、元数据。
- Capability 支持按关键字路由到特定能力。

### 6.4 智能编研表

主要实体：

- `agi_generation_template`
- `agi_generation_job`
- `agi_generation_output`

设计要点：

- 模板包含 schema、布局、DOCX 配置和绑定工作流。
- Job 保存输入变量、知识库、外部素材和工作流快照。
- Output 保存 Markdown、content_json、DOCX 路径和引用。

### 6.5 权限治理表

主要实体：

- 租户、组织、用户、角色。
- 第三方应用、密钥、Scope。
- 资产授权。
- 系统菜单、字典、审计日志。
- 系统设置。

---

## 7. 权限与安全设计

### 7.1 当前状态

当前 `SecurityConfig` 未启用强制拦截，所有请求 `permitAll`。系统已经具备业务层身份和权限结构，但全局鉴权需在第三方认证接口确定后启用。

### 7.2 租户上下文

业务数据以 `tenantId` 作为隔离字段。请求上下文来源包括：

- 登录态用户。
- OpenAPI Header。
- 默认租户。
- 第三方身份解析。

### 7.3 资产授权

资产授权用于控制 Bot、知识库、工作流和模型的使用范围。授权条件可按：

- 所属单位。
- 部门。
- 角色。
- 第三方应用 Scope。

测试知识库使用权限时，可以在列表层暂不隐藏无管理权限知识库，在运行和检索接口中验证实际授权。

### 7.4 第三方应用认证

开放 API 使用：

- `X-AGI-App-Code`
- `X-AGI-Api-Key`
- 用户/单位/部门/角色上下文 Header

第三方应用可配置密钥、启用状态和 Scope。

---

## 8. 存储与文件设计

### 8.1 本地文件目录

默认配置：

| 配置项 | 默认值 |
|---|---|
| `knowledge-document-dir` | `./data/knowledge-documents` |
| `knowledge-document-save-original` | `true` |
| `research-output-dir` | `./data/generation-outputs` |
| `research-docx-master-dir` | `./data/generation-docx-masters` |

### 8.2 管理端配置

系统管理提供“存储路径配置”页面。接口：

- `GET /api/system/storage-settings`
- `PUT /api/system/storage-settings`
- `POST /api/system/storage-settings/reset`

---

## 9. 非功能设计

### 9.1 可扩展性

- 工作流节点通过策略模式扩展。
- Store 接口支持替换持久化实现。
- 设计器通过 Core + React/Vue/WC 适配多前端框架。
- OpenAPI Client 支持业务系统快速接入。

### 9.2 可观测性

- 每次工作流运行保存执行记录。
- 每个节点保存输入、输出、耗时和错误。
- Bot 消息保存会话历史。
- 智能编研保存任务快照和成果。
- 系统审计记录关键管理操作。

### 9.3 可维护性

- 后端按业务域分包。
- 前端按页面和 API 模块拆分。
- 数据库迁移脚本版本化。
- 关键模块配套 JUnit 和 Vitest 测试。

### 9.4 生产风险

| 风险 | 建议 |
|---|---|
| 全局鉴权未强制启用 | 接入第三方认证后启用 Filter |
| 大文件解析耗时 | 引入异步任务队列 |
| Embedding 批量回填耗时 | 批处理、重试、进度表 |
| OpenAPI Key 明文风险 | 接入密钥加密和 KMS |
| 同步工作流长耗时 | 拆分异步工作流或任务化 |

---

## 10. 验收建议

### 10.1 工作流

- 能创建开始、知识库、大模型、问题分类、HTTP、结束节点。
- 能拖拽连线并配置连线条件。
- 能保存草稿、发布、运行并查看节点记录。

### 10.2 知识库

- 能创建普通知识库和专题型知识库。
- 能上传 PDF/DOCX/XLSX/PPTX/TXT/MD/HTML 并抽取文本。
- 能预览和编辑切片。
- 能检索并在工作流中调用。

### 10.3 Bot

- 能不绑定工作流，仅绑定模型或知识库创建智能体。
- 能多轮对话。
- 能返回知识库引用。
- 能通过 OpenAPI 调用。

### 10.4 智能编研

- 能维护模板并绑定工作流。
- 能提交任务。
- 能生成 Markdown、content_json、DOCX。
- 能下载和预览成果。

### 10.5 集成

- Java OpenAPI Client 能在第三方 Spring 项目中注入 Feign Client。
- OpenAPI Header 能传递用户、单位、角色上下文。
- Chat 前端能通过嵌入票据进入指定 Bot 会话。
