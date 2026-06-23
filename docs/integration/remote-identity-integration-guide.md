# AGI 远程身份接入开发指南

| 项目 | 说明 |
|------|------|
| 版本 | v1.0 |
| 日期 | 2026-06-18 |
| 读者 | AGI 后端开发、档案馆 BFF 开发、集成联调人员 |
| 关联 | [混合身份设计](./agi-hybrid-identity-design.md)、[开放 API 指南](../open-api-integration-guide.md)、[Adapter 接口速查](./archive-identity-adapter-spec.md) |

---

## 1. 背景

数字档案馆已有独立的：

- **auth-service**：认证、token 校验、用户基本信息
- **organization-service**：组织、用户、单位、部门、角色

上述微服务 **接口不可修改**。AGI 作为消费方，通过 **remote 模式 + Adapter** 接入。

接入完成后：

| 数据/能力 | 主数据来源 |
|-----------|------------|
| 登录与 token 校验 | auth-service |
| 组织 / 用户 / 角色 | organization-service |
| AI 资产（知识库、智能体、工作流等） | AGI |
| 管理端会话 token（JWT） | AGI 签发（基于外部身份） |
| 终端 AI 调用 | Open API（API Key + 身份透传） |

---

## 2. 架构总览

```text
┌─────────────────────────────────────────────────────────────┐
│                    数字档案馆业务系统                          │
│  前端 ──► 档案馆 BFF ──► auth-service / organization-service │
└───────────────┬─────────────────────────────┬─────────────┘
                │ SSO 换票（配置态）              │ Open API（运行态）
                ▼                               ▼
┌───────────────────────────────────────────────────────────────┐
│                         AGI 平台                               │
│  POST /api/auth/sso/exchange                                   │
│       └─► ExternalAuthAdapter                                  │
│       └─► ExternalOrganizationAdapter                          │
│       └─► 签发 AGI JWT ──► /api/* 管理接口                     │
│                                                                │
│  /api/open/* + API Key + userId/unitId/roleIds                 │
└───────────────────────────────────────────────────────────────┘
```

### 2.1 两条接入通道

| 通道 | 用途 | 谁调用 | 鉴权方式 |
|------|------|--------|----------|
| **SSO 换票** | 管理端 / iframe 配置 AI 资产 | 档案馆 BFF | 外部 accessToken → AGI JWT |
| **Open API** | 终端用户对话、编研、工作流运行 | 档案馆 BFF | API Key + 透传用户上下文 |

> SSO 解决「谁能进 AGI 配置页」；Open API 解决「业务系统如何调 AI」。两条通道互补，通常都需要。

---

## 3. 启用 remote 模式

### 3.1 配置项

`application.yml` 或环境变量：

```yaml
agi:
  identity:
    mode: remote                    # local | remote
    fixed-tenant-id: tenant_default
    auto-provision-shadow-user: false
    role-mapping:                   # 外部角色 → AGI 平台角色
      archive_admin: asset_manager
      archive_config: asset_manager
      archive_user: app_user
    remote:
      allow-break-glass-login: true   # 是否允许运维本地账号登录
      disable-local-org-admin: true   # 是否禁用 AGI 本地组织用户维护
    adapter:
      auth: unconfigured            # unconfigured | feign | 自定义 Bean 覆盖
      organization: unconfigured

third-party:
  auth:
    base-url: ${AUTH_SERVICE_BASE_URL:}
  organization:
    base-url: ${ORGANIZATION_SERVICE_BASE_URL:}
```

### 3.2 激活档案馆 Profile

```bash
SPRING_PROFILES_ACTIVE=archive-integrated
```

对应文件：`server/src/main/resources/application-archive-integrated.yml`

### 3.3 验证模式

```http
GET /api/auth/identity/mode
```

响应示例：

```json
{
  "success": true,
  "data": {
    "mode": "remote",
    "fixedTenantId": "tenant_default",
    "allowBreakGlassLogin": true,
    "disableLocalOrgAdmin": true
  }
}
```

---

## 4. 通道一：SSO 换票（认证接入）

### 4.1 调用方式

档案馆 BFF 在确认用户已登录后，将 auth-service 颁发的 access token 交给 AGI：

```http
POST /api/auth/sso/exchange
Content-Type: application/json

{
  "externalToken": "<auth-service access token>"
}
```

成功响应（与本地登录结构相同）：

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 3600,
    "user": {
      "id": "u_001",
      "username": "zhangsan",
      "tenantId": "tenant_default",
      "displayName": "张三",
      "userType": "EXTERNAL",
      "organizationIds": ["org_001"],
      "activeOrganizationId": "org_001",
      "unitIds": ["unit_001"],
      "activeUnitId": "unit_001",
      "departmentIds": ["dept_001"],
      "roleIds": ["asset_manager"]
    }
  }
}
```

### 4.2 AGI 内部处理流程

```text
1. 校验 agi.identity.mode == remote
2. ExternalAuthAdapter.introspect(externalToken)
3. ExternalAuthAdapter.getUser(userId)          ← 可选，失败则用 introspect 兜底
4. ExternalOrganizationAdapter.loadOrganizationContext(userId)
5. 合并 auth / org 两侧 roleIds
6. 按 agi.identity.role-mapping 映射为 AGI 角色
7. 组装 AuthUserPrincipal（userType=EXTERNAL, tenantId=fixed-tenant-id）
8. 签发 AGI accessToken + refreshToken
9. 写入登录会话（支持 refresh / logout）
```

### 4.3 iframe 管理端集成

```text
档案馆前端
  → GET 档案馆 BFF /api/agi/embed-session（BFF 校验档案馆 session）
  → BFF POST AGI /api/auth/sso/exchange
  → 前端收到 AGI tokens
  → 写入 localStorage 键名 agi_admin_auth
  → iframe 加载 AGI 管理页（?embed=1）
  → 后续请求带 Authorization: Bearer <accessToken>
```

`localStorage` 结构：

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 3600,
  "expiresAt": 1718700000000,
  "user": { ... }
}
```

### 4.4 Token 刷新与登出

与本地模式相同：

```http
POST /api/auth/refresh
{ "refreshToken": "..." }

POST /api/auth/logout
{ "refreshToken": "..." }

GET /api/auth/me
Authorization: Bearer <accessToken>
```

### 4.5 break-glass 运维登录

`remote` 模式下，`POST /api/auth/login` **仅允许 `userType=LOCAL` 的运维账号**（如 `platform_ops`），供平台排障。

档案馆业务人员 **不应** 使用 AGI 本地账号，应走 SSO 换票。

---

## 5. 通道二：Open API（运行态，组织上下文透传）

SSO 换票不替代 Open API。终端用户调智能体、编研等，仍由档案馆 BFF 代调：

```http
POST /api/open/bots/{botId}/chat/sessions/{sessionId}/messages/stream
X-AGI-App-Code: digital-archive
X-AGI-Api-Key: <密钥，仅 BFF 持有>
X-AGI-User-Id: u_001
X-AGI-Unit-Id: unit_001
X-AGI-Department-Ids: dept_001,dept_002
X-AGI-Role-Ids: archive_user
Content-Type: application/json

{ "message": "帮我查档案借阅流程" }
```

身份预检：

```http
POST /api/open/identity/resolve
```

详见 [开放 API 对接指南](../open-api-integration-guide.md)。

---

## 6. Adapter 开发（核心工作）

档案馆接口文档到位后，在 AGI 工程中实现两个 Spring Bean。**不要修改档案馆微服务。**

### 6.1 代码位置

```text
server/src/main/java/com/mw/ai/agi/auth/identity/adapter/
  ExternalAuthAdapter.java              ← 接口（已存在）
  ExternalOrganizationAdapter.java      ← 接口（已存在）
  archive/
    ArchiveExternalAuthAdapter.java     ← 待实现
    ArchiveExternalOrganizationAdapter.java  ← 待实现
```

注册 `@Component` 后，默认的 `UnconfiguredExternalAuthAdapter` 自动失效。

### 6.2 ExternalAuthAdapter

```java
public interface ExternalAuthAdapter {
    ExternalTokenIntrospection introspect(String accessToken);
    Optional<ExternalUserProfile> getUser(String userId);
}
```

#### 6.2.1 ExternalTokenIntrospection

| 字段 | 类型 | 必填 | 映射到 AGI |
|------|------|------|------------|
| active | boolean | 是 | false → 401 |
| userId | string | 是 | `AuthUserPrincipal.id` |
| username | string | 否 | `AuthUserPrincipal.username` |
| roleIds | string[] | 否 | 合并后映射 → `roleIds` |
| permissions | string[] | 否 | 暂不写入 JWT，可扩展 |
| expiresAtEpochSeconds | long | 否 | 可选校验 |

#### 6.2.2 ExternalUserProfile

| 字段 | 类型 | 必填 | 映射到 AGI |
|------|------|------|------------|
| userId | string | 是 | `id` |
| username | string | 是 | `username` |
| displayName | string | 否 | `displayName` |
| enabled | boolean | 是 | false → 403 |
| roleIds | string[] | 否 | 与 introspect 角色合并 |

#### 6.2.3 实现要点

1. `introspect` 必须调用档案馆 auth-service 校验 token
2. `getUser` 建议实现；若档案馆无单独用户接口，可返回 `Optional.empty()`，AGI 用 introspect 兜底
3. 网络/解析异常建议抛 `AuthException("AUTH_TOKEN_INVALID", UNAUTHORIZED, "...")`
4. 不要在 Adapter 中写死角色映射，映射由 `agi.identity.role-mapping` 配置

### 6.3 ExternalOrganizationAdapter

```java
public interface ExternalOrganizationAdapter {
    ExternalOrganizationProfile loadOrganizationContext(String userId);
}
```

#### 6.3.1 ExternalOrganizationProfile

| 字段 | 类型 | 必填 | 映射到 AGI |
|------|------|------|------------|
| userId | string | 是 | — |
| organizationIds | string[] | 否 | `organizationIds` |
| activeOrganizationId | string | 否 | `activeOrganizationId` |
| unitIds | string[] | 否 | `unitIds` |
| activeUnitId | string | 否 | `activeUnitId`，工作流/审计常用 |
| departmentIds | string[] | 否 | `departmentIds` |
| roleIds | string[] | 否 | 与 auth 角色合并后映射 |

#### 6.3.2 实现要点

1. 调用 organization-service 查询用户所属单位、部门
2. **至少提供** `unitIds/activeUnitId` 或 `departmentIds` 之一
3. 用户不存在时抛 `AuthException` 或返回空组织（按档案馆语义决定）
4. AGI **不会**把组织数据同步进 `agi_organization` 表

### 6.4 角色合并与映射规则

```text
最终 roleIds =
  map(
    introspect.roleIds
    ∪ userProfile.roleIds
    ∪ organizationProfile.roleIds
  )

map(role) = agi.identity.role-mapping[role] ?? role
```

示例配置：

```yaml
agi:
  identity:
    role-mapping:
      archive_admin: asset_manager
      archive_config: asset_manager
      archive_user: app_user
```

AGI 平台侧常用角色：

| 角色编码 | 用途 |
|----------|------|
| platform_admin | 平台运维 |
| asset_manager | 资产配置（知识库/智能体/工作流） |
| unit_admin | 单位管理员 |
| app_user | 普通业务用户 |
| auditor | 审计查看 |

### 6.5 参考实现模板

```java
@Component
public class ArchiveExternalAuthAdapter implements ExternalAuthAdapter {

    private final ArchiveAuthClient authClient; // 自行封装 HTTP 调用

    @Override
    public ExternalTokenIntrospection introspect(String accessToken) {
        // 1. 调档案馆 auth-service
        // 2. 将响应转换为 ExternalTokenIntrospection
        // 3. active=false 或 userId 为空时仍返回对象，由 Resolver 判 401
    }

    @Override
    public Optional<ExternalUserProfile> getUser(String userId) {
        // 调档案馆用户详情接口，映射为 ExternalUserProfile
    }
}
```

```java
@Component
public class ArchiveExternalOrganizationAdapter implements ExternalOrganizationAdapter {

    private final ArchiveOrganizationClient orgClient;

    @Override
    public ExternalOrganizationProfile loadOrganizationContext(String userId) {
        // 调档案馆 organization-service，映射单位/部门/组织角色
    }
}
```

### 6.6 Feign 占位实现（仅当接口契约一致时）

若档案馆接口 **恰好** 与 AGI 预留 Feign 一致，可临时启用：

```yaml
agi:
  identity:
    adapter:
      auth: feign
      organization: feign
```

| Feign 客户端 | 方法 | 预期路径 |
|--------------|------|----------|
| AuthenticationClient | introspect | `POST /api/auth/tokens/introspect` |
| AuthenticationClient | getUser | `GET /api/auth/users/{userId}` |
| OrganizationClient | getUser | `GET /api/organization/users/{userId}` |
| OrganizationClient | listUserDepartments | `GET /api/organization/users/{userId}/departments` |

**接口不一致时必须写自定义 Adapter，不要改档案馆服务。**

源码位置：

- `com.mw.ai.agi.integration.auth.AuthenticationClient`
- `com.mw.ai.agi.integration.organization.OrganizationClient`
- `com.mw.ai.agi.auth.identity.adapter.feign.FeignExternalAuthAdapter`
- `com.mw.ai.agi.auth.identity.adapter.feign.FeignExternalOrganizationAdapter`

---

## 7. remote 模式下的行为差异

### 7.1 禁用本地组织用户维护

`disable-local-org-admin=true` 时，以下写操作返回 **403 `IDENTITY_MODE_REMOTE`**：

| 方法 | 路径 |
|------|------|
| POST/PUT/DELETE | `/api/auth/admin/tenants` |
| POST/PUT/DELETE | `/api/auth/admin/organizations` |
| POST/PUT/DELETE | `/api/auth/admin/users` |
| POST/PUT/DELETE | `/api/auth/admin/roles` |

GET 仍可读本地数据（历史兼容）；**组织用户的主维护入口在档案馆系统**。

不受影响：

- `/api/auth/admin/integration-apps` 集成应用管理
- `/api/open/**` 开放 API
- AI 业务 API（知识库、智能体、工作流等）

### 7.2 租户策略

remote 模式固定 `tenantId = tenant_default`。JWT 与 AI 资产均使用该租户，组织隔离靠外部 `unitId` / `departmentId`。

### 7.3 用户类型

| userType | 来源 | 密码登录 |
|----------|------|----------|
| LOCAL | AGI 本地表 | remote 下仅 break-glass |
| EXTERNAL | SSO 换票 | 不允许 |

---

## 8. 档案馆 BFF 对接清单

### 8.1 配置态（iframe）

| 步骤 | BFF 动作 |
|------|----------|
| 1 | 校验档案馆用户 session / token |
| 2 | 确认用户具备 AI 配置权限（档案馆 RBAC） |
| 3 | 取 auth-service accessToken |
| 4 | `POST {agi}/api/auth/sso/exchange` |
| 5 | 将 AGI tokens 返回给前端（或写入 embed session） |
| 6 | 前端 iframe 加载 AGI，`?embed=1` |

### 8.2 运行态（Open API）

| 步骤 | BFF 动作 |
|------|----------|
| 1 | 校验档案馆用户身份 |
| 2 | 按档案馆 RBAC 判断可调哪些智能体/功能 |
| 3 | 持 AGI API Key 调 `/api/open/*` |
| 4 | 透传 `X-AGI-User-Id`、`X-AGI-Unit-Id`、`X-AGI-Role-Ids` 等 |

### 8.3 安全要求

- API Key **仅存 BFF 服务端**，不下发浏览器
- AGI 管理台 URL 建议内网 / VPN
- 外部 token 不要写入前端日志
- break-glass 账号不对档案馆业务人员分发

---

## 9. 错误码

| code | HTTP | 场景 | 处理建议 |
|------|------|------|----------|
| IDENTITY_MODE_LOCAL | 400 | local 模式调用 sso/exchange | 检查 `agi.identity.mode` |
| IDENTITY_MODE_REMOTE | 403 | remote 下维护组织用户 | 改在档案馆系统操作 |
| IDENTITY_ADAPTER_NOT_CONFIGURED | 503 | 未实现 Adapter | 注册 Archive*Adapter Bean |
| AUTH_TOKEN_INVALID | 401 | 外部 token 无效/过期 | BFF 重新取 token |
| AUTH_USER_DISABLED | 403 | 外部用户已禁用 | 档案馆侧处理 |
| AUTH_LOGIN_FAILED | 401 | remote 下非 LOCAL 用户密码登录 | 走 SSO |
| API_AUTH_REQUIRED | 401 | 管理 API 缺 Bearer | 检查 token |
| OPEN_API_AUTH_REQUIRED | 401 | Open API 缺 API Key | 检查集成应用配置 |

---

## 10. 联调步骤

### 10.1 AGI 侧

```bash
# 1. 启用 remote profile
SPRING_PROFILES_ACTIVE=archive-integrated

# 2. 实现并注册 Adapter
# 3. 启动服务

# 4. 确认模式
curl http://localhost:8080/api/auth/identity/mode

# 5. SSO 换票（用档案馆真实 token）
curl -X POST http://localhost:8080/api/auth/sso/exchange \
  -H "Content-Type: application/json" \
  -d '{"externalToken":"<token>"}'

# 6. 用返回的 accessToken 调管理 API
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

### 10.2 单元测试参考

- `ExternalIdentityResolverTest`：Mock Adapter 验证角色映射
- `SsoAuthServiceTest`：验证 remote/local 模式分支

路径：`server/src/test/java/com/mw/ai/agi/auth/identity/`

### 10.3 验收清单

- [ ] `agi.identity.mode=remote` 启动无报错
- [ ] 未配 Adapter 时 sso/exchange 返回 503 `IDENTITY_ADAPTER_NOT_CONFIGURED`
- [ ] 配置 Adapter 后 sso/exchange 返回 200 + JWT
- [ ] JWT 中 `tenantId=tenant_default`，`userType=EXTERNAL`
- [ ] `role-mapping` 生效（外部 `archive_admin` → `asset_manager`）
- [ ] `unitId` / `departmentIds` 写入 JWT，工作流可引用
- [ ] 组织用户写接口返回 403
- [ ] break-glass 本地账号可登录
- [ ] Open API 透传身份正常
- [ ] iframe 配置全流程可用

---

## 11. 常见问题

**Q：档案馆接口与 Feign 占位不一致怎么办？**  
A：写 `ArchiveExternalAuthAdapter` / `ArchiveExternalOrganizationAdapter`，在 Adapter 内做 HTTP 调用和字段映射。

**Q：组织用户还要在 AGI 管理台维护吗？**  
A：不要。remote 模式下组织用户角色以档案馆微服务为准，AGI 侧写接口已禁用。

**Q：SSO 换票和 Open API 都要接吗？**  
A：配置 iframe 需要 SSO；终端 AI 调用需要 Open API。通常两条都要。

**Q：Adapter 未实现时能启动吗？**  
A：能。但 `sso/exchange` 会返回 503，直到注册自定义 Adapter。

**Q：多个档案馆实例怎么隔离？**  
A：当前 remote 模式单实例单租户（`tenant_default`）。多实例部署多套 AGI。

---

## 12. 相关源码索引

| 模块 | 路径 |
|------|------|
| 身份模式配置 | `auth/identity/IdentityProperties.java` |
| SSO 换票 | `auth/identity/SsoAuthService.java` |
| 身份解析 | `auth/identity/ExternalIdentityResolver.java` |
| Auth 接口 | `auth/api/AuthController.java` |
| Adapter 接口 | `auth/identity/adapter/External*.java` |
| 组织用户写保护 | `auth/identity/RemoteIdentityAdminFilter.java` |
| Open API 请求头 | `auth/service/OpenApiAuthHeaders.java` |
| 档案馆 Profile | `resources/application-archive-integrated.yml` |

---

## 13. 文档修订

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-06-18 | 首版：remote 模式、Adapter 规范、双通道接入、联调清单 |
