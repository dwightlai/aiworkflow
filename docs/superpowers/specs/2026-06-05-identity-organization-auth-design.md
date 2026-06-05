# 身份、组织、租户与认证设计

## 1. 背景

智能体平台需要为知识库、智能体、生成模板、外部资料源和第三方开放 API 提供统一的身份与组织上下文。

当前阶段只考虑逻辑隔离，不做数据库物理隔离。平台需要内置默认用户、默认租户、默认单位、默认部门和默认角色，同时预留数字档案馆、OA 和其他业务系统接入能力。

本设计与《智能体、知识库、多单位共享与通用生成模板设计》配套使用。后续知识库、智能体和生成模板的授权判断，都依赖本设计提供的 `tenantId`、`unitId`、`departmentIds`、`roleIds` 和 `userId`。

## 2. 设计目标

- 提供平台默认租户、单位、部门、角色和管理员用户。
- 支持平台管理后台本地登录。
- 支持第三方系统通过 API Key、JWT 或 Token Introspection 接入。
- 支持第三方调用时传入业务用户上下文。
- 支持用户属于多个部门、拥有多个角色。
- 支持租户级逻辑隔离。
- 支持单位、部门、角色用于知识库、智能体和模板授权。
- 所有新增业务表统一使用 `agi_` 前缀。

## 3. 命名约束

新增表名统一使用 `agi_` 前缀。

示例：

```text
agi_tenant
agi_unit
agi_department
agi_role
agi_user
agi_integration_app
```

如果后续需要和历史表共存，历史表可以通过迁移逐步改名或新表承接，不在本设计阶段直接修改现有表。

## 4. 核心概念

### 4.1 租户

租户用于逻辑隔离。当前阶段不做物理库、物理 schema 或物理表隔离。

租户隔离规则：

- 平台资产、组织、用户、角色、第三方应用都带 `tenant_id`。
- 管理后台查询默认按当前 `tenant_id` 过滤。
- 第三方开放 API 根据应用身份和调用上下文确定 `tenant_id`。
- 审计日志记录 `tenant_id`。

默认租户：

```text
id: tenant_default
code: default
name: 默认租户
```

### 4.2 单位

单位是业务组织边界。数字档案馆、OA 或其他业务系统传入的业务单位标识映射到平台单位。

单位可以是：

- 平台默认单位。
- 数字档案馆中的档案馆、机关、业务单位。
- OA 中的组织机构。
- 其他业务系统中的业务主体。

默认单位：

```text
id: unit_default
tenant_id: tenant_default
name: 默认单位
```

### 4.3 部门

部门归属于单位，支持树形结构。用户可以属于多个部门。

默认部门：

```text
id: dept_default
tenant_id: tenant_default
unit_id: unit_default
name: 默认部门
```

### 4.4 角色

角色分为平台角色和业务角色。

平台角色用于管理后台和平台能力控制：

- `platform_admin`：平台管理员。
- `unit_admin`：单位管理员。
- `asset_manager`：资产管理员，维护知识库、智能体和模板。
- `app_user`：普通使用者。
- `integration_admin`：第三方接入管理员。
- `auditor`：审计员。

业务角色来自第三方系统或组织系统，例如：

- `archive_admin`
- `archive_user`
- `oa_user`
- `department_leader`

授权判断时，用户拥有多个角色只要命中任意一个授权角色即可。

### 4.5 用户

用户分为本地用户和外部用户。

- `LOCAL`：平台本地用户，用于管理后台登录和平台配置。
- `EXTERNAL`：来自数字档案馆、OA 或其他业务系统的用户映射。

平台需要一个默认管理员：

```text
username: admin
tenant_id: tenant_default
unit_id: unit_default
department_id: dept_default
role: platform_admin
```

默认管理员密码由部署配置或初始化脚本生成，不应在代码中写死明文。

## 5. 数据模型

### 5.1 agi_tenant

```text
agi_tenant
- id
- code
- name
- status              ACTIVE / DISABLED
- created_at
- updated_at
```

### 5.2 agi_unit

```text
agi_unit
- id
- tenant_id
- external_unit_id
- name
- unit_type           PLATFORM / ARCHIVE_ORG / OA_ORG / BUSINESS_ORG
- status              ACTIVE / DISABLED
- created_at
- updated_at
```

`external_unit_id` 用于映射第三方平台传入的单位标识。

### 5.3 agi_department

```text
agi_department
- id
- tenant_id
- unit_id
- external_department_id
- parent_id
- name
- sort_order
- status              ACTIVE / DISABLED
- created_at
- updated_at
```

`parent_id` 为空表示单位下的一级部门。

### 5.4 agi_role

```text
agi_role
- id
- tenant_id
- unit_id
- external_role_id
- code
- name
- role_type           PLATFORM / BUSINESS
- status              ACTIVE / DISABLED
- created_at
- updated_at
```

平台角色可以不绑定具体单位，业务角色通常绑定单位。

### 5.5 agi_user

```text
agi_user
- id
- tenant_id
- username
- password_hash
- display_name
- mobile
- email
- user_type           LOCAL / EXTERNAL
- source_app_id
- external_user_id
- status              ACTIVE / DISABLED / LOCKED
- last_login_at
- created_at
- updated_at
```

外部用户可以没有 `password_hash`，由第三方系统认证。

### 5.6 agi_user_unit

```text
agi_user_unit
- id
- tenant_id
- user_id
- unit_id
- primary_unit
- created_at
```

一个用户可以属于多个单位，第一阶段可以只使用一个主单位。

### 5.7 agi_user_department

```text
agi_user_department
- id
- tenant_id
- user_id
- department_id
- primary_department
- created_at
```

一个用户可以属于多个部门。

### 5.8 agi_user_role

```text
agi_user_role
- id
- tenant_id
- user_id
- role_id
- created_at
```

一个用户可以拥有多个角色。

### 5.9 agi_integration_app

第三方平台统一建模为接入应用。

```text
agi_integration_app
- id
- tenant_id
- code
- name
- app_type            ARCHIVE_SYSTEM / OA_SYSTEM / BUSINESS_SYSTEM / OTHER
- auth_type           API_KEY / JWT / TOKEN_INTROSPECTION / OAUTH2
- status              ACTIVE / DISABLED
- created_at
- updated_at
```

### 5.10 agi_integration_app_secret

```text
agi_integration_app_secret
- id
- tenant_id
- app_id
- secret_hash
- secret_prefix
- expires_at
- enabled
- created_at
- updated_at
```

密钥只保存 hash，不保存明文。`secret_prefix` 用于管理后台展示和排查。

### 5.11 agi_integration_app_scope

第三方应用访问范围。

```text
agi_integration_app_scope
- id
- tenant_id
- app_id
- scope_type          TENANT / UNIT / BOT / KNOWLEDGE_BASE / TEMPLATE / EXTERNAL_CORPUS_SOURCE
- scope_id
- permission          USE / MANAGE
- enabled
- created_at
- updated_at
```

### 5.12 agi_login_session

管理后台登录会话。

```text
agi_login_session
- id
- tenant_id
- user_id
- refresh_token_hash
- user_agent
- client_ip
- expires_at
- revoked_at
- created_at
```

### 5.13 agi_auth_audit_log

认证和第三方接入审计。

```text
agi_auth_audit_log
- id
- tenant_id
- event_type          LOGIN_SUCCESS / LOGIN_FAILED / TOKEN_REFRESH / API_KEY_USED / APP_DENIED / LOGOUT
- user_id
- app_id
- unit_id
- department_ids
- role_ids
- client_ip
- user_agent
- result
- error_code
- occurred_at
```

## 6. 认证链路

### 6.1 管理后台本地登录

管理后台使用本地账号登录：

```text
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me
```

登录成功返回：

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 3600,
  "user": {
    "id": "user_admin",
    "username": "admin",
    "tenantId": "tenant_default",
    "unitIds": ["unit_default"],
    "departmentIds": ["dept_default"],
    "roleIds": ["platform_admin"]
  }
}
```

### 6.2 平台 JWT

平台内部 access token 使用 JWT。

建议 claims：

```json
{
  "sub": "user_admin",
  "tenantId": "tenant_default",
  "unitIds": ["unit_default"],
  "departmentIds": ["dept_default"],
  "roleIds": ["platform_admin"],
  "userType": "LOCAL",
  "tokenType": "ACCESS"
}
```

后台接口从 JWT 中解析当前身份上下文。

### 6.3 第三方 API Key

第一阶段推荐第三方开放 API 使用 API Key。

请求头：

```text
X-AGI-App-Code: archive-system
X-AGI-Api-Key: agi_xxx
```

请求体携带业务用户上下文：

```json
{
  "unitId": "archive_unit_001",
  "departmentIds": ["dept_archive"],
  "roleIds": ["archive_user"],
  "userId": "external_user_001"
}
```

校验顺序：

```text
1. 根据 app code 找到 agi_integration_app。
2. 校验 API Key hash。
3. 校验应用状态和密钥状态。
4. 根据 app scope 校验应用是否可以代表该单位调用。
5. 构造运行上下文。
6. 进入智能体、知识库、模板和外部资料源授权判断。
7. 记录 agi_auth_audit_log。
```

### 6.4 第三方 JWT 预留

如果第三方系统可以签发 JWT，平台可配置公钥或 JWKS 地址。

预留配置：

```text
auth_type = JWT
jwks_url
issuer
audience
```

JWT claims 需要包含：

```json
{
  "sub": "external_user_001",
  "unitId": "archive_unit_001",
  "departmentIds": ["dept_archive"],
  "roleIds": ["archive_user"]
}
```

第一阶段只预留，不必完整实现。

### 6.5 Token Introspection 预留

如果第三方系统提供令牌校验接口，平台可通过 Token Introspection 获取用户上下文。

预留配置：

```text
auth_type = TOKEN_INTROSPECTION
introspection_url
client_id
client_secret_ref
```

第一阶段只预留，不必完整实现。

### 6.6 OAuth2 预留

OAuth2 适合后续更完整的服务间集成和单点登录。

预留方向：

- Client Credentials：第三方服务调用平台开放 API。
- Authorization Code：用户从第三方系统跳转进入平台管理页面。

第一阶段不强制实现 OAuth2。

## 7. 组织上下文解析

平台内部统一使用运行上下文：

```text
RuntimeIdentityContext
- tenantId
- appId
- userId
- unitId
- departmentIds
- roleIds
- authType
- source
```

上下文来源：

- 管理后台：从平台 JWT 解析。
- 第三方 API Key：从应用身份和请求体解析。
- 第三方 JWT：从 JWT claims 解析。
- Token Introspection：从第三方认证服务返回结果解析。

所有业务授权都只依赖 `RuntimeIdentityContext`，不直接依赖具体认证方式。

## 8. 第三方用户映射策略

第三方调用时，平台可以有两种处理方式：

### 8.1 仅使用运行上下文

不创建平台用户，只使用请求里的 `externalUserId`、`unitId`、`departmentIds` 和 `roleIds` 做权限判断和审计。

优点：

- 接入简单。
- 不需要同步大量用户。
- 适合智能体开放 API。

缺点：

- 平台内无法完整管理外部用户档案。

### 8.2 懒加载用户映射

第三方用户第一次调用时，平台创建或更新 `agi_user`，并记录 `source_app_id` 和 `external_user_id`。

优点：

- 审计和统计更完整。
- 后续可以在平台内查看外部用户使用情况。

缺点：

- 需要处理用户信息更新和失效。

推荐：

第一阶段开放 API 使用“仅运行上下文”，管理后台使用本地用户。后续如果需要精细统计，再启用懒加载用户映射。

## 9. 逻辑隔离规则

所有核心资产表建议包含：

```text
tenant_id
owner_unit_id
created_by
updated_by
```

适用对象：

- `agi_workflow`
- `agi_prompt_template`
- `agi_model_provider`
- `agi_knowledge_base`
- `agi_ai_bot`
- `agi_generation_template`
- `agi_external_corpus_source`
- `agi_integration_app`

查询规则：

```text
管理后台查询：
当前 tenant_id 内的数据。

第三方开放 API：
根据 app_id 和 unitId 校验后，只允许访问授权范围内的数据。

资产共享：
不靠 tenant_id 直接共享，靠 asset_grant 或 app_scope 授权。
```

## 10. 默认初始化数据

系统首次启动或迁移后初始化：

```text
agi_tenant:
- tenant_default / default / 默认租户

agi_unit:
- unit_default / 默认单位

agi_department:
- dept_default / 默认部门

agi_role:
- platform_admin / 平台管理员
- unit_admin / 单位管理员
- asset_manager / 资产管理员
- app_user / 普通使用者
- integration_admin / 第三方接入管理员
- auditor / 审计员

agi_user:
- admin / 平台管理员

agi_user_unit:
- admin -> unit_default

agi_user_department:
- admin -> dept_default

agi_user_role:
- admin -> platform_admin
```

默认管理员密码来源：

- 优先读取环境变量。
- 如果没有环境变量，生成一次性初始密码并输出到启动日志。
- 首次登录后要求修改密码。

## 11. 与资产授权的关系

身份和组织模块只负责回答：

```text
当前调用者是谁？
属于哪个租户？
代表哪个单位？
属于哪些部门？
拥有哪些角色？
来自哪个第三方应用？
```

知识库、智能体、模板和外部资料源授权模块负责回答：

```text
当前调用上下文是否可以 USE 或 MANAGE 某个资产？
```

两者通过 `RuntimeIdentityContext` 连接。

## 12. 前端管理设计

### 12.1 用户管理

- 用户列表。
- 新增本地用户。
- 启用、禁用、锁定用户。
- 重置密码。
- 分配单位、部门、角色。
- 查看外部用户映射。

### 12.2 单位管理

- 单位列表。
- 新增、编辑、禁用单位。
- 维护外部单位标识。
- 查看单位下部门和用户。

### 12.3 部门管理

- 树形部门管理。
- 新增、编辑、禁用部门。
- 维护外部部门标识。
- 分配部门用户。

### 12.4 角色管理

- 平台角色列表。
- 业务角色列表。
- 新增、编辑、禁用角色。
- 维护外部角色标识。
- 分配角色用户。

### 12.5 第三方应用管理

- 第三方应用列表。
- 新增数字档案馆、OA 或业务系统接入应用。
- 维护认证方式。
- 生成和轮换 API Key。
- 配置可代表单位范围。
- 配置可调用智能体、知识库、模板和外部资料源范围。

## 13. 异常策略

- 登录失败：`AUTH_LOGIN_FAILED`。
- 用户被禁用：`AUTH_USER_DISABLED`。
- 用户被锁定：`AUTH_USER_LOCKED`。
- Token 无效：`AUTH_TOKEN_INVALID`。
- Token 过期：`AUTH_TOKEN_EXPIRED`。
- 第三方应用不存在：`APP_NOT_FOUND`。
- 第三方应用禁用：`APP_DISABLED`。
- API Key 无效：`APP_SECRET_INVALID`。
- 第三方应用无单位范围：`APP_UNIT_SCOPE_DENIED`。
- 缺少运行上下文：`IDENTITY_CONTEXT_MISSING`。
- 租户不匹配：`TENANT_MISMATCH`。

所有认证失败和第三方应用拒绝事件都写入 `agi_auth_audit_log`。

## 14. 分阶段落地

### 阶段一：默认身份与本地登录

- 新增 `agi_tenant`、`agi_unit`、`agi_department`、`agi_role`、`agi_user`。
- 新增用户与单位、部门、角色关系表。
- 初始化默认租户、单位、部门、角色和 admin 用户。
- 实现本地登录、JWT、刷新和退出。

### 阶段二：第三方 API Key 接入

- 新增 `agi_integration_app`。
- 新增 `agi_integration_app_secret`。
- 新增 `agi_integration_app_scope`。
- 开放 API 支持 API Key。
- 开放 API 支持传入单位、部门、角色、用户上下文。

### 阶段三：运行上下文接入资产授权

- 引入 `RuntimeIdentityContext`。
- 知识库、智能体、模板和外部资料源授权统一读取上下文。
- 审计日志记录租户、单位、部门、角色、用户和第三方应用。

### 阶段四：第三方认证增强

- 支持第三方 JWT。
- 支持 Token Introspection。
- 预留 OAuth2 Client Credentials。
- 可选支持外部用户懒加载映射。

### 阶段五：组织同步

- 支持从数字档案馆、OA 或组织系统同步单位、部门、角色和用户。
- 支持定时同步和手动同步。
- 支持外部组织禁用后平台侧联动禁用。

## 15. 验收标准

- 系统初始化后存在默认租户、默认单位、默认部门、默认角色和 admin 用户。
- 管理后台可以通过本地账号登录并获得 JWT。
- JWT 中包含租户、单位、部门、角色和用户信息。
- 一个用户可以属于多个部门并拥有多个角色。
- 第三方应用可以通过 API Key 调用开放 API。
- 第三方调用时可以传入单位、部门、角色和用户上下文。
- 平台可以根据第三方应用范围判断是否允许代表某个单位调用。
- 所有新增表名都使用 `agi_` 前缀。
- 所有核心资产后续都能通过 `tenant_id` 做逻辑隔离。
- 认证失败、API Key 使用和应用拒绝都能进入审计日志。
