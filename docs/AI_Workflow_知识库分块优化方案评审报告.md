# AI Workflow 知识库分块优化方案评审报告

> 评审对象：AI Workflow 知识库分块优化设计文档 V1.1.2  
> 评审日期：2026-07-01  
> 评审范围：知识库分块优化设计方案、样例覆盖情况、专业 RAG 分块补充样例建议

## 1. 评审结论

本方案建议评审结论为：**有条件通过，可以进入 P0/P1 开发准备，但需要先修订若干 P0 级问题。**

方案的主线是正确的：它没有停留在调整 chunkSize，而是将知识库分块能力升级为“结构化解析 → 节点级策略路由 → 父子块 → 检索上下文组装 → 自动化回归评估”的生产级链路。这一方向与当前主流 RAG 工程实践一致。

核心判断：

| 评审项 | 结论 |
|---|---|
| 总体方向 | 正确 |
| P0/P1 是否可进入开发 | 可以，但需先修订 P0 级问题 |
| 样例集是否覆盖基础分块问题 | 基本覆盖 |
| 是否需要补充专业样例 | 需要 |
| 是否建议一期就做真实语义分块 | 不建议，先保留接口 |
| 是否建议一期就引入 reranker | 不强制，先预留接口 |

## 2. 方案优点

### 2.1 从“字符串切分”升级为“结构感知分块”

方案已经明确把现有问题定位为：固定长度截断、标题与正文脱离、表格表头丢失、FAQ 问答对拆散、代码块截断、流程步骤断裂、条款条件分散等。这些都是企业知识库真实场景中最常见的分块失败原因。

正确方向不是继续调大或调小 chunkSize，而是先做结构化解析，再按内容类型分块。这个判断是对的。

### 2.2 节点级策略路由设计正确

方案提出先解析为 DocumentStructure，再按 DocumentNode.type 分发到 FAQ、表格、代码、流程、条款、会议议题等不同 NodeChunkStrategy。这个设计比“整篇文档只选择一种分块策略”更合理。

一份真实文档往往同时包含：标题、普通段落、列表、表格、代码块、FAQ、流程步骤和附录。如果只在文档级别选择一种策略，就一定会牺牲其中某些结构。

### 2.3 父子块与检索补偿是正确核心

方案提出“小块用于检索，大块用于生成”，并通过 parentChunkId、groupId、邻居扩展、同组补齐来解决上下文不足问题。这是生产 RAG 中非常关键的能力。

建议继续坚持以下原则：

```text
子块：用于召回，保证精准。
父块：用于生成，保证上下文完整。
同组块：用于补齐 FAQ、表格、流程、条款等完整证据。
邻居块：用于补齐长段落、连续步骤、上下文解释。
```

### 2.4 TokenCounter 和 ChunkProfile 方向正确

统一 TokenCounter 可以避免预览、保存、向量化、检索上下文裁剪各自使用不同估算规则。ChunkProfile 可以替代零散的 chunkSize、chunkOverlap 参数，有利于版本化、回滚和回归测试。

### 2.5 一期、二期边界合理

方案把 Markdown/TXT/CSV 结构感知分块放入一期，把 DOCX/PDF/HTML/PPTX/XLSX 深度结构解析放入二期，这是合理的范围控制。

一期应该优先解决“结构化分块 + 父子块 + 检索补偿 + 自动化回归”，不要一开始就把 PDF 版面解析、复杂 Excel、真实语义分块、reranker、高级人工编辑全部纳入。

## 3. 必须修订的问题

### 3.1 P0-1：消除 ChunkStrategy 表述矛盾

文档前面已经明确“只保留节点级 NodeChunkStrategy，不再定义文档级 ChunkStrategy”，但 P0 实施计划里仍写到“建立 ChunkStrategy、NodeChunkStrategy 和节点级路由器”。这会导致开发重新做出两套路由接口。

建议修改为：

```text
建立 NodeChunkStrategy、ChunkStrategyRouter 和 ChunkProfile 驱动的节点级路由机制，不再新增文档级 ChunkStrategy 接口。
```

### 3.2 P0-2：权限过滤必须早于上下文扩展

父块回填、同组补齐、邻居扩展都可能扩大用户可见内容范围。如果权限过滤放在扩展之后，可能出现“用户有权看某个公开子块，但系统回填了无权查看的父块内容”的越权风险。

建议补充硬规则：

```text
所有 parentExpansion、groupExpansion、neighborExpansion 必须在权限过滤之后执行。
扩展出来的父块、同组块、邻居块必须逐块校验用户权限。
如果扩展块权限高于当前用户权限，只返回当前已授权证据，不允许跨权限补齐。
```

验收指标建议增加：

| 指标 | 目标 |
|---|---:|
| 上下文扩展越权率 | 0 |
| 受限块误召回率 | 0 |
| 引用中出现无权内容 | 0 |

### 3.3 P0-3：embeddingContent 必须保存快照

方案中把 embedding_content 放在二期独立字段是可以接受的，但一期至少要在 metadata_json 中保存实际参与向量化的 embeddingContentSnapshot。

原因是：同一个 content 不变，只要标题路径、文档标题、chunkType 拼接规则变了，向量结果就可能变化。如果没有保存当时实际向量化文本，后续很难排查召回问题。

建议一期保存：

```json
{
  "embeddingContentSnapshot": "文档：...
章节：...
正文：...",
  "contentHash": "...",
  "embeddingContentHash": "...",
  "profileVersion": "...",
  "parserVersion": "..."
}
```

### 3.4 P0-4：父块至少进入关键词/标题索引

方案默认只对子块建立向量索引，父块用于上下文和引用展示，这个设计可以降低成本。但父块标题通常具有很高检索价值。

建议一期不强制给父块建向量，但至少让父块标题、chunkTitle、sectionPath 进入 BM25/关键词索引。

建议规则：

```text
父块默认不生成向量。
父块标题、sectionPath、chunkTitle 进入关键词索引。
标题型问题召回不足时，再评估父块低权重向量索引。
```

### 3.5 P0-5：区分 logicalChunkId 与 chunkVersionId

文档已经正确提出重解析不能先删旧切片再生成新切片，而要版本化切换。但建议进一步区分两个 ID：

| ID | 含义 | 用途 |
|---|---|---|
| logicalChunkId | 逻辑切片 ID | 标识“同一段逻辑证据” |
| chunkVersionId | 切片版本 ID | 标识“某次解析生成的具体内容” |

历史回答、审计、引用应该绑定 chunkVersionId；新旧切片映射和引用迁移可以使用 logicalChunkId。

### 3.6 P0-6：当前 SEMANTIC 策略名称要避免误导

如果当前 SEMANTIC 只是按标点分句再合并，它不是真正的语义分块。建议在实现和前端命名上区分：

| 名称 | 含义 |
|---|---|
| SENTENCE_BOUNDARY | 按句子边界聚合，不计算 embedding 语义突变 |
| SEMANTIC | 使用 embedding 计算相邻窗口语义差异并选择切分点 |

否则用户看到“语义分块”，会误以为系统已经进行了 embedding-based semantic chunking。

## 4. 建议增强的问题

### 4.1 增加 EvidenceGroup 返回结构

检索接口不建议只返回扁平 chunk 列表。父块回填、同组补齐、邻居扩展后，实际返回的是一组证据。

建议增加 EvidenceGroup：

```json
{
  "seedChunk": {},
  "parentChunk": {},
  "groupChunks": [],
  "neighborChunks": [],
  "expansionReason": "PARENT_AND_GROUP_EXPANSION",
  "visible": true,
  "citationRange": {}
}
```

这样前端和智能体都能解释：为什么这个证据被带入上下文。

### 4.2 增加分块质量日志

建议每次入库和检索记录：

```text
parserVersion
profileVersion
chunkCount
parentChunkCount
childChunkCount
atomicBlockCount
overMaxTokenCount
underMinTokenCount
avgChunkTokens
expandedChunkCount
trimmedChunkCount
permissionFilteredChunkCount
```

这些日志对后续调参非常关键。

### 4.3 前端预览要显示“为什么这样切”

前端不只显示切片内容，还应显示：

```text
切片类型
标题路径
父块/子块关系
是否原子块
Token 数
分块策略
是否参与向量化
embeddingContent 预览
扩展回填规则
```

这能减少用户对“为什么这个地方被切开”的疑问。

### 4.4 增加“失败样例归档”机制

生产中用户反馈“答错了”，多数时候不是模型问题，而是证据召回或上下文组装问题。建议增加失败样例归档：

```text
用户问题
实际召回 chunks
期望证据
是否缺父块
是否缺同组块
是否权限过滤
是否版本错误
是否表格表头丢失
```

后续可以沉淀为新的回归测试。

## 5. 样例覆盖评审

### 5.1 原样例集判断

当前主样例集已经覆盖基础分块问题，可以作为 P0/P1 主回归集。

| 问题类型 | 覆盖情况 | 说明 |
|---|---|---|
| 标题路径丢失 | 已覆盖 | 可验证 sectionPath |
| 表格表头和数据分离 | 已覆盖 | 可验证表头重复与行组分块 |
| FAQ 问答对拆散 | 已覆盖 | 可验证 FAQ_PAIR |
| 流程步骤断裂 | 已覆盖 | 可验证 PROCEDURE_STEP |
| 代码/JSON 截断 | 已覆盖 | 可验证 ATOMIC_CODE |
| 条款条件分散 | 已覆盖 | 可验证 CLAUSE_GROUP |
| 长段落跨边界 | 已覆盖 | 可验证父段落/邻居回填 |
| 会议纪要多主题混杂 | 已覆盖 | 可验证 MEETING_TOPIC |
| 档案层级 metadata | 部分覆盖 | 建议后续引入真实档案样例 |

结论：原样例集已经覆盖 60% - 70% 的常见基础分块问题，足够支撑 P0/P1 的第一轮开发和自动化回归。

### 5.2 专业样例仍需补充

从专业知识库/RAG 角度看，还应补充以下边界样例：

| 补充类型 | 原因 |
|---|---|
| YAML/front matter 元数据 | 版本、生效日期、状态、安全级别不能丢 |
| 同名标题 | 需要父级 sectionPath 区分 |
| 术语表短定义 | 短块不应被错误合并 |
| 嵌套列表 | 条件层级不能被打平 |
| 跨条款引用 | 命中引用条款后要回填被引用条款 |
| 附录引用 | 正文问题的答案可能在附录 |
| 无 QA 标签的隐式 FAQ | 标题即问题，正文即答案 |
| OCR 无标点长文本 | 标点分句策略可能失效 |
| 多级表头 | 统计口径和单位不能丢 |
| 树形表格 | 父行、子行关系要保留 |
| CSV 重复表头/页脚 | 导出噪声不能干扰检索 |
| 多表同字段 | “失败率”等同名字段必须带表名 |
| JSONL 日志 | 每行是原子事件 |
| OpenAPI $ref | schema 引用关系不能被截断 |
| SQL DDL 注释 | 字段、注释、约束、索引关系要保留 |
| HTML 导航噪声 | nav、aside、footer 应过滤 |
| 图片图注 | alt 文本和图注应作为证据 |
| 脚注尾注 | 关键例外经常写在脚注中 |
| 邮件线程 | 最新回复优先，历史引用去重 |
| 权限边界 | 扩展上下文不能越权 |
| 版本冲突 | 默认引用 current 版本 |

### 5.3 样例集分层建议

建议将样例集分为三层：

| 层级 | 样例集 | 用途 | 是否阻塞上线 |
|---|---|---|---|
| 第一层 | 基础主回归集 | 验证 C01-C09 基础分块问题 | P0/P1 阻塞 |
| 第二层 | 专业边界样例集 | 验证 metadata、权限、版本、脚注、图注、HTML、复杂表格等 | P1 增强，P2 阻塞 |
| 第三层 | 真实客户文件集 | 验证 DOCX/PDF/XLSX/PPTX/扫描件真实效果 | P2 阻塞 |

## 6. 关键验收标准建议

### 6.1 P0/P1 必须通过

| 指标 | 目标 |
|---|---:|
| 预览、上传、重解析结果一致性 | 100% |
| FAQ 完整率 | 100% |
| 代码块完整率 | 100% |
| 表头保留率 | 100% |
| 标题路径覆盖率 | >= 95% |
| Raw Evidence Recall@5 | >= 80% |
| Expanded Evidence Recall@5 | >= 90% |
| 上下文扩展越权率 | 0 |
| 当前版本引用准确率 | 100% |
| Q012 全部采购条件覆盖 | 100% |

### 6.2 P2/P3 再纳入

| 指标 | 目标 |
|---|---:|
| DOCX 标题结构保留率 | >= 95% |
| PDF 页码保留率 | >= 95% |
| HTML 正文抽取准确率 | >= 95% |
| Excel 工作表/表名保留率 | 100% |
| 脚注/尾注证据保留率 | >= 90% |
| 图注/alt 文本证据保留率 | >= 90% |
| 权限边界测试通过率 | 100% |
| Profile A/B 对比可用 | 100% |

## 7. 建议直接修改进设计文档的内容

### 7.1 增加样例集分层章节

建议在“样例集定位”后增加：

```text
样例集分为三层：
1. 主回归集：用于 P0/P1 阻塞验收。
2. 专业边界集：用于验证 metadata、权限、版本、脚注、图注、HTML 噪声、复杂表格等问题，一期作为增强测试，二期纳入阻塞验收。
3. 真实生产集：用于 DOCX、PDF、XLSX、PPTX、扫描 OCR 文件的二期验收。
```

### 7.2 增加权限扩展规则

建议在 KnowledgeContextAssembler 中增加：

```text
所有扩展候选块必须先经过权限过滤。
父块、同组块、邻居块不得因为扩展逻辑绕过知识库权限、文档权限、切片权限和业务系统数据权限。
```

### 7.3 增加 embeddingContentSnapshot

建议在 metadata_json 中增加：

```text
embeddingContentSnapshot
contentHash
embeddingContentHash
parserVersion
profileVersion
```

### 7.4 修改 P0 任务描述

将：

```text
建立 ChunkStrategy、NodeChunkStrategy 和节点级路由器。
```

改为：

```text
建立 NodeChunkStrategy、ChunkStrategyRouter 和 ChunkProfile 驱动的节点级路由机制，不再新增文档级 ChunkStrategy。
```

## 8. 最终评审意见

最终建议：**有条件通过。**

在进入开发前，建议先完成以下 5 项修订：

1. 消除文档级 ChunkStrategy 与节点级 NodeChunkStrategy 的表述冲突。
2. 明确权限过滤必须发生在父块、同组块、邻居块扩展之前。
3. 一期保存 embeddingContentSnapshot 和相关 hash。
4. 区分 logicalChunkId 与 chunkVersionId。
5. 增加专业边界样例集，并将其作为 P1 增强测试、P2 阻塞测试。

完成以上修订后，该方案可以作为 AI Workflow 企业知识库分块优化的开发依据。

## 9. 参考资料

- LangChain Text Splitters 文档：说明递归分块、结构型分块、Markdown header metadata 等常用策略。
- Unstructured Chunking 文档：说明 chunking 应基于 partition 后的文档元素与 metadata。
- LlamaIndex Auto Merging Retriever / HierarchicalNodeParser：说明层级节点、父子节点和自动回填思路。
- IBM RAG Chunking 资料：说明 fixed-size、recursive、semantic、document-based、agentic 等分块策略。
- Adaptive Chunking 相关研究：指出 one-size-fits-all 分块策略难以适配不同文档结构和语义特征。
