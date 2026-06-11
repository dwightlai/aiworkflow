# 企业 Web 智能体服务设计方案

## 1. 背景与目标

### 1.1 建设背景

企业需要在内网或专有云环境中部署 AI 智能体能力，通过浏览器、企业门户、OA、档案系统、知识库、流程系统等入口，为最终业务用户提供自然语言交互、知识问答、业务查询、流程办理、档案检索、借阅申请、编研生成等能力。

当前平台已具备智能体 Bot、工作流编排、知识库 RAG、Open API、多租户与资产授权等基础能力，但主要面向管理员配置、实施人员集成和开发调试，缺少面向最终业务用户的统一 Web 交付层。

因此，需要在现有能力基础上建设企业 Web 智能体服务，使业务用户可以通过浏览器或嵌入页面直接使用智能体能力。

------

### 1.2 核心定位

本方案明确以下定位：

```text
Bot 是智能体本身；
Web 服务是 Bot 面向最终用户的交付层；
连接器是外部系统能力接入平台的统一抽象；
工作流是智能体执行复杂业务任务的受控调度引擎；
当前 P1a 阶段采用“单 Bot 单主工作流”的受控调度模式；
P1b 起支持轻量多工作流路由；
Phase 2 扩展 Webhook、SSO、审计查询等企业集成能力；
Phase 3 再演进到 MCP 工具调用和自主 Agent 调度能力。
```

------

### 1.3 建设目标

| 目标                     | 说明                                              |
| ------------------------ | ------------------------------------------------- |
| Web 化访问               | 用户无需安装客户端，通过浏览器即可使用智能体      |
| 多 Bot 共用一套 Web 服务 | 不为每个 Bot 单独部署前台，通过 botId 区分智能体  |
| 支持嵌入                 | 支持 Chat Portal、iframe、后续 Embed Widget       |
| 支持受控调度             | P1a 通过 Bot 主工作流调度知识库、连接器、确认节点 |
| 支持业务系统调用         | 可调用 OA、档案、流程、知识库等外部系统           |
| 支持身份透传             | 调用业务系统时携带当前用户身份或 Token            |
| 支持高风险确认           | 提交、审批、修改、删除等操作前必须用户确认        |
| 支持引用溯源             | 展示知识库、档案、业务系统返回内容来源            |
| 支持审计追踪             | 对对话、接口调用、确认、失败等行为留痕            |
| 支持渐进演进             | P1a 快速试点，P1b 产品增强，Phase 2/3 平台化扩展  |

------

## 2. 总体定位与边界

### 2.1 平台定位

企业 Web 智能体服务不是重新建设一套 OA、档案、流程或权限系统，而是在现有业务系统之上提供一层 AI 交互与智能编排能力。

平台定位为：

```text
智能体 Web 交付层
+ Bot 运行时
+ 工作流编排
+ 知识库问答
+ 连接器接入
+ 高风险操作确认
+ 调用审计
```

------

### 2.2 AI 平台负责

| 范围               | 说明                                                |
| ------------------ | --------------------------------------------------- |
| 用户进入智能体服务 | 用户登录、Token 校验、嵌入票据校验                  |
| Bot 使用权限       | 判断用户能否使用某个 Bot                            |
| Bot 能力配置       | 管理员配置 Bot 可使用哪些知识库、工作流、连接器能力 |
| 对话服务           | 会话、消息、流式输出、引用展示                      |
| 受控调度           | P1a 通过 Bot 主工作流完成任务调度                   |
| 高风险确认         | 提交、审批、修改、删除、发送前进行用户确认          |
| 连接器调用         | 通过 HTTP 连接器调用 OA、档案、流程等系统           |
| 审计日志           | 记录对话、接口调用、确认、失败等行为                |

------

### 2.3 业务系统负责

| 范围           | 说明                                     |
| -------------- | ---------------------------------------- |
| 档案数据权限   | 用户能否查看某份档案，由档案系统判断     |
| OA 待办权限    | 用户能否查看某条待办，由 OA 系统判断     |
| 流程办理权限   | 用户能否审批某个流程，由流程系统判断     |
| 业务数据权限   | 用户能否访问某条业务单据，由业务系统判断 |
| 业务操作合法性 | 借阅、审批、提交等业务规则由业务系统判断 |

一句话原则：

```text
AI 平台管“谁能用哪个 Bot、Bot 能调哪些能力”；
业务系统管“这个人能不能看这条数据、办这个业务”。
```

------

### 2.4 不重复建设业务权限

如果档案系统已有档案权限，OA 系统已有待办权限，流程系统已有办理权限，则 AI 平台不再重复维护这些业务权限。

AI 平台调用业务系统时，应透传当前用户身份，由业务系统按原有权限体系判断。

------

## 3. 与现有系统的演进关系

### 3.1 现有能力复用原则

P1a 不重做 Bot Runtime，不替换现有 Bot 会话体系，不破坏 Open API 兼容性。

采用原则：

```text
现有 Bot 能力优先复用；
Web 服务作为新的交付入口；
Chat Gateway 作为包装层，不重写 BotService；
新增能力围绕连接器、HITL、嵌入、安全和审计扩展。
```

------

### 3.2 现有能力映射表

| 设计能力      | 现有模块 / 表                                  | 当前阶段策略                                         |
| ------------- | ---------------------------------------------- | ---------------------------------------------------- |
| Bot 对话      | BotService.chat                                | 复用                                                 |
| Bot 单次运行  | BotService.run                                 | 复用                                                 |
| 管理端试聊    | BotsPage / BotController                       | 保持                                                 |
| Open API      | OpenBotController                              | 保持                                                 |
| Bot 会话      | bot_session / bot_message 或等价表             | P1a 优先复用                                         |
| 工作流执行    | workflow / engine                              | 复用                                                 |
| HTTP 节点     | HTTP_TOOL                                      | 改造支持连接器 Operation                             |
| 知识库检索    | KnowledgeBaseService                           | 复用                                                 |
| Bot / KB 授权 | AssetGrantService                              | 复用                                                 |
| 编研生成      | ResearchGenerationService / agi_generation_job | P1a 复用，Portal 做进度适配                          |
| 连接器        | 无或不完整                                     | 新增 agi_connector、agi_connector_operation          |
| HITL 确认     | 工作流待扩展                                   | 新增 agi_human_confirm_task，与 Workflow resume 打通 |
| 审计日志      | 如已有则扩展                                   | 新增或扩展 agi_agent_audit_log                       |
| Chat Portal   | 无                                             | 新增 apps/chat                                       |

------

### 3.3 Bot 会话演进策略

#### P1a：复用现有会话

P1a 阶段：

```text
Chat Gateway 包装现有 BotService.chat / run；
会话表优先复用现有 bot_session / bot_message；
Chat Portal 只是新增用户入口，不新建一套并行会话体系；
管理端试聊和 Portal 对话可以共用底层 Bot 会话能力。
```

如现有会话表缺少字段，可优先扩展字段，而不是新建完全独立的 chat_conversation / chat_message。

#### P1b / Phase 2：抽象 Chat 会话模型

如果后续 Chat Portal 需要更多前台特性，如多 Bot 工作台、收藏、置顶、分享、文件结果、任务卡片等，可再抽象：

```text
agi_chat_conversation
agi_chat_message
```

但应与现有 Bot 会话保持迁移或兼容关系，避免双轨长期并存。

------

### 3.4 bot_message 富消息扩展

P1a 复用现有 bot_message，但 Chat Portal 会出现文本、引用、确认、进度、错误等不同类型消息。

建议采用：

```text
bot_message.content 存文本内容；
bot_message.metadata JSON 存引用、确认、进度、工具调用等结构化信息；
bot_message.message_type 区分 text / citation / confirm / progress / error 等消息类型。
```

如果现有表没有 metadata 或 message_type 字段，可增加：

```sql
ALTER TABLE bot_message ADD COLUMN metadata TEXT;
ALTER TABLE bot_message ADD COLUMN message_type VARCHAR(32);
```

P1a 存储建议：

| 内容                      | P1a 存储策略                                          |
| ------------------------- | ----------------------------------------------------- |
| 文本消息                  | bot_message.content                                   |
| 引用卡片                  | bot_message.metadata                                  |
| 确认结果                  | bot_message.metadata + agi_human_confirm_task + audit |
| 任务最终结果              | bot_message.metadata                                  |
| 错误消息                  | bot_message.content + metadata                        |
| tool.started 等实时中间态 | 可不全部落消息表，审计表记录即可                      |

------

### 3.5 Bot 能力模型演进

当前现有 Bot 模型主要是：

```text
Bot 绑定单个 workflowId；
Bot 绑定多个 knowledgeBaseIds；
BotService 根据运行模式执行工作流或直连模型 + RAG。
```

#### P1a：保持单主工作流

P1a 不大改 Bot 模型：

```text
每个 Bot 保持一条主 workflowId；
自然语言输入后触发该主工作流；
连接器调用放在工作流 HTTP_TOOL 节点中；
Chat Gateway 不直接调用连接器 Operation。
```

#### P1b：轻量多工作流路由

P1b 可增加 Bot 能力关联表：

```sql
CREATE TABLE agi_bot_capability (
  id                VARCHAR(64) PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  bot_id            VARCHAR(64) NOT NULL,
  capability_type   VARCHAR(32) NOT NULL,
  capability_id     VARCHAR(64),
  capability_code   VARCHAR(128),
  is_primary        BOOLEAN DEFAULT FALSE,
  enabled           BOOLEAN DEFAULT TRUE,
  created_at        TIMESTAMP NOT NULL,
  updated_at        TIMESTAMP NOT NULL
);
```

P1b 支持一个 Bot 多个候选工作流，但意图路由只负责“选工作流”，不直接调接口。

------

## 4. 阶段划分

### 4.1 阶段划分原则

| 阶段    | 定位           | 核心目标                                                     |
| ------- | -------------- | ------------------------------------------------------------ |
| P1a     | 试点闭环版     | 复用现有 Bot，打通 Chat Portal、HTTP 连接器、HITL、ticket、安全和审计 |
| P1b     | 产品增强版     | 增强连接器管理、轻量意图路由、Embed SDK、调用日志页面        |
| Phase 2 | 企业集成增强版 | 完善 SSO、Webhook、组织同步、响应映射、审计查询              |
| Phase 3 | 智能化平台版   | 引入 MCP、多 Agent、动态工具选择、连接器平台化               |

------

### 4.2 P1a 建设目标

P1a 目标是：

```text
可演示、可试点、可闭环、风险可控。
```

P1a 重点完成：

1. 新增 Chat Portal。
2. 复用现有 BotService。
3. 复用现有 Bot 会话。
4. 打通 Bot 使用权限。
5. 打通 HTTP 连接器。
6. 打通工作流 HTTP_TOOL 调用连接器 Operation。
7. 打通 need_confirm 自动确认拦截。
8. 打通 iframe ticket。
9. 打通最小审计。
10. 打通 API 鉴权安全基线。

------

### 4.3 P1a 明确不做

| 暂不做                             | 原因                             | 后续        |
| ---------------------------------- | -------------------------------- | ----------- |
| 多工作流智能路由                   | P1a 保持单主工作流，降低改造风险 | P1b         |
| Chat Gateway 直调 Operation        | 避免形成第二套调度引擎           | 暂不做      |
| MCP 工具调用                       | 当前 HTTP API 足够试点           | Phase 3     |
| Webhook 完整配置平台               | 当前先打通主动调用               | Phase 2     |
| 连接器市场                         | 当前不是生态阶段                 | Phase 3     |
| Operation 版本管理                 | 接口少，人工控制即可             | Phase 2 / 3 |
| 完整 agent_job 替换 generation_job | 避免影响现有编研                 | Phase 2     |
| 多人确认 HITL                      | P1a 只做当前用户确认             | Phase 2 / 3 |
| chunk 级 ACL                       | 当前按 Bot 授权知识库            | 视需求后续  |
| Agent 自主规划                     | 风险高，需治理能力               | Phase 3     |

------

## 5. 总体架构与部署

### 5.1 P1a 总体架构

```text
┌──────────────────────────────────────────────┐
│ 使用入口                                      │
│ Chat Portal / iframe                          │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ Chat Gateway                                  │
│ - 登录校验                                    │
│ - Bot 使用权限校验                            │
│ - 会话包装                                    │
│ - SSE 事件转换                                │
│ - 确认卡片                                    │
│ - 引用卡片                                    │
│ - traceId 生成                                │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 现有 Bot Runtime                              │
│ - BotService.chat / run                       │
│ - Prompt                                      │
│ - 模型调用                                    │
│ - 知识库 RAG                                  │
│ - 主 workflowId                               │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 现有 Workflow Runtime                         │
│ - LLM 节点                                    │
│ - 知识库检索节点                              │
│ - HTTP_TOOL 节点                              │
│ - WAITING_CONFIRM 状态                        │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ HTTP 连接器层                                 │
│ - agi_connector                               │
│ - agi_connector_operation                     │
│ - 身份透传                                    │
│ - need_confirm 自动确认拦截                   │
│ - 调用日志                                    │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 外部业务系统                                  │
│ OA / 档案 / 流程 / 知识库 / 业务系统           │
│ 业务系统自行控制具体数据权限                   │
└──────────────────────────────────────────────┘
```

------

### 5.2 API 分层关系

P1a 阶段 API 关系如下：

```text
Chat Portal
  → /api/chat/*
  → Chat Gateway
  → BotService.chat / run

管理后台试聊
  → /api/bots/{id}/chat
  → BotController
  → BotService

第三方系统服务端调用
  → /api/open/bots/{id}/chat
  → OpenBotController
  → BotService
```

说明：

| API              | 说明                        | 是否改造 |
| ---------------- | --------------------------- | -------- |
| /api/chat/*      | 新增，面向最终用户 Web 服务 | 新增     |
| /api/bots/*      | 现有管理端 API              | 保持兼容 |
| /api/open/bots/* | 现有开放 API                | 保持兼容 |

Chat Gateway 是 Web 交付层 BFF，不替代 BotService。

------

### 5.3 前端部署结构

开发环境建议：

```text
admin: http://localhost:5173
chat:  http://localhost:5174
server: http://localhost:8080
```

生产环境建议同域部署：

```text
/admin → admin 应用
/chat  → chat 应用
/api   → 后端服务
```

建议通过 Nginx 或网关反向代理，减少跨域和 Cookie 安全问题。

------

### 5.4 CORS 策略

开发环境：

```text
允许 localhost:5173
允许 localhost:5174
允许后端 API 调试域名
```

生产环境：

```text
优先同域部署；
如必须跨域，使用 CORS 白名单；
禁止 Access-Control-Allow-Origin: *；
携带 Cookie 时必须限定 Origin。
```

------

## 6. 前端技术方案

### 6.1 技术栈确认

现有前端栈：

| 项         | 现状                            |
| ---------- | ------------------------------- |
| 框架       | React 18 + TypeScript           |
| UI         | Ant Design 5 + Pro Components   |
| 构建       | Vite + pnpm workspace           |
| 数据请求   | TanStack React Query            |
| 智能体管理 | BotsPage 在 admin 中配置 + 试聊 |

结论：

```text
当前前端技术栈与新增 Chat 应用没有冲突。
推荐采用 admin + chat 双应用，共用 React / Ant Design / Vite / pnpm workspace。
Chat 对话 UI 使用 Ant Design X。
```

------

### 6.2 前端应用结构

推荐 monorepo 结构：

```text
web/
  apps/
    admin/        # 现有管理后台：Bot、工作流、知识库、连接器配置
    chat/         # 新增终端用户 Web 智能体入口

  packages/
    chat-ui/      # 可选：公共对话组件，供 admin 试聊和 chat 共用
    api-client/   # 可选：统一封装后端 API
    shared/       # 可选：类型、工具函数、权限常量
```

------

### 6.3 admin 与 chat 边界

| 应用  | 面向对象         | 主要功能                               |
| ----- | ---------------- | -------------------------------------- |
| admin | 管理员、实施人员 | 配 Bot、工作流、知识库、连接器、授权   |
| chat  | 最终业务用户     | 对话、查看引用、确认操作、查看任务进度 |

P1a 不建议把最终用户 Chat Portal 强行塞进 admin 中。

------

### 6.4 Ant Design X 定位

P1a 前端建议采用 Ant Design X 作为 Web 智能体对话 UI 的基础组件库，用于快速实现消息气泡、输入框、对话布局、Markdown 渲染等通用对话能力。

平台不直接依赖 Ant Design X 的数据协议，而是在前端增加一层 Chat Adapter。

Chat Adapter 负责对接平台 SSE 协议，将以下事件转换为统一的 PlatformMessage：

```text
message.delta
tool.started
tool.completed
citation.added
confirm.required
job.progress
error
done
```

引用卡片、确认卡片、任务进度卡片、错误卡片、iframe ticket 嵌入逻辑由平台自研，以保证与连接器、HITL、异步任务、审计和权限体系一致。

Ant Design X 仅作为表现层组件，不作为平台消息协议、调度状态机或业务交互模型的基础依赖。后续如需替换 UI 库，可通过适配层平滑切换。

------

### 6.5 不采用 assistant-ui 作为主方案

assistant-ui 也是 React 生态中的对话 UI 方案，但其风格偏 shadcn，与当前 Ant Design 体系不完全一致。

当前项目已有 Ant Design 5 和 Pro Components，因此 P1a 不建议引入 assistant-ui 作为主方案，避免 UI 风格割裂和组件体系复杂化。

------

### 6.6 chat-ui 组件结构建议

```text
packages/chat-ui/
  components/
    ChatShell
    MessageRenderer
    CitationCard
    ConfirmCard
    ProgressCard
    ErrorCard
    ToolStatusCard

  adapters/
    sseAdapter.ts
    messageNormalizer.ts

  services/
    chatApi.ts
    ticketApi.ts

  types/
    chat.ts
    sse.ts
    message.ts
```

------

## 7. Web 交付层设计

### 7.1 Chat Portal

#### P1a 建设内容

| 功能         | P1a                            |
| ------------ | ------------------------------ |
| Bot 列表     | 显示当前用户有权访问的 Bot     |
| Bot 对话页   | 支持用户和 Bot 多轮对话        |
| Bot 能力说明 | 展示“我能帮你做什么”           |
| 会话列表     | 复用现有 Bot 会话能力          |
| 历史消息     | 复用现有 Bot 消息能力          |
| 流式输出     | 支持 SSE                       |
| 引用卡片     | 展示知识库、档案、外部系统来源 |
| 确认卡片     | 高风险操作前展示确认           |
| 错误提示     | 展示权限不足、接口失败、超时等 |
| 任务进度     | 适配编研任务进度               |

------

### 7.2 P1a 登录方式

P1a Chat Portal 支持两种访问方式。

#### 方式一：独立访问

用户访问 `/chat` 时，如果未登录，则跳转到现有平台登录页。

登录成功后获得平台 JWT，再访问：

```text
/api/chat/**
```

P1a 不单独建设新的 `/chat/login`。

#### 方式二：嵌入访问

用户从 OA、档案、门户等系统进入 iframe。

业务系统后端通过 Integration App 调用：

```text
POST /api/chat/embed-tickets
```

签发短期 ticket。

Chat Gateway 校验 ticket 后创建 Chat Session。

#### 后续演进

Phase 2 再对接企业 SSO / OIDC / SAML。

------

### 7.3 Bot 能力说明

Chat Portal 应在用户进入 Bot 时展示“我能帮你做什么”。

P1a 可优先复用：

```text
Bot.description
Bot.openingMessage
```

P1b 建议增加字段：

```text
capability_hint
suggested_questions
```

示例：

```json
{
  "capabilityHint": "我可以帮你查询档案、申请借阅、解释档案制度、生成编研报告。",
  "suggestedQuestions": [
    "帮我查一下某项目的合同档案",
    "如何申请档案借阅？",
    "帮我生成一份某主题编研报告"
  ]
}
```

------

### 7.4 iframe 嵌入

P1a 支持 iframe 嵌入，采用短期一次性 ticket。

访问方式：

```text
https://ai.example.com/chat/{botId}?ticket=short_lived_ticket
```

正式环境禁止使用长期 token 放入 URL。

------

### 7.5 Embed Widget

P1a 可以先不做完整 JS SDK，优先用 iframe 方式满足嵌入需求。

P1b 再建设基础 Embed Widget。

------

## 8. 嵌入 ticket 机制

### 8.1 设计目标

ticket 用于解决 iframe / 嵌入场景下的安全登录问题。

目标：

1. 不在 URL 中暴露长期 token。
2. 由可信服务端签发。
3. ticket 短期有效。
4. ticket 一次性使用。
5. ticket 绑定 tenantId、userId、botId。
6. ticket 使用后换取服务端 Chat Session。
7. ticket 签发和使用均可审计。

------

### 8.2 签发方

ticket 由以下可信后端签发请求触发：

| 签发请求方   | 场景                           |
| ------------ | ------------------------------ |
| 企业门户后端 | 企业门户嵌入 Chat              |
| OA 后端      | OA 页面嵌入 Chat               |
| 档案系统后端 | 档案详情页嵌入 Chat            |
| SSO 网关     | 统一身份入口生成 Chat 访问票据 |

浏览器前端不能直接签发 ticket。

------

### 8.3 Integration App 代签边界

Integration App 只能为其授权范围内的 tenantId、组织、系统和 Bot 签发 ticket。

签发 ticket 前必须校验：

1. Integration App 是否有效。
2. Integration App 是否属于当前 tenantId。
3. Integration App 是否有目标 botId 的 Scope。
4. userId 是否属于当前 tenant。
5. 如果配置了组织范围，userId 必须在允许组织范围内。

P1a 至少必须校验：

```text
app 有效性；
tenantId；
botId scope。
```

组织范围校验可在 P1b 增强。

------

### 8.4 ticket 签发接口

```http
POST /api/chat/embed-tickets
Authorization: Bearer <integration-app-token>
```

请求体：

```json
{
  "tenantId": "tenant_001",
  "userId": "u001",
  "botId": "bot_archive",
  "expireSeconds": 300,
  "businessContext": {
    "pageType": "archive_detail",
    "archiveId": "A001"
  }
}
```

返回：

```json
{
  "ticket": "tkt_xxx",
  "expireAt": "2026-06-10T10:00:00"
}
```

------

### 8.5 ticket 使用流程

```text
业务系统后端 / 企业门户后端
  ↓ 使用 Integration App 凭证
POST /api/chat/embed-tickets
  ↓
AI 平台签发短期一次性 ticket
  ↓
业务系统前端打开 iframe
  ↓
GET /chat/{botId}?ticket=xxx
  ↓
Chat Gateway 校验 ticket
  ↓
创建服务端 Chat Session / HttpOnly Cookie
  ↓
ticket 标记为 used
```

------

### 8.6 ticket 安全要求

| 要求               | 说明                   |
| ------------------ | ---------------------- |
| 短期有效           | 建议 1 到 5 分钟       |
| 一次性使用         | 使用后失效             |
| 绑定租户           | tenantId 必须匹配      |
| 绑定用户           | userId 必须匹配        |
| 绑定 Bot           | botId 必须匹配         |
| 服务端签发         | 前端不能直接签发       |
| 审计               | 签发、使用、失败均记录 |
| 禁止长期 token URL | 正式环境禁止           |

------

### 8.7 agi_embed_ticket 表

```sql
CREATE TABLE agi_embed_ticket (
  id                VARCHAR(64) PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  user_id           VARCHAR(64) NOT NULL,
  bot_id            VARCHAR(64) NOT NULL,
  ticket            VARCHAR(256) NOT NULL,
  status            VARCHAR(32) NOT NULL,
  expire_at         TIMESTAMP NOT NULL,
  used_at           TIMESTAMP,
  business_context  TEXT,
  source_app_id     VARCHAR(64),
  created_at        TIMESTAMP NOT NULL,
  updated_at        TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_agi_embed_ticket_ticket
ON agi_embed_ticket(ticket);
```

状态：

```text
PENDING
USED
EXPIRED
REVOKED
```

------

## 9. 受控智能调度设计

### 9.1 P1a 当前阶段定位

P1a 不建设独立智能调度子系统。

P1a 采用：

```text
单 Bot 单主工作流
+ 工作流内受控调度
+ 连接器 Operation 调用
+ need_confirm 自动确认拦截
```

也就是说：

```text
用户消息
  ↓
Chat Gateway
  ↓
BotService.chat
  ↓
Bot 绑定的主工作流
  ↓
工作流内完成知识检索、参数处理、HTTP_TOOL 调用、确认拦截
```

P1a 中，Chat Gateway 不直接调用连接器 Operation。

------

### 9.2 P1a 智能能力

| 能力         | P1a                            |
| ------------ | ------------------------------ |
| 自然语言理解 | 由 Bot / LLM 完成              |
| 多轮对话     | 复用 BotService 能力           |
| 参数提取     | 在主工作流中完成               |
| 知识问答     | 复用 RAG                       |
| 业务查询     | 通过工作流 HTTP_TOOL 调连接器  |
| 高风险识别   | 由 Operation.need_confirm 控制 |
| 缺失信息追问 | 工作流或 Bot 提示完成          |
| 多工作流路由 | 不做                           |

------

### 9.3 P1b 轻量意图路由

P1b 可支持一个 Bot 绑定多个候选工作流。

路由只负责选择工作流：

```text
用户输入
  ↓
轻量意图识别
  ↓
选择 workflowId
  ↓
执行工作流
```

P1b 仍不建议 Chat Gateway 直接调用 Operation。

------

### 9.4 Phase 3 自主 Agent 调度

Phase 3 在 MCP、工具权限、审计、安全沙箱成熟后，再支持：

1. Agent 动态选择工具。
2. 自动任务规划。
3. 多 Agent 协作。
4. MCP Tool 调用。
5. 工具调用安全策略。

------

## 10. 简化权限控制

### 10.1 权限原则

当前阶段采用简化权限模型：

```text
平台管入口，业务系统管数据。
```

------

### 10.2 权限分工

| 权限               | 负责方        | 当前阶段处理              |
| ------------------ | ------------- | ------------------------- |
| 用户是否登录       | AI 平台 / SSO | JWT / ticket 校验         |
| 用户能否访问 Bot   | AI 平台       | AssetGrant / 全员开关     |
| Bot 能否调用能力   | AI 平台       | 管理员配置                |
| 用户能否看具体档案 | 档案系统      | AI 平台不重复判断         |
| 用户能否看 OA 待办 | OA 系统       | AI 平台不重复判断         |
| 用户能否办理流程   | 流程系统      | AI 平台不重复判断         |
| 高风险操作确认     | AI 平台       | need_confirm 自动确认拦截 |

------

### 10.3 Bot 使用权限与 AssetGrant

P1a 不新建独立 Bot 授权体系，优先复用现有 AssetGrantService。

权限判断优先级：

```text
1. 租户隔离
2. Bot 状态 enabled
3. Integration App Scope，针对第三方服务端调用
4. AssetGrant 授权，针对 Web 用户
5. Bot 全员可用开关
```

Web 用户访问 Bot：

```text
用户身份
  ↓
tenantId 校验
  ↓
Bot enabled 校验
  ↓
AssetGrant / 全员开关校验
  ↓
允许进入 Chat
```

第三方系统服务端调用 Bot：

```text
Integration App
  ↓
Scope 校验
  ↓
Bot enabled 校验
  ↓
调用 BotService
```

------

## 11. 连接器设计

### 11.1 连接器定义

连接器是平台对外部系统和外部能力的统一抽象，用于将 OA、档案、流程、知识库、MCP Server 等能力接入到智能体和工作流中。

P1a 只实现：

```text
HTTP 连接器
```

后续阶段扩展：

```text
Webhook 回调连接器
MCP 工具连接器
数据库连接器
消息队列连接器
```

------

### 11.2 连接器模型

P1a 采用两级模型：

```text
连接器 Connector
  └── 工具 / 接口 Operation
```

产品界面可展示为：

```text
连接器
  ├── 档案系统连接器
  │     ├── 档案检索
  │     ├── 档案详情
  │     └── 提交借阅申请
  │
  ├── OA 系统连接器
  │     ├── 查询待办
  │     └── 提交审批
  │
  └── 流程系统连接器
        ├── 查询流程
        └── 发起流程
```

------

### 11.3 表命名规范

新增表统一采用 `agi_` 前缀。

已有表保持兼容，不强制重命名。

------

### 11.4 agi_connector

```sql
CREATE TABLE agi_connector (
  id             VARCHAR(64) PRIMARY KEY,
  tenant_id      VARCHAR(64) NOT NULL,
  name           VARCHAR(128) NOT NULL,
  code           VARCHAR(128) NOT NULL,
  type           VARCHAR(64) NOT NULL,
  access_type    VARCHAR(32) NOT NULL,
  base_url       VARCHAR(512),
  auth_mode      VARCHAR(64),
  auth_config    TEXT,
  enabled        BOOLEAN NOT NULL,
  description    TEXT,
  created_at     TIMESTAMP NOT NULL,
  updated_at     TIMESTAMP NOT NULL
);
```

字段说明：

| 字段        | 说明                                       |
| ----------- | ------------------------------------------ |
| name        | 连接器名称，例如档案系统连接器             |
| code        | 连接器编码，例如 archive                   |
| type        | OA / ARCHIVE / WORKFLOW / KB / GENERIC     |
| access_type | HTTP / MCP / WEBHOOK，P1a 只实现 HTTP      |
| base_url    | HTTP 接口根地址                            |
| auth_mode   | USER_TOKEN / API_KEY / FIXED_HEADER / NONE |
| enabled     | 是否启用                                   |

------

### 11.5 agi_connector_operation

```sql
CREATE TABLE agi_connector_operation (
  id                       VARCHAR(64) PRIMARY KEY,
  tenant_id                VARCHAR(64) NOT NULL,
  connector_id             VARCHAR(64) NOT NULL,
  name                     VARCHAR(128) NOT NULL,
  code                     VARCHAR(128) NOT NULL,
  method                   VARCHAR(16),
  path                     VARCHAR(512),
  operation_type           VARCHAR(32) NOT NULL,
  risk_level               VARCHAR(32) NOT NULL,
  need_confirm             BOOLEAN NOT NULL,
  confirm_summary_template TEXT,
  request_template         TEXT,
  enabled                  BOOLEAN NOT NULL,
  description              TEXT,
  created_at               TIMESTAMP NOT NULL,
  updated_at               TIMESTAMP NOT NULL
);
```

字段说明：

| 字段                     | 说明                          |
| ------------------------ | ----------------------------- |
| name                     | 接口名称，例如档案检索        |
| code                     | 接口编码，例如 archive.search |
| method                   | GET / POST                    |
| path                     | 接口路径                      |
| operation_type           | QUERY / ACTION                |
| risk_level               | LOW / HIGH                    |
| need_confirm             | 是否需要用户确认              |
| confirm_summary_template | 确认卡片摘要模板              |
| request_template         | 简单请求模板，可选            |

------

### 11.6 P1a 认证方式

| auth_mode    | 说明               |
| ------------ | ------------------ |
| USER_TOKEN   | 透传当前用户 Token |
| API_KEY      | 使用系统级 API Key |
| FIXED_HEADER | 固定 Header        |
| NONE         | 内网可信服务       |

P1a 暂不支持复杂 OAuth2、动态授权码、复杂签名协议。

------

### 11.7 USER_TOKEN 来源

`auth_mode=USER_TOKEN` 时，Token 来源包括：

| 来源                           | 场景                 |
| ------------------------------ | -------------------- |
| Portal 登录 Token              | 用户直接登录 AI 平台 |
| Embed Ticket 换取身份          | iframe / 嵌入场景    |
| Integration App 传入 userToken | 业务系统服务端代调用 |

IdentityContext 结构：

```json
{
  "tenantId": "tenant_001",
  "userId": "u001",
  "username": "zhangsan",
  "displayName": "张三",
  "unitId": "unit_001",
  "departmentIds": ["dept_001"],
  "roleIds": ["role_clerk"],
  "userToken": "optional_downstream_token",
  "source": "SSO|EMBED|PORTAL|OPEN_API"
}
```

如果没有可透传的业务系统 Token，则使用：

```text
API_KEY + userId / unitId / departmentIds
```

由业务系统根据 userId 进行权限判断。

------

## 12. HTTP_TOOL 与连接器 Operation

### 12.1 改造目标

P1a 不再建议 HTTP_TOOL 节点直接手写完整 URL，而是选择：

```text
connectorCode + operationCode
```

平台根据连接器配置组装请求。

------

### 12.2 节点配置示例

```json
{
  "nodeType": "HTTP_TOOL",
  "connectorCode": "archive",
  "operationCode": "archive.search",
  "inputMapping": {
    "keyword": "{{input.keyword}}",
    "pageNo": "{{input.pageNo}}"
  },
  "outputKey": "archiveSearchResult"
}
```

------

### 12.3 执行流程

```text
HTTP_TOOL 节点开始
  ↓
读取 connectorCode
  ↓
读取 operationCode
  ↓
加载 agi_connector
  ↓
加载 agi_connector_operation
  ↓
校验 enabled
  ↓
判断 need_confirm
  ↓
如需确认，进入 WAITING_CONFIRM
  ↓
确认通过后组装 baseUrl + path
  ↓
注入身份上下文
  ↓
执行 HTTP 请求
  ↓
结果写入 workflow context[outputKey]
```

------

### 12.4 输出约定

P1a 不做复杂响应映射，外部系统返回结果整体挂到 `outputKey`。

示例：

```json
{
  "archiveSearchResult": {
    "success": true,
    "data": {
      "items": [],
      "total": 0
    },
    "raw": {}
  }
}
```

后续节点通过以下方式读取：

```text
{{archiveSearchResult.data.items}}
```

Phase 2 再支持 JSONPath 响应映射。

------

### 12.5 need_confirm 与确认机制

P1a 采用 HTTP_TOOL 自动确认拦截机制。

当 HTTP_TOOL 节点绑定的 Operation 满足：

```text
need_confirm = true
```

时，HTTP_TOOL 在真正调用外部系统接口前自动创建 `agi_human_confirm_task`，并将当前 WorkflowRun 置为：

```text
WAITING_CONFIRM
```

执行流程：

```text
HTTP_TOOL 执行到高风险 Operation
  ↓
发现 need_confirm = true
  ↓
生成 payload_snapshot
  ↓
生成 payload_hash
  ↓
创建 agi_human_confirm_task
  ↓
WorkflowRun 进入 WAITING_CONFIRM
  ↓
SSE 推送 confirm.required
  ↓
用户确认
  ↓
Confirm API 内部调用 Workflow resume
  ↓
继续执行该 HTTP_TOOL 节点
  ↓
真正调用外部系统接口
```

P1a 不要求管理员在工作流中手工配置 HUMAN_CONFIRM 节点。

显式 HUMAN_CONFIRM 节点可作为 P1b / Phase 2 的增强能力，用于非连接器类确认场景。

------

## 13. HITL 高风险确认

### 13.1 统一原则

P1a 只建设一套 HITL 机制。

```text
Operation.need_confirm 是确认触发条件；
HTTP_TOOL 是确认拦截执行点；
agi_human_confirm_task 是确认任务表；
Chat 确认卡片只是前端展示；
用户确认后 resume 工作流；
用户取消后 reject / terminate / branch。
```

不在 Chat 层单独建设第二套确认状态机。

------

### 13.2 HITL 时序

```text
HTTP_TOOL 发现 Operation.need_confirm=true
  ↓
写入 agi_human_confirm_task
  ↓
WorkflowRun 状态变为 WAITING_CONFIRM
  ↓
SSE 推送 confirm.required
  ↓
Chat 前端展示确认卡片
  ↓
用户点击确认 / 取消
  ↓
调用 Confirm API
  ↓
校验任务状态和用户身份
  ↓
确认：Workflow resume
  ↓
取消：Workflow reject / terminate / branch
```

------

### 13.3 agi_human_confirm_task

```sql
CREATE TABLE agi_human_confirm_task (
  id                VARCHAR(64) PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  conversation_id   VARCHAR(64),
  bot_id            VARCHAR(64),
  workflow_run_id   VARCHAR(64),
  node_id           VARCHAR(64),
  user_id           VARCHAR(64) NOT NULL,
  title             VARCHAR(255),
  summary           TEXT,
  payload_snapshot  TEXT,
  connector_code    VARCHAR(64),
  operation_code    VARCHAR(64),
  payload_hash      VARCHAR(128),
  status            VARCHAR(32) NOT NULL,
  expire_at         TIMESTAMP,
  created_at        TIMESTAMP NOT NULL,
  confirmed_at      TIMESTAMP
);
```

------

### 13.4 确认状态

```text
PENDING      待确认
CONFIRMED    已确认
REJECTED     已取消
EXPIRED      已过期
FAILED       执行失败
```

------

### 13.5 确认卡片展示内容来源

用户确认前必须看到关键操作信息。

确认卡片内容来自：

| 内容             | 来源                              |
| ---------------- | --------------------------------- |
| title            | Operation 名称或确认节点配置      |
| summary          | confirm_summary_template 渲染结果 |
| payload_snapshot | HTTP_TOOL 根据最终请求参数生成    |
| payload_hash     | 基于 payload_snapshot 生成        |
| operation_code   | 当前 Operation 编码               |
| connector_code   | 当前连接器编码                    |

payload_snapshot 示例：

```json
{
  "operation": "archive.borrow.submit",
  "archiveTitle": "某项目合同档案",
  "archiveId": "A001",
  "borrowReason": "项目审计需要",
  "applicant": "张三"
}
```

要求：

```text
payload_snapshot 存 JSON，保存用户确认时看到的关键参数；
payload_hash 基于 payload_snapshot 生成；
确认时校验 hash，防止确认内容与实际执行内容不一致。
```

------

### 13.6 确认 API

前端只调用 Confirm API。

```http
POST /api/chat/confirm-tasks/{taskId}/confirm
POST /api/chat/confirm-tasks/{taskId}/reject
```

Confirm API 内部调用 Workflow Runtime：

```text
workflowRuntime.resume(runId, taskId)
workflowRuntime.reject(runId, taskId)
```

P1a 不建议前端直接调用：

```text
/api/workflow-runs/{runId}/resume
/api/workflow-runs/{runId}/reject
```

避免绕过确认任务。

确认校验：

| 校验项                | P1a 是否实现 |
| --------------------- | ------------ |
| 当前用户是否为发起人  | 实现         |
| 任务是否 PENDING      | 实现         |
| 任务是否过期          | 实现         |
| 是否重复提交          | 实现         |
| payload_hash 是否一致 | 建议实现     |
| 多人确认              | 不实现       |

------

## 14. 异步任务与编研任务演进

### 14.1 当前问题

现有系统已有：

```text
agi_generation_job
ResearchGenerationService
```

设计中新增：

```text
agi_agent_job
```

如果不说明关系，会造成两套任务体系并存。

------

### 14.2 P1a 策略

P1a 不替换现有编研任务。

```text
编研任务继续复用 agi_generation_job 和 ResearchGenerationService；
Chat Portal 通过适配层读取编研任务进度；
前端统一展示 progress 事件；
不在 P1a 强行迁移到 agi_agent_job。
```

------

### 14.3 P1b / Phase 2 策略

后续再建设通用任务外壳：

```sql
CREATE TABLE agi_agent_job (
  id               VARCHAR(64) PRIMARY KEY,
  tenant_id        VARCHAR(64) NOT NULL,
  user_id          VARCHAR(64) NOT NULL,
  bot_id           VARCHAR(64),
  conversation_id  VARCHAR(64),
  source_job_id    VARCHAR(64),
  job_type         VARCHAR(64),
  status           VARCHAR(32),
  progress         INTEGER,
  current_step     VARCHAR(255),
  result           TEXT,
  error_message    TEXT,
  created_at       TIMESTAMP NOT NULL,
  updated_at       TIMESTAMP NOT NULL
);
```

其中：

```text
job_type = RESEARCH
source_job_id = agi_generation_job.id
```

------

## 15. SSE 事件协议与流式能力

### 15.1 统一 SSE 接口

P1a 统一使用以下流式消息接口：

```http
POST /api/chat/conversations/{conversationId}/messages/stream
Authorization: Bearer <jwt>
```

该接口表示：

```text
在指定会话中发送一条用户消息，并以 SSE 方式返回 Bot 响应、工具调用状态、引用、确认卡片、任务进度和错误事件。
```

废弃以下不一致写法：

```text
/api/chat/stream
/api/chat/conversations/{conversationId}/stream
```

------

### 15.2 事件类型

| 事件              | 说明             |
| ----------------- | ---------------- |
| message.delta     | 助手文本增量     |
| message.completed | 助手消息完成     |
| tool.started      | 外部接口调用开始 |
| tool.completed    | 外部接口调用完成 |
| tool.failed       | 外部接口调用失败 |
| citation.added    | 新增引用来源     |
| confirm.required  | 需要用户确认     |
| job.started       | 长任务开始       |
| job.progress      | 长任务进度       |
| job.completed     | 长任务完成       |
| error             | 错误             |
| done              | 本轮响应结束     |

------

### 15.3 SSE 示例

```text
event: message.delta
data: {"content":"正在为你查询相关档案..."}

event: tool.started
data: {"connectorCode":"archive","operationCode":"archive.search","name":"档案检索"}

event: citation.added
data: {"title":"某项目合同档案","sourceType":"ARCHIVE","sourceId":"A001"}

event: confirm.required
data: {"taskId":"HT001","summary":"是否提交档案借阅申请？"}

event: done
data: {}
```

------

### 15.4 BotService 流式能力策略

P1a 必须提供 SSE 协议接口。

如果现有 BotService.chat 暂时只支持同步返回，则 Chat Gateway 先将同步结果包装为 SSE 事件，保证前端协议稳定。

包装示例：

```text
同步返回完整 answer
  ↓
Chat Gateway 输出 message.delta
  ↓
Chat Gateway 输出 message.completed
  ↓
Chat Gateway 输出 done
```

如果 LLM 客户端和 Workflow Runtime 已支持流式回调，则 P1a 可进一步实现真正的 message.delta 逐步输出。

因此后端任务拆分为：

| 编号       | 任务                                           | 要求                 |
| ---------- | ---------------------------------------------- | -------------------- |
| P1A-BOT-01 | BotService 结果包装为 SSE 事件                 | 必须实现             |
| P1A-BOT-02 | LLM / Workflow Runtime 支持真正 delta 流式回调 | 建议实现，可后置 P1b |

------

### 15.5 SSE 安全要求

| 项           | P1a 策略                                |
| ------------ | --------------------------------------- |
| 鉴权         | stream 请求必须携带 JWT 或 Chat Session |
| 会话归属     | 只能访问当前用户自己的 conversation     |
| token 防泄露 | 不使用 URL 长期 token                   |
| 断线重连     | P1a 可简化为重新拉取消息 / 任务状态     |
| 单用户连接数 | 建议限制                                |
| 超时关闭     | 建议设置服务端连接超时                  |
| 审计         | 记录 stream 开始、结束、异常            |

------

## 16. API 安全基线

### 16.1 当前问题

如果现有 SecurityConfig 存在 `permitAll`，则不满足企业 Web 智能体上线要求。

P1a 必须增加安全基线改造任务。

------

### 16.2 API 鉴权规则

| API                | 鉴权方式                               |
| ------------------ | -------------------------------------- |
| /api/chat/**       | JWT / Chat Session / ticket 换 session |
| /api/open/**       | Integration App / API Key / Scope      |
| /api/admin/**      | 管理员 JWT                             |
| /api/bots/**       | 管理端 JWT                             |
| /api/connectors/** | 管理端 JWT                             |
| /api/health        | 白名单                                 |
| 静态资源           | 白名单                                 |

------

### 16.3 P1a 安全任务

| 编号       | 任务               | 说明                       |
| ---------- | ------------------ | -------------------------- |
| P1A-SEC-01 | /api/** 强制鉴权   | 默认关闭 permitAll         |
| P1A-SEC-02 | /api/chat/** 鉴权  | JWT / ticket session       |
| P1A-SEC-03 | /api/open/** 鉴权  | Integration App            |
| P1A-SEC-04 | /api/admin/** 鉴权 | 管理员登录                 |
| P1A-SEC-05 | 白名单路径         | 登录、健康检查、静态资源   |
| P1A-SEC-06 | 高风险操作审计     | 确认、提交、审批等必须记录 |

------

## 17. traceId 全链路追踪

### 17.1 生成规则

traceId 由 Chat Gateway 在每次用户请求入口生成。

如果上游已经传入合法 traceId，可继续沿用；否则由 Gateway 新建。

------

### 17.2 传递链路

```text
Chat Gateway
  ↓ traceId
BotService
  ↓ traceId
WorkflowRun
  ↓ traceId
HTTP_TOOL
  ↓ traceId
Connector Runtime
  ↓ X-Trace-Id
业务系统
```

------

### 17.3 记录要求

所有关键日志和审计表均记录同一 traceId：

| 模块                | 是否记录   |
| ------------------- | ---------- |
| Chat 请求           | 记录       |
| BotService 调用     | 记录       |
| WorkflowRun         | 记录       |
| HTTP_TOOL           | 记录       |
| Connector 调用      | 记录       |
| HITL 确认           | 记录       |
| 审计日志            | 记录       |
| 外部系统请求 Header | X-Trace-Id |

------

## 18. 审计与合规

### 18.1 审计范围

| 行为           | P1a 记录 |
| -------------- | -------- |
| 用户访问 Bot   | 记录     |
| 用户发送消息   | 记录     |
| Bot 回复       | 记录     |
| 知识库检索     | 记录     |
| 连接器调用     | 记录     |
| 高风险确认生成 | 记录     |
| 用户确认       | 记录     |
| 用户取消       | 记录     |
| 接口调用失败   | 记录     |
| 权限不足       | 记录     |
| 意图识别失败   | 建议记录 |
| 参数不足追问   | 建议记录 |

------

### 18.2 agi_agent_audit_log

```sql
CREATE TABLE agi_agent_audit_log (
  id               VARCHAR(64) PRIMARY KEY,
  tenant_id        VARCHAR(64) NOT NULL,
  user_id          VARCHAR(64),
  bot_id           VARCHAR(64),
  conversation_id  VARCHAR(64),
  message_id       VARCHAR(64),
  connector_code   VARCHAR(64),
  operation_code   VARCHAR(64),
  event_type       VARCHAR(64) NOT NULL,
  request_summary  TEXT,
  response_summary TEXT,
  status           VARCHAR(32),
  error_message    TEXT,
  trace_id         VARCHAR(64),
  created_at       TIMESTAMP NOT NULL
);
```

------

### 18.3 敏感数据处理

P1a 审计记录采用摘要方式：

```text
默认记录 request_summary / response_summary；
不默认记录完整业务数据；
敏感字段脱敏；
高风险操作记录参数 hash；
完整 prompt 记录作为配置项。
```

Prompt 记录模式建议：

```text
prompt_log_mode = NONE / SUMMARY / FULL
```

默认：

```text
SUMMARY
```

------

### 18.4 会话与消息保留策略

P1a 可先使用默认保留策略，Phase 2 增强配置化。

建议默认：

| 数据           | 默认策略                |
| -------------- | ----------------------- |
| 会话消息       | 保留 180 天或按企业要求 |
| 审计日志       | 保留 1 年或按企业要求   |
| 高风险确认记录 | 保留 1 年以上           |
| 失败日志       | 保留 180 天             |

------

## 19. Webhook 与 MCP 演进

### 19.1 Webhook 定位

Webhook 是外部业务系统主动通知 AI 平台的机制。

Webhook 不替代连接器，主要用于异步结果通知。

适合场景：

| 场景           | 示例                          |
| -------------- | ----------------------------- |
| 流程办结通知   | OA 流程审批完成后通知 AI 平台 |
| 借阅结果通知   | 档案借阅审批通过后回调        |
| 长任务完成通知 | 外部系统处理完成后通知        |
| 新事件通知     | 新待办、新档案入库等事件      |

P1a 只预留，不做完整 Webhook 管理平台。

------

### 19.2 MCP 定位

MCP 是后续阶段的标准工具协议扩展，用于让 Agent 通过标准协议发现和调用外部工具。

MCP 不替代 P1a 的 HTTP 连接器。

MCP 适合后续场景：

| 场景               | 说明                             |
| ------------------ | -------------------------------- |
| Agent 动态选择工具 | Agent 根据任务自动选择工具       |
| 多 Agent 协作      | 不同 Agent 调用不同工具          |
| 第三方工具生态     | 外部厂商提供 MCP Server          |
| 标准化工具接入     | 工具通过统一协议暴露             |
| 开发工具集成       | 数据库、文件系统、代码仓库等工具 |

------

### 19.3 阶段边界

| 能力                 | P1a    | P1b    | Phase 2    | Phase 3      |
| -------------------- | ------ | ------ | ---------- | ------------ |
| HTTP 连接器          | 实现   | 增强   | 增强       | 保留         |
| Webhook              | 预留   | 预留   | 实现       | 增强         |
| MCP                  | 不实现 | 不实现 | 不作为重点 | 实现         |
| Agent 动态工具选择   | 不实现 | 不实现 | 可探索     | 实现         |
| 连接器市场           | 不实现 | 不实现 | 不实现     | 可选         |
| 工具 Schema 自动同步 | 不实现 | 不实现 | 可选       | MCP 阶段实现 |

------

## 20. 管理后台设计

### 20.1 Bot 管理

P1a Bot 管理需要支持：

| 功能                 | P1a                        |
| -------------------- | -------------------------- |
| 新建 Bot             | 已有则复用                 |
| 配置名称、描述、头像 | 已有则复用                 |
| 配置系统提示词       | 已有则复用                 |
| 配置开场白           | 已有则复用                 |
| 配置“我能帮你做什么” | 建议新增或复用 description |
| 绑定知识库           | 复用                       |
| 绑定主工作流         | 复用                       |
| 配置 Bot 使用范围    | 复用 AssetGrant            |
| 启用 / 停用          | 复用                       |
| 管理员试聊           | 保持                       |

------

### 20.2 连接器管理

菜单名称：

```text
连接器
```

P1a 页面：

```text
连接器管理
工具 / 接口管理
```

P1a 可先实现基础 CRUD 和测试能力，调用日志页面可放 P1b。

#### 连接器字段

| 字段       | P1a                                        |
| ---------- | ------------------------------------------ |
| 连接器名称 | 支持                                       |
| 连接器编码 | 支持                                       |
| 类型       | OA / ARCHIVE / WORKFLOW / KB / GENERIC     |
| 接入方式   | 当前固定 HTTP，预留 MCP / WEBHOOK          |
| baseUrl    | 支持                                       |
| 认证方式   | USER_TOKEN / API_KEY / FIXED_HEADER / NONE |
| 启用状态   | 支持                                       |

#### Operation 字段

| 字段         | P1a            |
| ------------ | -------------- |
| 所属连接器   | 支持           |
| 接口名称     | 支持           |
| 接口编码     | 支持           |
| method       | GET / POST     |
| path         | 支持           |
| 操作类型     | QUERY / ACTION |
| 风险等级     | LOW / HIGH     |
| 是否需要确认 | 支持           |
| 确认摘要模板 | 支持           |
| 启用状态     | 支持           |
| 简单请求模板 | 可选           |

------

## 21. 典型业务场景

### 21.1 档案检索

```text
用户：帮我查一下某项目的合同档案
  ↓
平台校验用户登录
  ↓
平台校验用户可使用档案助手
  ↓
BotService 调用档案助手主工作流
  ↓
工作流提取参数
  ↓
HTTP_TOOL 调用 archive.search
  ↓
平台透传当前用户身份
  ↓
档案系统返回该用户可见档案
  ↓
Bot 总结并展示引用
```

------

### 21.2 档案借阅申请

```text
用户：帮我申请借阅这份档案
  ↓
BotService 调用档案助手主工作流
  ↓
工作流执行 HTTP_TOOL
  ↓
HTTP_TOOL 发现 archive.borrow.submit need_confirm=true
  ↓
创建 agi_human_confirm_task
  ↓
WorkflowRun 进入 WAITING_CONFIRM
  ↓
Chat 页面展示确认卡片
  ↓
用户确认
  ↓
Confirm API 内部调用 Workflow resume
  ↓
HTTP_TOOL 正式调用档案系统接口
  ↓
档案系统判断用户是否有权限申请
  ↓
返回申请结果
```

------

### 21.3 OA 待办查询

```text
用户：我今天有哪些待办？
  ↓
平台校验用户可使用 OA 助手
  ↓
BotService 调用 OA 助手主工作流
  ↓
HTTP_TOOL 调用 oa.todo.list
  ↓
平台透传用户 Token
  ↓
OA 系统返回当前用户待办
  ↓
Bot 整理后回复
```

------

### 21.4 编研生成

```text
用户：围绕 XX 主题生成一份编研报告
  ↓
BotService 调用编研助手主工作流
  ↓
工作流触发 ResearchGenerationService
  ↓
复用 agi_generation_job
  ↓
Chat Portal 适配进度事件
  ↓
SSE 推送进度
  ↓
生成完成后返回报告内容或文件链接
```

------

## 22. P1a 任务清单

### 22.1 前端与 Web 交付层

| 编号       | 任务           | 说明                           |
| ---------- | -------------- | ------------------------------ |
| P1A-WEB-01 | 新建 apps/chat | Vite + React + Ant Design X    |
| P1A-WEB-02 | Bot 列表       | 只展示有权限 Bot               |
| P1A-WEB-03 | Bot 对话页     | 对接 Chat Gateway              |
| P1A-WEB-04 | 会话列表       | 复用现有 Bot 会话              |
| P1A-WEB-05 | 消息渲染       | Ant Design X + MessageRenderer |
| P1A-WEB-06 | SSE Adapter    | 转换平台 SSE 事件              |
| P1A-WEB-07 | 引用卡片       | 自研 CitationCard              |
| P1A-WEB-08 | 确认卡片       | 自研 ConfirmCard               |
| P1A-WEB-09 | 任务进度卡片   | 自研 ProgressCard              |
| P1A-WEB-10 | 错误卡片       | 自研 ErrorCard                 |
| P1A-WEB-11 | Bot 能力说明   | 展示“我能帮你做什么”           |

------

### 22.2 Chat Gateway

| 编号      | 任务                                         | 说明                       |
| --------- | -------------------------------------------- | -------------------------- |
| P1A-GW-01 | /api/chat/bots                               | 查询当前用户可访问 Bot     |
| P1A-GW-02 | /api/chat/conversations                      | 包装现有会话能力           |
| P1A-GW-03 | /api/chat/conversations/{id}/messages/stream | 统一流式消息接口           |
| P1A-GW-04 | traceId                                      | 生成并向下传递链路 ID      |
| P1A-GW-05 | SSE 事件转换                                 | 转为前端统一协议           |
| P1A-GW-06 | 错误统一处理                                 | 权限、接口、超时、任务失败 |

------

### 22.3 BotService 与流式能力

| 编号       | 任务                      | 说明                                    |
| ---------- | ------------------------- | --------------------------------------- |
| P1A-BOT-01 | BotService 结果包装为 SSE | 必须实现，即使底层同步返回              |
| P1A-BOT-02 | LLM / Workflow delta 回调 | 建议实现，可后置 P1b                    |
| P1A-BOT-03 | Bot 会话富消息 metadata   | 支持引用、确认、进度等结构化信息        |
| P1A-BOT-04 | Bot 能力说明字段          | 复用 description 或新增 capability_hint |

------

### 22.4 安全与权限

| 编号       | 任务                     | 说明                        |
| ---------- | ------------------------ | --------------------------- |
| P1A-SEC-01 | /api/** 强制鉴权         | 关闭默认 permitAll          |
| P1A-SEC-02 | /api/chat/** 鉴权        | JWT / Chat Session          |
| P1A-SEC-03 | /api/open/** 鉴权        | Integration App             |
| P1A-SEC-04 | Bot 使用权限             | 复用 AssetGrant             |
| P1A-SEC-05 | 禁止前端传可信身份       | 只允许 businessContext      |
| P1A-SEC-06 | SSE 鉴权                 | 校验 conversation 归属      |
| P1A-SEC-07 | ticket 审计              | 签发和使用记录              |
| P1A-SEC-08 | Integration App 代签校验 | 校验 app、tenant、bot scope |

------

### 22.5 ticket 嵌入

| 编号       | 任务                | 说明                             |
| ---------- | ------------------- | -------------------------------- |
| P1A-TKT-01 | agi_embed_ticket 表 | 存储 ticket                      |
| P1A-TKT-02 | 签发接口            | POST /api/chat/embed-tickets     |
| P1A-TKT-03 | ticket 校验         | 访问 /chat/{botId}?ticket=       |
| P1A-TKT-04 | ticket 换 session   | 服务端 session / HttpOnly Cookie |
| P1A-TKT-05 | 一次性失效          | 使用后失效                       |

------

### 22.6 连接器

| 编号        | 任务                     | 说明                                |
| ----------- | ------------------------ | ----------------------------------- |
| P1A-CONN-01 | agi_connector            | 连接器表                            |
| P1A-CONN-02 | agi_connector_operation  | Operation 表                        |
| P1A-CONN-03 | HTTP 连接器运行时        | baseUrl + path 调用                 |
| P1A-CONN-04 | auth_mode                | USER_TOKEN / API_KEY / FIXED_HEADER |
| P1A-CONN-05 | 身份透传                 | IdentityContext                     |
| P1A-CONN-06 | need_confirm             | 高风险接口标记                      |
| P1A-CONN-07 | 调用超时                 | 默认超时                            |
| P1A-CONN-08 | 接口测试                 | 简单测试能力                        |
| P1A-CONN-09 | confirm_summary_template | 确认摘要模板                        |

------

### 22.7 工作流改造

| 编号      | 任务                     | 说明                          |
| --------- | ------------------------ | ----------------------------- |
| P1A-WF-01 | HTTP_TOOL 支持 Operation | connectorCode + operationCode |
| P1A-WF-02 | outputKey                | 结果写入 workflow context     |
| P1A-WF-03 | need_confirm 自动拦截    | HTTP_TOOL 执行前检查          |
| P1A-WF-04 | WAITING_CONFIRM 状态     | 工作流暂停                    |
| P1A-WF-05 | workflow resume / reject | Confirm API 内部调用          |
| P1A-WF-06 | payload_snapshot 生成    | 用于确认卡片展示              |

------

### 22.8 HITL

| 编号        | 任务                   | 说明             |
| ----------- | ---------------------- | ---------------- |
| P1A-HITL-01 | agi_human_confirm_task | 确认任务表       |
| P1A-HITL-02 | confirm.required SSE   | 推送确认事件     |
| P1A-HITL-03 | Confirm API            | 确认             |
| P1A-HITL-04 | Reject API             | 取消             |
| P1A-HITL-05 | 幂等校验               | 防重复点击       |
| P1A-HITL-06 | payload_hash           | 防篡改，建议实现 |
| P1A-HITL-07 | payload_snapshot       | 确认前展示内容   |

------

### 22.9 编研适配

| 编号       | 任务                    | 说明               |
| ---------- | ----------------------- | ------------------ |
| P1A-RES-01 | 复用 agi_generation_job | 不迁移             |
| P1A-RES-02 | 进度适配接口            | 转为 Chat progress |
| P1A-RES-03 | SSE 推送进度            | job.progress       |
| P1A-RES-04 | 结果链接展示            | 报告结果卡片       |

------

### 22.10 审计与 traceId

| 编号         | 任务                | 说明                                 |
| ------------ | ------------------- | ------------------------------------ |
| P1A-AUDIT-01 | agi_agent_audit_log | 审计表                               |
| P1A-AUDIT-02 | 对话审计            | 提问 / 回复                          |
| P1A-AUDIT-03 | 连接器调用审计      | Operation 调用                       |
| P1A-AUDIT-04 | HITL 审计           | 确认 / 取消                          |
| P1A-AUDIT-05 | 错误审计            | 权限不足 / 调用失败                  |
| P1A-AUDIT-06 | 脱敏摘要            | request_summary / response_summary   |
| P1A-AUDIT-07 | traceId 全链路      | Gateway → Bot → Workflow → Connector |

------

## 23. P1b 任务清单

| 编号         | 任务                 | 说明                    |
| ------------ | -------------------- | ----------------------- |
| P1B-ROUTE-01 | Bot 多工作流配置     | agi_bot_capability      |
| P1B-ROUTE-02 | 轻量意图路由         | 只选择工作流            |
| P1B-CONN-01  | 连接器管理后台增强   | CRUD 完善               |
| P1B-CONN-02  | Operation 测试增强   | 测试参数、响应          |
| P1B-LOG-01   | 调用日志页面         | 可视化查询              |
| P1B-CHAT-01  | Embed Widget 基础版  | JS SDK                  |
| P1B-SSE-01   | 断线重连优化         | 重新拉取消息 / 任务状态 |
| P1B-JOB-01   | agi_agent_job 外壳   | 通用任务模型            |
| P1B-AUDIT-01 | traceId 链路查看     | 串联完整调用链          |
| P1B-BOT-01   | capability_hint 字段 | Bot 能力说明增强        |
| P1B-BOT-02   | suggested_questions  | 推荐问题配置            |

------

## 24. Phase 2 任务清单

| 编号        | 任务             | 说明                  |
| ----------- | ---------------- | --------------------- |
| P2-SSO-01   | OIDC 对接        | 企业统一身份          |
| P2-SSO-02   | SAML 对接        | 大型企业适配          |
| P2-ORG-01   | 组织同步         | 用户、部门、角色同步  |
| P2-WH-01    | Webhook 接收接口 | 接收业务系统回调      |
| P2-WH-02    | Webhook 签名校验 | 防伪造                |
| P2-WH-03    | 回调事件日志     | 记录回调              |
| P2-CONN-01  | 连接器模板       | OA、档案、流程模板    |
| P2-CONN-02  | 响应映射         | JSONPath              |
| P2-CONN-03  | 简单重试         | 查询类重试            |
| P2-CONN-04  | API Key 加密保存 | 密钥安全              |
| P2-AUDIT-01 | 审计查询页面     | 按用户、Bot、接口查询 |
| P2-AUDIT-02 | 审计导出         | Excel / CSV           |
| P2-CHAT-01  | 移动端 H5        | 移动适配              |

------

## 25. Phase 3 任务清单

| 编号        | 任务               | 说明          |
| ----------- | ------------------ | ------------- |
| P3-MCP-01   | MCP Server 注册    | 配置 MCP 服务 |
| P3-MCP-02   | MCP Tool 同步      | 读取工具列表  |
| P3-MCP-03   | MCP Tool Schema    | 参数结构      |
| P3-MCP-04   | MCP Tool 调用      | Agent 调用    |
| P3-MCP-05   | MCP Tool HITL      | 高风险确认    |
| P3-AGENT-01 | Agent 动态工具选择 | 按意图选工具  |
| P3-AGENT-02 | 自动任务规划       | 拆解复杂任务  |
| P3-AGENT-03 | 多 Agent 协作      | 多智能体分工  |
| P3-CONN-01  | 连接器市场         | 内置 / 第三方 |
| P3-CONN-02  | Operation 版本管理 | 接口变更可控  |
| P3-CONN-03  | 熔断限流           | 企业级保护    |
| P3-KB-01    | Rerank             | 检索重排      |
| P3-KB-02    | 多跳检索           | 跨文档问答    |
| P3-KB-03    | chunk 级 ACL       | 知识片段权限  |

------

## 26. P1a 验收标准

### 26.1 Web 服务验收

| 验收项                       | 标准 |
| ---------------------------- | ---- |
| 用户登录后可进入 Chat Portal | 通过 |
| 只显示有权限 Bot             | 通过 |
| 可与 Bot 多轮对话            | 通过 |
| 可查看历史会话               | 通过 |
| 支持 SSE 流式接口            | 通过 |
| 可展示引用来源               | 通过 |
| 可展示确认卡片               | 通过 |
| Bot 页面展示能力说明         | 通过 |

------

### 26.2 与现有 Bot 兼容验收

| 验收项                                    | 标准 |
| ----------------------------------------- | ---- |
| 管理后台试聊不受影响                      | 通过 |
| Open API 不受影响                         | 通过 |
| Chat Portal 可复用 BotService             | 通过 |
| 现有 bot_session / bot_message 可继续使用 | 通过 |
| 现有 Bot 单 workflowId 模式可继续运行     | 通过 |

------

### 26.3 SSE 与流式验收

| 验收项                             | 标准     |
| ---------------------------------- | -------- |
| 统一使用 /messages/stream          | 通过     |
| SSE 请求必须鉴权                   | 通过     |
| 支持 message.delta                 | 通过     |
| 支持 confirm.required              | 通过     |
| 支持 job.progress                  | 通过     |
| BotService 同步返回可包装为 SSE    | 通过     |
| 如果底层支持流式，可逐步输出 delta | 可选增强 |

------

### 26.4 连接器验收

| 验收项                     | 标准 |
| -------------------------- | ---- |
| 可配置 HTTP 连接器         | 通过 |
| 可配置 Operation           | 通过 |
| HTTP_TOOL 可选择 Operation | 通过 |
| 查询类接口可直接执行       | 通过 |
| 高风险接口触发确认         | 通过 |
| 调用失败有错误提示         | 通过 |
| 接口调用有超时控制         | 通过 |

------

### 26.5 HITL 验收

| 验收项                             | 标准 |
| ---------------------------------- | ---- |
| need_confirm=true 自动生成确认任务 | 通过 |
| WorkflowRun 进入 WAITING_CONFIRM   | 通过 |
| 前端显示确认卡片                   | 通过 |
| 确认卡片展示 payload_snapshot      | 通过 |
| 用户确认后 workflow resume         | 通过 |
| 用户取消后不执行后续操作           | 通过 |
| 重复点击不会重复提交               | 通过 |
| 确认操作有审计日志                 | 通过 |

------

### 26.6 安全验收

| 验收项                       | 标准 |
| ---------------------------- | ---- |
| /api/chat/** 必须鉴权        | 通过 |
| 无权限用户不能访问 Bot       | 通过 |
| 前端伪造 userId 不生效       | 通过 |
| iframe ticket 一次性使用     | 通过 |
| 长期 token 不出现在 URL      | 通过 |
| SSE 校验 conversation 归属   | 通过 |
| Integration App 不可越权代签 | 通过 |

------

### 26.7 审计与 traceId 验收

| 验收项                     | 标准 |
| -------------------------- | ---- |
| 用户提问有日志             | 通过 |
| Bot 回复有日志             | 通过 |
| 连接器调用有日志           | 通过 |
| 确认 / 取消有日志          | 通过 |
| 失败信息有日志             | 通过 |
| traceId 可关联一次完整调用 | 通过 |
| request_summary 脱敏       | 通过 |
| 外部接口传递 X-Trace-Id    | 通过 |

------

## 27. 待确认问题

### 27.1 P0 阻塞项

| 问题                                            | 默认建议 |
| ----------------------------------------------- | -------- |
| P1a 是否复用现有 bot_session / bot_message      | 是       |
| P1a 是否保持 Bot 单主 workflowId                | 是       |
| P1a 是否禁止 Chat Gateway 直调 Operation        | 是       |
| need_confirm 是否由 HTTP_TOOL 自动拦截          | 是       |
| Confirm API 是否内部调用 Workflow resume/reject | 是       |
| 编研是否继续复用 agi_generation_job             | 是       |
| 新增表是否统一 agi_ 前缀                        | 是       |
| /api/** 是否必须从 permitAll 改为鉴权           | 是       |
| SSE 路径是否统一为 /messages/stream             | 是       |
| 前端是否采用 admin + chat 双应用                | 是       |
| Chat UI 是否采用 Ant Design X                   | 是       |

------

### 27.2 P1 可默认项

| 问题                  | 默认建议           |
| --------------------- | ------------------ |
| ticket 过期时间       | 300 秒             |
| 会话消息保留时间      | 180 天             |
| 审计日志保留时间      | 1 年               |
| prompt_log_mode       | SUMMARY            |
| Operation 默认超时    | 10 秒              |
| 高风险确认过期时间    | 30 分钟            |
| SSE 断线重连          | P1a 简化，P1b 增强 |
| payload_hash 是否必须 | 建议实现           |
| 组织范围代签校验      | P1b 增强           |

------

### 27.3 P2 后续确认项

| 问题                       | 阶段             |
| -------------------------- | ---------------- |
| 是否支持完整 SSO / OIDC    | Phase 2          |
| 是否支持 Webhook 回调平台  | Phase 2          |
| 是否建设通用 agi_agent_job | P1b / Phase 2    |
| 是否引入 MCP               | Phase 3          |
| 是否建设连接器市场         | Phase 3          |
| 是否建设 chunk 级 ACL      | 视知识库权限需求 |

------

## 28. 总结

本方案将企业 Web 智能体服务定位为：

```text
企业业务智能体入口
+ 受控智能调度平台
+ 连接器接入平台
+ 高风险操作确认平台
+ AI 对话交付层
```

P1a 的核心策略是：

```text
复用现有 BotService；
复用现有 Bot 会话；
复用现有编研任务；
保持 Bot 单主工作流；
通过工作流调用连接器；
通过 HTTP_TOOL 的 need_confirm 自动拦截高风险操作；
Confirm API 内部 resume / reject 工作流；
新增 Chat Portal 作为最终用户入口；
新增 HTTP 连接器作为外部系统接入；
新增 ticket 机制解决嵌入安全；
新增最小审计、traceId 和 API 鉴权安全基线。
```

P1a 不做：

```text
多工作流智能路由；
Chat Gateway 直调 Operation；
MCP；
多 Agent；
复杂连接器市场；
复杂权限矩阵；
完整通用任务中心；
多人确认流。
```

后续阶段演进：

```text
P1b：
轻量多工作流路由、连接器管理增强、调用日志页面、Embed Widget、通用任务外壳。

Phase 2：
SSO、Webhook、组织同步、响应映射、审计查询、密钥加密。

Phase 3：
MCP 连接器、Agent 动态工具选择、多 Agent 协作、连接器平台化、复杂知识库权限。
```

最终目标是形成一个既能快速落地，又能长期演进的企业级 Web 智能体平台。
