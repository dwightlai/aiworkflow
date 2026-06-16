# AI Workflow 平台产品生成过程记录

| 项目 | 说明 |
|------|------|
| 文档版本 | v1.0 |
| 记录日期 | 2026-06-12 |
| 项目名称 | AI Workflow / AGI 企业 AI 应用平台 |
| 记录范围 | 从 2026-05 立项至 2026-06 当前版本的完整演进过程 |

本文档汇总平台从 0 到 1 的**需求来源、设计决策、实现里程碑、集成方案与文档交付**，便于团队复盘、交接与对外说明。

---

## 1. 项目起源

### 1.1 初始目标

2026-05-27 前后，项目以「企业级 AI 工作流编排平台」为定位启动，核心诉求：

- 用 **Java Spring Boot** 自研轻量 **DAG 工作流引擎**（不引入 Flowable/Camunda）
- 组合大模型、Prompt、知识库检索、HTTP 工具、条件分支
- 提供可视化设计器、管理控制台、运行观测与第三方集成能力

首版设计文档：[superpowers/specs/2026-05-27-ai-workflow-platform-design.md](./superpowers/specs/2026-05-27-ai-workflow-platform-design.md)

### 1.2 参考对象

开发过程中多次参考 **BladeX AI 平台**（`https://ai.bladex.cn/`）的节点属性、操作手册格式与技术文档结构，作为交互与文档样板，但底层引擎与代码均为自研实现。

### 1.3 典型落地场景

后期明确 **数字档案馆** 为首要集成方：知识库问答、档案规范检查、编研生成、Headless 嵌入第三方业务系统。

---

## 2. 演进总览（阶段地图）

```
2026-05-27 ~ 05-28   基础平台（工作流引擎 + 设计器 + SDK）
        ↓
2026-05-28 ~ 06-01   AI Studio 能力补齐（模型/知识库/智能体/Prompt）
        ↓
2026-06-02 ~ 06-05   架构升级（MyBatis + 微服务 + 组织认证 + 达梦适配设计）
        ↓
2026-06-08          工作流节点对齐 BladeX + 变量/异常/多知识库 + 权限体系
        ↓
2026-06-09 ~ 06-10   第三方集成（开放 API + 集成应用 + Headless 方案）
        ↓
2026-06-10 ~ 06-11   系统治理（菜单/字典/日志/审计字段/资产授权树）
        ↓
2026-06-11 ~ 06-12   文档体系 + 演示环境 + 企业智能体 Chat + 真流式 + 开放 API 扩展
```

---

## 3. 第一阶段：工作流基础平台（2026-05-27 ~ 05-28）

### 3.1 需求与目标

- 可视化 DAG 工作流设计、发布、运行
- 执行记录与节点级观测
- React / Vue / Web Component 多形态嵌入
- TypeScript SDK 供第三方调用

### 3.2 主要实现

| 模块 | 内容 | 关键路径/提交 |
|------|------|---------------|
| 工作流引擎 | START/END、Prompt、LLM、条件分支、上下文传递 | `2026-05-28-workflow-engine` 计划 |
| 工作流定义 | JSON DAG、合法性校验、版本与发布 | `2026-05-28-workflow-definition` |
| 管理控制台 | 工作流卡片、设计器、运行历史 | `2026-05-28-admin-workflow-console` |
| 设计器 | LogicFlow 画布、拖拽连线、节点面板 | `workflow-designer-*` 包 |
| SDK | `@aiworkflow/workflow-sdk` | `2026-05-28-sdk-integration` |
| 运行监控 | 执行详情、节点 I/O 展示 | `feat: add workflow run monitoring console` |

### 3.3 产出文档

- [integration-guide.md](./integration-guide.md) — 本地启动与嵌入说明
- [superpowers/plans/2026-05-28-complete-ai-studio.md](./superpowers/plans/2026-05-28-complete-ai-studio.md)

---

## 4. 第二阶段：AI Studio 能力补齐（2026-05-28 ~ 06-01）

### 4.1 需求

在「工作流」之上补齐 AI 应用工厂所需资产：

- 大模型 Provider 配置
- Prompt 模板管理
- 知识库（文档上传、切分、向量化、检索）
- 智能体 Bot（绑定工作流、多轮对话）

### 4.2 主要实现

| 能力 | 说明 |
|------|------|
| 模型管理 | Provider CRUD、运行时解析、Embedding 模型 |
| 知识库 | 文档/切片/向量存储、检索模式、多格式上传 |
| 智能体 | Bot 管理、会话、同步对话 |
| 工作流节点扩展 | 知识库检索、HTTP 工具、模板/循环节点 |
| 持久化 | PostgreSQL + Flyway；JDBC/MyBatis 存储切换 |

### 4.3 代表性 Git 提交

```
fa77a426 feat: add knowledge base management
1cc26591 feat: add bot management
7f5d1ae7 feat: support multi-turn bot conversations
6f8da22d feat: resolve workflow model providers at runtime
```

---

## 5. 第三阶段：架构与企业级底座（2026-06-02 ~ 06-05）

### 5.1 架构调整

| 变更 | 说明 |
|------|------|
| 包名统一 | `com.aiworkflow` → `com.mw.ai.agi` |
| 持久层 | JPA/JDBC → **MyBatis Plus** |
| 表前缀 | 统一 `agi_` |
| 微服务化 | Nacos 注册发现、Feign Client |
| 多数据库 | PostgreSQL 为主，**达梦** 适配设计与迁移脚本 |
| JDK | 支持 JDK 17/21 |

### 5.2 身份与组织

设计并实现：

- 租户、组织、单位、部门、用户、角色
- 认证 Feign 对接外部 auth-service
- 运行时身份上下文 `RuntimeIdentityContext`

设计文档：

- [superpowers/specs/2026-06-05-identity-organization-auth-design.md](./superpowers/specs/2026-06-05-identity-organization-auth-design.md)
- [superpowers/specs/2026-06-05-agent-knowledge-multi-tenant-generation-design.md](./superpowers/specs/2026-06-05-agent-knowledge-multi-tenant-generation-design.md)

### 5.3 智能编研

增加编研模板、章节生成、Markdown 输出等工作流与前端页面（Research 模块）。

---

## 6. 第四阶段：工作流节点深度对齐（2026-06-08）

> 本阶段需求密度最高，通过 Cursor 对话驱动迭代，参考 BladeX 节点属性面板。

### 6.1 节点能力

按用户要求实现并对齐以下节点：

| 节点 | 关键能力 |
|------|----------|
| 开始 | 入参变量按需添加（非默认全有） |
| 知识库检索 | **多知识库**、异常处理、变量选择规则 |
| 大模型 | 系统/用户 Prompt、异常处理、Provider 选择 |
| 问题分类 | 分类项、路由分支、LLM 分类逻辑 |
| HTTP 请求 | 连接器调用 |
| 结束 | 输出变量汇总 |

### 6.2 变量与面板规则（用户明确要求）

- 变量来源仅限：**上一节点输出**、**开始节点变量**、**系统内置变量**
- 「上一节点输出」与「系统变量」位置互换；不展示隐藏/无效上游变量
- 属性面板加宽；异常处理置于输出变量之前
- 不实现「下一节点 / 跳转到节点」属性（界面不展示）

### 6.3 系统变量

补齐可实际注入工作流上下文的系统变量（租户、用户、单位、部门等）。

### 6.4 调试与样例

- 工作流调试按**开始节点入参**驱动
- 自动创建「问题分类」测试流程
- 跑通「知识库测试」工作流并绑定智能体
- 样例工作流：客服问答助手、合同条款抽取

### 6.5 其他

- 工作流逻辑删除（删除后列表不可见）
- 归档工作流支持恢复至发布 Tab
- 设计器连线、箭头样式多轮 UX 优化

---

## 7. 第五阶段：权限与资产授权（2026-06-08 ~ 06-10）

### 7.1 业务规则

依据多租户设计文档实现：

| 资产 | 授权粒度 | 规则要点 |
|------|----------|----------|
| 知识库 | 单位级 | 不含部门维度 |
| 智能体 | 单位 + 部门 | 单位范围：仅本级 / 本级及下属；部门同理 |
| 工作流、大模型 | 纳入资产授权 | 后期补充 |

### 7.2 前端演进

1. 初版：表单式授权（单位/部门多选、范围同行展示）
2. 优化：授权单位/部门改为**树形多选**
3. 拆分：**资产授权**从知识库/智能体维护页剥离，独立菜单（便于 iframe 嵌入第三方时不暴露平台侧授权）

### 7.3 后端

- `AssetGrant` 模型与 `V17__asset_owner_and_grants.sql` 迁移
- 知识库检索、智能体使用、开放 API 调用均做权限校验
- 归属单位默认取创建人所属单位

---

## 8. 第六阶段：第三方集成与开放能力（2026-06-09 ~ 06-11）

### 8.1 集成方案演进

| 方案 | 说明 | 文档 |
|------|------|------|
| 路径 A / 方案 1 | 第三方管用户组织；AGI 管 AI 资产；**集成应用白名单**控制可调 Bot/KB | [agi-hybrid-identity-design.md](./integration/agi-hybrid-identity-design.md) |
| Headless | AGI 纯后端 JAR；档案馆自建管理页；BFF + Feign | [agi-headless-integration-plan.md](./integration/agi-headless-integration-plan.md) |
| 数字档案馆完整设计 | 配置、授权、调用链路 | [digital-archive-integration-design.md](./integration/digital-archive-integration-design.md) |

### 8.2 集成应用（Integration App）

- App Code + API Key 认证
- 资产白名单（智能体、知识库、工作流、大模型）
- 独立菜单「第三方应用」
- Key 列表展示、删除、中文名称显示

### 8.3 开放 API `/api/open/*`

**第一版（2026-06-10 前后）：**

- 智能体：列表、详情、run、chat
- 知识库：列表、详情、search
- 工作流：列表、详情、runs、执行查询
- 大模型：列表、详情（不含密钥）
- 身份：resolve

**第二版（2026-06-12，企业智能体能力扩展）：**

| 类型 | 接口 |
|------|------|
| 新增 | `GET /bots/{id}/capabilities` |
| 新增 | `GET/POST /bots/{id}/sessions` |
| 新增 | `GET /bots/{id}/sessions/{sid}/messages` |
| 新增 | `POST .../messages/stream`（SSE 真流式） |
| 增强 | list/get/chat/run 支持多工作流路由、系统提示词、引用、完整租户上下文 |

### 8.4 客户端 SDK

- `client/aiworkflow-open-api-client` — Open API Feign Client
- `client/aiworkflow-admin-api-client` — 管理 API Feign Client（Headless 管理面）

### 8.5 文档与入口

- OpenAPI YAML：[openapi/open-api.yaml](./openapi/open-api.yaml)
- Admin 菜单「开放 API 文档」→ Swagger UI（分组 `open-api`）
- 对接指南：[open-api-integration-guide.md](./open-api-integration-guide.md)

### 8.6 微服务与 Nacos

- 后端配置 Nacos：`192.168.10.126:8848`
- 服务名 `aiworkflow-server`，支持 Feign 按服务名或 baseUrl 调用

---

## 9. 第七阶段：系统治理与运维（2026-06-10 ~ 06-11）

### 9.1 系统管理功能

| 功能 | 说明 |
|------|------|
| 菜单管理 | 动态菜单、权限绑定 |
| 数据字典 | 字典类型与字典项 |
| 日志管理 | 操作/审计日志查询（含性能索引优化） |
| 租户管理 | 多租户隔离完善 |

### 9.2 审计字段

知识库、智能体、模型及系统管理类数据统一补充：

- `createdBy` / `updatedBy`
- `createdAt` / `updatedAt`

### 9.3 数据库

- Flyway 迁移至 V37+
- 高频查询表补充索引（日志、会话、消息等）

---

## 10. 第八阶段：文档与演示交付（2026-06-11）

### 10.1 文档体系

| 文档 | 路径 | 说明 |
|------|------|------|
| 用户手册 | [aiworkflow-user-manual.md](./aiworkflow-user-manual.md) | 含界面截图，参考 BladeX 操作手册 |
| 技术手册 | [aiworkflow-tech-manual.md](./aiworkflow-tech-manual.md) | 架构、部署、API |
| 系统设计说明书 | [aiworkflow-system-design.md](./aiworkflow-system-design.md) | 功能点细化 |
| 白皮书 | [aiworkflow-whitepaper.md](./aiworkflow-whitepaper.md) | 市场/管理层 |
| 商务演示 | [aiworkflow-business-presentation.md](./aiworkflow-business-presentation.md) | 领导汇报 |

PDF/DOCX 生成脚本：`build-manual-pdf.py`、`build-docs-pdf.py`、`build-system-design-doc.py`、`generate_whitepaper_docx.py`

### 10.2 演示环境

- 路径：`D:\aiworkflow`
- 单体 JAR + 静态前端，`start.bat` / `stop.bat` 一键启停
- 独立库 `aiworkflow_demo` + 演示数据初始化脚本（与开发库 `aiworkflow` 隔离）

---

## 11. 第九阶段：企业 Web 智能体（P1a，2026-06-11 ~ 06-12）

设计依据：[superpowers/specs/2026-06-10-enterprise-agent-web-service-design.md](./superpowers/specs/2026-06-10-enterprise-agent-web-service-design.md)

### 11.1 Chat Portal（`web/apps/chat`，端口 5174）

| 能力 | 状态 |
|------|------|
| 多 Bot 共用 Web 服务 | ✅ |
| 会话列表 / 新建 / 删除 / 重命名 / 置顶 | ✅ |
| DeepSeek 风格 UI（提问框固定底部、用户/助手分区） | ✅ |
| SSE 流式对话 | ✅（P1a-BOT-02 真流式） |
| 思考过程折叠、回答正文默认展开 | ✅ |
| 引用文档置于回答下方、简洁列表 | ✅ |
| 会话标题自动生成（非全部「新对话」） | ✅ |
| Markdown 渲染（`**` 等符号正确显示） | ✅ |

### 11.2 Admin 试聊（智能体管理页）

- `BotRunChatDrawer` 对齐 Chat 风格
- 引用在回答下方；测试消息/附加变量固定底部
- Markdown 渲染

### 11.3 后端运行时

| 模块 | 说明 |
|------|------|
| `ChatModelClient.generateStream` | OpenAI 兼容 SSE 解析 |
| `WorkflowStreamContext` / `SseWorkflowStreamSink` | 工作流级流式回调 |
| `LlmNodeExecutor` | 真流式；节点 system 为空时用 Bot `systemPrompt` |
| `KnowledgeRetrievalNodeExecutor` | 检索后实时 `citation.added` |
| `BotService.streamChat` | 智能体流式对话 |
| 多工作流路由 | `BotCapabilityService` + 问题分类/关键词路由 |
| 引用去重与阈值 | 无关知识库不展示引用 |

### 11.4 开放 API 与智能体对齐

- `OpenBotController` 扩展会话与流式接口
- `OpenApiRequestContext.grantContext()` 注入完整租户/组织上下文
- Feign Client 模型类同步

### 11.5 典型问题与修复（对话驱动）

| 问题 | 处理 |
|------|------|
| Chat 流式期间消息被 refetch 覆盖 | 前端 streaming 状态保护 |
| 系统提示词不生效 | LLM 节点空 system 时用 `{{systemPrompt}}` |
| 知识库混入设计文档 | 移除错误文档、调检索阈值 |
| Swagger 只见「大模型」 | 需重启后端；智能体分组在页面上方 |
| 回答显示 `**` 原文 | 接入 `react-markdown` |

---

## 12. 技术架构（当前）

```
┌─────────────────────────────────────────────────────────────┐
│  前端                                                        │
│  ├── Admin (5173)  工作流/知识库/智能体/系统管理/开放API文档   │
│  └── Chat  (5174)  企业智能体对话门户                          │
├─────────────────────────────────────────────────────────────┤
│  aiworkflow-server (8080)                                    │
│  ├── /api/*        管理 API（JWT）                            │
│  ├── /api/open/*   开放 API（AppCode + ApiKey）               │
│  └── 工作流引擎 / Bot 运行时 / RAG / 连接器                    │
├─────────────────────────────────────────────────────────────┤
│  PostgreSQL + Flyway  │  Nacos  │  Elasticsearch（可选）      │
└─────────────────────────────────────────────────────────────┘

客户端 JAR：
  aiworkflow-open-api-client   → 第三方运行时调用
  aiworkflow-admin-api-client  → Headless 管理面调用
```

---

## 13. 需求 → 实现 追踪表（摘要）

| 用户需求（摘要） | 实现结果 |
|------------------|----------|
| 参考 BladeX 实现 6 类工作流节点 | 节点属性 + 执行器 + 设计器面板 |
| 系统变量可用 | `WorkflowVariableResolver` + 开放 API 上下文注入 |
| 知识库/智能体单位部门授权 | AssetGrant + 树形授权 UI + 运行时校验 |
| 第三方集成不改 AGI 组织数据 | 集成应用 + 白名单 + Headless 设计 |
| 开放 API 文档可在线看 | Swagger 分组 + YAML + 对接指南 |
| 档案馆 Headless 部署 | JAR + BFF + Feign 方案文档 |
| 企业智能体 Web 对话 | Chat Portal P1a |
| 真流式 SSE | LLM/知识库节点 + ChatGateway |
| 演示环境不影响开发 | D:\aiworkflow 独立部署 + demo 库 |
| 档案规范助手 | 系统提示词 + 工作流 LLM 节点变量绑定 |

---

## 14. 当前进度与待办

### 14.1 已完成（P1a 主体）

- 工作流全链路（设计 → 发布 → 调试 → 运行）
- 知识库 RAG + 多库检索
- 智能体配置、试聊、Chat Portal
- 资产授权与开放 API
- 真流式对话与 Markdown 展示
- 文档体系（手册/设计/白皮书/PDF）

### 14.2 规划中（P1b / Phase 2）

依据企业智能体设计文档：

- Embed Widget 嵌入组件
- 连接器调用日志与可观测性
- `agent_job` 长任务运行时完善
- Webhook、SSO 深度集成
- Phase 3：MCP 工具与自主 Agent 调度

---

## 15. 相关文档索引

| 类别 | 文档 |
|------|------|
| 产品设计 | [2026-05-27-ai-workflow-platform-design.md](./superpowers/specs/2026-05-27-ai-workflow-platform-design.md) |
| 企业智能体 | [2026-06-10-enterprise-agent-web-service-design.md](./superpowers/specs/2026-06-10-enterprise-agent-web-service-design.md) |
| 多租户权限 | [2026-06-05-agent-knowledge-multi-tenant-generation-design.md](./superpowers/specs/2026-06-05-agent-knowledge-multi-tenant-generation-design.md) |
| 档案馆集成 | [digital-archive-integration-design.md](./integration/digital-archive-integration-design.md) |
| Headless | [agi-headless-integration-plan.md](./integration/agi-headless-integration-plan.md) |
| 开放 API | [open-api-integration-guide.md](./open-api-integration-guide.md) |
| 用户手册 | [aiworkflow-user-manual.md](./aiworkflow-user-manual.md) |
| 白皮书 | [aiworkflow-whitepaper.md](./aiworkflow-whitepaper.md) |

---

## 16. 开发方式说明

本平台主要采用 **「需求对话 + AI 辅助编码」** 迭代：

1. 用户提供参考产品截图/URL或业务规则
2. 在 Cursor 中分任务实现（后端 Java、前端 React、迁移脚本、文档）
3. 本地 PostgreSQL + 前后端热启动验证
4. 复杂方案先出 MD 设计文档，确认后编码
5. 集成与交付阶段同步产出 OpenAPI、Feign Client、演示包与 PDF 文档

本文档将随版本迭代持续更新；重大里程碑可在文末追加 **变更记录**。

### 变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-06-12 | v1.0 | 首版：汇总 2026-05-27 至 2026-06-12 全链路生成过程 |
