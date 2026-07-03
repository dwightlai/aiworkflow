# 切片器清理与语义阈值配置设计

## 目标

1. 完全移除已废弃的 `KnowledgeSplitter`，统一使用 `TokenCounter`、`KnowledgeDocumentSplitter` 和 `TokenWindowSplitter`。
2. 将语义相似度阈值作为知识库默认配置持久化，并允许文档上传时覆盖。

## 后端设计

- `KnowledgeBase` 增加 `semanticSimilarityThreshold`，默认值为 `0.78`，合法范围为 `0.00-1.00`。
- PostgreSQL、达梦数据库迁移脚本同步增加字段。
- 创建、编辑、查询知识库 API 返回并接收该字段。
- 文档预览、上传、手工新增和重新解析链路将知识库默认值写入 `KnowledgeSplitRequest`。
- 上传请求显式提供阈值时覆盖知识库默认值。
- 文档 `splitterConfig` 保存实际阈值，保证后续重新解析行为一致。
- Chunk Profile 的 `configJson` 继续承载可选阈值，Profile 预览时读取该值。

## 切片器清理

- 删除 `KnowledgeSplitter` 类。
- `KnowledgeBaseService` 使用 `TokenCounter` 计算父块、编辑块和合并块的 token 数。
- 删除 `KnowledgeDocumentSplitter(KnowledgeSplitter)` 兼容构造器。
- 测试统一通过真实 `HeuristicTokenCounter` 和显式 `EmbeddingClient` 构造。
- 不保留同名代理或兼容壳，避免重新形成双轨。

## 前端设计

- 知识库新增、编辑表单在选择 `SEMANTIC` 时显示“语义相似度阈值”。
- 文档上传向导在选择语义分段时显示同一字段，默认继承知识库配置。
- 控件使用 `InputNumber`，范围 `0-1`、步长 `0.01`、默认 `0.78`。
- 非语义策略隐藏该字段且不影响其他分段策略。
- API 类型、JSON 请求和 multipart 上传均传递该值。

## 验证

- 后端：默认值、范围归一化、数据库映射、请求覆盖、重新解析和 Profile 传递测试。
- 前端：条件显示、默认值和 API/multipart 参数测试。
- 回归：知识库切片测试、500 份业务样例、Maven 打包、前端生产构建。

## 非目标

- 不批量重新解析历史文档。
- 不修改现有历史文档的切片数据。
