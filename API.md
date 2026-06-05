# AI Workflow Platform API

> Base URL: `http://localhost:8080`
> Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
> OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 通用说明

所有接口返回统一格式 `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

- `success: true` 表示请求成功，`data` 中包含业务数据
- `success: false` 表示请求失败，`error` 中包含错误信息

**ErrorResponse:**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | string | 错误码 |
| message | string | 错误信息 |
| requestId | string | 请求 ID |
| details | Map<string, object> | 详细信息 |

分页接口返回 `ApiResponse<PageResponse<T>>`:

| 字段 | 类型 | 说明 |
|------|------|------|
| items | array[T] | 数据列表 |
| total | long | 总数量 |

---

## 目录

1. [WORKFLOW](#1-workflow)
2. [VECTOR STORE CONFIG](#2-vector-store-config)
3. [PROMPT TEMPLATE](#3-prompt-template)
4. [MODEL PROVIDER](#4-model-provider)
5. [KNOWLEDGE BASE](#5-knowledge-base)
6. [BOT](#6-bot)
7. [OPEN WORKFLOW RUN](#7-open-workflow-run)
8. [WORKFLOW RUN](#8-workflow-run)
9. [INTEGRATION APP](#9-integration-app)

---
## 1. WORKFLOW

<a id="1-workflow"></a>
### GET `/api/workflows`

list

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseWorkflowResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/workflows`

create

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `description` | string | 否 | - |
| `definition` | `WorkflowDefinition` | **是** | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseWorkflowResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### GET `/api/workflows/{workflowId}`

get

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `workflowId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseWorkflowResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/workflows/{workflowId}/archive`

archive

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `workflowId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseWorkflowResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### PUT `/api/workflows/{workflowId}/draft`

updateDraft

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `workflowId` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `definition` | `WorkflowDefinition` | **是** | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseWorkflowResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/workflows/{workflowId}/publish`

publish

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `workflowId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseWorkflowResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 2. VECTOR STORE CONFIG

<a id="2-vector-store-config"></a>
### GET `/api/vector-store-configs`

list_2

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseVectorStoreConfigResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/vector-store-configs`

create_1

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `storeType` | string | **是** | - |
| `endpoint` | string | 否 | - |
| `indexName` | string | **是** | - |
| `username` | string | 否 | - |
| `password` | string | 否 | - |
| `apiKey` | string | 否 | - |
| `connectTimeoutMs` | int | 否 | - |
| `readTimeoutMs` | int | 否 | - |
| `enabled` | boolean | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseVectorStoreConfigResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### PUT `/api/vector-store-configs/{id}`

update

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `storeType` | string | **是** | - |
| `endpoint` | string | 否 | - |
| `indexName` | string | **是** | - |
| `username` | string | 否 | - |
| `password` | string | 否 | - |
| `apiKey` | string | 否 | - |
| `connectTimeoutMs` | int | 否 | - |
| `readTimeoutMs` | int | 否 | - |
| `enabled` | boolean | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseVectorStoreConfigResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### DELETE `/api/vector-store-configs/{id}`

delete

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseVoid` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 3. PROMPT TEMPLATE

<a id="3-prompt-template"></a>
### GET `/api/prompts`

list_3

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponsePromptTemplate` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/prompts`

create_2

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `template` | string | **是** | - |
| `description` | string | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePromptTemplate` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### PUT `/api/prompts/{id}`

update_1

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `template` | string | **是** | - |
| `description` | string | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePromptTemplate` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### DELETE `/api/prompts/{id}`

delete_1

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseVoid` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 4. MODEL PROVIDER

<a id="4-model-provider"></a>
### GET `/api/model-providers`

list_4

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseModelProvider` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/model-providers`

create_3

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `modelType` | string | **是** | - |
| `modelUsage` | string | 否 | - |
| `description` | string | 否 | - |
| `visionSupport` | boolean | 否 | - |
| `pricePerMillionTokens` | number | 否 | - |
| `baseUrl` | string | **是** | - |
| `model` | string | **是** | - |
| `apiKeyRef` | string | **是** | - |
| `enabled` | boolean | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseModelProvider` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### PUT `/api/model-providers/{id}`

update_2

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `modelType` | string | **是** | - |
| `modelUsage` | string | 否 | - |
| `description` | string | 否 | - |
| `visionSupport` | boolean | 否 | - |
| `pricePerMillionTokens` | number | 否 | - |
| `baseUrl` | string | **是** | - |
| `model` | string | **是** | - |
| `apiKeyRef` | string | **是** | - |
| `enabled` | boolean | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseModelProvider` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### DELETE `/api/model-providers/{id}`

delete_2

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseVoid` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 5. KNOWLEDGE BASE

<a id="5-knowledge-base"></a>
### GET `/api/knowledge-bases`

list_5

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseKnowledgeBase` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases`

create_4

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `description` | string | 否 | - |
| `embeddingModelId` | string | 否 | - |
| `vectorStoreConfigId` | string | 否 | - |
| `vectorDimension` | int | 否 | - |
| `splitterType` | string | 否 | - |
| `chunkSize` | int | 否 | - |
| `chunkOverlap` | int | 否 | - |
| `retrievalMode` | string | 否 | - |
| `topK` | int | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseKnowledgeBase` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### PUT `/api/knowledge-bases/{id}`

update_3

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `description` | string | 否 | - |
| `embeddingModelId` | string | 否 | - |
| `vectorStoreConfigId` | string | 否 | - |
| `vectorDimension` | int | 否 | - |
| `splitterType` | string | 否 | - |
| `chunkSize` | int | 否 | - |
| `chunkOverlap` | int | 否 | - |
| `retrievalMode` | string | 否 | - |
| `topK` | int | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseKnowledgeBase` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### DELETE `/api/knowledge-bases/{id}`

delete_3

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseVoid` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### PUT `/api/knowledge-bases/{id}/chunks/{chunkId}`

updateChunk

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |
| `chunkId` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | string | **是** | - |
| `enabled` | boolean | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseKnowledgeChunk` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### GET `/api/knowledge-bases/{id}/documents`

documents

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseKnowledgeDocument` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/{id}/documents`

addDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `content` | string | **是** | - |
| `splitterType` | string | 否 | - |
| `chunkSize` | int | 否 | - |
| `chunkOverlap` | int | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseKnowledgeDocument` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### DELETE `/api/knowledge-bases/{id}/documents/{documentId}`

deleteDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |
| `documentId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseVoid` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### GET `/api/knowledge-bases/{id}/documents/{documentId}/chunks`

chunks

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |
| `documentId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseKnowledgeChunk` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/{id}/documents/{documentId}/reparse`

reparseDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |
| `documentId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseKnowledgeDocument` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/{id}/documents/manual`

addManualDataset

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `entries` | array[object] | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseListKnowledgeDocument` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/{id}/documents/table/upload`

uploadTableDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |
| `chunkSize` | query | int | 否 | `200` | - |

**Request Body** `multipart/form-data`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file (binary) | **是** | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseKnowledgeDocument` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/{id}/documents/text/upload`

uploadTextDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |
| `splitterType` | query | string | 否 | `FIXED_LENGTH` | - |
| `chunkSize` | query | int | 否 | `200` | - |
| `separator` | query | string | 否 | - | - |

**Request Body** `multipart/form-data`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file (binary) | **是** | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseKnowledgeDocument` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/{id}/documents/upload`

uploadDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |
| `splitterType` | query | string | 否 | - | - |
| `chunkSize` | query | int | 否 | `0` | - |
| `chunkOverlap` | query | int | 否 | `-1` | - |

**Request Body** `multipart/form-data`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file (binary) | **是** | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseKnowledgeDocument` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/{id}/embeddings/backfill`

backfillEmbeddings

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseBackfillEmbeddingsResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/{id}/search`

search

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `query` | string | **是** | - |
| `topK` | int | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseListKnowledgeSearchResult` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/chunks/preview`

previewChunks

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | string | **是** | - |
| `splitterType` | string | 否 | - |
| `chunkSize` | int | 否 | - |
| `chunkOverlap` | int | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseListKnowledgeChunkPreview` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/documents/table/upload/preview`

previewUploadedTableDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `chunkSize` | query | int | 否 | `200` | - |

**Request Body** `multipart/form-data`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file (binary) | **是** | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseUploadedDocumentPreview` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/documents/text/upload/preview`

previewUploadedTextDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `splitterType` | query | string | 否 | `FIXED_LENGTH` | - |
| `chunkSize` | query | int | 否 | `200` | - |
| `separator` | query | string | 否 | - | - |

**Request Body** `multipart/form-data`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file (binary) | **是** | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseUploadedDocumentPreview` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/knowledge-bases/documents/upload/preview`

previewUploadedDocument

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `splitterType` | query | string | 否 | - | - |
| `chunkSize` | query | int | 否 | `0` | - |
| `chunkOverlap` | query | int | 否 | `-1` | - |

**Request Body** `multipart/form-data`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file (binary) | **是** | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseUploadedDocumentPreview` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 6. BOT

<a id="6-bot"></a>
### GET `/api/bots`

list_7

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseAiBot` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/bots`

create_5

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `description` | string | 否 | - |
| `avatar` | string | 否 | - |
| `workflowId` | string | 否 | - |
| `modelProviderId` | string | 否 | - |
| `knowledgeBaseId` | string | 否 | - |
| `systemPrompt` | string | 否 | - |
| `openingMessage` | string | 否 | - |
| `status` | `ENABLED` | `DISABLED` | 否 | - (可选值: `ENABLED`, `DISABLED`) |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseAiBot` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### PUT `/api/bots/{id}`

update_4

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json` (必填):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | **是** | - |
| `description` | string | 否 | - |
| `avatar` | string | 否 | - |
| `workflowId` | string | 否 | - |
| `modelProviderId` | string | 否 | - |
| `knowledgeBaseId` | string | 否 | - |
| `systemPrompt` | string | 否 | - |
| `openingMessage` | string | 否 | - |
| `status` | `ENABLED` | `DISABLED` | 否 | - (可选值: `ENABLED`, `DISABLED`) |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseAiBot` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### DELETE `/api/bots/{id}`

delete_4

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseVoid` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/bots/{id}/chat`

chat

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | 否 | - |
| `message` | string | 否 | - |
| `input` | Map<string, object> | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseBotChatResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/bots/{id}/run`

run_1

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Request Body** `application/json`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message` | string | 否 | - |
| `input` | Map<string, object> | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseBotRunResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### GET `/api/bots/{id}/sessions`

sessions

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseBotSession` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### GET `/api/bots/{id}/sessions/{sessionId}/messages`

messages

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `id` | path | string | **是** | - | - |
| `sessionId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseBotMessage` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 7. OPEN WORKFLOW RUN

<a id="7-open-workflow-run"></a>
### GET `/openapi/v1/workflow-runs/{runId}`

getRun

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `runId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseMapStringString` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/openapi/v1/workflows/{workflowId}/runs`

createRun

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `workflowId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseMapStringString` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 8. WORKFLOW RUN

<a id="8-workflow-run"></a>
### GET `/api/workflow-runs`

list_1

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponsePageResponseWorkflowExecutionResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### GET `/api/workflow-runs/{executionId}`

get_1

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `executionId` | path | string | **是** | - | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseWorkflowExecutionResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

### POST `/api/workflows/{workflowId}/runs`

run

**Parameters:**

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `workflowId` | path | string | **是** | - | - |

**Request Body** `application/json`:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `input` | Map<string, object> | 否 | - |

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseWorkflowExecutionResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 9. INTEGRATION APP

<a id="9-integration-app"></a>
### GET `/api/integration-apps`

list_6

**Responses:**

| 状态码 | 说明 | Response Schema |
|--------|------|----------------|
| `200` | OK | `ApiResponseListIntegrationAppResponse` |
| `400` | Bad Request | `ApiResponseVoid` |
| `404` | Not Found | `ApiResponseVoid` |
| `500` | Internal Server Error | `ApiResponseVoid` |

---

## 核心 Schema 定义

### KnowledgeBase

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `name` | string | - |
| `description` | string | - |
| `embeddingModelId` | string | - |
| `vectorStoreConfigId` | string | - |
| `vectorDimension` | int | - |
| `splitterType` | string | - |
| `chunkSize` | int | - |
| `chunkOverlap` | int | - |
| `retrievalMode` | string | - |
| `topK` | int | - |
| `status` | string | - |
| `documentCount` | int | - |
| `chunkCount` | int | - |
| `createdAt` | datetime | - |
| `updatedAt` | datetime | - |

### KnowledgeDocument

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `knowledgeBaseId` | string | - |
| `name` | string | - |
| `chunkCount` | int | - |
| `createdAt` | datetime | - |
| `datasetType` | string | - |
| `processingStatus` | string | - |
| `tags` | string | - |
| `category` | string | - |
| `source` | string | - |
| `rowCount` | int | - |
| `parserType` | string | - |
| `splitterType` | string | - |
| `splitterConfig` | string | - |
| `rawContent` | string | - |
| `errorMessage` | string | - |

### KnowledgeChunk

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `knowledgeBaseId` | string | - |
| `documentId` | string | - |
| `documentName` | string | - |
| `content` | string | - |
| `index` | int | - |
| `enabled` | boolean | - |
| `tokenEstimate` | int | - |

### KnowledgeSearchResult

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `documentName` | string | - |
| `content` | string | - |
| `score` | int | - |

### KnowledgeChunkPreview

| 字段 | 类型 | 说明 |
|------|------|------|
| `index` | int | - |
| `content` | string | - |
| `tokenEstimate` | int | - |

### UploadedDocumentPreview

| 字段 | 类型 | 说明 |
|------|------|------|
| `fileName` | string | - |
| `characterCount` | int | - |
| `chunks` | array[object] | - |

### VectorStoreConfigResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `name` | string | - |
| `storeType` | string | - |
| `endpoint` | string | - |
| `indexName` | string | - |
| `username` | string | - |
| `passwordConfigured` | boolean | - |
| `apiKeyConfigured` | boolean | - |
| `connectTimeoutMs` | int | - |
| `readTimeoutMs` | int | - |
| `enabled` | boolean | - |
| `createdAt` | datetime | - |
| `updatedAt` | datetime | - |

### WorkflowResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `tenantId` | string | - |
| `name` | string | - |
| `description` | string | - |
| `status` | `DRAFT` | `PUBLISHED` | `ARCHIVED` | - (可选值: `DRAFT`, `PUBLISHED`, `ARCHIVED`) |
| `currentVersionId` | string | - |
| `createdBy` | string | - |
| `createdAt` | datetime | - |
| `updatedAt` | datetime | - |
| `latestVersion` | object | - |

### WorkflowVersionResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `workflowId` | string | - |
| `version` | int | - |
| `definition` | `WorkflowDefinitionResponse` | - |
| `status` | `DRAFT` | `PUBLISHED` | `DISABLED` | - (可选值: `DRAFT`, `PUBLISHED`, `DISABLED`) |
| `publishedBy` | string | - |
| `publishedAt` | datetime | - |
| `createdAt` | datetime | - |

### WorkflowDefinition

| 字段 | 类型 | 说明 |
|------|------|------|
| `nodes` | array[object] | - |
| `edges` | array[object] | - |
| `variables` | array[object] | - |

### WorkflowNode

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `type` | `START` | `END` | `LLM` | `PROMPT` | `KNOWLEDGE_RETRIEVAL` | `HTTP_TOOL` | `CONDITION` | `TEXT_TRANSFORM` | `CONTENT_TEMPLATE` | `LOOP` | - (可选值: `START`, `END`, `LLM`, `PROMPT`, `KNOWLEDGE_RETRIEVAL`, `HTTP_TOOL`, `CONDITION`, `TEXT_TRANSFORM`, `CONTENT_TEMPLATE`, `LOOP`) |
| `name` | string | - |
| `config` | Map<string, object> | - |

### WorkflowEdge

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `sourceNodeId` | string | - |
| `targetNodeId` | string | - |
| `condition` | string | - |

### WorkflowVariable

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | - |
| `type` | `STRING` | `NUMBER` | `BOOLEAN` | `OBJECT` | `ARRAY` | - (可选值: `STRING`, `NUMBER`, `BOOLEAN`, `OBJECT`, `ARRAY`) |
| `required` | boolean | - |

### WorkflowExecutionResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `workflowId` | string | - |
| `workflowVersionId` | string | - |
| `status` | `RUNNING` | `SUCCEEDED` | `FAILED` | - (可选值: `RUNNING`, `SUCCEEDED`, `FAILED`) |
| `input` | Map<string, object> | - |
| `output` | Map<string, object> | - |
| `errorMessage` | string | - |
| `startedAt` | datetime | - |
| `finishedAt` | datetime | - |
| `nodeExecutions` | array[object] | - |

### NodeExecutionResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `workflowExecutionId` | string | - |
| `nodeId` | string | - |
| `nodeType` | `START` | `END` | `LLM` | `PROMPT` | `KNOWLEDGE_RETRIEVAL` | `HTTP_TOOL` | `CONDITION` | `TEXT_TRANSFORM` | `CONTENT_TEMPLATE` | `LOOP` | - (可选值: `START`, `END`, `LLM`, `PROMPT`, `KNOWLEDGE_RETRIEVAL`, `HTTP_TOOL`, `CONDITION`, `TEXT_TRANSFORM`, `CONTENT_TEMPLATE`, `LOOP`) |
| `status` | `RUNNING` | `SUCCEEDED` | `FAILED` | `SKIPPED` | - (可选值: `RUNNING`, `SUCCEEDED`, `FAILED`, `SKIPPED`) |
| `input` | Map<string, object> | - |
| `output` | Map<string, object> | - |
| `errorMessage` | string | - |
| `startedAt` | datetime | - |
| `finishedAt` | datetime | - |

### ModelProvider

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `name` | string | - |
| `modelType` | string | - |
| `modelUsage` | string | - |
| `description` | string | - |
| `visionSupport` | boolean | - |
| `pricePerMillionTokens` | number | - |
| `baseUrl` | string | - |
| `model` | string | - |
| `apiKeyRef` | string | - |
| `enabled` | boolean | - |
| `createdAt` | datetime | - |
| `updatedAt` | datetime | - |

### PromptTemplate

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `name` | string | - |
| `template` | string | - |
| `description` | string | - |
| `createdAt` | datetime | - |
| `updatedAt` | datetime | - |

### AiBot

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `name` | string | - |
| `description` | string | - |
| `avatar` | string | - |
| `workflowId` | string | - |
| `modelProviderId` | string | - |
| `knowledgeBaseId` | string | - |
| `systemPrompt` | string | - |
| `openingMessage` | string | - |
| `status` | `ENABLED` | `DISABLED` | - (可选值: `ENABLED`, `DISABLED`) |
| `conversationCount` | int | - |
| `publishedAt` | datetime | - |
| `createdAt` | datetime | - |
| `updatedAt` | datetime | - |

### BotSession

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `botId` | string | - |
| `title` | string | - |
| `messageCount` | int | - |
| `createdAt` | datetime | - |
| `updatedAt` | datetime | - |

### BotMessage

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `sessionId` | string | - |
| `botId` | string | - |
| `role` | `USER` | `ASSISTANT` | - (可选值: `USER`, `ASSISTANT`) |
| `content` | string | - |
| `createdAt` | datetime | - |

### BotRunResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `bot` | object | - |
| `execution` | object | - |

### BotChatResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `session` | object | - |
| `messages` | array[object] | - |
| `reply` | object | - |
| `execution` | object | - |

### IntegrationAppResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | - |
| `name` | string | - |
| `status` | string | - |

