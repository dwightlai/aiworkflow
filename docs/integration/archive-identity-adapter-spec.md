# 数字档案馆身份 Adapter 接口速查

| 项目 | 说明 |
|------|------|
| 版本 | v1.0 |
| 状态 | 可实施 |
| 完整开发指南 | [远程身份接入开发指南](./remote-identity-integration-guide.md) |
| 关联 | `agi-hybrid-identity-design.md` |

> 面向开发的完整接入说明（架构、SSO、Open API、联调、验收）请阅读 **[远程身份接入开发指南](./remote-identity-integration-guide.md)**。本文档仅保留 Adapter 接口与字段速查。

---

## 1. 目标

档案馆 **auth-service**、**organization-service** 接口不可改，由 AGI 通过 Adapter 接入。

接入后：

- 认证主数据 → auth-service
- 组织/用户/角色主数据 → organization-service
- AGI 签发平台 JWT，供管理端 `/api/*` 使用
- Open API 机制不变（API Key + 身份透传）

---

## 2. 模式开关

```yaml
agi:
  identity:
    mode: remote          # local | remote
    fixed-tenant-id: tenant_default
    role-mapping:
      archive_admin: asset_manager
      archive_config: asset_manager
      archive_user: app_user
    remote:
      allow-break-glass-login: true
      disable-local-org-admin: true
```

激活档案馆 profile：

```bash
SPRING_PROFILES_ACTIVE=archive-integrated
```

---

## 3. AGI 提供的接入点

### 3.1 SSO 换票（管理端 / iframe）

```http
POST /api/auth/sso/exchange
Content-Type: application/json

{
  "externalToken": "<档案馆 auth-service 颁发的 access token>"
}
```

成功响应与本地登录相同：`accessToken`、`refreshToken`、`user`。

### 3.2 查询当前身份模式

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

### 3.3 break-glass 本地登录

`remote` 模式下，仅 `userType=LOCAL` 的运维账号可走 `POST /api/auth/login`。

---

## 4. 需要实现的 Adapter

在 AGI 工程中新增两个 Spring Bean，**不要改档案馆微服务**。

### 4.1 ExternalAuthAdapter

包路径：`com.mw.ai.agi.auth.identity.adapter`

```java
public interface ExternalAuthAdapter {
    ExternalTokenIntrospection introspect(String accessToken);
    Optional<ExternalUserProfile> getUser(String userId);
}
```

#### ExternalTokenIntrospection

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| active | boolean | 是 | token 是否有效 |
| userId | string | 是 | 外部用户 ID |
| username | string | 否 | 登录名 |
| roleIds | string[] | 否 | 外部角色编码 |
| permissions | string[] | 否 | 权限点（可选） |
| expiresAtEpochSeconds | long | 否 | 过期时间 |

#### ExternalUserProfile

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | string | 是 | 用户 ID |
| username | string | 是 | 登录名 |
| displayName | string | 否 | 显示名 |
| enabled | boolean | 是 | 是否启用 |
| roleIds | string[] | 否 | 角色 |

**实现要求：**

1. `introspect` 必须能判断 token 是否有效
2. `getUser` 可选；未实现时 AGI 用 introspect 结果兜底
3. 失败抛 `AuthException` 或让 AGI 包装为 401

### 4.2 ExternalOrganizationAdapter

```java
public interface ExternalOrganizationAdapter {
    ExternalOrganizationProfile loadOrganizationContext(String userId);
}
```

#### ExternalOrganizationProfile

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | string | 是 | 用户 ID |
| organizationIds | string[] | 否 | 所属组织 |
| activeOrganizationId | string | 否 | 当前组织 |
| unitIds | string[] | 否 | 单位 ID 列表 |
| activeUnitId | string | 否 | 当前单位 |
| departmentIds | string[] | 否 | 部门 ID 列表 |
| roleIds | string[] | 否 | 组织侧角色（与 auth 角色合并后映射） |

**实现要求：**

1. 按档案馆 organization-service 实际 API 查询
2. 至少返回 `unitIds` / `activeUnitId` 或 `departmentIds` 之一，供审计和工作流变量使用

---

## 5. 实现步骤（档案馆接口文档到位后）

### Step 1：创建 Adapter 类

```text
server/src/main/java/com/mw/ai/agi/auth/identity/adapter/archive/
  ArchiveExternalAuthAdapter.java
  ArchiveExternalOrganizationAdapter.java
```

```java
@Component
public class ArchiveExternalAuthAdapter implements ExternalAuthAdapter {
    // 调用档案馆 auth-service HTTP API
}
```

注册为 `@Component` 后，默认的 `UnconfiguredExternalAuthAdapter` 自动失效。

### Step 2：联调 SSO 换票

```text
档案馆 BFF
  → 拿用户 accessToken
  → POST AGI /api/auth/sso/exchange
  → 返回 AGI JWT
  → iframe 写入 localStorage(agi_admin_auth)
```

### Step 3：配置角色映射

将档案馆角色编码映射到 AGI 平台角色（如 `asset_manager`、`platform_admin`）。

### Step 4：验证 remote 限制

- `POST/PUT/DELETE /api/auth/admin/users|organizations|roles` → 403 `IDENTITY_MODE_REMOTE`
- `GET` 仍可读（后续可改为代理档案馆只读接口）
- 集成应用管理不受影响

---

## 6. 参考实现（Feign 占位）

若档案馆接口恰好与 AGI 预留 Feign 契约一致，可临时启用：

```yaml
agi:
  identity:
    adapter:
      auth: feign
      organization: feign

third-party:
  auth:
    base-url: http://auth-service:8080
  organization:
    base-url: http://organization-service:8080
```

Feign 契约见：

- `AuthenticationClient` → `POST /api/auth/tokens/introspect`
- `OrganizationClient` → `GET /api/organization/users/{userId}`

**接口不一致时，必须写自定义 Adapter，不要改档案馆服务。**

---

## 7. 运行时身份解析流程

```text
externalToken
  → ExternalAuthAdapter.introspect
  → ExternalAuthAdapter.getUser (optional)
  → ExternalOrganizationAdapter.loadOrganizationContext
  → merge roleIds
  → agi.identity.role-mapping
  → AuthUserPrincipal
  → JwtTokenService.issueAccessToken
```

---

## 8. 错误码

| code | HTTP | 含义 |
|------|------|------|
| IDENTITY_MODE_LOCAL | 400 | local 模式调用 sso/exchange |
| IDENTITY_MODE_REMOTE | 403 | remote 模式禁用本地组织用户维护 |
| IDENTITY_ADAPTER_NOT_CONFIGURED | 503 | 未实现 Adapter |
| AUTH_TOKEN_INVALID | 401 | 外部 token 无效 |
| AUTH_USER_DISABLED | 403 | 外部用户已禁用 |

---

## 9. Open API（不变）

档案馆 BFF 继续用：

```http
X-AGI-App-Code
X-AGI-Api-Key
X-AGI-User-Id
X-AGI-Unit-Id
...
```

SSO 换票解决的是 **管理端 iframe 配置**；终端运行仍推荐 Open API。

---

## 10. 验收清单

- [ ] `agi.identity.mode=remote` 启动成功
- [ ] 自定义 Adapter 注册后，`sso/exchange` 返回 AGI JWT
- [ ] JWT 中 `tenantId=tenant_default`，`userType=EXTERNAL`
- [ ] 角色映射生效
- [ ] 本地组织用户写接口被禁用
- [ ] break-glass 本地账号可登录
- [ ] Open API 回归通过
