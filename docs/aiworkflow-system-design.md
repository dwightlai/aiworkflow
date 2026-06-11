# AI Workflow 管理端软件设计说明书

> **密级**：内部公开  
> **项目**：AI Workflow 企业 AI 应用平台  
> **模块**：AI Workflow 管理端  
> **版本**：1.1  
> **日期**：2026-06-11  
> **状态**：待确认（确认后生成 DOCX/PDF）

| 版本号 | 简要说明 | 变更人 | 变更日期 | 复核人 |
|--------|----------|--------|----------|--------|
| 1.0 | 新建 | | 2026-06-11 | |
| 1.1 | 细化各功能点字段、规则、接口与交互 | | 2026-06-11 | |

---

## 目录

1. [概述](#1-概述)
2. [业务场景](#2-业务场景)
3. [用户角色与权限](#3-用户角色与权限)
4. [业务流程](#4-业务流程)
5. [应用功能设计](#5-应用功能设计)
6. [工作流节点配置规格](#6-工作流节点配置规格)
7. [系统架构设计](#7-系统架构设计)
8. [附录](#8-附录)

---

## 1 概述

### 1.1 编写目的

本说明书描述 AI Workflow 管理端**已实现功能**的详细设计，包括页面元素、表单字段、校验规则、操作步骤、业务约束、接口契约与异常处理，供产品评审、研发实现对照、测试用例编写及实施交付使用。

### 1.2 系统定位

AI Workflow 管理端是企业 AI 应用平台的**配置与运营控制台**，承担：

- AI 资产编排：模型、知识库、工作流、智能体、编研模板
- 运行观测：工作流执行记录、节点级诊断
- 租户与权限：组织用户、资产授权、审计日志
- 开放集成：第三方应用、API Key、开放 API 文档

### 1.3 系统边界

| 范围内 | 范围外 |
|--------|--------|
| 管理端 SPA（React）全部页面 | 开放 API 调用方业务系统内部逻辑 |
| 后端 `/api/**` 管理接口 | 模型 Provider 远端服务 SLA |
| 工作流引擎同步执行与记录 | 异步队列/Webhook 推送（规划中） |
| 知识库向量检索（ES/内存） | Elasticsearch 集群运维 |
| 多租户身份与资产授权 | 统一身份源（LDAP/OAuth）同步 |

### 1.4 设计原则

1. **工作流优先**：复杂 AI 能力通过 DAG 组合，避免为单一场景新增专用节点类型。
2. **发布不可变**：已发布工作流版本快照不可修改，执行始终引用发布版本。
3. **租户隔离**：业务数据按 `tenant_id` 隔离，开放 API 携带调用方上下文做资产过滤。
4. **可插拔存储**：Store 模式支持 InMemory（local Profile）与 MyBatis（PostgreSQL/达梦）。

### 1.5 术语

| 术语 | 说明 |
|------|------|
| DAG | 有向无环图，工作流节点与连线的集合 |
| Bot / 智能体 | 绑定工作流或直连模型的对话应用 |
| RAG | 检索增强生成，知识库检索 + 大模型 |
| 编研模板 | 定义专题成果章节结构与变量的 schema |
| 资产授权 | 按组织/部门/角色控制 Bot、知识库、工作流、模型的 USE 权限 |

---

## 2 业务场景

### 2.1 场景清单

| 编号 | 场景 | 参与模块 | 目标 |
|------|------|----------|------|
| S01 | 知识库问答 | 知识库、工作流、Bot | 基于企业资料回答用户问题 |
| S02 | HTTP 工具调用 | 工作流 HTTP 节点 | 编排调用外部 REST API |
| S03 | 问题分类分流 | 问题分类节点、条件边 | 按意图路由不同分支 |
| S04 | 档案智能编研 | 编研模板、智能编研、工作流 | 生成专题 Markdown/DOCX |
| S05 | 多租户运营 | 租户、组织用户、资产授权 | SaaS/集团多实例隔离 |
| S06 | 第三方集成 | 第三方应用、开放 API | 业务系统嵌入 AI 能力 |

### 2.2 场景 S01：知识库问答（步骤）

1. **模型配置**：新增对话模型 + 嵌入模型并启用。
2. **知识库**：创建知识库 → 配置向量库（可选 ES）→ 上传文档 → 检索测试。
3. **工作流**：开始 → 知识库检索 → 大模型（引用 `content`）→ 结束 → 发布。
4. **智能体**：新建 Bot，绑定已发布工作流 → 运行对话验证。
5. **（可选）资产授权**：为业务组织配置 Bot USE 权限。
6. **（可选）开放 API**：第三方应用配置 Key 与白名单后调用 `/api/open/bots/{id}/chat`。

### 2.3 场景 S04：智能编研（步骤）

1. **编研模板**：维护 `templateSchema`（变量 + 章节 instruction）并绑定工作流。
2. **智能编研向导**：填写主题 → 选主题库 → 选知识库 → 选模板 → 提交任务。
3. **后台执行**：`ResearchController` 创建 Job → 执行绑定工作流 → 写入 Output。
4. **成果查看**：步骤 5 展示大纲、分节正文、引用关系，支持 DOCX 下载。

---

## 3 用户角色与权限

### 3.1 角色定义

| 角色 | 编码 | 说明 |
|------|------|------|
| 平台管理员 | platform_admin | 默认租户 `tenant_default` 下跨租户运维 |
| 单位管理员 | unit_admin | 本租户组织、用户、授权管理 |
| 应用用户 | app_user | 普通业务配置与使用 |
| 审计员 | auditor | 只读审计与运行记录 |
| 第三方应用 | integration_app | 仅开放 API，无管理端 UI |

### 3.2 功能权限矩阵（管理端）

| 功能 | platform_admin | unit_admin | app_user | auditor |
|------|:--------------:|:----------:|:--------:|:-------:|
| 租户管理 | ✓ | | | |
| 组织用户 | ✓ | ✓ | | |
| 菜单/字典 | ✓ | ✓ | | |
| 日志管理 | ✓ | ✓ | ✓(只读) | ✓ |
| 资产授权 | ✓ | ✓ | | |
| 工作流/知识库/Bot | ✓ | ✓ | ✓ | 只读 |
| 模型/编研模板 | ✓ | ✓ | ✓ | |

> 实际权限以后端 `TenantAdminGuard` 与菜单可见性为准；上表为设计目标。

### 3.3 开放 API 权限

- 认证：`X-AGI-App-Code` + `X-AGI-Api-Key`（必填）。
- 上下文 Header（可选）：`X-AGI-User-Id`、`X-AGI-Unit-Id`、`X-AGI-Department-Ids`、`X-AGI-Role-Ids`。
- 资产可见性 = 应用白名单 ∩ 资产授权 ∩ 资产启用状态。

---

## 4 业务流程

### 4.1 流程总览

| 编号 | 名称 | 触发 | 结束状态 |
|------|------|------|----------|
| P01 | 用户登录 | 提交登录表单 | 进入工作台或失败提示 |
| P02 | 工作流创建 | 空白工作流/新建 | DRAFT + 设计器 |
| P03 | 工作流发布 | 设计器「发布」 | PUBLISHED + 新版本号 |
| P04 | 工作流执行 | 调试/运行/Bot/OpenAPI | SUCCEEDED/FAILED 执行记录 |
| P05 | 工作流归档 | 卡片「归档工作流」 | ARCHIVED |
| P06 | 工作流恢复 | 已归档 Tab「恢复发布」 | PUBLISHED 或 DRAFT |
| P07 | Bot 多轮对话 | 运行 Drawer 发送消息 | 会话消息追加 |
| P08 | 智能编研 | 向导提交 | Job 完成 + Output |
| P09 | 资产授权 | 配置授权 Drawer 保存 | Grant 记录持久化 |

### 4.2 P01 用户登录

```
用户打开 /login
  → 填写 tenantCode(可选) + username + password
  → POST /api/auth/login
  → 成功：存储 AuthSession，跳转 /
  → 失败：message.error 展示错误码消息
```

**规则**：
- 租户编码留空时使用默认租户 `tenant_default`。
- Token 通过 `Authorization: Bearer` 传递；刷新/登出使用 refreshToken。

### 4.3 P03–P06 工作流生命周期

```
[新建] workflowId=new → 保存 → redirect /workflows/{id}/designer (DRAFT)
[编辑草稿] PUT /draft → 更新 definition
[发布] POST /publish → 生成 WorkflowVersion，status=PUBLISHED
[运行] POST /runs → 必须 PUBLISHED（调试面板会先 publish）
[归档] POST /archive → status=ARCHIVED
[恢复] POST /restore → 有 currentVersionId → PUBLISHED，否则 DRAFT
[删除] DELETE → 二次确认（Popconfirm）
```

**DAG 发布校验**（后端 `DagValidator`）：
- 有且仅有一个 START 节点
- 至少一个 END 节点
- 节点 ID 唯一；边 source/target 存在
- 无环（DFS 检测）

### 4.4 P07 Bot 对话

```
选择/新建 session
  → 输入 message + 可选 input JSON
  → POST /api/bots/{id}/chat
  → 若绑定 workflowId：封装 input 执行工作流
  → 否则：ChatModelClient 直连 + 可选知识库 RAG
  → 保存 USER/ASSISTANT 消息，返回 reply + execution
```

---

## 5 应用功能设计

### 5.1 全局规则

#### 5.1.1 统一响应格式

```json
{ "success": true, "data": {}, "error": null }
{ "success": false, "data": null, "error": { "code": "...", "message": "...", "requestId": "..." } }
```

#### 5.1.2 删除规则

| 对象 | 确认方式 | 依赖检查 |
|------|----------|----------|
| 工作流 | Popconfirm | 无强制后端引用检查 |
| 知识库 | 直接删除 | 被工作流引用时业务上应提示（前端展示引用状态 Tag） |
| Bot | 直接删除 | — |
| 租户 | 不可删 `tenant_default` | — |
| 菜单/字典 | Modal.confirm | 字典删除提示含字典项 |

#### 5.1.3 启用/禁用规则

- Bot、模型 Provider、编研模板、菜单、字典等含 `ENABLED`/`DISABLED` 或 `enabled` 字段。
- 禁用模型不出现在 LLM 节点下拉（仅 `enabled=true` 且用途匹配）。
- 禁用 Bot 不可被开放 API 白名单选中。

#### 5.1.4 翻页与列表

| 页面 | 分页 | 默认 pageSize |
|------|------|---------------|
| Bot 列表 | 是 | 10 |
| 运行监控 | 是 | 10 |
| 日志管理 | 是 | 20 |
| 工作流卡片 | 否（网格） | — |
| 知识库列表 | 是 | 10 |

#### 5.1.5 筛选规则

| 页面 | 筛选方式 | 说明 |
|------|----------|------|
| 工作流 | Segmented + 输入框 | 状态 Tab + 名称/描述客户端过滤，**无重置按钮** |
| Bot | 输入框 + 搜索/重置 | 客户端过滤名称与描述 |
| 运行监控 | 输入框 | executionId/workflowId contains |
| 资产授权 | 输入框 + 重置 | 按资产名称 |
| 日志 | 表单查询 | 改条件重置 page=1 |

#### 5.1.6 JSON 输入约定

运行参数、调试 input、Bot 附加变量均要求：
- 合法 JSON
- 解析结果为**对象**（非数组、非 null 字符串）

---

### 5.2 功能清单

| 编号 | 模块 | 路由 | 优先级 |
|------|------|------|--------|
| F001 | 登录 | /login | P0 |
| F002 | 工作台 | / | P0 |
| F003 | 智能体 Bots | /bots | P0 |
| F004 | 工作流列表 | /workflows | P0 |
| F005 | 工作流设计器 | /workflows/{id}/designer | P0 |
| F006 | 运行监控 | /workflow-runs | P0 |
| F007 | 执行详情 | /workflow-runs/{id} | P0 |
| F008 | 知识库 | /knowledge | P0 |
| F009 | 知识库文档 | /knowledge/{id}/documents | P0 |
| F010 | 新增/编辑文档 | /knowledge/{id}/documents/new|edit | P1 |
| F011 | 模型配置 | /models | P0 |
| F012 | 编研模板 | /research/templates | P1 |
| F013 | 智能编研 | /research/compile | P1 |
| F014 | 租户管理 | /system/tenants | P1 |
| F015 | 组织用户 | /system/users | P1 |
| F016 | 资产授权 | /system/asset-grants | P1 |
| F017 | 第三方应用 | /system/integration-apps | P1 |
| F018 | 开放 API 文档 | /system/open-api-docs | P2 |
| F019 | 菜单管理 | /system/menus | P2 |
| F020 | 数据字典 | /system/dictionary | P2 |
| F021 | 日志管理 | /system/logs | P1 |
| F022 | Prompt 模板 | /prompts | P2（菜单可隐藏） |

---

### 5.3 F001 登录

#### 5.3.1 页面原型

![登录页](images/manual/login.png)

#### 5.3.2 页面元素

| 元素 | 类型 | 属性/约束 |
|------|------|-----------|
| 租户编码 | Input | placeholder「留空则登录默认租户」 |
| 用户名 | Input | 必填，autocomplete=username |
| 密码 | Password | 必填，autocomplete=current-password |
| 登录 | Button | type=primary, htmlType=submit |
| 提示 Alert | Alert | 首次登录修改密码（信息提示） |
| 默认账号说明 | Text | admin/admin123 |

#### 5.3.3 表单校验

| 字段 | 规则 | 错误提示 |
|------|------|----------|
| username | required | 请输入用户名 |
| password | required | 请输入密码 |
| tenantCode | 无 | — |

#### 5.3.4 交互逻辑

1. 点击登录 → `loading=true` → 调用 API。
2. 成功 → `message.success('登录成功')` → `onLogin(session)` → 路由 `/`。
3. 失败 → `message.error(error.message)`。

#### 5.3.5 接口

| 方法 | 路径 | 请求体 | 响应 |
|------|------|--------|------|
| POST | /api/auth/login | `{ username, password, tenantCode? }` | `AuthSession`（含 accessToken、refreshToken、user） |

---

### 5.4 F002 工作台

#### 5.4.1 页面原型

![工作台](images/manual/dashboard.png)

#### 5.4.2 指标卡片

| 卡片 | 数据来源 | 计算规则 |
|------|----------|----------|
| 总工作流 | listWorkflows | 总数；副标题「N 个已发布」 |
| 今日执行 | listWorkflowRuns | **当前为全部记录数**（非按日过滤） |
| 运行成功率 | runs | succeeded/total × 100%，≥80% 绿色 |
| 平均耗时 | runs | 仅 `finishedAt` 非空记录，毫秒均值 |

#### 5.4.3 最近工作流列表

- 最多展示 **5** 条（按接口返回顺序）。
- 字段：名称、描述、版本号、状态 Tag（草稿/已发布/已归档）。
- 操作：**编辑**、**调试** → `/workflows/{id}/designer`。

#### 5.4.4 快捷入口

| 入口 | 跳转 |
|------|------|
| 工作流运营台 | /workflows |
| 运行监控台 | /workflow-runs |
| 集成指南 | /tools（占位页） |

#### 5.4.5 接口

- `GET /api/workflows`
- `GET /api/workflow-runs`

---

### 5.5 F003 智能体 Bots

#### 5.5.1 页面原型

![智能体列表](images/manual/bots.png)

#### 5.5.2 统计区

| 指标 | 计算 |
|------|------|
| 智能体总数 | bots.length |
| 已启用 | status=ENABLED 数量 |
| 累计会话 | sum(conversationCount) |

#### 5.5.3 列表表格

| 列名 | 字段 | 展示规则 |
|------|------|----------|
| 智能体 | name, description | 名称加粗 + 灰色描述 |
| 应用方式 | workflowId | 有→「绑定工作流: {name}」Tag；无→「直连智能体」 |
| 模型 | modelProviderId | 绑定工作流→「跟随工作流」；否则显示模型名或「未配置」 |
| 知识库 | knowledgeBaseIds | 多 Tag；空→「未绑定」 |
| 会话 | conversationCount | 数字 |
| 状态 | status | ENABLED 绿 / DISABLED 灰 |
| 操作 | — | 运行、编辑、删除 |

#### 5.5.4 搜索

- 字段 `keyword`：过滤 name、description（不区分大小写）。
- 「搜索」按钮：无额外逻辑（依赖 React 状态）。
- 「重置」：清空 keyword。

#### 5.5.5 新增/编辑 Drawer 表单

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| name | Input | ✓ | — | 智能体名称 |
| description | TextArea | | null | 描述 |
| workflowId | Select | | null | 选项：**仅 PUBLISHED** 工作流 |
| modelProviderId | Select | | null | 选项：enabled + CHAT 用途模型 |
| knowledgeBaseIds | Select multiple | | [] | 直连模式可选多个知识库 |
| openingMessage | TextArea | | 你好，我是你的智能助手。 | 开场白 |
| systemPrompt | TextArea | | '' | 系统提示词 |
| status | Radio | | ENABLED | ENABLED / DISABLED |
| avatar | hidden | | robot | 预留 |

**提交校验**（前端）：
- `workflowId`、`modelProviderId`、`knowledgeBaseIds` **至少填一项**，否则 `message.error('请至少绑定工作流、默认模型或默认知识库之一')`。

#### 5.5.6 运行 Drawer（多轮对话）

**布局**：左会话列表 + 右消息区。

| 元素 | 说明 |
|------|------|
| 新会话 | 清空 currentSessionId，开始新对话 |
| 会话列表 | GET sessions，点击切换加载 messages |
| message | 必填，用户输入 |
| input | TextArea JSON，默认 `{}`，附加工作流变量 |
| 发送 | 校验 JSON 为对象 → POST chat |

**发送逻辑**：
1. 解析 `input`，失败提示「附加变量必须是 JSON 对象」。
2. `POST /api/bots/{id}/chat` body: `{ sessionId?, message, input }`。
3. 成功：刷新 messages、更新 execution 状态 Tag。

#### 5.5.7 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/bots | 列表 |
| POST | /api/bots | 创建 |
| PUT | /api/bots/{id} | 更新 |
| DELETE | /api/bots/{id} | 删除 |
| GET | /api/bots/{id}/sessions | 会话列表 |
| GET | /api/bots/{id}/sessions/{sid}/messages | 消息列表 |
| POST | /api/bots/{id}/chat | 对话 |
| POST | /api/bots/{id}/run | 单次运行（工作流模式） |

---

### 5.6 F004 工作流列表

#### 5.6.1 页面原型

![工作流运营台](images/manual/workflows.png)

#### 5.6.2 汇总卡片

| 卡片 | 规则 |
|------|------|
| 总工作流 | 全部数量 |
| 已发布 | status=PUBLISHED |
| 草稿 | status=DRAFT |
| 最近运行 | 全部 runs 中最新 startedAt |

#### 5.6.3 筛选区

| 控件 | 行为 |
|------|------|
| 搜索框 | placeholder「请输入工作流名称」，实时过滤 name/description |
| 全部/草稿/已发布/已归档 | Segmented，切换 statusFilter |

#### 5.6.4 工作流卡片

| 区域 | 内容 |
|------|------|
| 标题行 | 名称 + 状态 Tag |
| 描述 | description，空则显示内置 demo 文案 |
| 元信息 | 最新版本 vN、更新时间 |
| 操作栏 | 设置、运行、编辑、归档或恢复发布、删除 |

#### 5.6.5 卡片操作详述

| 操作 | 条件 | 行为 |
|------|------|------|
| 设置 | 始终 | 打开设置 Drawer，改 name/description |
| 运行 | 始终 | 打开运行 Drawer |
| 编辑 | 始终 | → 设计器 |
| 归档工作流 | status≠ARCHIVED | POST archive |
| 恢复发布 | status=ARCHIVED | POST restore |
| 删除 | 始终 | Popconfirm → DELETE |

#### 5.6.6 运行 Drawer

| 字段 | 说明 |
|------|------|
| 运行参数 JSON | TextArea；打开时 GET workflow 尝试用 START 节点 inputParams 生成模板 |
| 默认文本 | `{"input":"请在这里填写运行参数"}` |
| 校验 | 合法 JSON 对象 |
| 非 PUBLISHED | 显示 warning，仍可尝试运行（后端可能拒绝） |
| 成功 | 跳转 `/workflow-runs/{executionId}` |

#### 5.6.7 设置 Drawer

| 字段 | 约束 |
|------|------|
| name | maxLength 200，空则保留原名 |
| description | 可空 |

#### 5.6.8 顶部按钮

| 按钮 | 行为 |
|------|------|
| 导入 DSL | **占位，无 onClick** |
| 创建工作流 | → `/workflows/new/designer` |
| 空白工作流卡片 | 同上 |

#### 5.6.9 接口

| 方法 | 路径 |
|------|------|
| GET | /api/workflows |
| GET | /api/workflows/{id} |
| PUT | /api/workflows/{id}/metadata |
| POST | /api/workflows/{id}/archive |
| POST | /api/workflows/{id}/restore |
| DELETE | /api/workflows/{id} |
| POST | /api/workflows/{id}/runs |

---

### 5.7 F005 工作流设计器

#### 5.7.1 页面原型

![工作流设计器](images/manual/workflow-designer.png)

#### 5.7.2 顶栏信息

| 元素 | 说明 |
|------|------|
| 工作流名称 | 可 inline 编辑 |
| 状态 Tag | 草稿/已发布 |
| 统计 | N 节点、M 连线、配置完成度%、最近执行状态 |
| Tab | 编排（有效）、API（占位） |

#### 5.7.3 工具栏按钮

| 按钮 | 新建模式 | 已有 ID | 说明 |
|------|:--------:|:-------:|------|
| 返回 | ✓ | ✓ | → /workflows |
| 属性 | ✓ | ✓ | 打开属性 Drawer（工作流/节点/边） |
| 调试 | | ✓ | 底部 Debug Drawer |
| 保存草稿/创建工作流 | ✓ | | 校验后 POST 或 PUT draft |
| 发布 | | ✓ | 校验通过后 publish |
| 运行 | | ✓ | draft→publish→run 链式调用 |

#### 5.7.4 画布操作

| 操作 | 说明 |
|------|------|
| 拖拽节点 | 从节点库到画布 |
| 点击节点库 | 在默认位置添加节点 |
| 端口连线 | source → target |
| 选中节点/边 | 高亮，启用复制/删除 |
| 放大/缩小/适配/自动布局 | 画布工具栏 |
| 复制节点 | 复制选中节点及配置 |
| 删除节点/连线 | 删除选中元素 |

#### 5.7.5 配置完成度算法

节点「已配置」判定（影响完成度百分比）：

| 类型 | 必填 config 键 |
|------|----------------|
| PROMPT | template, outputKey |
| LLM | providerId, model, promptKey, outputKey |
| CONDITION | contextKey, operator |
| QUESTION_CLASSIFIER | inputKey, outputKey, categories(非空) |
| 其他 | 默认视为已配置 |

#### 5.7.6 校验与错误展示

- 保存/发布/运行前调用 schema 校验 + 业务校验。
- 失败：`message.error`，最多展示 **5** 条 issue。
- 新建保存成功：redirect `/workflows/{newId}/designer`。

#### 5.7.7 调试 Drawer（DebugPanel）

| 字段 | 说明 |
|------|------|
| debugInput | JSON TextArea，默认根据 START inputParams 生成 |
| 运行 | 同工具栏「运行」逻辑 |
| 结果 | 展示 execution 状态、output、节点级结果 |

#### 5.7.8 接口

| 方法 | 路径 |
|------|------|
| GET | /api/workflows/{id} |
| POST | /api/workflows |
| PUT | /api/workflows/{id}/draft |
| PUT | /api/workflows/{id}/metadata |
| POST | /api/workflows/{id}/publish |
| POST | /api/workflows/{id}/runs |

节点配置字段详见 [第 6 章](#6-工作流节点配置规格)。

---

### 5.8 F006/F007 运行监控与执行详情

#### 5.8.1 运行监控原型

![运行监控](images/manual/workflow-runs.png)

#### 5.8.2 指标

| 指标 | 计算 |
|------|------|
| 成功率 | succeeded / total |
| 平均耗时 | 已完成记录均值(ms) |
| 失败运行 | failed 计数 |
| 执行记录 | total |

#### 5.8.3 执行记录表

| 列 | 说明 |
|----|------|
| 执行 ID | 可复制 |
| 工作流 ID | — |
| 状态 | RUNNING/SUCCEEDED/FAILED Tag |
| 开始时间 | locale 格式 |
| 耗时 | ms |
| 节点数 | nodeExecutions.length |
| 操作 | 详情 → /workflow-runs/{id} |

#### 5.8.4 执行详情页（只读）

| 区块 | 内容 |
|------|------|
| 概览 | 执行 ID、工作流 ID、耗时、节点数 |
| 输入/输出 | JSON pre 格式化 |
| 节点时间线 | nodeId、type、status、耗时、errorMessage、input/output |
| 诊断侧栏 | 失败 Alert、失败节点数 |

#### 5.8.5 接口

- `GET /api/workflow-runs`
- `GET /api/workflow-runs/{executionId}`

---

### 5.9 F008–F010 知识库

#### 5.9.1 知识库列表原型

![知识库](images/manual/knowledge.png)

#### 5.9.2 列表列

| 列 | 说明 |
|----|------|
| 知识库名称 | name + description + 向量维度 Tag |
| 向量库配置 | 配置名或「默认内存」 |
| 向量维度 | N 维 |
| 分段策略 | splitterType |
| 检索模式 | KEYWORD/VECTOR/HYBRID |
| 文档/切片 | docCount / chunkCount |
| 状态 | 「工作流已引用」等 Tag |
| 操作 | 编辑、管理文档、删除 |

#### 5.9.3 新建/编辑知识库 Drawer

| 字段 | 必填 | 默认 | UI 可见 |
|------|:----:|------|:-------:|
| name | ✓ | — | ✓ |
| description | | null | ✓ |
| embeddingModelId | | null | ✓ |
| vectorDimension | ✓ | 1536 | ✓ |
| vectorStoreConfigId | | null | ✓ |
| splitterType | | SIMPLE_TEXT | **隐藏，保存时强制** |
| chunkSize | | 500 | 隐藏 |
| chunkOverlap | | 50 | 隐藏 |
| retrievalMode | | HYBRID | 隐藏 |
| topK | | 3 | 隐藏 |

> 隐藏字段在提交时被硬编码写入，UI 暂不可编辑。

#### 5.9.4 向量库配置 Drawer

| 字段 | 必填 | 说明 |
|------|:----:|------|
| name | ✓ | 配置名称 |
| storeType | ✓ | MEMORY / ELASTICSEARCH |
| endpoint | ES 时 | 集群地址 |
| indexName | ✓ | 索引名 |
| username/password | ES | 编辑时空密码表示不变 |
| apiKey | ES | 可选 |
| connectTimeoutMs | | 100–120000 |
| readTimeoutMs | | 100–300000 |
| enabled | | boolean |

#### 5.9.5 管理文档 Drawer（宽屏）

**新增数据入口**（Dropdown）：
- 手动数据集
- 文本文档
- 表格文档

**文档列表操作**：
| 操作 | 规则 |
|------|------|
| 查看切片 | 展开 chunk 列表 |
| 重新解析 | status=PROCESSING 时 disabled |
| 删除 | 确认后删除 |

**检索测试**：
- query 必填，topK 1–10，POST search 展示结果片段。

#### 5.9.6 手动数据集表单

| 字段 | 必填 |
|------|:----:|
| entries[].title | ✓ |
| entries[].content | ✓ |
| entries[].tags | |
| entries[].category | |
| entries[].source | |

支持动态增删条目行。

#### 5.9.7 文本文档上传

| 项 | 规则 |
|----|------|
| 文件类型 | .txt .md .doc .docx .pdf .html |
| 分段策略 | FIXED_LENGTH / PARAGRAPH / SEMANTIC / SYMBOL |
| chunkSize | 必填 |
| separator | SYMBOL 策略必填 |
| 流程 | 先 preview 分段 → 确认 upload |

#### 5.9.8 表格文档上传

| 项 | 规则 |
|----|------|
| 文件类型 | .xls .xlsx |
| 分段 | STRUCTURED + chunkSize |

#### 5.9.9 文档管理页 `/knowledge/{id}/documents`

- 表格：文档名、切片数、状态、操作（查看切片/编辑/删除）。
- 命中测试：query + topK(1–20)。
- 按钮：返回、手动/文本/表格新增 → `/documents/new?type=`。

#### 5.9.10 接口（摘要）

| 方法 | 路径 |
|------|------|
| GET/POST | /api/knowledge-bases |
| PUT/DELETE | /api/knowledge-bases/{id} |
| GET/POST/PUT/DELETE | /api/vector-store-configs |
| GET/POST/DELETE | /api/knowledge-bases/{kbId}/documents... |
| POST | /api/knowledge-bases/{kbId}/search |
| POST | /api/knowledge-bases/chunks/preview |

---

### 5.10 F011 模型配置

#### 5.10.1 页面原型

![模型配置](images/manual/models.png)

#### 5.10.2 表单字段

| 字段 | 必填 | 默认 | 说明 |
|------|:----:|------|------|
| name | ✓ | — | 显示名称 |
| modelType | ✓ | DeepSeek | 11 种 Provider 类型 |
| modelUsage | ✓ | CHAT | CHAT/EMBEDDING/RERANK/MULTIMODAL |
| description | | null | |
| visionSupport | | false | 能力 Tag |
| pricePerMillionTokens | | null | ≥0 |
| baseUrl | ✓ | — | API 根地址 |
| apiKeyRef | ✓ | — | 密钥引用 |
| model | ✓ | — | 模型标识 |
| enabled | | true | |

#### 5.10.3 规则

- 工作流 LLM 节点：仅选 `modelUsage=CHAT` 且 `enabled=true`。
- 知识库嵌入：仅选 `EMBEDDING` 用途。
- 删除前无强制引用检查（业务上应谨慎）。

#### 5.10.4 接口

- `GET/POST /api/model-providers`
- `PUT/DELETE /api/model-providers/{id}`

---

### 5.11 F012 编研模板

#### 5.11.1 页面原型

![编研模板](images/manual/research-templates.png)

#### 5.11.2 列表列

模板名称、编码、章节数（可点）、绑定工作流 Tag、状态、预览/编辑/删除。

#### 5.11.3 表单

| 字段 | 必填 | 说明 |
|------|:----:|------|
| templateName | ✓ | 来自 schema.title |
| code | ✓ | 唯一编码 |
| description | | |
| category | ✓ | RESEARCH/REPORT/GENERAL |
| outputType | ✓ | MARKDOWN/DOCX/HTML/JSON |
| status | ✓ | ENABLED/DISABLED |
| workflowId | ✓ | 绑定工作流（含各状态） |
| templateSchemaText | ✓ | JSON 文本，实时 parse |

**schema 结构**：
- `variables[]`: name, type, required, description
- `sections[]`: id, title, instruction, requiredSources, citationRequired, outputFormat

保存按钮：`templateSchemaText` JSON 无效时 **disabled**。

#### 5.11.4 预览 Drawer

- `GET /api/generation-templates/{id}/runtime`
- 展示章节树 + 绑定工作流节点 Steps

#### 5.11.5 接口

- `GET/POST/PUT/DELETE /api/generation-templates`
- `GET /api/generation-templates/{id}/runtime`

---

### 5.12 F013 智能编研

#### 5.12.1 页面原型

![智能编研](images/manual/research-compile.png)

#### 5.12.2 五步向导

| 步骤 | 字段 | 必填 | 默认 |
|------|------|:----:|------|
| 0 编研主题 | topic | ✓ | — |
| 0 | audience | | 档案管理人员 |
| 1 主题库 | themeLibraryId | ✓ | theme_001 |
| 2 知识库 | knowledgeBaseIds | ✓ | 至少 1 个 |
| 3 编研模板 | templateId | ✓ | template_research_001 |
| 4 生成成果 | — | | 展示结果 |

#### 5.12.3 步骤 3 附加展示

- 选中模板后：章节预览 Table（标题、instruction 摘要）。
- 右侧：绑定工作流节点 Steps 预览。

#### 5.12.4 步骤 4 成果区

| 元素 | 说明 |
|------|------|
| 任务状态 | Job status Tag |
| 大纲 | Steps 组件 |
| 分节正文 | Markdown 渲染 + citations |
| 下载 DOCX | `hasDocx=true` 时显示，GET docx 流 |

#### 5.12.5 提交流程

```
validate 当前步骤
  → step<3: step+1
  → step=3: POST /api/research/jobs
  → poll GET /api/research/jobs/{id}/output
  → step=4 展示结果
```

#### 5.12.6 接口

- `GET /api/research/theme-libraries`
- `POST /api/research/jobs`
- `GET /api/research/jobs/{id}/output`
- `GET /api/research/outputs/{id}/docx`

---

### 5.13 F014–F021 系统管理

#### 5.13.1 F014 租户管理

![租户管理](images/manual/system-tenants.png)

| 项目 | 规则 |
|------|------|
| 权限 | 仅 `platform_admin` + 默认租户 |
| 新建 | code + name |
| 编辑 | code 不可改；`tenant_default` 不可改 status |
| 删除 | `tenant_default` 不可删 |
| 管理 | → `/system/tenants/{id}` 工作台 |

API: `/api/auth/admin/tenants`

#### 5.13.2 F015 组织用户

![组织用户](images/manual/system-users.png)

**Tab：组织架构 | 用户 | 角色**

**组织 Tab**：

| 字段 | 新建 | 编辑 |
|------|:----:|:----:|
| code | ✓ | 不可改 |
| name | ✓ | ✓ |
| orgType | ✓ | ✓ |
| parentId | | ✓ |
| externalOrgId | | ✓ |
| sortOrder | 0 | ✓ |

**用户 Tab**：
- 左侧组织树过滤（含「全部组织」）。
- 用户 Drawer：username、password(新建必填)、displayName、mobile、email、sortOrder、organizationIds、roleCodes。
- 操作：启用/禁用/锁定/重置密码/删除。
- 批量排序：须选具体组织，逐用户改 sortOrder。

**角色 Tab**：
- code、name、roleType(PLATFORM/BUSINESS/INTEGRATION)、organizationId、externalRoleId。

API 前缀：`/api/auth/admin/tenants/{tenantId}/...`

#### 5.13.3 F016 资产授权

![资产授权](images/manual/asset-grants.png)

| Tab | 列表来源 |
|-----|----------|
| 智能体 | GET /api/bots |
| 知识库 | GET /api/knowledge-bases |
| 工作流 | GET /api/workflows |
| 大模型 | GET /api/model-providers |

**授权 Drawer**：

| 字段 | 说明 |
|------|------|
| unitIds | 多选，空=全部单位 |
| unitScope | SELF / SUBTREE |
| departmentIds | 多选，空=全部部门 |
| departmentScope | SELF / SUBTREE |

保存：`POST /api/asset-grants` body 含 assetType、assetId、scopes。

#### 5.13.4 F017 第三方应用

| 功能 | 说明 |
|------|------|
| 新增 | code、name、appType、authType |
| 配置 Drawer | 生成 API Key（一次性展示完整 key）、资产白名单 |
| 白名单 | scopeType: BOT/KB/WORKFLOW/MODEL/TENANT + 多选资产 |
| 过滤 | Bot 仅 ENABLED；Workflow 排除 ARCHIVED；Model 仅 enabled |

API：`/api/auth/admin/tenants/{tenantId}/integration-apps`

#### 5.13.5 F019 菜单管理

![菜单管理](images/manual/system-menus.png)

| 字段 | 必填 | 编辑限制 |
|------|:----:|----------|
| groupTitle | ✓ | |
| menuKey | ✓ | 编辑 disabled |
| title | ✓ | |
| path | ✓ | 以 / 开头 |
| sortOrder | | 0 |
| visible | | true |
| platformOnly | | false |
| status | | ENABLED |

#### 5.13.6 F020 数据字典

![数据字典](images/manual/system-dictionary.png)

- 左栏字典：code(新建必填)、name、description、status。
- 右栏字典项：label、value（同字典内唯一）、description、sortOrder、status。
- 删除字典提示含全部字典项。

#### 5.13.7 F021 日志管理

![日志管理](images/manual/system-logs.png)

| 筛选项 | 说明 |
|--------|------|
| eventType | 下拉，来自 GET event-types |
| userId | 文本 |
| result | SUCCESS / FAILURE |
| 时间范围 | RangePicker，默认近 7 天 |

| 列 | 说明 |
|----|------|
| 时间 | createdAt |
| 事件 | LOGIN_SUCCESS 等 |
| 用户 | userId |
| 结果 | Tag |
| 错误码 | 失败时 |
| IP | clientIp |
| 应用 | appId |

---

## 6 工作流节点配置规格

### 6.1 节点类型一览

| 类型 | 名称 | 执行器 |
|------|------|--------|
| START | 开始 | StartNodeExecutor |
| END | 结束 | EndNodeExecutor |
| LLM | 大模型 | LlmNodeExecutor |
| PROMPT | Prompt 模板 | PromptNodeExecutor |
| KNOWLEDGE_RETRIEVAL | 知识库检索 | KnowledgeRetrievalNodeExecutor |
| HTTP_TOOL | HTTP 请求 | HttpToolNodeExecutor |
| CONDITION | 条件分支 | ConditionNodeExecutor |
| QUESTION_CLASSIFIER | 问题分类 | QuestionClassifierNodeExecutor |
| TEXT_TRANSFORM | 文本处理 | TextTransformNodeExecutor |
| CONTENT_TEMPLATE | 内容模板 | ContentTemplateNodeExecutor |
| LOOP | 循环 | LoopNodeExecutor |

### 6.2 START 节点

| 配置项 | 类型 | 说明 |
|--------|------|------|
| inputParams[] | array | name, type, required |

输出：将 input 原样写入 context。

### 6.3 LLM 节点

| 配置项 | 说明 |
|--------|------|
| providerId | 模型 Provider ID |
| model | 模型名 |
| systemPrompt / userPrompt | 支持 `{{var}}` |
| temperature / topP / topK | 采样参数 |
| maxTokens | 最大输出 |
| outputKey | 输出变量名 |
| timeoutMs / maxRetries | 超时重试 |
| errorStrategy | 失败策略 |

### 6.4 KNOWLEDGE_RETRIEVAL 节点

| 配置项 | 说明 |
|--------|------|
| knowledgeBaseIds | 可多库 |
| keywordTemplate / queryTemplate | 查询文本模板 |
| fetchCount / topK | 返回条数 |

输出：`content`（拼接文本）、`sources`、`query`。

### 6.5 HTTP_TOOL 节点

| 配置项 | 说明 |
|--------|------|
| method | GET/POST/PUT/PATCH/DELETE |
| url | 支持模板 |
| headers / params | 键值对 |
| bodyType | JSON / FORM_DATA |
| bodyTemplate | 请求体 |
| responseBodyType | JSON/TEXT |
| timeoutMs | 超时 |

输出：`statusCode`、`body`、`headers` 等（按 responseKey 配置）。

### 6.6 QUESTION_CLASSIFIER 节点

| 配置项 | 说明 |
|--------|------|
| inputKey | 待分类文本变量 |
| outputKey | 输出 categoryId |
| categories[] | id, name, keywords[], matchMode |
| defaultCategoryId | 未命中默认 |

### 6.7 LOOP 节点

| 配置项 | 说明 |
|--------|------|
| loopVar / itemsExpression | 数组表达式 |
| itemVar / indexVar | 循环变量名 |
| maxIterations | 上限 |
| loopSteps[] | 循环体内步骤（CONTENT_TEMPLATE/HTTP/LLM 等） |

输出：`loopResults` 数组。

### 6.8 END 节点

| 配置项 | 说明 |
|--------|------|
| outputParams[] | name, value(变量引用), type |

---

## 7 系统架构设计

### 7.1 总体架构

```
┌──────────────────┐     HTTPS/JSON      ┌─────────────────────────────┐
│  Admin SPA       │ ──────────────────→ │  aiworkflow-server :8080     │
│  React18+Vite    │                     │  Spring Boot 3.3             │
└──────────────────┘                     │  ├─ workflow engine          │
┌──────────────────┐   AppCode+ApiKey    │  ├─ knowledge / bot / auth   │
│  第三方业务系统   │ ──→ /api/open/** ──→│  └─ Store → MyBatis          │
└──────────────────┘                     └──────────────┬──────────────┘
                                                        │
                                              PostgreSQL / 达梦 + Flyway
```

### 7.2 后端模块

| 包 | 职责 |
|----|------|
| auth | 登录、租户、Filter 链 |
| workflow | DAG、执行器、运行记录 |
| knowledge | 文档、切片、向量检索 |
| bot | 智能体、会话、消息 |
| model / prompt | 模型与 Prompt 模板 |
| generation | 编研模板与任务 |
| asset | 资产授权 |
| system | 菜单、字典、审计日志 |
| integration | 第三方应用 |

### 7.3 前端 Monorepo

| 包 | 说明 |
|----|------|
| apps/admin | 管理控制台 |
| workflow-schema | 类型与校验 |
| workflow-designer-* | 设计器 React/Vue/WC |
| workflow-sdk | TS 客户端 |

### 7.4 部署

| 组件 | 端口 | 说明 |
|------|------|------|
| server | 8080 | `mvn spring-boot:run` |
| admin dev | 5173 | `pnpm --filter @aiworkflow/admin dev` |
| PostgreSQL | 5432 | docker-compose |

环境变量：`POSTGRES_JDBC_URL`、`SPRING_PROFILES_ACTIVE`、`FLYWAY_LOCATIONS` 等。

---

## 8 附录

### 8.1 已知占位/待完善项

| 项 | 位置 | 说明 |
|----|------|------|
| 导入 DSL | 工作流列表 | 按钮无事件 |
| API Tab | 设计器 | 无内容 |
| 导入节点模板 | 节点库 | 无事件 |
| 集成指南 | /tools | PlaceholderPage |
| 工作台「今日执行」 | Dashboard | 未按自然日过滤 |
| 知识库分段 UI | 创建 Drawer | 字段隐藏，后端写死默认值 |

### 8.2 相关文档

- [aiworkflow-tech-manual.md](./aiworkflow-tech-manual.md) — 技术手册
- [aiworkflow-user-manual.md](./aiworkflow-user-manual.md) — 操作手册
- [openapi/open-api.yaml](./openapi/open-api.yaml) — 开放 API 规范

### 8.3 确认说明

本文档版本 **1.1** 为细化稿。请确认章节结构与功能描述后，再生成：

- `aiworkflow-system-design.docx`
- `aiworkflow-system-design.pdf`

（生成时将内嵌 `docs/images/manual/` 下全部界面截图。）
