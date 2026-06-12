# AGI 平台 Headless 集成方案

| 项目 | 说明 |
|------|------|
| 文档版本 | v1.0 |
| 日期 | 2026-06-11 |
| 集成方 | 第三方数字档案馆系统 |
| 集成模式 | AGI 降级为纯后端服务（Headless），管理页面在档案馆侧自建 |

---

## 1. 背景与问题

### 1.1 约束条件

| 约束 | 说明 |
|------|------|
| 不能有独立维护界面 | AGI 平台不能再有对外的管理台，所有管理入口必须在数字档案馆内 |
| 安检流程繁琐 | 独立部署一套完整 AGI 平台（含前端）需要过安检和代码审查 |
| 需求范围有限 | 数字档案馆仅需知识库 + 大模型 + 智能体 + 工作流能力，不需要完整 AGI 平台 |

### 1.2 与 iframe 方案的差异

本文档描述的是**比 iframe 嵌入更深度的集成方式**：AGI 平台降级为纯后端服务（Headless），管理页面在档案馆前端原生开发。

| 对比维度 | iframe 嵌入方案 | Headless 方案（本文档） |
|----------|:--------------:|:----------------------:|
| 管理页面位置 | AGI 前端，iframe 嵌到档案馆 | 档案馆前端原生页面 |
| 管理 API 调用方式 | AGI 前端直接调 `/api/*` | 档案馆后端通过 Feign client 代理调 `/api/*` |
| 流程设计器 | iframe 整页 | `<ai-workflow-designer>` Web Component |
| 用户体验 | 页面加载慢、样式割裂、跨域通信复杂 | 原生页面体验、统一风格 |
| 安检范围 | AGI 前端 + 后端都需要过审 | 仅 AGI 后端（headless jar），前端在档案馆体系内 |
| 开发工作量 | 低（前端已有） | 中（需自建管理页面） |

**两种方案可共存**：核心管理页面走 Headless，低频系统管理页面走 iframe。

---

## 2. 总体架构

```
数字档案馆系统（唯一用户入口）
┌─────────────────────────────────────────────────────────────┐
│  档案馆前端                                                   │
│  ├── 知识库管理页（自建 React/Vue 页面）                        │
│  ├── 大模型配置页（自建页面）                                   │
│  ├── 智能体管理页（自建页面）                                   │
│  ├── Prompt 模板管理（自建页面）                                │
│  ├── 流程编排器 ← <ai-workflow-designer> Web Component        │
│  ├── 对话测试页（自建页面，调运行时 API）                        │
│  └── 系统管理页 ← iframe 嵌入（低频，不值得重建）               │
│                                                             │
│  档案馆后端（BFF）                                            │
│  ├── ArchiveAgiAdminService → AgiAdmin*Client (Feign)        │
│  ├── ArchiveAgiRuntimeService → AgiOpen*Client (Feign, 已有) │
│  └── 权限校验：档案馆 RBAC + 数据范围                          │
│                                                             │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP (内网)
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  AGI Server（纯后端，无前端界面）                               │
│  java -jar aiworkflow-server.jar --server.port=18080         │
│                                                             │
│  ├── /api/*          管理 API（知识库 CRUD、模型 CRUD 等）     │
│  ├── /api/open/*     开放 API（对话、检索、工作流运行）         │
│  ├── /api/auth/*     认证 API（登录、代登录）                  │
│  └── 鉴权：JWT / API Key + AppCode + 白名单                   │
│                                                             │
│  依赖：PostgreSQL + Elasticsearch + Ollama/LLM API           │
└─────────────────────────────────────────────────────────────┘
```

### 2.1 关键原则

1. **AGI 不暴露外网端口**：仅被档案馆 BFF 内网调用，不对公网开放
2. **API Key 不下浏览器**：仅存档案馆后端，前端不可见
3. **权限在档案馆**：用户能看/能调哪些资产，由档案馆 RBAC 控制
4. **AGI 仅做能力层**：提供 AI 资产托管、编排、推理，不做业务权限

---

## 3. 模块集成方案

### 3.1 已有能力（不需额外开发）

运行时 OpenAPI 已经具备，Feign client 可用：

| Feign Client | 能力 | 状态 |
|-------------|------|:----:|
| `AgiOpenBotClient` | 智能体列表/详情/运行/对话 | ✅ |
| `AgiOpenKnowledgeBaseClient` | 知识库列表/详情/检索 | ✅ |
| `AgiOpenWorkflowClient` | 工作流列表/详情/运行 | ✅ |
| `AgiOpenModelProviderClient` | 大模型列表/详情 | ✅ |
| `AgiOpenIdentityClient` | 身份解析 | ✅ |

### 3.2 需要补充：管理 API 的 Feign Client

AGI Server 的 `/api/*` 管理接口后端已全部实现（参看附录 A），只需在 `aiworkflow-open-api-client` 模块中新增对应的 Feign 接口声明。

#### 新增 Admin Feign Client 清单

| Client | 覆盖的管理 API | 优先级 |
|--------|--------------|:------:|
| `AgiAdminKnowledgeBaseClient` | 知识库 CRUD、文档上传/删除/重解析、向量库配置 | P0 |
| `AgiAdminBotClient` | 智能体 CRUD、对话会话/消息查询 | P0 |
| `AgiAdminWorkflowClient` | 工作流 CRUD、草稿更新、发布/归档 | P0 |
| `AgiAdminModelProviderClient` | 大模型 Provider CRUD | P1 |
| `AgiAdminPromptClient` | Prompt 模板 CRUD | P1 |

#### Feign Client 示例（知识库管理）

```java
// client/.../feign/AgiAdminKnowledgeBaseClient.java
@FeignClient(
    contextId = "agiAdminKnowledgeBaseClient",
    name = "${agi.openapi.service-name:aiworkflow-server}",
    url = "${agi.openapi.base-url:}"
)
public interface AgiAdminKnowledgeBaseClient {

    @GetMapping("/api/knowledge-bases")
    ApiResponse<PageResponse<KnowledgeBaseView>> list(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    );

    @PostMapping("/api/knowledge-bases")
    ApiResponse<KnowledgeBaseView> create(@RequestBody CreateKnowledgeBaseRequest request);

    @GetMapping("/api/knowledge-bases/{id}")
    ApiResponse<KnowledgeBaseView> get(@PathVariable("id") String id);

    @PutMapping("/api/knowledge-bases/{id}")
    ApiResponse<KnowledgeBaseView> update(
        @PathVariable("id") String id,
        @RequestBody UpdateKnowledgeBaseRequest request
    );

    @DeleteMapping("/api/knowledge-bases/{id}")
    ApiResponse<Void> delete(@PathVariable("id") String id);

    // 文档管理
    @GetMapping("/api/knowledge-bases/{id}/documents")
    ApiResponse<PageResponse<DocumentView>> listDocuments(
        @PathVariable("id") String kbId,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    );

    @PostMapping(value = "/api/knowledge-bases/{id}/documents/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<DocumentView> uploadDocument(
        @PathVariable("id") String kbId,
        @RequestPart("file") MultipartFile file
    );

    @DeleteMapping("/api/knowledge-bases/{kbId}/documents/{docId}")
    ApiResponse<Void> deleteDocument(
        @PathVariable("kbId") String kbId,
        @PathVariable("docId") String docId
    );

    // 检索预览
    @PostMapping("/api/knowledge-bases/{id}/search")
    ApiResponse<List<KnowledgeSearchResult>> search(
        @PathVariable("id") String kbId,
        @RequestBody SearchRequest request
    );
}
```

#### Admin 与 Open 的差异

| 维度 | Open API (`/api/open/*`) | Admin API (`/api/*`) |
|------|--------------------------|----------------------|
| 鉴权方式 | API Key + AppCode | JWT (Bearer Token) |
| 用途 | 运行时调用（对话、检索、运行） | 管理配置（CRUD） |
| 调用方 | 档案馆 BFF | 档案馆 BFF |
| Token 来源 | 配置注入 `agi.openapi.api-key` | 服务账号代登录获取 |
| 暴露范围 | 白名单内的资产 | 租户内全部管理权 |

#### 档案馆 BFF 封装示例

```java
@Service
@RequiredArgsConstructor
public class ArchiveAgiAdminService {

    private final AgiAdminKnowledgeBaseClient adminKbClient;
    private final AgiAdminBotClient adminBotClient;
    private final AgiAdminWorkflowClient adminWorkflowClient;
    private final AgiAuthService agiAuthService;  // JWT 管理

    @Cacheable(value = "agi:kb:list", key = "#user.tenantId")
    public List<KnowledgeBaseView> listKnowledgeBases(ArchiveUser user) {
        assertPermission(user, "ai:knowledge:config");
        return AgiOpenApiSupport.requireData(
            adminKbClient.list(1, 100)
        ).items();
    }

    public KnowledgeBaseView createKnowledgeBase(CreateKnowledgeBaseRequest req, ArchiveUser user) {
        assertPermission(user, "ai:knowledge:config");
        return AgiOpenApiSupport.requireData(
            adminKbClient.create(req)
        );
    }

    // Admin API 的 RequestInterceptor 需要注入 JWT
    // 通过 AgiOpenApiRequestContextHolder 传递
}
```

> **注意**：Admin API 和 Open API 使用不同的鉴权机制（JWT vs API Key），需要在 Feign 配置中区分 `RequestInterceptor`，或通过 `AgiOpenApiRequestContext` 扩展字段携带 JWT。

---

### 3.3 流程编排器：复用 Web Component

项目已有的 `<ai-workflow-designer>` Web Component（`packages/workflow-designer-wc`）：

- **框架无关**：可在任何前端框架（React / Vue / jQuery）中使用
- **Shadow DOM 隔离**：样式不污染也不被污染
- **属性/事件接口**：`value` 属性读写工作流定义，`workflow-change` 事件监听变更

#### 集成方式

**步骤 1**：构建 Web Component 单文件

```bash
cd web
pnpm --filter @aiworkflow/workflow-designer-wc build
# 产出 dist/ai-workflow-designer-wc.js
```

**步骤 2**：在档案馆前端页面引入

```html
<script src="/assets/ai-workflow-designer-wc.js"></script>

<!-- 直接使用 -->
<ai-workflow-designer id="wf-designer"></ai-workflow-designer>
```

**步骤 3**：在页面脚本中交互

```javascript
const designer = document.getElementById('wf-designer');

// 加载工作流定义
async function loadWorkflow(workflowId) {
  const resp = await fetch(`/archive-api/agi/workflows/${workflowId}`);
  const data = await resp.json();
  designer.value = data.definition; // 注入 DAG 定义
}

// 保存
async function saveWorkflow(workflowId) {
  const definition = designer.value; // 读取当前 DAG 定义
  await fetch(`/archive-api/agi/workflows/${workflowId}/draft`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ definition })
  });
}

// 监听实时变更
designer.addEventListener('workflow-change', (e) => {
  console.log('当前 DAG:', e.detail);
  markDirty();
});
```

#### Web Component 当前能力边界

当前 `<ai-workflow-designer>` 版本为**只读展示**（渲染节点 + 边 + 布局），不含交互式拖拽编辑。如需完整交互编辑能力，有两种路径：

| 方案 | 做法 | 适用 |
|------|------|------|
| A | 使用 `@aiworkflow/workflow-designer-react`，档案馆用 React 时直接 import | 档案馆前端为 React |
| B | 增强 `workflow-designer-wc`，加入拖拽/连线/属性编辑 | 档案馆前端为非 React |
| C | iframe 嵌入现成设计器页面（兜底方案） | 快速上线，后续迭代迁移 |

建议 P0 阶段用方案 C（iframe 嵌入 `/workflows/:id/designer`），P1 阶段根据档案馆前端技术栈选择方案 A 或 B。

---

### 3.4 系统管理页面：iframe 嵌入（低频场景）

租户管理、菜单管理、数据字典、日志查询等系统管理类页面，使用频率极低，不值得在档案馆侧重建。

| 页面 | AGI 路由 | 嵌入建议 |
|------|---------|:--------:|
| 第三方应用管理 | `/system/integration-apps` | iframe |
| 菜单管理 | `/system/menus` | iframe |
| 数据字典 | `/system/dictionary` | iframe |
| 日志查询 | `/system/logs` | iframe |
| 资产授权 | `/system/asset-grants` | ❌ 不使用 |
| 组织用户 | `/system/identity` | ❌ 不使用 |

iframe 嵌入方式参考 `docs/integration/digital-archive-integration-design.md` 第 6 章。

---

## 4. 部署方案

### 4.1 网络拓扑

```
                    ┌──────────┐
                    │  浏览器    │
                    └────┬─────┘
                         │ HTTPS
                    ┌────▼─────┐
                    │  Nginx /  │
                    │  Gateway  │
                    └────┬─────┘
                         │
              ┌──────────┴──────────┐
              │                     │
         ┌────▼─────┐         ┌────▼─────────────────┐
         │ 档案馆前端 │         │ 档案馆后端 (BFF)       │
         │ (SPA)    │         │ 端口 8080            │
         └──────────┘         │                      │
                              │ 权限校验 → Feign →    │──┐
                              └──────────────────────┘  │
                                                        │ 内网 HTTP
                              ┌──────────────────────┐  │
                              │ AGI Server (Headless) │◄─┘
                              │ 端口 18080            │
                              │                      │
                              │ 不暴露外网端口         │
                              │ 部署于同一内网          │
                              └──────────┬───────────┘
                                         │
                              ┌──────────┴───────────┐
                              │ PostgreSQL + ES       │
                              │ + Ollama / LLM API    │
                              └──────────────────────┘
```

### 4.2 启动命令

```bash
# AGI Server（纯后端，headless 模式）
java -jar aiworkflow-server.jar \
  --server.port=18080 \
  --spring.profiles.active=default \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/aiworkflow \
  --spring.datasource.username=aiworkflow \
  --spring.datasource.password=${AGI_DB_PASSWORD} \
  --agi.openapi.cors.allowed-origins=${ARCHIVE_DOMAIN}
```

### 4.3 依赖关系

| 组件 | 依赖 | 说明 |
|------|------|------|
| 档案馆后端 | `aiworkflow-open-api-client` (Feign SDK) | 含 Admin + Open 全部 Feign client |
| AGI Server | PostgreSQL | 业务数据 + 用户/租户/应用 |
| AGI Server | Elasticsearch | 知识库向量存储与检索 |
| AGI Server | Ollama / 第三方 LLM API | Embedding + Chat 推理 |

---

## 5. 鉴权方案

### 5.1 双通道鉴权

```
                    ┌──────────────────┐
                    │   档案馆 BFF       │
                    └───┬──────────┬───┘
                        │          │
              ┌─────────▼──┐  ┌───▼──────────┐
              │ Admin 通道  │  │ Runtime 通道  │
              │ /api/*     │  │ /api/open/*  │
              ├────────────┤  ├──────────────┤
              │ Auth: JWT  │  │ Auth: API Key│
              │ (服务账号)   │  │ + AppCode    │
              │            │  │ + 白名单      │
              │ 用途: CRUD  │  │ 用途: 运行    │
              └────────────┘  └──────────────┘
```

### 5.2 Admin Token 管理（JWT）

Admin API 的 JWT 来源于 **AGI 自身的认证系统**，通过"服务账号代登录"机制获取，流程如下：

1. **创建服务账号**：在 AGI 的 `tenant_archive` 租户下预先创建服务账号（如 `svc_archive_admin`），设置密码（对应 Phase 1 的"创建 iframe 代登录服务账号"步骤）。
2. **BFF 代登录换 JWT**：档案馆后端 `AgiAuthService` 拿服务账号的用户名/密码，调 AGI 的 `/api/auth/*` 登录接口，换取 JWT `accessToken`。
3. **缓存复用**：获取到的 JWT 缓存在 BFF 内存中，每次使用前检查是否快过期（提前 60 秒），快过期时自动重新代登录刷新。
4. **注入 Feign 请求**：JWT 通过 `RequestInterceptor` 以 `Bearer <token>` 形式注入到 Admin Feign Client 的请求头中。

> **安全保证**：JWT 全程只存在于档案馆 BFF 内存中，不下发到浏览器，不暴露给前端。

档案馆 BFF 内部维护一个服务账号的 JWT，用于调用管理 API：

```java
@Component
public class AgiAuthService {

    private volatile String cachedToken;
    private volatile Instant expiresAt;

    public String getAdminToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return cachedToken;
        }
        // 代登录刷新 token
        var resp = authClient.login(new LoginRequest(
            "svc_archive_admin", "***", "tenant_archive"
        ));
        var data = AgiOpenApiSupport.requireData(resp);
        this.cachedToken = data.accessToken();
        this.expiresAt = Instant.now().plusSeconds(data.expiresIn());
        return cachedToken;
    }
}
```

### 5.3 Runtime Token 管理（API Key）

运行时 API 使用配置注入的 `appCode` + `apiKey`，已有的 `AgiOpenApiClientAutoConfiguration` 会自动注入到 Open Feign client 的请求头中。

---

## 6. 前端页面建设清单

### 6.1 需要在档案馆前端自建的页面

| 页面 | 调用 API | 复杂度 | 优先级 |
|------|---------|:------:|:------:|
| 知识库列表 | `AgiAdminKnowledgeBaseClient.list` | 表格 + 搜索 + 删除 | P0 |
| 知识库创建/编辑 | `AgiAdminKnowledgeBaseClient.create/update` | 表单 | P0 |
| 文档列表 | `AgiAdminKnowledgeBaseClient.listDocuments` | 表格 + 上传 + 删除 | P0 |
| 文档上传 | `AgiAdminKnowledgeBaseClient.uploadDocument` | 文件上传 | P0 |
| 智能体列表 | `AgiAdminBotClient.list` | 表格 | P0 |
| 智能体创建/编辑 | `AgiAdminBotClient.create/update` | 表单（关联知识库/模型/工作流） | P0 |
| 大模型配置列表 | `AgiAdminModelProviderClient.list` | 表格 | P1 |
| 大模型配置创建/编辑 | `AgiAdminModelProviderClient.create/update` | 表单 | P1 |
| 工作流列表 | `AgiAdminWorkflowClient.list` | 卡片/表格 | P0 |
| 工作流设计器 | Web Component / iframe | 嵌入 | P0 |
| Prompt 模板列表 | `AgiAdminPromptClient.list` | 表格 | P2 |
| Prompt 模板创建/编辑 | `AgiAdminPromptClient.create/update` | 表单 | P2 |

### 6.2 已有的页面组件（可参考/复用）

AGI 管理台现有页面位于 `web/apps/admin/src/pages/`，可作为档案馆侧页面开发的参考实现：

```
web/apps/admin/src/pages/
├── knowledge/
│   ├── KnowledgeBasesPage.tsx          # 知识库列表
│   ├── KnowledgeDocumentsPage.tsx      # 文档列表
│   ├── KnowledgeDocumentCreatePage.tsx # 创建文档
│   └── KnowledgeDocumentEditPage.tsx   # 编辑文档
├── bots/
│   └── BotsPage.tsx                    # 智能体管理
├── models/
│   └── ModelProvidersPage.tsx          # 大模型配置
├── prompts/
│   └── PromptTemplatesPage.tsx         # Prompt 模板
├── workflows/
│   ├── WorkflowCardsPage.tsx           # 工作流卡片列表
│   ├── WorkflowDesignerPage.tsx        # 流程设计器
│   └── WorkflowRunsPage.tsx            # 运行历史
└── ...
```

---

## 7. 实施步骤

### Phase 1：基础环境（1 天）

- [ ] 部署 AGI Server（headless 模式，端口 18080）
- [ ] 创建档案馆租户 `tenant_archive`
- [ ] 注册第三方应用 `digital-archive`，生成 API Key
- [ ] 配置资产白名单（试点：1 知识库 + 1 智能体 + 1 工作流）
- [ ] 创建 iframe 代登录服务账号
- [ ] 将 `aiworkflow-open-api-client` 发布到内部 Maven 仓库（或本地 install）
- [ ] 档案馆后端引入 SDK 依赖，配置 `agi.openapi.*` 参数
- [ ] 验证运行时 OpenAPI 调用链路通畅

### Phase 2：管理 API Feign Client（2-3 天）

- [ ] 新建 `AgiAdminKnowledgeBaseClient` + 对应 model 类
- [ ] 新建 `AgiAdminBotClient` + 对应 model 类
- [ ] 新建 `AgiAdminWorkflowClient` + 对应 model 类
- [ ] 新建 `AgiAdminModelProviderClient` + 对应 model 类
- [ ] 新建 `AgiAdminPromptClient` + 对应 model 类
- [ ] 实现 Admin 通道 JWT 注入（区分 Admin 和 Open 的 `RequestInterceptor`）
- [ ] 编写集成测试验证管理 API 调用

### Phase 3：档案馆前端页面（5-8 天）

- [ ] 知识库管理（列表 + 创建/编辑 + 文档管理 + 上传）
- [ ] 智能体管理（列表 + 创建/编辑）
- [ ] 大模型配置（列表 + 创建/编辑）
- [ ] 工作流列表 + 设计器嵌入（iframe 兜底，Web Component 后续）
- [ ] Prompt 模板管理
- [ ] 档案馆菜单/权限接入（`ai:*:config` 权限点）

### Phase 4：优化与上线（2-3 天）

- [ ] 工作流设计器 Web Component 本地化部署
- [ ] 系统管理页面 iframe 嵌入（菜单/字典/日志）
- [ ] 审计日志联调（User-Id / Unit-Id 贯穿）
- [ ] 性能测试与密钥轮换演练

---

## 8. 与 iframe 方案的对比总结

| 维度 | iframe 嵌入（现有方案） | Headless（本文方案） |
|------|:----------------------:|:--------------------:|
| 管理页面开发 | 无（复用 AGI 页面） | 需自建管理页面 |
| 前端技术栈耦合 | 无（iframe 隔离） | 需与档案馆前端统一 |
| 用户体验 | iframe 加载慢、样式割裂 | 原生页面、统一风格 |
| 维护成本 | 低（随 AGI 升级） | 中（页面需同步维护） |
| 安检范围 | AGI 前端 + 后端 | 仅 AGI 后端 jar |
| 权限控制粒度 | 粗（iframe 整体可见/不可见） | 细（页面级/按钮级/数据级） |
| 适合场景 | 快速接入、全功能可用 | 深度定制、安检严格 |

**建议策略**：核心高频页面用 Headless 方案（知识库、智能体、工作流），低频系统页面用 iframe 兜底，流程设计器先用 iframe 后续迁移到 Web Component。

---

## 附录 A：AGI 后端 Controller 清单

这些管理 API 后端已实现，Feign client 只需声明接口即可调用：

| Controller | 路径前缀 | 主要能力 |
|-----------|---------|---------|
| `KnowledgeBaseController` | `/api/knowledge-bases` | 知识库 CRUD、文档上传/删除/分块/重解析/回填 |
| `VectorStoreConfigController` | `/api/vector-store-configs` | 向量库配置 CRUD |
| `BotController` | `/api/bots` | 智能体 CRUD、运行、对话、会话/消息 |
| `WorkflowController` | `/api/workflows` | 工作流 CRUD、草稿更新、发布、归档 |
| `WorkflowRunController` | `/api` | 工作流运行、执行查询 |
| `ModelProviderController` | `/api/model-providers` | 大模型 Provider CRUD |
| `PromptTemplateController` | `/api/prompts` | Prompt 模板 CRUD |
| `AuthAdminController` | `/api/auth/admin/*` | 租户/用户/角色 CRUD |
| `AuthController` | `/api/auth/*` | 登录/登出/Token 刷新 |
| `IntegrationAppController` | `/api/integration-apps` | 第三方应用管理 |
| `AssetGrantController` | `/api/asset-grants` | 资产授权（独立模式用） |
| `SystemMenuController` | `/api/system/menus` | 菜单管理 |
| `DataDictionaryController` | `/api/system/dictionary` | 数据字典 |
| `AuthAuditLogController` | `/api/system/audit-logs` | 审计日志 |

## 附录 B：Client SDK 模块需新增的文件

```
client/aiworkflow-open-api-client/src/main/java/com/mw/ai/agi/openapi/client/
├── feign/
│   ├── AgiOpenBotClient.java              ✅ 已有
│   ├── AgiOpenKnowledgeBaseClient.java    ✅ 已有
│   ├── AgiOpenWorkflowClient.java         ✅ 已有
│   ├── AgiOpenModelProviderClient.java    ✅ 已有
│   ├── AgiOpenIdentityClient.java         ✅ 已有
│   ├── AgiAdminKnowledgeBaseClient.java   🆕 需新增
│   ├── AgiAdminBotClient.java            🆕 需新增
│   ├── AgiAdminWorkflowClient.java       🆕 需新增
│   ├── AgiAdminModelProviderClient.java  🆕 需新增
│   └── AgiAdminPromptClient.java         🆕 需新增
├── model/
│   ├── bot/          ✅ 已有
│   ├── knowledge/    ✅ 已有
│   ├── workflow/     ✅ 已有
│   ├── knowledge/    🆕 需新增 admin 相关 DTO
│   ├── bot/          🆕 需新增 admin 相关 DTO
│   └── ...           🆕 按需补充
├── autoconfigure/
│   └── AgiOpenApiClientAutoConfiguration.java   🆕 需扩展 admin 通道 JWT 注入
└── config/
    └── AgiAdminFeignConfig.java          🆕 需新增（Admin 通道独立 Feign 配置）
```

## 附录 C：修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-06-11 | 初稿，Headless 集成方案整体设计 |
