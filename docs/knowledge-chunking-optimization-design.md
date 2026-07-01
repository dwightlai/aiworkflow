# AI Workflow 知识库分块优化设计文档

## 1. 文档信息

| 项目 | 内容 |
|---|---|
| 文档名称 | AI Workflow 知识库分块优化设计文档 |
| 文档版本 | V1.1.1 |
| 编制日期 | 2026-07-01 |
| 适用系统 | AI Workflow 企业 AI 应用平台 |
| 适用模块 | 知识库、文档解析、知识分块、向量索引、知识检索、工作流知识库节点、智能体问答 |
| 样例依据 | `sampledata/rag_chunking_samples`、`sampledata/chunking-samples02` |
| 当前状态 | 设计阶段 |

### 1.1 修订记录

| 版本 | 日期 | 修订说明 |
|---|---|---|
| V1.0 | 2026-07-01 | 初始设计，形成结构感知分块、父子块和检索补偿总体方案 |
| V1.1 | 2026-07-01 | 根据设计评审补充节点级策略路由、TokenCounter、检索组装算法、数据库分期、样例集定位及 P0-P3 实施范围 |
| V1.1.1 | 2026-07-01 | 根据二次评审统一 C01 策略、移除双轨接口、修正前端范围，并补充 Profile 映射、混合路由测试和重解析引用迁移 |

## 2. 建设背景

AI Workflow 已实现知识库创建、文档上传、文本抽取、分块预览、切片保存、Embedding、向量检索、关键词检索和混合检索等基础能力，并支持固定长度、段落、语义、符号和结构化表格等分块方式。

现有分块能力可以完成基础文本入库，但在企业制度、档案材料、操作流程、FAQ、API 文档、代码、表格和会议纪要等真实资料中，仍会出现以下问题：

- 流程步骤被截断，命中单个步骤后缺少完整流程上下文。
- 表头与数据行分离，数值失去字段含义。
- FAQ 的问题和答案被拆到不同切片。
- JSON、YAML、SQL 和代码块被字符截断。
- 正文脱离所属标题，召回结果无法判断章节语境。
- 条件列表被拆散，无法回答“第三个条件是什么”。
- 长段落中的结论与解释分属不同切片。
- 会议纪要多个议题混入同一切片，降低检索精度。

本次优化目标不是简单调整 `chunkSize`，而是将现有字符串分割升级为面向生产环境的结构感知分块体系。

## 3. 样例问题分析

### 3.1 样例覆盖范围

两组样例数据覆盖以下九类典型问题：

| 编号 | 问题类型 | 典型表现 | 目标能力 |
|---|---|---|---|
| C01 | 上下文断裂 | 报销制度章节被切成孤立片段 | `STRUCTURE_AWARE` 章节父块、`sectionPath` 和章节回填 |
| C02 | 表格截断 | 表头与数据行分离 | 表格结构识别、按行组分块、重复表头 |
| C03 | QA 配对打散 | 问题和答案分别落入不同切片 | FAQ 原子单元 |
| C04 | 代码块截断 | JSON 或代码字段不完整 | 代码块和结构化对象原子化 |
| C05 | 标题内容分离 | 正文失去章节归属 | 标题树和 `sectionPath` |
| C06 | 步骤流程断裂 | 步骤名、命令和说明被拆开 | 步骤组识别和顺序关系 |
| C07 | 长段落边界 | 结论和原因跨切片 | 分句合并、重叠和父段落回填 |
| C08 | 条款引用缺上下文 | 条件组和条件项分散 | 条款组父块和条件子块 |
| C09 | 多主题混杂 | 多个会议议题处于同一切片 | 按议题标题切分并继承会议元数据 |

### 3.2 样例集定位

| 目录 | 定位 | 使用方式 |
|---|---|---|
| `sampledata/rag_chunking_samples` | 主回归集 | 九类问题文件和 14 道测试题作为自动化测试与正式验收基准 |
| `sampledata/chunking-samples02` | 补充变体集 | 用于验证 Markdown 表格、FAQ 标题格式及混合结构，不替代主回归集 |

`chunking-samples02/02_表格被截断_产品销售数据.md` 应作为 C02 的 Markdown 内嵌表格扩展用例，但不计入核心 14 题。

### 3.3 核心判断标准

分块质量采用以下核心标准：

> 一个可回答问题所需的最小证据，应落在同一个切片内；无法落入同一小切片时，应通过子块召回和父块回填获得完整证据。

## 4. 当前实现评估

### 4.1 已实现能力

当前系统已经具备：

- Apache Tika 多格式文本抽取。
- Markdown、文本、PDF、Word、PPT、Excel、HTML 文件接入。
- 固定长度、段落、本地语义、符号和结构化表格分块。
- 分块预览和切片编辑。
- `chunkSize`、`chunkOverlap` 配置。
- Embedding 模型和向量维度配置。
- PostgreSQL、达梦数据库持久化。
- Elasticsearch 外部向量库。
- 关键词、向量和混合检索。
- 知识库节点和智能体知识问答。
- 切片级来源、页码、引用、安全等级等扩展字段。

### 4.2 当前不足

#### 4.2.1 分块仍以字符串处理为主

`KnowledgeDocumentSplitter` 中的固定长度策略直接按字符截断。段落和符号策略在内容超长时同样退化为固定字符截断。

当前 `SEMANTIC` 策略仅按标点分句并按长度聚合，没有使用 Embedding 计算语义突变点，因此应视为“句子边界分块”，不能作为真正的语义分块。

当前 `KnowledgeDocumentSplitter.splitSemantic()` 的中文分句正则已经出现乱码，导致中文句号、问号和感叹号不能被可靠识别。该问题必须纳入 P0 修复范围。

#### 4.2.2 文档结构在抽取阶段丢失

`DocumentTextExtractor` 最终返回纯字符串。标题、表格、代码块、列表、页码、工作表、PPT 页和 HTML DOM 等结构信息无法完整传递到分块阶段。

#### 4.2.3 分块配置存在两套路径

当前同时存在：

- `KnowledgeSplitter`
- `KnowledgeDocumentSplitter`

两者支持的策略、默认长度和 overlap 行为不完全一致，预览、上传、重解析可能产生不同结果。

`KnowledgeSplitter` 已支持 `MARKDOWN_HEADING`，但 `KnowledgeDocumentSplitter` 没有暴露相同能力。两套实现合并后，以 `KnowledgeDocumentSplitter` 作为兼容门面，内部统一委托 `ChunkStrategyRouter`，所有预览、上传和重解析不得再直接调用旧分割器。

#### 4.2.4 分块结果缺少结构元数据

`KnowledgeChunkPreview` 当前只有：

- `index`
- `content`
- `tokenEstimate`

虽然 `KnowledgeChunk` 已有 `chunkTitle`、`chunkType`、`sourcePage`、`citationText` 和 `metadataJson` 等字段，但普通文档保存时大部分字段没有被填充。

#### 4.2.5 检索缺少上下文补偿

当前检索直接返回命中的切片，没有实现：

- 父块回填。
- 前后相邻块扩展。
- 同一流程、FAQ、表格或条件组聚合。
- 小块召回、大块生成。
- 上下文 Token 预算控制。

#### 4.2.6 Token 估算不统一

当前 `KnowledgeSplitter.estimateTokens()` 使用 `length / 4` 估算 Token。该规则不适合中文，也无法反映不同 Embedding 模型和大模型 Tokenizer 的差异。

V1.1 要求通过统一 `TokenCounter` 提供 Token 计数。首期无法获得模型 Tokenizer 时可使用中英文混合估算器，但不得在各分块策略内自行实现不同换算规则。

## 5. 建设目标

### 5.1 总体目标

建立统一的企业级文档分块链路：

```text
原始文件
  → 结构化解析
  → 文档结构树
  → 原子单元识别
  → 策略路由
  → 父子块生成
  → 元数据增强
  → Embedding 与关键词索引
  → 子块召回
  → 父块及相邻块回填
  → 重排和上下文组装
```

### 5.2 设计原则

1. 文档结构优先，长度限制兜底。
2. 代码块、表格行、FAQ 对等原子单元默认不可拆。
3. 用 Token 作为长度度量，不再以字符数作为唯一标准。
4. 小块用于检索，大块用于生成。
5. 分块结果必须可解释、可预览、可编辑、可追溯。
6. 分块策略和解析版本必须可回滚。
7. PostgreSQL、达梦和 Elasticsearch 的字段语义保持一致。
8. 现有 API 和历史知识库应保持兼容。

## 6. 总体架构

```text
┌──────────────────────────────────────────────────────────────┐
│ 文档接入层                                                    │
│ TXT / MD / HTML / PDF / DOCX / PPTX / CSV / XLSX            │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 结构化解析层                                                  │
│ 格式解析器 → DocumentStructure → 标题/段落/表格/代码/页码     │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 原子单元识别层                                                │
│ FAQ / 代码块 / 表格行 / 步骤组 / 条款组 / 会议议题           │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 分块策略层                                                    │
│ 结构感知 / FAQ / 表格 / 代码 / 流程 / 条款 / 会议 / 语义兜底 │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 分块增强层                                                    │
│ Breadcrumb / 父子关系 / 相邻关系 / 来源 / 页码 / 版本        │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 索引与检索层                                                  │
│ 子块向量索引 + 关键词索引 → 父块回填 → 邻居扩展 → 重排       │
└──────────────────────────────────────────────────────────────┘
```

## 7. 结构化文档模型

### 7.1 DocumentStructure

文档抽取结果不再只返回字符串，建议统一为：

```java
public record DocumentStructure(
        String documentTitle,
        String sourceType,
        String rawText,
        List<DocumentNode> nodes,
        Map<String, Object> metadata
) {
}
```

### 7.2 DocumentNode

```java
public record DocumentNode(
        String id,
        NodeType type,
        String text,
        List<String> sectionPath,
        Integer pageStart,
        Integer pageEnd,
        Integer order,
        String groupId,
        Map<String, Object> metadata,
        List<DocumentNode> children
) {
}
```

### 7.3 NodeType

建议至少支持：

```text
TITLE
PARAGRAPH
LIST
LIST_ITEM
PROCEDURE
PROCEDURE_STEP
FAQ
TABLE
TABLE_ROW
CODE_BLOCK
JSON_BLOCK
QUOTE
CLAUSE
MEETING_TOPIC
```

## 8. 分块结果模型

### 8.1 KnowledgeChunkPreview 扩展

建议扩展为：

```java
public record KnowledgeChunkPreview(
        int index,
        String content,
        String embeddingContent,
        String displayContent,
        int tokenEstimate,
        String chunkType,
        String chunkTitle,
        List<String> sectionPath,
        String parentChunkId,
        String prevChunkId,
        String nextChunkId,
        String groupId,
        Integer pageStart,
        Integer pageEnd,
        boolean atomic,
        Map<String, Object> metadata
) {
}
```

字段含义：

| 字段 | 说明 |
|---|---|
| `content` | 完整切片正文 |
| `embeddingContent` | 用于生成向量的增强文本 |
| `displayContent` | 前端预览和最终引用文本 |
| `chunkType` | FAQ、表格、步骤、代码等类型 |
| `sectionPath` | 标题路径 |
| `parentChunkId` | 完整章节或完整语义单元 |
| `prevChunkId` | 前一相邻块 |
| `nextChunkId` | 后一相邻块 |
| `groupId` | FAQ、流程、条款、表格等所属组 |
| `atomic` | 是否为不可拆原子块 |

### 8.2 Embedding 文本

向量化文本建议采用：

```text
[文档标题]
[标题路径]
[分块类型]
切片正文
```

示例：

```text
文档：软件订阅服务采购管理制度
章节：二、采购权限
类型：采购条件

3. 单笔金额超过 50,000 元的，须取得至少三家供应商报价……
```

展示文本不必包含全部技术前缀，避免影响最终回答可读性。

## 9. 分块配置模型

### 9.1 ChunkProfile

建议使用版本化 Profile 代替大量零散参数：

```java
public record ChunkProfile(
        String id,
        String version,
        String strategy,
        int targetTokens,
        int maxTokens,
        int minTokens,
        int overlapTokens,
        boolean preserveAtomicBlocks,
        boolean includeSectionPath,
        boolean createParentChunks,
        boolean createChildChunks,
        int neighborWindow,
        Map<String, Object> options
) {
}
```

### 9.2 默认配置

```json
{
  "strategy": "STRUCTURE_AWARE",
  "targetTokens": 500,
  "maxTokens": 800,
  "minTokens": 120,
  "overlapTokens": 60,
  "preserveAtomicBlocks": true,
  "includeSectionPath": true,
  "createParentChunks": true,
  "createChildChunks": true,
  "neighborWindow": 1
}
```

### 9.3 兼容策略

历史参数仍保留：

- `splitterType`
- `chunkSize`
- `chunkOverlap`

服务端将其转换为临时 `ChunkProfile`，避免已有 API 和知识库失效。

### 9.4 TokenCounter

所有策略必须使用统一 Token 计数接口：

```java
public interface TokenCounter {
    int count(String text, String modelId);
}
```

实现优先级：

1. 使用知识库 Embedding 模型对应的 Tokenizer。
2. 使用平台已配置大模型对应的 Tokenizer。
3. 无法获得精确 Tokenizer 时，使用统一中英文混合估算器。

临时估算建议：

- 中文字符按约 0.6 至 0.8 Token/字估算。
- 英文按单词、数字、标点组合估算。
- 代码、JSON 和 URL 单独提高估算系数。

具体估算系数应通过样例语料与实际模型 Tokenizer 校准，不能把“中文字符数除以固定值”作为长期实现。

前端展示、预览、保存和检索上下文裁剪必须使用同一 Token 计数规则。

## 10. 分块策略设计

### 10.1 节点级策略路由

混合文档不能整篇只选择一种分块策略。系统应先将文档解析为 `DocumentStructure`，完成原子单元标注，再按每个 `DocumentNode.type` 分发策略。

```text
DocumentStructure
  → 标题/正文节点        → STRUCTURE_AWARE
  → FAQ 节点             → FAQ_PAIR
  → 表格节点             → TABLE_ROW_GROUP
  → 代码/JSON 节点       → ATOMIC_CODE
  → PROCEDURE_STEP 节点   → ProcedureStepChunkStrategy
  → 条款条件组节点       → CLAUSE_GROUP
  → 会议议题节点         → MEETING_TOPIC
  → 无明显结构长文本     → SEMANTIC 或 RECURSIVE_FALLBACK
```

同一份部署指南可以同时产出：

```text
TITLE
PARAGRAPH
PROCEDURE_STEP
TABLE_ROW_GROUP
CODE_BLOCK
```

`ChunkStrategyRouter` 的职责不是在多个策略中整篇二选一，而是遍历结构树，根据节点类型、父节点语境和 `ChunkProfile` 分发对应的 `NodeChunkStrategy`，最后统一建立父子关系、相邻关系和索引顺序。

正式接口定义见 14.2 节。10.1 只定义路由语义和优先级，避免在不同章节维护重复接口签名。

路由优先级：

1. 已标记的原子节点。
2. 明确业务结构节点。
3. 标题和普通段落。
4. 语义分块。
5. 递归长度兜底。

### 10.2 STRUCTURE_AWARE

适用于 Markdown、Word、PDF、HTML、PPT 等通用文档。

处理优先级：

1. 识别代码块、表格、FAQ 和步骤组。
2. 根据 H1-H6 或等效标题构建章节树。
3. 以标题、段落、句子和列表为自然边界。
4. 超过 `maxTokens` 时按下一级结构拆分。
5. 小于 `minTokens` 时与同级相邻块合并。
6. 注入完整 `sectionPath`。
7. 最后才使用递归长度切分兜底。

章节型制度默认使用 `STRUCTURE_AWARE`。例如报销制度中的“差旅申请”和“异常处理”应分别形成章节父块，并通过 `sectionPath` 增强检索，不应强行把整篇制度识别为线性流程。

### 10.3 FAQ_PAIR

识别格式：

```text
Q:
A:
问题：
答案：
## 问题标题
```

规则：

- 一个问题和答案作为一个父块。
- 短问答直接作为检索块。
- 长答案按段落或句子生成多个子块。
- 每个子块都注入原始问题。
- 问题、答案和子块使用同一个 `groupId`。

### 10.4 TABLE_ROW_GROUP

规则：

- 每个表格块必须保留表名、工作表名和表头。
- 单行数据不可按字符截断。
- 默认每组 3 至 20 行。
- 优先按分类列、部门、设备类型等字段分组。
- 超长行独立成块。
- 复杂单元格可按字段级子块建立索引，但展示时回填完整行。

### 10.5 ATOMIC_CODE

支持：

- Markdown fenced code。
- JSON 和 YAML。
- Java、JavaScript、Python、Shell。
- SQL。
- XML。

规则：

- 代码块默认不可拆。
- JSON 按完整对象或数组元素拆分。
- Java 按类、方法边界拆分。
- SQL 按语句拆分。
- 超长代码块应保留语言、文件名、类名和必要 import。
- 不允许在字符串、JSON 对象和函数中间切断。

### 10.6 PROCEDURE

适用于操作指南和业务流程。

规则：

- 完整流程作为父块。
- 每个步骤作为子块。
- 步骤编号、步骤标题、说明、命令和注意事项保持完整。
- 保存 `stepNumber`、`stepTitle`、`groupId`。
- 命中单个步骤时可回填完整流程或相邻步骤。

`PROCEDURE` 仅用于存在明确流程语义的区域，例如：

- `### 第一步：基础环境准备`
- `1. 提交申请`
- “办理流程”“操作步骤”“部署步骤”下的有序内容

如果文档只有章节编号而没有流程语义，应继续使用 `STRUCTURE_AWARE`。

### 10.7 CLAUSE_GROUP

适用于制度、合同和采购条款。

规则：

- “满足以下条件”与后续列表构成父块。
- 每个条件项可生成子块。
- 子块必须注入制度名称、章节和条件组说明。
- 保存条款编号、条件序号和是否为“全部满足”。

### 10.8 MEETING_TOPIC

规则：

- 会议名称、日期、地点和参会人作为公共元数据。
- 每个议题单独成为父块。
- 议题内的结论、负责人、金额和日期保持完整。
- 不同议题不合并。

### 10.9 SEMANTIC

真正的语义分块应：

1. 先按句子切分。
2. 使用知识库配置的 Embedding 模型生成句向量。
3. 计算相邻窗口的语义差异。
4. 在语义突变点切分。
5. 使用 `minTokens`、`targetTokens` 和 `maxTokens` 约束结果。

该策略适用于论文、白皮书和缺少明确结构的长文本，不应作为所有文档的默认策略。

### 10.10 RECURSIVE_FALLBACK

兜底分隔符优先级：

```text
章节标题
空行
换行
句号、问号、感叹号
分号
逗号
空格
字符边界
```

字符边界只能作为最后手段。

## 11. 父子块设计

### 11.1 基本原则

```text
父块：完整章节、完整流程、完整 FAQ、完整条款组、完整表格组
子块：段落、单步骤、单条件、表格行组、FAQ 答案片段
```

默认只对子块建立向量索引，父块用于生成上下文和引用展示。

父块标题通常具有较高检索价值。首期采用以下方式增强，不要求立即为父块建立独立向量索引：

- 子块 `embeddingContent` 必须注入文档标题和完整 `sectionPath`。
- 子块同时参与关键词和向量检索。
- 命中子块后，根据 `parentChunkId` 回填父块。
- 如实测标题类问题召回不足，再增加父块 BM25 或标题轻量索引。

### 11.2 检索流程

```text
用户问题
  → 子块向量召回和关键词召回
  → 融合与去重
  → 重排
  → 父块回填
  → 相邻块扩展
  → 同组内容补齐
  → Token 预算裁剪
  → 返回给知识库节点或智能体
```

### 11.3 检索组装算法

建议实现独立 `KnowledgeContextAssembler`，不要把父块回填和预算裁剪直接堆入 `KnowledgeBaseService.search()`。

伪代码：

```text
input:
  query
  candidateTopK = 15
  finalTopK = 5
  maxContextTokens = 6000
  maxDocumentRatio = 0.60
  neighborWindow = 1

1. candidates = hybridSearch(query, candidateTopK)
2. candidates = rerankOrWeightedSort(candidates)
3. seeds = take(candidates, finalTopK)

4. referenceIds = collect:
     seeds.chunkId
     seeds.parentChunkId
     seeds.groupId
     seeds.documentId + chunkIndex ± neighborWindow

5. relatedChunks = batchLoad(referenceIds)

6. groups = assemble by seed:
     exact child
     → parent
     → same group
     → previous/next neighbor

7. deduplicate using:
     chunkId
     parentChunkId + groupId
     normalized content hash

8. sort each group:
     seed score descending
     document order ascending

9. enforce per-document budget:
     each document tokens <= maxContextTokens * maxDocumentRatio

10. enforce global budget:
     preserve seed evidence first
     preserve atomic/group completeness second
     trim distant neighbors first
     trim duplicate parent text second

11. return assembled evidence and citation metadata
```

#### 11.3.1 去重规则

去重键按以下顺序使用：

1. `chunkId` 完全一致。
2. `parentChunkId + groupId` 指向同一语义组。
3. 规范化正文 SHA-256 一致。
4. 父块已完整包含子块正文时，不重复拼接子块正文，但保留子块命中信息和引用位置。

#### 11.3.2 Token 裁剪优先级

必须保留：

1. 直接命中的证据子块。
2. FAQ、代码块、表格行、条件组等不可拆证据。
3. 解释直接命中内容所需的父级标题和条件说明。

优先裁剪：

1. 距离较远的相邻块。
2. 已被父块完整覆盖的重复子块。
3. 同一文档中的低分候选。
4. 超过单文档占比限制的补充内容。

#### 11.3.3 批量读取与缓存

- ES 返回候选时应携带 `parentChunkId`、`groupId`、`documentId`、`chunkIndex`。
- 一次收集所有父块、同组块和邻居块 ID。
- 使用 MyBatis-Plus 批量查询，不允许逐命中块查询数据库。
- 单次检索请求内使用请求级 Map 缓存。
- 热点父块可增加短时本地缓存，缓存键包含 `chunkId + profileVersion`。
- 更新或重解析文档时必须失效相关缓存。

### 11.4 推荐默认值

| 参数 | 建议值 |
|---|---:|
| 初始子块召回 | Top 15 |
| 重排后保留 | Top 5 |
| 邻居扩展 | 前后各 1 块 |
| 父块回填 | 开启 |
| 最终上下文预算 | 6000 tokens |
| 单文档最大占比 | 60% |

## 12. 数据库改造

### 12.1 agi_knowledge_chunk

建议增加：

| 字段 | 类型建议 | 说明 |
|---|---|---|
| `parent_chunk_id` | VARCHAR(64) | 父块 ID |
| `prev_chunk_id` | VARCHAR(64) | 前一块 ID |
| `next_chunk_id` | VARCHAR(64) | 后一块 ID |
| `group_id` | VARCHAR(64) | 原子组或业务组 |
| `chunk_level` | VARCHAR(16) | PARENT/CHILD |
| `section_path` | TEXT | 标题路径 JSON |
| `embedding_content` | TEXT | 向量化文本 |
| `atomic_flag` | BOOLEAN/NUMBER(1) | 是否不可拆 |
| `parser_version` | VARCHAR(64) | 解析器版本 |
| `profile_version` | VARCHAR(64) | 分块 Profile 版本 |

### 12.2 字段分期

为降低 PostgreSQL、达梦双迁移风险，字段分两期落地。

#### 一期独立字段

| 字段 | 原因 |
|---|---|
| `parent_chunk_id` | 父块回填的核心关系 |
| `group_id` | FAQ、表格、流程和条款组补齐 |
| `chunk_level` | 区分父块与检索子块 |
| `section_path` | 标题路径检索和展示 |

一期暂不增加 `prev_chunk_id` 和 `next_chunk_id`。邻居关系通过 `document_id + chunk_index ± N` 批量计算，避免增加冗余关系维护成本。

#### 二期独立字段

| 字段 | 一期临时存储 |
|---|---|
| `embedding_content` | `metadata_json` 或构建索引时动态生成 |
| `atomic_flag` | `metadata_json` |
| `parser_version` | `metadata_json` |
| `profile_version` | `metadata_json` |
| `prev_chunk_id` | 通过 `chunk_index` 推导 |
| `next_chunk_id` | 通过 `chunk_index` 推导 |

二期迁列前应依据真实查询频率和数据量验证是否值得建立独立字段。

### 12.3 索引

建议增加：

```text
(knowledge_base_id, document_id, chunk_index)
(knowledge_base_id, parent_chunk_id)
(knowledge_base_id, group_id)
(knowledge_base_id, chunk_type)
```

PostgreSQL 和达梦迁移脚本必须同时提供。

## 13. Elasticsearch 索引改造

向量索引文档建议增加：

```json
{
  "chunkId": "chunk_xxx",
  "parentChunkId": "chunk_parent_xxx",
  "groupId": "group_xxx",
  "chunkType": "PROCEDURE_STEP",
  "sectionPath": ["员工差旅报销管理流程", "三、报销提交流程"],
  "documentId": "doc_xxx",
  "documentName": "员工差旅报销管理流程.md",
  "content": "第五步：校验通过后提交审批……",
  "embeddingContent": "文档……章节……第五步……",
  "enabled": true,
  "datasetId": "dataset_xxx",
  "securityLevel": "INTERNAL",
  "vector": []
}
```

关键词字段和向量字段分开配置，避免将技术元数据直接混入最终引用正文。

## 14. 服务端改造

### 14.1 新增包结构

```text
knowledge/chunking/
  DocumentStructure.java
  DocumentNode.java
  NodeType.java
  StructuredDocumentParser.java
  AtomicUnitExtractor.java
  ChunkProfile.java
  ChunkContext.java
  ChunkBuildResult.java
  NodeChunkStrategy.java
  ChunkStrategyRouter.java

knowledge/chunking/strategy/
  StructureAwareNodeChunkStrategy.java
  FaqNodeChunkStrategy.java
  TableNodeChunkStrategy.java
  CodeNodeChunkStrategy.java
  ProcedureStepChunkStrategy.java
  ClauseNodeChunkStrategy.java
  MeetingTopicNodeChunkStrategy.java
  SemanticNodeChunkStrategy.java
  RecursiveFallbackNodeChunkStrategy.java
```

### 14.2 ChunkStrategyRouter 与 NodeChunkStrategy

V1.1.1 只保留节点级策略接口，不再定义文档级 `ChunkStrategy`，避免实现时形成第二套分发体系。

```java
public interface NodeChunkStrategy {
    boolean supports(
            DocumentNode node,
            ChunkProfile profile,
            ChunkContext context
    );

    List<ChunkCandidate> split(
            DocumentNode node,
            ChunkProfile profile,
            ChunkContext context
    );
}
```

```java
public interface ChunkStrategyRouter {
    ChunkBuildResult split(
            DocumentStructure document,
            ChunkProfile profile,
            ChunkContext context
    );
}
```

`ChunkStrategyRouter` 负责遍历文档结构树、选择 `NodeChunkStrategy`、合并候选块并统一建立父子关系。`NodeChunkStrategy` 只处理单个节点及其局部上下文。

文档类型预设和 `ChunkProfile.strategy` 只用于配置启用的策略集合和优先级，不作为另一套文档级策略接口。

### 14.3 现有类调整

| 类 | 调整内容 |
|---|---|
| `DocumentTextExtractor` | 保留兼容接口，新增结构化解析接口 |
| `KnowledgeDocumentSplitter` | 改为 `ChunkStrategyRouter` 门面 |
| `KnowledgeSplitter` | 合并为递归兜底策略，消除两套逻辑 |
| `KnowledgeSplitRequest` | 增加 overlap、Token 上限、Profile 和选项 |
| `KnowledgeChunkPreview` | 增加结构、父子关系和元数据 |
| `KnowledgeBaseService` | 保存完整切片属性和父子关系 |
| `ElasticsearchVectorStoreClient` | 保存和返回新增检索字段 |
| `KnowledgeBaseService.search` | 增加回填、扩展、去重和上下文预算 |

## 15. API 改造

### 15.1 分块预览请求

```json
{
  "content": "...",
  "profile": {
    "strategy": "STRUCTURE_AWARE",
    "targetTokens": 500,
    "maxTokens": 800,
    "minTokens": 120,
    "overlapTokens": 60,
    "preserveAtomicBlocks": true,
    "includeSectionPath": true,
    "createParentChunks": true
  }
}
```

### 15.2 分块预览响应

响应增加：

- `chunkType`
- `chunkTitle`
- `sectionPath`
- `parentChunkId`
- `groupId`
- `atomic`
- `pageStart`
- `pageEnd`
- `embeddingContent`
- `metadata`

### 15.3 检索参数

新增可选参数：

```json
{
  "topK": 5,
  "candidateTopK": 15,
  "parentExpansion": true,
  "neighborWindow": 1,
  "groupExpansion": true,
  "maxContextTokens": 6000
}
```

原 API 不传新增参数时采用知识库默认配置。

## 16. 前端设计

### 16.1 分块策略

新增推荐策略：

- 智能结构分块。
- FAQ 问答分块。
- 表格行组分块。
- 代码/API 文档分块。
- 流程步骤分块。
- 制度/合同条款分块。
- 会议议题分块。
- 语义分块。
- 自定义符号分块。
- 固定长度分块。

### 16.2 文档类型预设

```text
通用文档
制度/合同
FAQ
API/代码文档
表格
操作流程
会议纪要
长篇论文/白皮书
```

用户选择预设后，系统填充对应 `ChunkProfile`，仍允许高级用户调整参数。

预设映射：

| 文档类型预设 | 默认节点策略组合 | target/max/min Tokens | 特殊配置 |
|---|---|---|---|
| 通用文档 | `STRUCTURE_AWARE` + `RECURSIVE_FALLBACK` | 500/800/120 | 注入 `sectionPath`，创建章节父块 |
| 制度/合同 | `STRUCTURE_AWARE` + `CLAUSE_GROUP` | 450/750/120 | 条件列表成组，章节父块回填 |
| FAQ | `FAQ_PAIR` + `STRUCTURE_AWARE` | 400/700/80 | 问答对原子化，长答案生成子块 |
| API/代码文档 | `ATOMIC_CODE` + `STRUCTURE_AWARE` | 600/1200/100 | 代码、JSON、SQL 原子化 |
| 表格 | `TABLE_ROW_GROUP` | 500/900/100 | 重复表头，每组默认 3 至 20 行 |
| 操作流程 | `PROCEDURE_STEP` + `ATOMIC_CODE` + `TABLE_ROW_GROUP` | 450/800/100 | 步骤父子块，相邻步骤扩展 |
| 会议纪要 | `MEETING_TOPIC` + `STRUCTURE_AWARE` | 500/900/120 | 继承会议元数据，不跨议题扩展 |
| 长篇论文/白皮书 | `STRUCTURE_AWARE` + `SEMANTIC` | 600/1000/150 | 有结构时优先标题，无结构段落才启用语义边界 |

所有预设默认：

```json
{
  "overlapTokens": 60,
  "preserveAtomicBlocks": true,
  "includeSectionPath": true,
  "createParentChunks": true,
  "createChildChunks": true,
  "neighborWindow": 1
}
```

### 16.3 分块预览

首期预览页面展示：

- 分块序号。
- 分块类型。
- 标题路径。
- Token 数。
- 父块/子块关系。
- 页码或工作表。
- 是否为原子块。
- 前后块关系。
- Embedding 文本。
- 最终展示文本。

首期只提供查看、重新预览和现有切片正文编辑能力，不增加复杂的结构关系编辑。

以下高级操作延后到 P3：

- 合并相邻切片。
- 手工拆分切片。
- 修改标题路径。
- 切换父块。
- 标记为原子块。

## 17. 样例问题对应方案

| 样例 | 分块策略 | 检索补偿 |
|---|---|---|
| 报销流程（C01） | `STRUCTURE_AWARE`，按 `##` 建立章节父块 | `sectionPath` + 邻居扩展 + 章节父块回填 |
| 设备采购表 | `TABLE_ROW_GROUP` | 返回完整数据行和表头 |
| FAQ | `FAQ_PAIR` | 长答案子块回填完整问答对 |
| API 文档 | `ATOMIC_CODE` | 代码块整体返回 |
| 技术规范 | `STRUCTURE_AWARE` | 注入完整标题路径 |
| 部署指南（C06） | `PROCEDURE_STEP`，识别 `### 第 N 步` | 步骤子块命中后回填流程父块或相邻步骤 |
| 合同条款 | `CLAUSE_GROUP` | 结论和解释回填到同一父块 |
| 采购制度 | `CLAUSE_GROUP` | 回填完整条件列表 |
| 会议纪要 | `MEETING_TOPIC` | 继承会议元数据，不扩展到其他议题 |

## 18. 测试设计

### 18.1 单元测试

每种策略至少覆盖：

- 正常结构。
- 超长原子块。
- 空内容。
- 混合换行。
- 中文和英文标点。
- 极短块合并。
- 最大 Token 限制。
- 父子关系。
- 相邻关系。
- 重解析结果稳定性。
- 混合 Markdown 文档能够同时产出 `PROCEDURE_STEP`、`CODE_BLOCK` 和 `TABLE_ROW_GROUP`。
- 节点级路由不能把原子代码块再次交给递归兜底策略切分。

### 18.2 样例回归测试

将 `sampledata/rag_chunking_samples/14_chunking_test_questions.csv` 接入自动化测试。

测试流程：

1. 创建临时知识库。
2. 按推荐 Profile 导入样例文件。
3. 生成切片及向量。
4. 使用 CSV 中的问题执行检索。
5. 判断 Top-K 是否包含期望答案证据。
6. 输出每个问题的命中切片、父块、排名和分数。

主回归集固定使用：

```text
sampledata/rag_chunking_samples
```

补充变体集使用：

```text
sampledata/chunking-samples02
```

补充变体集用于发现解析兼容问题，不影响核心 14 题的正式指标计算。

Q012 必须进行答案要点级验证。仅命中任意一个采购条件不算通过，最终组装上下文必须覆盖全部四个条件。

### 18.3 质量指标

#### 18.3.1 分块结构质量

| 指标 | 说明 | 首期目标 |
|---|---|---:|
| FAQ 完整率 | 问题与答案是否完整 | 100% |
| 代码块完整率 | 代码块是否被截断 | 100% |
| 表头保留率 | 表格块是否包含表头 | 100% |
| 标题路径覆盖率 | 切片是否包含章节路径 | ≥ 95% |
| 超长块比例 | 超过最大 Token 的普通块 | 0% |
| 过短块比例 | 低于最小 Token 且无业务意义的块 | ≤ 5% |

#### 18.3.2 检索与组装质量

| 指标 | 说明 | 首期目标 |
|---|---|---:|
| Raw Evidence Recall@5 | 未回填前 Top 5 小块是否包含核心证据 | ≥ 80% |
| Expanded Evidence Recall@5 | 父块、同组和邻居回填后是否包含完整证据 | ≥ 90% |
| Parent Recall@5 | 需要父上下文的问题是否成功回填 | ≥ 90% |
| MRR | 正确证据首次出现位置 | ≥ 0.75 |
| Answer Coverage | 期望答案要点覆盖比例 | ≥ 90% |

检索首期默认启用：

```text
candidateTopK = 15
finalTopK = 5
HYBRID 检索
关键词加权
父块/同组回填
```

系统应预留轻量 reranker 接口，但 P0 不强制引入新的重排模型。

## 19. 实施计划

### P0：统一入口与 Markdown 结构感知

- 合并 `KnowledgeSplitter` 和 `KnowledgeDocumentSplitter`。
- 修复中文分句正则。
- 将 `chunkOverlap` 纳入统一请求。
- 建立统一 `TokenCounter`。
- 建立 `ChunkStrategy`、`NodeChunkStrategy` 和节点级路由器。
- 实现 Markdown 标题树。
- 识别 Markdown 代码块、列表、FAQ、表格、步骤标题和普通章节。
- 支持同一 Markdown 文档生成混合类型切片。
- 改造 Markdown 和 CSV 表格行组切分。
- 增加 `sectionPath` 和 `chunkType`。
- 子块 `embeddingContent` 强制注入文档标题和 `sectionPath`。
- 预览、上传和重解析统一走同一入口。

交付结果：

- 解决双 Splitter 结果不一致问题。
- C03、C04、C05 基础可用。
- C02 的 CSV 和 Markdown 表格变体基础可用。
- C06 的 Markdown 步骤、表格和代码混合结构可识别。

### P1：父子块与检索补偿

- 增加一期数据库字段。
- 建立父块、子块和 `groupId`。
- 通过 `documentId + chunkIndex` 计算相邻关系。
- 子块向量召回。
- 父块、邻居和同组内容回填。
- 实现 `KnowledgeContextAssembler`。
- 实现批量读取、去重和单文档占比限制。
- 上下文 Token 预算。
- 默认启用 `candidateTopK=15` 和 `finalTopK=5`。
- 接入 14 道题自动化回归。

交付结果：

- C01 使用章节父块和 `sectionPath` 改善。
- C07 使用父段落和相邻块回填改善。
- C08 条件组能够完整回填。
- C09 会议议题能够独立召回。
- Expanded Evidence Recall@5 可自动计算。

### P2：多格式结构解析

- CSV、Excel 表头和工作表结构。
- DOCX 标题、表格和分页。
- PDF 页码和版面段落。
- HTML DOM。
- PPT 页和标题。
- 所有解析结果统一进入 `DocumentStructure`。
- 增加 PostgreSQL、达梦和 Elasticsearch 集成测试。

交付结果：

- 同一分块策略可跨文件格式运行。
- C02 覆盖 CSV、Markdown 和 Excel。
- DOCX/PDF 中的标题、表格和页码可进入切片元数据。

### P3：语义分块与运营工具

- 使用现有 Embedding 模型实现语义边界。
- 提供分块质量报告。
- 支持 Profile 版本对比和回滚。
- 提供轻量 reranker 扩展点。
- 实现前端切片合并、拆分、修改 `sectionPath` 和父块调整。
- 依据查询频率决定二期字段是否从 `metadataJson` 迁列。

交付结果：

- 无结构长文本获得真实语义分块能力。
- 支持 Profile 效果对比和运营调整。
- 前端具备高级人工校正能力。

## 20. 风险与控制

| 风险 | 控制措施 |
|---|---|
| 父子块增加存储量 | 仅对子块生成向量，父块只存正文 |
| 结构解析器差异较大 | 统一转换为 `DocumentStructure` |
| 历史 API 不兼容 | 保留旧参数并转换为 Profile |
| 重解析导致 chunkId 和引用变化 | 保存 parser/profile 版本；重解析先生成新版本切片，通过 `sourcePosition + contentHash + groupId` 建立新旧映射；切换成功后再更新 Bot 引用和审计关联；保留旧版本用于回滚 |
| 上下文扩展过多 | 使用 Token 预算、去重和单文档占比限制 |
| 语义分块调用成本高 | 仅用于无结构长文本，可配置关闭 |
| PostgreSQL 与达梦差异 | 同步维护迁移脚本和集成测试 |

### 20.1 重解析版本切换

重解析不能先删除旧切片再生成新切片，应采用版本化切换：

```text
旧版本 ACTIVE
  → 使用新 parser/profile 生成候选切片版本
  → 建立新旧切片映射
  → 生成向量并完成质量检查
  → 原子切换文档 activeChunkVersion
  → 更新依赖切片 ID 的 Bot 引用和审计关联
  → 旧版本进入 RETIRED，保留至回滚窗口结束
```

新旧切片匹配优先级：

1. `sourcePosition + groupId`。
2. 规范化 `contentHash`。
3. `sectionPath + chunkType + 相对顺序`。
4. 无法自动匹配时记录为新增或删除，不强行关联。

运行记录和历史审计继续引用原切片版本，不能因重解析覆盖历史证据。在线知识库检索只读取当前激活版本。

## 21. 验收标准

本方案完成后应满足：

1. 九类样例问题均有对应的结构化分块策略。
2. 预览、正式上传和重解析结果一致。
3. FAQ、代码块和表格行不被字符截断。
4. 每个普通文本切片具备标题路径。
5. 流程、条款和 FAQ 支持父子块。
6. 检索支持父块、邻居和同组扩展。
7. 14 个样例问题的 Raw Evidence Recall@5 不低于 80%，Expanded Evidence Recall@5 不低于 90%。
8. PostgreSQL、达梦和 Elasticsearch 均通过集成测试。
9. 历史知识库和旧版 API 可以继续运行。
10. 前端可以预览并解释每个切片的生成依据。
11. Q012 最终组装上下文能够完整覆盖四个采购条件。
12. 混合 Markdown 文档能够同时生成章节、步骤、代码和表格类型切片。

## 22. 结论

本次优化应优先建设结构感知分块和父子块检索，不建议首先投入复杂的大模型语义分块。

推荐实施路线为：

```text
P0 统一入口 + Markdown 结构感知 + 节点级路由
  → P1 父子块 + 检索组装 + 自动化回归
  → P2 多格式结构解析
  → P3 真实语义分块 + 运营工具
```

该路线可以最大限度复用项目现有的知识库、MyBatis-Plus、PostgreSQL、达梦、Elasticsearch、Embedding 和检索能力，并逐步将当前基础分块功能升级为可在企业知识库和数字档案馆场景中实际运行的生产级能力。
