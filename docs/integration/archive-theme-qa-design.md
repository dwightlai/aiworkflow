# 专题资料知识问答设计方案

| 项目 | 说明 |
|------|------|
| 文档版本 | v1.0 |
| 日期 | 2026-06-18 |
| 适用场景 | 数字档案馆专题资料接入 AGI 知识问答 |
| 关联文档 | [档案条级权限设计方案](./archive-record-level-permission-design.md)、[档案专题资料接入 AGI 方案](./archive-knowledge-base-design.md)、[专题编研知识库数据集化改造设计文档](../专题编研知识库数据集化改造设计文档.md) |

---

## 术语约定

本文统一使用以下术语：

| 术语 | 含义 |
|------|------|
| **专题** | 数字档案馆中的一个编研/汇编主题对象，对应 `theme` |
| **专题资料** | 专题下的一条资料，对应 `theme_material`；可来自档案，也可为手工资料 |
| **挂接档案** | 被专题资料引用的案卷/条目/附件，正文与权限主数据在档案馆档案体系 |
| **手工资料** | 非档案来源、由用户录入或上传的专题资料 |
| **专题问答入口** | AGI 侧的 Bot / 工作流 / Open API 调用入口 |
| **入口授权** | AGI 侧对专题问答入口的可用性控制，如 `agi_asset_grant`、Bot / 应用授权 |
| **档案馆检索 API** | 数字档案馆提供的已授权检索接口，如 `theme/search` |
| **HTTP 连接器委托检索** | AGI 通过 `HTTP_TOOL` + 连接器调用档案馆检索 API 的方式 |

## 1. 背景与目标

### 1.1 业务数据组成

专题（档案汇编/编研专题）资料由三部分组成，均存放在 **数字档案馆**：

| 组成部分 | 说明 | 当前索引 |
|----------|------|----------|
| ① 专题名称 + 正文 | `theme` 表 | 无全文/向量索引 |
| ② 挂接档案 | 案卷、文件（条目）等，经 `theme_material` 引用 | `efile_idx` 已有全文 + 向量 |
| ③ 手工资料 | 用户录入/上传，非档案来源 | 无索引 |

### 1.2 目标

用户对指定专题提问，系统综合 **① 专题说明、② 有权查看的挂接档案、③ 有权查看的手工资料** 生成回答，并返回引用（档号、题名、专题概述等）。

### 1.3 约束

- 权限与 `AuthorityRuleManageService`、Voters **留在档案馆**，AGI 不重写规则引擎。
- AGI 通过 **`HTTP_TOOL` + 连接器** 调用档案馆专题检索 API，不在 AGI 内核中增加专用检索分支。
- 检索时 **禁止**「先查专题全部 materialId，再 terms 查 `efile_idx`」。

---

## 2. 专题与档案关联模型

专题通过 **专题资料**（`theme_material`）关联档案，不修改 `efile_idx` 主数据结构（方案一）：

```text
theme（专题）
  └── theme_material（汇编列表每一行）
        ├── materialId
        ├── themeId
        ├── materialSourceType
        │     ARCHIVE_VOLUME | ARCHIVE_ITEM | ARCHIVE_ATTACHMENT | MANUAL | UPLOAD
        ├── archiveObjectType + archiveObjectId   // 引用档案时指向 efile_idx 业务主键
        └── title, mj, dh ...（展示冗余）
```

| 文件类型 | materialSourceType | archiveObjectId |
|----------|-------------------|-----------------|
| 案卷 | ARCHIVE_VOLUME | 案卷 ID |
| 文件/条目 | ARCHIVE_ITEM | 条目 ID |
| 附件 | ARCHIVE_ATTACHMENT | 附件 ID |
| 手工录入 | MANUAL / UPLOAD | 无 |

**权限原则**：挂接档案以原档案权限为准，专题 membership **不能放大** 权限；手工资料走专题 ACL + 资料密级。

---

## 3. 推荐方案：统一专题检索索引（theme_search_idx）

### 3.1 架构

```text
用户提问
  → AGI 智能体 / 工作流入口
  → POST /openapi/archive/theme/search
       档案馆：theme_search_idx 一次 ES 检索
         ├── TOPIC_PROFILE    ← ① 专题名称+正文
         ├── ARCHIVE_REF      ← ② 挂接档案（正文+向量从 efile_idx 拷贝）
         └── TOPIC_MATERIAL   ← ③ 手工资料
       权限：专题 ACL + AuthorityRuleManageService + Voters
  → TopN items + theme 摘要
  → LLM 问答 + 引用
```

```text
Elasticsearch 集群
├── efile_idx           ← 已有，档案主索引
└── theme_search_idx    ← 新建，专题问答专用
```

### 3.2 三类 ES 文档

| docType | 组成部分 | 写入时机 | 正文/向量来源 |
|---------|----------|----------|----------------|
| `TOPIC_PROFILE` | ① | 专题保存 | `theme` 表字段，调用 embedding 模型 |
| `ARCHIVE_REF` | ② | 档案加入专题 | 从 `efile_idx` **拷贝** contentText + embedding + 权限扁平字段 |
| `TOPIC_MATERIAL` | ③ | 手工资料增删改 | 专题资料表正文，调用 embedding 模型 |

文档示例见 §5、§6。

### 3.3 检索

```text
filter: themeId = ?
filter: 可扁平化的权限字段（密级、unitId 等）
knn + keyword: query
topK
→ ARCHIVE_REF 候选：AuthorityRuleManageService + Voters（见条级权限专篇 §12.2.1）
→ TOPIC_PROFILE / TOPIC_MATERIAL：专题 ACL + 密级
```

### 3.4 维护事件

| 事件 | 动作 |
|------|------|
| 专题保存 | upsert `TOPIC_PROFILE` |
| 档案加入专题 | insert `ARCHIVE_REF`（§4） |
| 档案移出专题 | delete `ARCHIVE_REF` by materialId |
| `efile_idx` 正文/向量更新 | 刷新所有引用该 objectId 的 `ARCHIVE_REF` |
| 档案权限字段变更 | 更新 `ARCHIVE_REF` 扁平字段，和/或检索后 Voter |
| 手工资料增删改 | upsert/delete `TOPIC_MATERIAL` |

---

## 4. ARCHIVE_REF：从 efile_idx 拷贝正文与向量

本节为方案一的核心实现：**在档案加入专题时物化索引**，问答时不再访问 `efile_idx`。

### 4.1 触发时机

```text
用户「添加档案资料」进专题
  → 写入 theme_material（DB）
  → 调用 ThemeIndexService.onArchiveLinked(material)
  → GET efile_idx → 组装 ARCHIVE_REF → INDEX theme_search_idx
```

移除专题：`DELETE theme_search_idx/_doc/{materialId}`

### 4.2 从 efile_idx 读取

按 `archiveObjectType + archiveObjectId` 定位文档：

```http
GET efile_idx/_doc/{docId}
```

或：

```json
POST efile_idx/_search
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "objectType": "ITEM" } },
        { "term": { "objectId": "item_xxx" } }
      ]
    }
  },
  "size": 1
}
```

**拷贝字段清单：**

| 源字段（efile_idx） | 目标（ARCHIVE_REF） | 说明 |
|---------------------|---------------------|------|
| title / 题名 | title | 展示、检索 |
| contentText / fullText | contentText | RAG 正文 |
| embedding / dense_vector | embedding | **原样拷贝**，不重新 embed |
| dh, mj, kfcd, unitId, archiveDeptId… | 同名扁平字段 | ES filter + 展示 |
| objectType, objectId | archiveObjectType, archiveObjectId | 回指原档案 |

挂接 **案卷** 则 get 案卷层 doc；挂接 **条目** 则 get 条目层 doc。加案卷时 **不自动展开** 全部子条目，除非产品明确要求（展开 = 多条 `theme_material` + 多个 `ARCHIVE_REF`）。

### 4.3 写入 theme_search_idx

建议以 `materialId` 作为 ES `_id`：

```json
POST theme_search_idx/_doc/mat_001
{
  "themeId": "测试040201",
  "docType": "ARCHIVE_REF",
  "materialId": "mat_001",
  "archiveObjectType": "ITEM",
  "archiveObjectId": "item_xxx",
  "title": "西配楼主体工程卷",
  "contentText": "<从 efile_idx 拷贝>",
  "embedding": [<float[] 原样拷贝>],
  "dh": "004-KJ-KY-TVS2M-0040",
  "mj": "HXSM",
  "unitId": "fonds_001",
  "kfcd": "PTSM"
}
```

### 4.4 向量拷贝要求

- `theme_search_idx` 与 `efile_idx` 的 `embedding` 字段：**维度相同、相似度算法相同**（如 1024 维 cosine）。
- **禁止**对 `ARCHIVE_REF` 重新调用 embedding 模型（与源索引不一致且浪费算力）。
- 若源 doc 尚无向量、仅有全文：对 `ARCHIVE_REF` 使用与 `efile_idx` **相同模型** embed 一次，或等待 `efile_idx` 补全后重试 link。

### 4.5 服务伪代码

```java
void onArchiveLinked(ThemeMaterial material) {
    EfileDoc src = efileIndexRepo.getByObject(
        material.getArchiveObjectType(),
        material.getArchiveObjectId()
    );
    if (src == null) {
        scheduleRetry(material.getMaterialId()); // 档案尚未入 efile_idx
        return;
    }

    ThemeSearchDoc doc = new ThemeSearchDoc();
    doc.setThemeId(material.getThemeId());
    doc.setDocType("ARCHIVE_REF");
    doc.setMaterialId(material.getMaterialId());
    doc.setArchiveObjectType(material.getArchiveObjectType());
    doc.setArchiveObjectId(material.getArchiveObjectId());
    doc.setTitle(src.getTitle());
    doc.setContentText(src.getContentText());
    doc.setEmbedding(src.getEmbedding());  // 原样拷贝 float[]
    doc.copyAclFieldsFrom(src);

    themeSearchIndexRepo.upsert(material.getMaterialId(), doc);
}
```

### 4.6 档案变更的增量同步

`efile_idx` 更新后发布事件 `ArchiveIndexedEvent(objectType, objectId)`，消费者：

```text
1. SELECT material_id FROM theme_material
     WHERE archive_object_type = ? AND archive_object_id = ?
2. 对每个 materialId 重新执行 onArchiveLinked（或 partial update contentText + embedding）
```

| 事件 | 处理 |
|------|------|
| 正文/向量更新 | 刷新关联 `ARCHIVE_REF` |
| 权限字段更新 | 更新 `ARCHIVE_REF` 扁平字段 |
| 从 `efile_idx` 删除 | 删除或 disabled 对应 `ARCHIVE_REF` |
| 从专题移除 | `DELETE theme_search_idx/_doc/{materialId}` |

### 4.7 源文档不存在

档案已加入专题但尚未完成 `efile_idx` 入库时：

- 写入 `ARCHIVE_REF` 占位（仅 metadata，无 contentText/embedding），或
- 延迟队列重试（推荐），列表展示正常、检索暂不可命中正文。

---

## 5. TOPIC_PROFILE 与 TOPIC_MATERIAL

### 5.1 TOPIC_PROFILE（专题名称+正文）

专题保存时：

```text
contentText = theme.title + "\n" + theme.content
embedding   = embed(contentText)   // 与 efile_idx 相同模型
upsert theme_search_idx / _id = {themeId}_profile
```

```json
{
  "themeId": "测试040201",
  "docType": "TOPIC_PROFILE",
  "materialId": "theme_profile",
  "title": "测试040201",
  "contentText": "专题说明正文...",
  "embedding": []
}
```

### 5.2 TOPIC_MATERIAL（手工资料）

手工录入/上传保存时，正文解析后 embed 写入：

```json
{
  "themeId": "测试040201",
  "docType": "TOPIC_MATERIAL",
  "materialId": "mat_099",
  "title": "编研补充说明",
  "contentText": "手工录入正文...",
  "embedding": [],
  "mj": "NB"
}
```

---

## 6. ES Mapping 要点（theme_search_idx）

```json
{
  "mappings": {
    "properties": {
      "themeId": { "type": "keyword" },
      "docType": { "type": "keyword" },
      "materialId": { "type": "keyword" },
      "archiveObjectType": { "type": "keyword" },
      "archiveObjectId": { "type": "keyword" },
      "title": { "type": "text", "analyzer": "ik_smart" },
      "contentText": { "type": "text", "analyzer": "ik_smart" },
      "embedding": {
        "type": "dense_vector",
        "dims": 1024,
        "index": true,
        "similarity": "cosine"
      },
      "mj": { "type": "keyword" },
      "unitId": { "type": "keyword" },
      "kfcd": { "type": "keyword" },
      "dh": { "type": "keyword" }
    }
  }
}
```

`dims` 与 `efile_idx.embedding` 保持一致。

---

## 7. 对外 API

### 7.1 专题已授权检索

```http
POST /openapi/archive/theme/search
X-User-Id: user_001
X-Unit-Id: unit_001
X-Department-Ids: dept_a,dept_b
X-Role-Ids: archive_user

{
  "themeId": "测试040201",
  "query": "这个专题主要讲什么？",
  "topK": 8
}
```

### 7.2 响应

```json
{
  "theme": {
    "themeId": "测试040201",
    "title": "测试040201",
    "summary": "专题说明摘要或全文"
  },
  "items": [
    {
      "docType": "TOPIC_PROFILE",
      "sourceId": "theme_profile",
      "title": "测试040201",
      "contentText": "专题说明...",
      "score": 0.92,
      "citation": { "title": "专题说明", "locator": "专题概述" }
    },
    {
      "docType": "ARCHIVE_REF",
      "sourceId": "mat_001",
      "materialSourceType": "ARCHIVE_ITEM",
      "archiveObjectType": "ITEM",
      "archiveObjectId": "item_xxx",
      "title": "西配楼主体工程卷",
      "contentText": "...",
      "metadata": { "dh": "004-KJ-..." },
      "citation": { "title": "西配楼主体工程卷", "locator": "档号 004-KJ-..." }
    },
    {
      "docType": "TOPIC_MATERIAL",
      "sourceId": "mat_099",
      "title": "编研补充说明",
      "contentText": "...",
      "citation": { "locator": "专题资料" }
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `sourceId` | `materialId`，编研选材 / batch-get 用 |
| `archiveObjectId` | 有则「查看原文」跳档案详情（仍验原档案权限） |

### 7.3 档案馆内部检索流程

```text
1. 解析身份 → ArchiveSecurityContext
2. 校验专题级 ACL
3. theme_search_idx：filter themeId + 权限 filter + knn/keyword
4. 对 ARCHIVE_REF 命中走 AuthorityRuleManageService + Voters
5. 对 TOPIC_* 命中走专题 ACL + 密级
6. 取 topK，组装 theme + items
```

---

## 8. AGI 接入

### 8.1 Bot / 工作流参数

```json
{
  "connectorCode": "archive_platform",
  "operationCode": "theme_search",
  "themeId": "测试040201",
  "defaultTopK": 8
}
```

+ 若需限制入口范围，继续使用现有 Bot / 应用授权能力。

### 8.2 连接器

| operationCode | method | path |
|---------------|--------|------|
| `theme_search` | POST | `/openapi/archive/theme/search` |

### 8.3 问答链路

```text
Bot + 工作流
  → HTTP_TOOL
  → theme/search（透传 userId / unitId / departmentIds / roleIds）
  → LLM prompt：

     【专题】{{theme.title}}：{{theme.summary}}
     【参考资料】
     [{{citation.locator}}] {{contentText}}
     用户问题：{{message}}
```

| 阶段 | AGI |
|------|-----|
| P0 | HTTP_TOOL 工作流 + Bot（无需改引擎） |
| P1 | 将连接器、Prompt 模板、返回结果映射沉淀为可复用工作流模板 |

---

## 9. 权限分层

```text
AGI Bot / 应用授权       → 能不能使用这个专题问答入口
档案馆专题 ACL           → 能不能进这个专题
AuthorityRuleManageService → ARCHIVE_REF 对应原档案能不能看
专题资料密级 + ACL       → TOPIC_MATERIAL 能不能看
```

---

## 10. 备选方案：分层三路合并

改造量更小，检索质量略低，适用于快速上线。详见 [档案条级权限设计方案 §13.4](./archive-record-level-permission-design.md#134-方案二分层三路合并最小改造-efile_idx)。

| 组成部分 | 方案二做法 |
|----------|------------|
| ① 专题正文 | DB 直读，API 固定返回 `theme.summary` |
| ② 挂接档案 | `efile_idx` append `themeIds`，不拷贝正文 |
| ③ 手工资料 | 独立 `theme_manual_idx` |

对外 API 路径与 AGI 接入 **与推荐方案相同**，仅档案馆内部检索实现不同。

---

## 11. 实施分期

| 阶段 | 档案馆 | AGI |
|------|--------|-----|
| **P0** | `theme_search_idx` mapping；`ARCHIVE_REF` 拷贝流水线；`theme/search` 关键词检索 | HTTP_TOOL + Bot |
| **P1** | `TOPIC_PROFILE` / `TOPIC_MATERIAL` knn；`efile_idx` 变更事件刷新 `ARCHIVE_REF`；完整 Voters | 工作流模板 + 引用 |
| **P2** | 混合检索权重调优；batch-get 编研选材 | Open API / 工作流复用 |

---

## 12. 模块职责

| 模块 | 职责 |
|------|------|
| `ThemeMaterialService` | 专题资料 CRUD |
| `ThemeIndexService` | `onArchiveLinked` / `onArchiveUnlinked` / `onThemeSaved` / `onManualMaterialSaved` |
| `EfileIndexRepository` | 读 `efile_idx` |
| `ThemeSearchIndexRepository` | 写 `theme_search_idx` |
| `ThemeSearchService` | `theme/search` API：检索 + 鉴权 + 组装响应 |
| `ArchiveIndexEventConsumer` | 监听 `efile_idx` 更新，刷新 `ARCHIVE_REF` |

---

## 13. 变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-06-25 | v1.1 | 统一术语：专题 / 专题资料 / 专题问答入口 / 入口授权 |
| 2026-06-18 | v1.0 | 首版：theme_search_idx 方案 + efile_idx 拷贝实现 |
