# 知识库分块策略配置 API

## 创建分块策略

接口：`POST /api/v2/kb/chunk-profiles`

请求体示例：

```json
{
  "name": "archive-policy-v1",
  "description": "档案材料专用分块策略",
  "chunkSize": 2048,
  "chunkOverlap": 128,
  "separators": ["\\n## ", "\\n### ", "\\n\\n", "\\n", " "],
  "tableStrategy": "ROW_GROUP",
  "tableMaxRows": 20,
  "codeBlockStrategy": "ATOMIC",
  "codeBlockMaxTokens": 4000,
  "preserveHeaders": true,
  "headerPathDepth": 3,
  "metadataFields": [
    "docTitle",
    "sectionPath",
    "pageStart",
    "pageEnd",
    "chunkType",
    "sourceRefId",
    "parserVersion",
    "profileVersion"
  ]
}
```

响应体示例：

```json
{
  "profileId": "profile_archive_001",
  "status": "ACTIVE",
  "createdAt": "2026-07-01T10:00:00Z"
}
```

## 查询分块策略

接口：`GET /api/v2/kb/chunk-profiles/{profileId}`

> ⚠ 注意：以上 JSON 请求体和响应体必须作为整体保存。若被按字符数硬切成
> 多个 chunk，用户问"创建分块策略需要哪些参数"时将漏掉关键字段如
> `tableStrategy`、`codeBlockStrategy`、`metadataFields` 等。
