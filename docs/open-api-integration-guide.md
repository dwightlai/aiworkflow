# AGI 开放 API 对接指南

供第三方业务系统（如数字档案馆）集成 AGI 平台能力。本文档面向后端开发人员。

| 项目 | 说明 |
|------|------|
| API 版本 | v1 |
| 基础路径 | `/api/open` |
| 机器可读规范 | [docs/openapi/open-api.yaml](./openapi/open-api.yaml) |
| 在线文档 | Admin → 开放 API 文档（Swagger，分组「开放 API（第三方集成）」） |

---

## 1. 接入前准备

### 1.1 获取凭证

在 AGI 管理台 **系统管理 → 集成应用** 中创建第三方应用，获取：

| 凭证 | 请求头 | 说明 |
|------|--------|------|
| App Code | `X-AGI-App-Code` | 应用编码，必填 |
| API Key | `X-AGI-Api-Key` | 应用密钥，必填，**仅保存在调用方后端** |

管理员需为应用配置 **资产授权范围**（智能体、知识库、工作流、大模型等）。未授权的资产不会出现在列表接口中，直接调用会返回 `403 APP_ASSET_SCOPE_DENIED`。

### 1.2 服务地址

```
https://{agi-host}/api/open/...
```

生产环境建议：AGI 仅内网可达，由业务系统 BFF 代理转发，API Key 不下发到浏览器。

---

## 2. 认证与上下文

### 2.1 必填请求头

```http
X-AGI-App-Code: your-app-code
X-AGI-Api-Key: your-api-key
Content-Type: application/json
```

### 2.2 可选上下文头（推荐携带）

用于审计、权限过滤、工作流变量注入：

| 请求头 | 说明 | 格式 |
|--------|------|------|
| `X-AGI-User-Id` | 调用方用户 ID | 字符串 |
| `X-AGI-Unit-Id` | 当前单位 ID | 字符串 |
| `X-AGI-Department-Ids` | 部门 ID 列表 | 逗号分隔 |
| `X-AGI-Role-Ids` | 角色 ID 列表 | 逗号分隔 |

也可在请求体中传 `userId`、`unitId`、`departmentIds`、`roleIds`，与请求头合并，请求体优先。

**会话相关接口**（列出/创建会话、流式对话等）必须提供 `X-AGI-User-Id` 或请求体 `userId`。

### 2.3 身份预检（可选）

```http
POST /api/open/identity/resolve
X-AGI-App-Code: your-app-code
X-AGI-Api-Key: your-api-key
Content-Type: application/json

{
  "userId": "u-001",
  "unitId": "unit-001",
  "departmentIds": ["dept-a", "dept-b"],
  "roleIds": ["role-admin"]
}
```

响应 `data` 为解析后的运行时身份上下文（租户、应用、用户、组织信息），可用于调试凭证与上下文是否正确。

---

## 3. 通用约定

### 3.1 响应信封

除 SSE 流式接口外，均返回 JSON 信封：

```json
{
  "success": true,
  "data": { },
  "error": null
}
```

失败时：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "OPEN_API_AUTH_REQUIRED",
    "message": "X-AGI-App-Code and X-AGI-Api-Key are required.",
    "requestId": "可选",
    "details": {}
  }
}
```

### 3.2 常见错误码

| HTTP | code | 含义 |
|------|------|------|
| 401 | `OPEN_API_AUTH_REQUIRED` | 缺少或无效 App Code / API Key |
| 403 | `APP_ASSET_SCOPE_DENIED` | 应用无权访问该资产 |
| 404 | — | 资产不存在或未启用 |
| 400 | — | 参数错误（如缺少 userId） |

### 3.3 分页结构

列表接口的 `data` 形如：

```json
{
  "items": [ ... ],
  "total": 10
}
```

---

## 4. 推荐对接流程

### 4.1 智能体对话（最常用）

```
1. GET  /api/open/bots                          → 获取可用智能体列表
2. GET  /api/open/bots/{id}                     → 读取开场白、推荐问题
3. POST /api/open/bots/{id}/sessions            → 创建会话（需 userId）
4. POST /api/open/bots/{id}/sessions/{sid}/messages/stream  → SSE 流式对话
   或 POST /api/open/bots/{id}/chat              → 同步多轮对话
5. GET  /api/open/bots/{id}/sessions/{sid}/messages         → 拉取历史消息
```

### 4.2 知识库检索

```
1. GET  /api/open/knowledge-bases
2. POST /api/open/knowledge-bases/{id}/search   → 向量/关键词检索
```

### 4.3 工作流直调

```
1. GET  /api/open/workflows
2. POST /api/open/workflows/{id}/runs
3. GET  /api/open/workflow-runs/{executionId}   → 查询执行结果
```

---

## 5. 接口说明

### 5.1 智能体

#### 列出智能体

```http
GET /api/open/bots
```

`data.items[]` 字段：

| 字段 | 说明 |
|------|------|
| id | 智能体 ID |
| name / description / avatar | 基本信息 |
| workflowId | 默认工作流 |
| modelProviderId | 绑定大模型 |
| knowledgeBaseIds | 关联知识库 |
| openingMessage | 开场白 |
| capabilityHint | 能力说明 |
| suggestedQuestions | 推荐问题 |
| status | `ENABLED` / `DISABLED` |

#### 获取详情

```http
GET /api/open/bots/{id}
```

#### 列出多工作流路由能力

```http
GET /api/open/bots/{id}/capabilities
```

智能体可配置多个工作流能力，平台按用户消息关键词自动路由。

#### 单次运行（无会话）

```http
POST /api/open/bots/{id}/run
```

```json
{
  "message": "请帮我检查这份档案著录是否规范",
  "userId": "u-001",
  "unitId": "unit-001",
  "departmentIds": ["dept-a"],
  "roleIds": ["archivist"],
  "input": {
    "archiveType": "文书",
    "customField": "任意工作流变量"
  }
}
```

`input` 会与平台注入的租户/组织上下文合并，供工作流节点使用（如 `{{tenantId}}`、`{{userId}}`）。

响应 `data` 含 `bot` 与 `execution`（工作流执行详情）。

#### 多轮对话（同步）

```http
POST /api/open/bots/{id}/chat
```

```json
{
  "sessionId": "已有会话ID，首次可省略",
  "message": "继续刚才的话题",
  "userId": "u-001",
  "input": {}
}
```

响应 `data`：

| 字段 | 说明 |
|------|------|
| session | 会话信息 |
| messages | 完整消息列表 |
| reply | 本次助手回复 |
| execution | 工作流执行详情 |

#### 会话管理

```http
GET  /api/open/bots/{id}/sessions
POST /api/open/bots/{id}/sessions
GET  /api/open/bots/{id}/sessions/{sessionId}/messages
```

创建会话：

```json
{
  "title": "档案规范咨询",
  "userId": "u-001"
}
```

#### 流式对话（SSE，推荐）

```http
POST /api/open/bots/{id}/sessions/{sessionId}/messages/stream
Accept: text/event-stream
Content-Type: application/json
```

```json
{
  "message": "这份元数据哪里不符合规范？",
  "userId": "u-001",
  "input": {}
}
```

**SSE 事件类型：**

| event | 说明 | data 示例 |
|-------|------|-----------|
| `message.delta` | LLM 增量文本 | `{"content":"根据"}` |
| `citation.added` | 知识库引用 | `{"documentName":"...","content":"...","score":85}` |
| `tool.completed` | 工具/连接器执行完成 | `{"connectorCode":"...","operationCode":"..."}` |
| `tool.failed` | 工具执行失败 | 同上 |
| `confirm.required` | 需人工确认 | `{"taskId":"...","summary":"..."}` |
| `job.progress` | 长任务进度 | `{"jobId":"...","progress":50}` |
| `job.completed` | 长任务完成 | `{"jobId":"...","outputId":"...","downloadUrl":"..."}` |
| `message.completed` | 完整回复 | `{"content":"...","messageId":"..."}` |
| `error` | 错误 | `{"message":"..."}` |
| `done` | 流结束 | `{}` |

**JavaScript 消费示例：**

```javascript
const res = await fetch(url, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-AGI-App-Code': appCode,
    'X-AGI-Api-Key': apiKey,
    'X-AGI-User-Id': userId,
    'Accept': 'text/event-stream'
  },
  body: JSON.stringify({ message: '你好' })
});

const reader = res.body.getReader();
const decoder = new TextDecoder();
let buffer = '';

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  buffer += decoder.decode(value, { stream: true });
  const blocks = buffer.split('\n\n');
  buffer = blocks.pop() ?? '';
  for (const block of blocks) {
    const eventLine = block.match(/^event: (.+)$/m);
    const dataLine = block.match(/^data: (.+)$/m);
    if (eventLine && dataLine) {
      const event = eventLine[1];
      const data = JSON.parse(dataLine[1]);
      // 按 event 类型处理
    }
  }
}
```

> 流式接口需由后端代理转发 SSE，或前端通过 BFF 获取短期 token；**不要**在浏览器直接暴露 API Key。

---

### 5.2 知识库

#### 列出知识库

```http
GET /api/open/knowledge-bases
```

#### 检索

```http
POST /api/open/knowledge-bases/{id}/search
```

```json
{
  "query": "档案著录规范",
  "topK": 5,
  "userId": "u-001"
}
```

响应 `data[]`：

| 字段 | 说明 |
|------|------|
| id | 片段 ID |
| documentName | 来源文档 |
| content | 片段内容 |
| score | 相关度分数 |

---

### 5.3 工作流

#### 列出已发布工作流

```http
GET /api/open/workflows
```

#### 运行工作流

```http
POST /api/open/workflows/{id}/runs
```

```json
{
  "input": {
    "question": "..."
  },
  "userId": "u-001"
}
```

#### 查询执行

```http
GET /api/open/workflow-runs/{executionId}
```

`data.status`：`RUNNING` / `SUCCEEDED` / `FAILED` / `WAITING_CONFIRM` 等。

---

### 5.4 大模型

仅返回可用 Provider 元信息，**不含 API Key 等敏感配置**。

```http
GET /api/open/model-providers
GET /api/open/model-providers/{id}
```

---

### 5.5 Chat 嵌入票据（可选）

用于在业务系统内嵌 AGI Chat 页面，免重复登录：

```http
POST /api/open/chat/embed-tickets
```

```json
{
  "userId": "u-001",
  "botId": "bot-id",
  "expireSeconds": 300,
  "businessContext": { "source": "archive-system" }
}
```

响应 `data.ticket` 用于跳转 Chat Portal。

---

## 6. Java SDK（Feign Client）

项目提供 Spring Boot 自动配置客户端模块 `aiworkflow-open-api-client`。

**依赖配置（application.yml）：**

```yaml
agi:
  openapi:
    enabled: true
    base-url: http://agi-server:8080
    app-code: your-app-code
    api-key: your-api-key
```

**调用示例：**

```java
@Autowired
private AgiOpenBotClient botClient;

@Autowired
private AgiOpenApiRequestContextHolder contextHolder;

public void chat() {
    contextHolder.setUserId("u-001");
    contextHolder.setUnitId("unit-001");

    var bots = botClient.listBots();
    var result = botClient.chatBot("bot-id", new OpenBotChatRequest(
            null, "你好", null, null, null, null, Map.of()
    ));
}
```

可用 Client：

| Client | 能力 |
|--------|------|
| `AgiOpenBotClient` | 智能体列表、运行、对话、会话 |
| `AgiOpenKnowledgeBaseClient` | 知识库列表、检索 |
| `AgiOpenWorkflowClient` | 工作流列表、运行、查询 |
| `AgiOpenModelProviderClient` | 大模型列表 |
| `AgiOpenIdentityClient` | 身份解析 |

> SSE 流式接口需自行用 `HttpClient` / `WebClient` 消费，Feign Client 暂未封装。

---

## 7. cURL 快速验证

```bash
# 列出智能体
curl -s http://localhost:8080/api/open/bots \
  -H "X-AGI-App-Code: demo-app" \
  -H "X-AGI-Api-Key: demo-key"

# 同步对话
curl -s http://localhost:8080/api/open/bots/{botId}/chat \
  -H "Content-Type: application/json" \
  -H "X-AGI-App-Code: demo-app" \
  -H "X-AGI-Api-Key: demo-key" \
  -H "X-AGI-User-Id: u-001" \
  -d '{"message":"你好"}'

# 流式对话
curl -N http://localhost:8080/api/open/bots/{botId}/sessions/{sessionId}/messages/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -H "X-AGI-App-Code: demo-app" \
  -H "X-AGI-Api-Key: demo-key" \
  -H "X-AGI-User-Id: u-001" \
  -d '{"message":"你好"}'
```

---

## 8. 安全建议

1. **API Key 仅存后端**，通过 BFF 代理所有开放 API 调用
2. 携带 `X-AGI-User-Id` 便于审计追溯
3. 按最小权限配置集成应用资产授权
4. 生产环境 AGI 不对公网暴露 `/api/open`
5. 对 SSE 长连接设置合理超时与断线重连

---

## 9. 附录

| 资源 | 路径 |
|------|------|
| OpenAPI YAML | [docs/openapi/open-api.yaml](./openapi/open-api.yaml) |
| Swagger UI | `{agi-host}/swagger-ui/index.html?urls.primaryName=open-api` |
| Headless 集成方案 | [docs/integration/agi-headless-integration-plan.md](./integration/agi-headless-integration-plan.md) |
| 混合身份设计 | [docs/integration/agi-hybrid-identity-design.md](./integration/agi-hybrid-identity-design.md) |
