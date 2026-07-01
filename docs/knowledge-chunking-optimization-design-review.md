# 知识库分块优化设计评审

## 1. 文档信息

| 项目 | 内容 |
|---|---|
| 评审对象 | `docs/knowledge-chunking-optimization-design.md`（V1.0） |
| 样例依据 | `sampledata/rag_chunking_samples`、`sampledata/chunking-samples02` |
| 代码依据 | `KnowledgeDocumentSplitter`、`KnowledgeSplitter`、`KnowledgeBaseService` |
| 评审日期 | 2026-07-01 |
| 二次评审日期 | 2026-07-01 |
| 评审结论 | **V1.1 可进入实施；修复 3 处文档自相矛盾后可作为开发基线** |

## 2. 二次评审（V1.1）

### 2.1 评审对象

- 设计文档：`docs/knowledge-chunking-optimization-design.md` **V1.1**
- 对照：首轮评审 `knowledge-chunking-optimization-design-review.md` 全部改进项

### 2.2 首轮意见落实情况

| 改进项 | 状态 | V1.1 对应章节 |
|---|---|---|
| 样例集主/补定位 | ✅ 已落实 | 3.2、18.2 |
| 中文分句 bug 写入 P0 | ✅ 已落实 | 4.2.1、P0 |
| 双 Splitter 合并方向 | ✅ 已落实 | 4.2.3、P0 |
| TokenCounter 统一 | ✅ 已落实 | 4.2.6、9.4 |
| 节点级策略路由 | ✅ 已落实 | 10.1 |
| C01 章节 vs 流程区分 | ⚠️ 部分落实 | 10.2/10.6/P1 已写清，但 3.1、17 节仍写 PROCEDURE |
| 检索组装伪代码 | ✅ 已落实 | 11.3 |
| 去重 / Token 裁剪 / 批量缓存 | ✅ 已落实 | 11.3.1–11.3.3 |
| 数据库分期 | ✅ 已落实 | 12.2 |
| 前端能力瘦身 | ✅ 已落实 | 16.3 |
| Raw / Expanded 分层验收 | ✅ 已落实 | 18.3、21 |
| Q012 要点级验证 | ✅ 已落实 | 18.2、21 |
| candidateTopK=15 首期必开 | ✅ 已落实 | 11.4、18.3.2、P1 |
| P0–P3 实施范围 | ✅ 已落实 | 19 |

**落实率：12/13 项完全落实，1 项文档内前后不一致。**

### 2.3 二次评审结论

V1.1 已吸收首轮评审的核心建议，设计完整度从「可实施基线」提升到「**可进入开发**」。架构、分期、验收指标均达到开工标准。

剩余问题以**文档自相矛盾**和**接口命名对齐**为主，不影响总体方向，建议在 V1.1.1 小修订中修正（约 30 分钟文档工作量）。

### 2.4 仍须修正的问题（阻塞文档定稿）

#### 问题 1：C01 策略前后矛盾（高）

正文已明确报销制度属于章节型文档：

- 10.2：`章节型制度默认使用 STRUCTURE_AWARE`
- P1：`C01 使用章节父块和 sectionPath 改善`

但以下位置仍写 `PROCEDURE`：

- 3.1 C01 目标能力：「流程父块、步骤子块」
- 17 节对照表：报销流程 → `PROCEDURE`

**建议统一为：**

| 样例 | 分块策略 | 检索补偿 |
|---|---|---|
| 报销流程（C01） | `STRUCTURE_AWARE`（按 `##` 章节父块） | `sectionPath` + 邻居扩展 + 章节父块回填 |
| 部署指南（C06） | `PROCEDURE`（`### 第 N 步`） | 步骤子块命中后回填流程父块或相邻步骤 |

#### 问题 2：接口定义双轨（中）

- 10.1 定义 `NodeChunkStrategy`（按节点分发，正确方向）
- 14.2 仍保留文档级 `ChunkStrategy.supports(DocumentStructure)`

**建议：** 14.2 改为 `ChunkStrategyRouter` + `NodeChunkStrategy`；文档级 `ChunkStrategy` 标为废弃或仅作 Profile 预设入口，避免实现时两套接口并存。

#### 问题 3：16.3「重新预览」重复（低）

首期能力列表含「重新预览」，P3 延后列表又出现「重新预览」，语义冲突。

**建议：** P3 列表删除「重新预览」，仅保留合并、拆分、改 sectionPath、切换父块、标记原子块。

### 2.5 建议补充（非阻塞）

| 项 | 说明 |
|---|---|
| 文档类型预设 → Profile 映射 | 16.2 七种预设应附默认 `ChunkProfile` JSON 或策略组合表 |
| 混合文档路由单测 | 18.1 增加「单文档产出 PROCEDURE_STEP + CODE_BLOCK + TABLE_ROW_GROUP」用例 |
| 重解析引用变更 | 20 风险表补充：重解析后 chunkId 变化对 Bot 引用/审计的影响及迁移策略 |
| `PROCEDURE` vs `PROCEDURE_STEP` | 10.1 路由表写「流程步骤节点 → PROCEDURE」，NodeType 有 `PROCEDURE_STEP`，建议路由目标写 `PROCEDURE_STEP` 策略处理 `PROCEDURE_STEP` 节点 |

### 2.6 评分对比

| 维度 | V1.0 评审 | V1.1 二次评审 |
|---|---|---|
| 问题诊断 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 架构设计 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 样例覆盖 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 可实施性 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 可验收性 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 文档一致性 | — | ⭐⭐⭐⭐（C01 矛盾待修） |

### 2.7 二次评审结论

**批准进入 P0 开发**，建议同步发布 V1.1.1 修订：

1. 统一 C01 为 `STRUCTURE_AWARE`
2. 对齐 14.2 与 10.1 接口定义
3. 修正 16.3 文案

---

## 3. 评审范围（首轮）

本次评审对照以下材料：

- 设计文档：`docs/knowledge-chunking-optimization-design.md`
- 主样例集：`sampledata/rag_chunking_samples`（9 类问题文件 + `14_chunking_test_questions.csv` + `15_分块问题与推荐策略.md`）
- 补充样例：`sampledata/chunking-samples02`（通用方案说明 + 6 类 Markdown 变体）
- 当前实现：`server/src/main/java/com/mw/ai/agi/knowledge/service/KnowledgeDocumentSplitter.java`、`KnowledgeSplitter.java`

## 3. 总体评价

设计方案准确识别了当前系统的核心短板：

- 分块以字符串硬切为主，超长内容退化为固定字符截断
- 文档结构在抽取阶段丢失，无法传递标题、表格、代码块等信息
- `KnowledgeSplitter` 与 `KnowledgeDocumentSplitter` 双入口，预览/上传/重解析可能不一致
- 检索直接返回命中切片，缺少父块回填、邻居扩展与同组补齐

九类样例问题（C01–C09）与 `15_分块问题与推荐策略.md`、`14_chunking_test_questions.csv` 一一对应。验收指标（Evidence Recall@5 ≥ 90%）可量化，分阶段实施路线合理。

**结论：方案可作为 V1.0 实施依据。**

## 4. 样例匹配度分析

| 编号 | 样例 | 设计策略 | 匹配度 | 说明 |
|---|---|---|---|---|
| C01 | 上下文断裂（报销流程） | `PROCEDURE` + 父块回填 | ⚠️ 部分 | Q001/Q002 分别命中「一、差旅申请」「四、异常处理」，并非线性第 N 步；**章节父块 + sectionPath** 比纯流程策略更关键 |
| C02 | 表格截断（设备采购 CSV） | `TABLE_ROW_GROUP` | ✅ | 须保留表名、表头；`chunking-samples02` 的 Markdown 内嵌表格是另一类输入 |
| C03 | QA 配对打散 | `FAQ_PAIR` | ✅ | `.txt` 用 `Q:/A:`，samples02 用 `##` 标题，识别规则须覆盖两种格式 |
| C04 | 代码块截断（API 文档） | `ATOMIC_CODE` | ✅ | Q007 答案在 JSON 字段列表中，代码/JSON 必须原子完整 |
| C05 | 标题内容分离 | `STRUCTURE_AWARE` | ✅ | 与 samples02「breadcrumb 注入」一致 |
| C06 | 步骤流程断裂（部署指南） | `PROCEDURE` | ⚠️ 需加强 | 步骤为 `### 第一步` 而非纯 `1.` 列表；且与环境表、代码块共存 |
| C07 | 长段落边界（合同条款） | 语义 + 父块回填 | ✅ | 「结论 + 原因」同段，结构分块 + overlap 可能已足够 |
| C08 | 条款引用缺上下文 | `CLAUSE_GROUP` | ✅ | 4 条条件 +「全部满足」说明须同组保留 |
| C09 | 多主题混杂（会议纪要） | `MEETING_TOPIC` | ✅ | 仅 rag 样例集覆盖，须纳入主回归 |

### 4.1 样例集使用建议

| 目录 | 定位 |
|---|---|
| `sampledata/rag_chunking_samples` | **主回归集**（9 文件 + 14 题 CSV），作为自动化测试与验收基准 |
| `sampledata/chunking-samples02` | 补充 Markdown 表格变体、通用四层架构说明；**不替代**主回归集 |

建议将 `chunking-samples02/02_表格被截断_产品销售数据.md` 作为 C02 的 Markdown 变体纳入扩展测试，不计入核心 14 题。

## 5. 方案亮点

1. **「小块检索、大块生成」**：与 samples02「小检索大上下文」一致，是 C01、C07 的正解。
2. **ChunkProfile 版本化**：优于散落参数，且兼容旧 `splitterType` / `chunkSize` / `chunkOverlap`。
3. **embeddingContent / displayContent 分离**：避免技术前缀污染引用展示。
4. **分阶段实施**：统一入口 → 结构感知 → 父子块 → 多格式解析 → 真语义，优先级合理。
5. **架构边界清晰**：逻辑留在 Knowledge Service 层，不侵入工作流引擎，符合产品化原则。

## 6. 问题与改进建议

### 6.1 当前代码诊断应更具体

设计文档第 4 节判断正确，建议补充以下已知缺陷并写入第一阶段必改项：

- `KnowledgeDocumentSplitter.splitSemantic()` 中文分句正则已损坏（`(?<=[銆傦紒锛?!?])`），**SEMANTIC 对中文基本无效**
- `KnowledgeSplitter` 已有 `MARKDOWN_HEADING` 分支，但 `KnowledgeDocumentSplitter` 未暴露；两套默认参数不一致
- 合并 Splitter 时须明确以哪套为门面，并保证预览、上传、重解析走同一路径

### 6.2 策略路由规则未定义

文档列出 9 种策略，但未说明**混合文档**如何路由。例如 `06_步骤流程断裂_部署指南.md` 同时含表格、代码块、`### 第一步` 步骤。

**建议补充：**

```text
解析 → 原子单元标注 → 按 DocumentNode.type 选策略（非整篇文档单策略）
同一文档可产出 PROCEDURE_STEP + CODE_BLOCK + TABLE_ROW_GROUP 混合块
```

`ChunkStrategyRouter` 应按节点类型分发，而非 `supports(document)` 整篇二选一。

### 6.3 C01 不宜过度依赖 PROCEDURE

报销样例是**章节型制度**，不是 1→6 线性步骤：

- Q001 证据在「一、差旅申请」
- Q002 证据在「四、异常处理」

**建议：**

- 默认 `STRUCTURE_AWARE` 按 `##` 建章节父块
- `PROCEDURE` 仅用于明确标注为「步骤/流程」的段落
- 检索补偿优先 `sectionPath` + 邻居扩展，而非强行回填完整流程

### 6.4 Token 度量缺少落地方案

当前 `KnowledgeSplitter.estimateTokens()` 使用 `length / 4`，与「以 Token 为长度标准」不一致。

**建议明确：**

- 首期：中文按 `字符数 / 1.5` 估算，或复用 Embedding 模型对应 tokenizer
- `ChunkProfile` 的 `targetTokens` / `maxTokens` 与前端展示统一换算规则

### 6.5 仅子块建向量可能漏检

父块标题（如「二、采购权限」）往往更易被问题命中。

**建议：**

- 子块：向量索引（主召回）
- 父块：关键词/BM25 辅召回，或标题单独建轻量索引
- 子块 `embeddingContent` 强制注入完整 `sectionPath`（标为**必做**）

### 6.6 检索补偿实现细节不足

父块回填、邻居扩展、同组补齐、Token 预算 — 架构清晰，但缺少：

- 去重键（`parentChunkId` + `groupId` + content hash）
- 单文档 60% 上限的裁剪算法
- ES 与 PG 双读时的 N+1 查询与缓存策略

建议在原设计文档 11.2 节补充「检索组装伪代码」或序列图。

### 6.7 数据库改造可分两期

一次新增 9 个字段偏重，建议：

| 阶段 | 落列字段 | 其余字段 |
|---|---|---|
| 一期 | `parent_chunk_id`、`group_id`、`chunk_level`、`section_path` | — |
| 二期 | `embedding_content`、`parser_version`、`profile_version` 等 | 可先放 `metadataJson`，验证后再迁列 |

降低 PostgreSQL / 达梦双迁移风险。

### 6.8 前端手工编辑能力范围过大

原设计 16.3 节「合并/拆分/改 sectionPath/切换父块」适合第三期后做。首期预览展示 `chunkType`、`sectionPath`、父子关系即可。

### 6.9 90% Recall@5 偏乐观

14 题中 Q012（4 条条件完整列举）、Q013/Q014（会议多议题）对召回 + 组装要求高，无 reranker 时仅靠向量 Top5 可能不足。

**建议验收分两层：**

| 层级 | 指标 | 目标 |
|---|---|---|
| 分块质量 | FAQ 完整率、代码完整率、表头保留率 | 100% |
| 检索质量 | Evidence Recall@5 | ≥ 90% |

检索层 `candidateTopK=15` 应标为**首期必开**，并预留轻量 rerank 或关键词加权。

## 7. 实施优先级建议

与原设计 19 节基本一致，微调如下：

| 阶段 | 内容 | 交付 |
|---|---|---|
| **P0** | 合并双 Splitter；修中文分句；统一 Token 估算；`sectionPath` 注入；FAQ/代码块/表格原子识别（Markdown 先行） | 预览/上传/重解析一致；C03/C04/C05 基础可用 |
| **P1** | 父子块 + 检索回填/邻居扩展/同组补齐；接入 14 题自动化回归 | C01/C07/C08 明显改善；Recall 可测 |
| **P2** | CSV/Excel/PDF/DOCX 结构解析统一到 `DocumentStructure` | C02 多格式；C06 混合文档 |
| **P3** | 真语义分块、Profile 版本对比、前端高级编辑 | C07 长尾；运营工具 |

**不建议**将真语义分块提前 — C07 用结构分块 + overlap + 父块回填大概率已可解决。

## 8. 需在原设计文档中补充的章节

开工前建议在 `knowledge-chunking-optimization-design.md` 中新增或修订：

1. **节点级策略路由**：混合文档按 `DocumentNode.type` 分发，附路由表
2. **章节型 vs 流程型文档区分**：C01 用 `STRUCTURE_AWARE` 为主，`PROCEDURE` 为辅
3. **检索组装算法**：去重、Token 预算裁剪、单文档占比限制的伪代码
4. **第一阶段必改项**：中文分句 bug、双 Splitter 合并、Token 估算统一
5. **样例集定位**：明确 `rag_chunking_samples` 为主回归集

## 9. 评分汇总

| 维度 | 评分 | 说明 |
|---|---|---|
| 问题诊断 | ⭐⭐⭐⭐⭐ | 与代码、样例完全一致 |
| 架构设计 | ⭐⭐⭐⭐ | 缺混合文档路由与检索组装细节 |
| 样例覆盖 | ⭐⭐⭐⭐ | 九类齐全，C01 策略可再细化 |
| 可实施性 | ⭐⭐⭐⭐ | 分阶段合理，DB/前端可再瘦身 |
| 可验收性 | ⭐⭐⭐⭐⭐ | CSV 14 题 + 结构指标清晰 |

## 10. 评审结论

`knowledge-chunking-optimization-design.md` **可作为 V1.0 实施依据**。

优先建设结构感知分块与父子块检索补偿，不建议首先投入复杂的大模型语义分块。推荐路线：

```text
统一入口
  → 结构感知 + 原子单元（Markdown 先行）
  → 元数据增强（sectionPath 必做）
  → 父子块 + 检索补偿
  → 多格式解析
  → 真语义分块（可选）
```

与样例数据、现有知识库能力（PostgreSQL、达梦、Elasticsearch、Embedding、混合检索）匹配度高，可在企业知识库与数字档案馆场景中逐步落地。
