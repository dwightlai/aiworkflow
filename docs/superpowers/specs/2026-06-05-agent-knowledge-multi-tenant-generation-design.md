# 智能体、知识库、多单位共享与通用生成模板设计

## 1. 背景

本设计用于补充智能体平台的知识库、智能体、第三方业务系统接入和通用内容生成能力。

平台需要同时满足以下场景：

- 一个单位只使用自己维护的知识库和智能体。
- 多个单位共享同一个知识库或智能体。
- 授权范围需要细化到单位、部门、角色。
- 第三方业务系统，例如数字档案馆、OA 或其他应用系统，可以代表当前业务用户调用智能体。
- 第三方调用时能够传入当前用户的单位标识、部门标识、角色标识和用户标识。
- 智能体通常固定绑定一批知识库，但第三方调用时可以指定本次只使用哪些知识库。
- 数字档案馆的主题库、OA 的流程附件、业务系统的记录集合等动态资料，不应强行复制成平台知识库，而应作为运行时资料源参与生成。
- 编研成果、公文、报告、纪要、分析材料等内容生成应抽象为平台通用能力，而不是只服务某一个业务系统。

## 2. 设计目标

- 支持知识库和智能体的归属单位与共享使用分离。
- 支持按单位、部门、角色授权使用或管理知识库和智能体。
- 支持第三方系统通过开放 API 调用智能体，并携带运行上下文。
- 支持智能体默认知识库和本次调用知识库替换。
- 支持外部主题库、附件、业务记录等动态资料源接入。
- 支持通用生成模板，覆盖数字档案馆智能编研、OA 公文报告、业务系统分析报告等场景。
- 支持完整审计，能够还原每次调用使用了哪些智能体、知识库、外部资料、模板和来源片段。

## 3. 核心概念

### 3.1 单位、部门、角色

单位、部门、角色由业务系统或组织系统提供。AI 平台可以缓存基础信息，但运行时权限判断以第三方调用传入的上下文为准。

调用上下文包含：

```json
{
  "unitId": "unit_001",
  "departmentIds": ["dept_archive"],
  "roleIds": ["archive_user", "dept_admin"],
  "userId": "user_001"
}
```

### 3.2 知识库

知识库是平台内部长期维护、可切分、可向量化、可检索的资料集合。

知识库有归属单位：

```text
knowledge_base.owner_unit_id
```

归属单位负责维护知识库，不代表只有归属单位可以使用。共享使用由授权规则控制。

### 3.3 智能体

智能体是可被管理后台或第三方系统调用的 AI 应用。智能体可以绑定模型、系统提示词、默认知识库、工作流和生成模板。

智能体有归属单位：

```text
ai_bot.owner_unit_id
```

归属单位负责维护智能体，不代表只有归属单位可以使用。

### 3.4 外部资料源

外部资料源是第三方系统维护的动态资料集合，例如：

- 数字档案馆主题库。
- 档案条目和附件。
- OA 流程、正文和附件。
- 业务系统记录、明细、附件。
- 用户临时选择的一批业务对象。

外部资料源不一定永久进入 AI 平台知识库。平台在运行智能体时通过连接器读取资料，临时整理、切分、检索和引用。

### 3.5 生成模板

生成模板是平台通用的内容生成定义，描述要生成什么内容、使用哪些资料、如何组织章节、如何引用来源、输出为什么格式。

数字档案馆中可显示为“编研模板”，OA 中可显示为“公文模板”或“报告模板”，但平台底层统一称为生成模板。

## 4. 授权模型

### 4.1 资产授权表

知识库和智能体使用统一授权表：

```text
asset_grant
- id
- asset_type              KNOWLEDGE_BASE / BOT / GENERATION_TEMPLATE / EXTERNAL_CORPUS_SOURCE
- asset_id
- permission              USE / MANAGE
- unit_id                 必填
- department_ids          可为空，空表示该单位全部部门
- role_ids                可为空，空表示该单位全部角色
- enabled
- created_by
- created_at
- updated_at
```

第一阶段只实现 `ALLOW` 授权，不引入 `DENY`。后续如需复杂权限，可以扩展 `effect` 字段。

### 4.2 权限匹配规则

权限判断使用“单位必匹配，部门和角色命中任意一个”的规则：

```text
unit_id 必须等于调用上下文 unitId

department_ids 为空
或调用上下文 departmentIds 命中其中任意一个

role_ids 为空
或调用上下文 roleIds 命中其中任意一个
```

如果一条授权规则同时配置了部门和角色，则必须同时满足部门条件和角色条件。

示例：

```text
授权：单位 A，部门 档案管理科，角色 档案员
用户：单位 A，部门 [档案管理科]，角色 [普通用户, 档案员]
结果：允许

授权：单位 A，部门为空，角色 管理员
用户：单位 A，部门 [办公室]，角色 [管理员]
结果：允许

授权：单位 A，部门 档案管理科，角色为空
用户：单位 A，部门 [档案管理科]，角色 [普通用户]
结果：允许
```

### 4.3 USE 与 MANAGE

权限分为使用权限和管理权限：

- `USE`：可运行智能体、检索知识库、使用生成模板、读取外部资料源。
- `MANAGE`：可编辑智能体、维护知识库文档、配置授权、维护模板。

多个单位可以共享同一个知识库或智能体，但只有具备 `MANAGE` 权限的单位、部门或角色可以维护它。

## 5. 智能体知识库选择

### 5.1 默认知识库绑定

智能体固定绑定一批默认知识库：

```text
bot_knowledge_binding
- id
- bot_id
- knowledge_base_id
- sort_order
- enabled
```

如果第三方调用时没有传 `knowledgeBaseIds`，智能体使用默认绑定知识库。

### 5.2 本次调用知识库替换

第三方调用时可以传入 `knowledgeBaseIds`。一旦传入，则替换默认知识库，本次只检索传入的知识库。

```json
{
  "message": "电子文件归档前要检查什么？",
  "knowledgeBaseIds": ["kb_standard", "kb_policy"]
}
```

### 5.3 知识库选择模式

智能体增加知识库选择模式：

```text
knowledge_selection_mode
```

可选值：

- `FIXED_ONLY`：只能使用默认绑定知识库。第三方传入 `knowledgeBaseIds` 时拒绝请求。
- `ALLOW_SUBSET`：第三方只能从默认绑定知识库中选择一部分。传入后替换默认知识库。推荐默认值。
- `ALLOW_AUTHORIZED_EXTRA`：第三方可以指定默认绑定之外的知识库。传入后替换默认知识库，但每个知识库都必须通过第三方应用范围和当前上下文 `USE` 权限校验。

### 5.4 运行校验顺序

```text
1. 校验第三方应用身份。
2. 校验当前上下文是否有权 USE 智能体。
3. 读取智能体默认知识库。
4. 如果未传 knowledgeBaseIds，使用默认知识库。
5. 如果传了 knowledgeBaseIds，根据 knowledge_selection_mode 校验是否允许。
6. 对最终知识库逐个校验 USE 权限。
7. 只检索最终有权访问的知识库。
8. 记录本次实际使用的知识库。
```

## 6. 第三方开放调用

### 6.1 调用请求

开放 API 建议使用：

```text
POST /openapi/v1/bots/{botId}/runs
```

请求示例：

```json
{
  "unitId": "unit_001",
  "departmentIds": ["dept_archive"],
  "roleIds": ["archive_user"],
  "userId": "user_001",
  "message": "请根据主题库生成一份编研初稿",
  "knowledgeBaseIds": ["kb_standard"],
  "templateId": "template_research_001",
  "externalCorpus": {
    "type": "ARCHIVE_THEME_LIBRARY",
    "id": "theme_001"
  },
  "variables": {
    "topic": "电子文件归档管理",
    "audience": "档案管理人员"
  }
}
```

### 6.2 第三方应用访问范围

第三方应用需要配置访问范围：

```text
integration_app_scope
- id
- app_id
- scope_type              UNIT / BOT / KNOWLEDGE_BASE / TEMPLATE / EXTERNAL_CORPUS_SOURCE
- scope_id
- permission              USE / MANAGE
- enabled
```

运行时同时校验：

- 第三方应用是否允许代表该单位调用。
- 第三方应用是否允许调用目标智能体。
- 第三方应用是否允许使用目标知识库、模板和外部资料源。
- 当前业务用户上下文是否有对应资产的 `USE` 权限。

## 7. 外部资料源连接器

### 7.1 统一资料源接口

AI 平台不直接依赖数字档案馆、OA 或业务系统的内部数据结构。平台通过外部资料源连接器读取资料，并转换成统一结构。

统一资料项：

```json
{
  "sourceType": "ARCHIVE_ITEM",
  "sourceId": "archive_001",
  "title": "关于某事项的通知",
  "summary": "资料摘要",
  "contentText": "正文或附件解析后的文本",
  "metadata": {
    "档号": "A001-2024-0001",
    "责任者": "某单位",
    "形成时间": "2024-01-12"
  },
  "attachments": [
    {
      "attachmentId": "att_001",
      "fileName": "附件.pdf",
      "contentText": "附件正文"
    }
  ],
  "citation": {
    "title": "关于某事项的通知",
    "locator": "档号 A001-2024-0001 / 附件.pdf"
  }
}
```

### 7.2 数字档案馆主题库

主题库可以保留在数字档案馆系统中。AI 平台通过接口读取：

```text
GET /openapi/theme-libraries/{themeLibraryId}
GET /openapi/theme-libraries/{themeLibraryId}/items
GET /openapi/archive-items/{archiveItemId}
GET /openapi/archive-items/{archiveItemId}/attachments
GET /openapi/attachments/{attachmentId}/content
```

如果数字档案馆系统已经完成附件正文提取，应优先返回 `contentText`。如果只能返回文件下载地址，则 AI 平台需要在运行时下载并解析。

### 7.3 OA 与其他业务系统

OA 系统可以把流程单据、正文、审批意见和附件作为外部资料源：

```json
{
  "externalCorpus": {
    "type": "OA_PROCESS",
    "id": "process_001"
  }
}
```

业务系统可以把业务记录集合作为外部资料源：

```json
{
  "externalCorpus": {
    "type": "BUSINESS_RECORD_SET",
    "id": "case_001"
  }
}
```

平台只要求连接器能够返回统一资料项、正文、附件文本、元数据和引用信息。

### 7.4 运行时处理

外部资料源进入本次运行后，平台可以按资料规模选择处理方式：

- 小规模资料：直接切分并参与本次检索。
- 中等规模资料：临时向量化，运行结束后清理。
- 大规模资料：要求第三方提供可检索接口，平台按查询条件分页拉取 Top N 资料。

外部资料源不默认写入平台知识库，除非用户明确执行同步或沉淀操作。

## 8. 通用生成模板

### 8.1 定位

生成模板是平台通用能力，不限定为数字档案馆编研模板。

业务侧命名可以不同：

- 数字档案馆：编研模板、编研成果。
- OA：公文模板、报告模板、会议纪要模板。
- 业务系统：分析模板、汇总模板、处置建议模板。

平台底层统一使用：

- `GenerationTemplate`
- `GenerationJob`
- `GenerationOutput`

### 8.2 模板字段

```text
generation_template
- id
- name
- code
- description
- category
- owner_unit_id
- applicable_agent_types
- output_type              MARKDOWN / HTML / DOCX / JSON
- template_schema          结构定义
- prompt_policy            生成策略
- citation_policy          引用策略
- variables_schema         入参定义
- status
- version
- created_by
- created_at
- updated_at
```

### 8.3 模板结构

模板不只是 Prompt，而是结构、资料要求、生成规则和输出格式的组合。

示例：

```json
{
  "title": "专题编研成果模板",
  "variables": [
    { "name": "topic", "label": "主题", "type": "string", "required": true },
    { "name": "audience", "label": "面向对象", "type": "string", "required": false }
  ],
  "sections": [
    {
      "key": "overview",
      "title": "一、背景概述",
      "instruction": "根据资料概括主题背景，不超过800字。",
      "requiredSources": ["INTERNAL_KNOWLEDGE_BASE", "EXTERNAL_CORPUS"],
      "citationRequired": true
    },
    {
      "key": "timeline",
      "title": "二、发展脉络",
      "instruction": "按时间顺序梳理关键事件。",
      "outputFormat": "timeline",
      "citationRequired": true
    },
    {
      "key": "conclusion",
      "title": "三、总结建议",
      "instruction": "结合资料形成总结，不得编造事实。",
      "citationRequired": false
    }
  ]
}
```

### 8.4 资料源声明

模板可以声明需要的资料能力：

```text
INTERNAL_KNOWLEDGE_BASE
EXTERNAL_CORPUS
USER_UPLOAD
BUSINESS_RECORD
API_RESULT
```

模板不直接绑定具体数字档案馆主题库或 OA 流程。具体资料源由运行请求传入。

### 8.5 生成任务

一次智能体内容生成创建一个生成任务：

```text
generation_job
- id
- bot_id
- template_id
- requester_app_id
- unit_id
- department_ids
- role_ids
- user_id
- selected_knowledge_base_ids
- external_corpus_ref
- variables
- status
- started_at
- completed_at
- error_message
```

### 8.6 生成成果

生成成果保存最终内容、引用来源和资料快照：

```text
generation_output
- id
- job_id
- title
- output_type
- content_markdown
- content_html
- content_json
- file_id
- citations
- source_snapshot
- status
- created_at
```

引用来源统一结构：

```json
{
  "sourceType": "ARCHIVE_ITEM",
  "sourceId": "archive_001",
  "title": "关于某事项的通知",
  "metadata": {
    "档号": "A001-2024-0001",
    "责任者": "某单位",
    "形成时间": "2024-01-12"
  },
  "attachmentName": "附件.pdf",
  "chunkId": "chunk_001"
}
```

## 9. 生成运行流程

```text
1. 第三方系统或管理后台调用智能体。
2. 传入单位、部门、角色、用户、模板、变量、知识库和外部资料源。
3. 校验第三方应用身份和访问范围。
4. 校验当前上下文是否有权 USE 智能体。
5. 根据 knowledge_selection_mode 确定本次知识库范围。
6. 校验本次知识库 USE 权限。
7. 校验模板 USE 权限。
8. 校验外部资料源 USE 权限。
9. 读取内部知识库片段和外部资料源资料。
10. 对外部资料进行切分、摘要、检索或临时向量化。
11. 按生成模板的 section 逐段生成。
12. 合并为完整成果。
13. 校验引用、格式和生成约束。
14. 保存生成任务、生成成果、引用来源和审计日志。
15. 返回成果给第三方系统。
```

## 10. 审计与可追溯

每次智能体调用至少记录：

- 第三方应用 ID。
- 单位、部门、角色、用户。
- 智能体 ID。
- 模板 ID。
- 最终使用的知识库 ID。
- 外部资料源类型和 ID。
- 命中的知识库片段。
- 使用的外部资料条目和附件。
- 模型调用信息。
- 生成成果 ID。
- 请求时间、耗时、状态和错误信息。

数字档案馆编研成果需要特别保留档案引用来源，例如档号、题名、责任者、形成时间、附件名等。

## 11. 前端管理设计

### 11.1 知识库管理

知识库页面增加：

- 归属单位。
- 使用授权。
- 管理授权。
- 授权到单位、部门、角色。

### 11.2 智能体管理

智能体页面增加：

- 归属单位。
- 使用授权。
- 管理授权。
- 默认知识库绑定。
- 知识库选择模式。
- 可用生成模板。
- 是否允许外部资料源。

### 11.3 第三方应用管理

第三方应用页面增加：

- 可代表的单位范围。
- 可调用的智能体范围。
- 可使用的知识库范围。
- 可使用的生成模板范围。
- 可使用的外部资料源范围。
- API Key 或 OAuth2 凭据。

### 11.4 生成模板管理

生成模板页面包括：

- 模板列表。
- 模板版本。
- 变量定义。
- 章节结构。
- 每个章节的生成说明。
- 资料源要求。
- 引用策略。
- 输出格式。
- 使用授权。

## 12. 异常策略

- 第三方应用无权调用：返回 `APP_SCOPE_DENIED`。
- 当前上下文无权使用智能体：返回 `BOT_ACCESS_DENIED`。
- 指定知识库不符合选择模式：返回 `KNOWLEDGE_SELECTION_DENIED`。
- 当前上下文无权使用知识库：返回 `KNOWLEDGE_ACCESS_DENIED`。
- 当前上下文无权使用模板：返回 `TEMPLATE_ACCESS_DENIED`。
- 外部资料源不可用：返回 `EXTERNAL_CORPUS_UNAVAILABLE`。
- 外部资料源权限不足：返回 `EXTERNAL_CORPUS_ACCESS_DENIED`。
- 模板变量缺失：返回 `TEMPLATE_VARIABLE_INVALID`。
- 生成失败：返回 `GENERATION_FAILED`。

所有错误都需要进入审计日志。

## 13. 分阶段落地

### 阶段一：资产归属与授权

- 知识库和智能体增加 `owner_unit_id`。
- 新增 `asset_grant`。
- 管理端支持知识库和智能体授权到单位、部门、角色。
- 运行智能体和检索知识库时加入权限校验。

### 阶段二：智能体动态知识库选择

- 新增 `bot_knowledge_binding`。
- 新增 `knowledge_selection_mode`。
- 第三方调用智能体支持 `knowledgeBaseIds`。
- 传入 `knowledgeBaseIds` 时替换默认知识库。

### 阶段三：第三方应用范围

- 新增 `integration_app_scope`。
- 开放 API 接收单位、部门、角色、用户上下文。
- 按应用范围和用户上下文双重校验。

### 阶段四：外部资料源

- 新增外部资料源连接器接口。
- 支持数字档案馆主题库作为第一个连接器。
- 支持条目、附件、正文、元数据和引用信息读取。
- 支持运行时切分和临时检索。

### 阶段五：通用生成模板

- 新增生成模板、生成任务、生成成果。
- 支持按模板章节生成。
- 支持引用来源输出。
- 支持数字档案馆编研成果场景。

### 阶段六：扩展到 OA 和其他业务系统

- 增加 OA 流程资料源连接器。
- 增加业务记录集合连接器。
- 复用生成模板能力生成报告、公文、纪要和分析材料。

## 14. 验收标准

- 一个知识库可以授权给多个单位、部门和角色使用。
- 一个智能体可以授权给多个单位、部门和角色使用。
- 用户角色命中任意一个授权角色即可获得权限。
- 第三方调用智能体时可以传入单位、部门、角色和用户标识。
- 第三方不传 `knowledgeBaseIds` 时使用智能体默认知识库。
- 第三方传 `knowledgeBaseIds` 时替换默认知识库，只检索传入知识库。
- 系统根据知识库选择模式判断是否允许本次指定知识库。
- 智能体运行时只检索当前上下文有权使用的知识库。
- 数字档案馆主题库可以作为外部资料源参与生成，不需要永久同步成知识库。
- 生成模板可以同时支持编研成果、OA 报告和业务分析材料。
- 生成成果包含引用来源和资料快照。
- 审计日志能还原每次调用使用的智能体、知识库、模板、外部资料源和命中片段。
