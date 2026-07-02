# Milvus 与 Pgvector 向量库接入设计

## 1. 目标

在现有 Memory、Elasticsearch 向量存储能力基础上，增加 Milvus 和 Pgvector 两种生产可用的外部向量库，并保证知识库选择对应配置后，文档向量写入、相似度检索、文档删除、知识库删除和重解析均实际路由到所选向量库。

本次实现不是单纯增加前端选项。配置、连接校验、存储初始化、运行时路由、异常处理和测试必须形成完整闭环。

## 2. 范围

### 2.1 本期包含

- Milvus 配置新增、编辑、删除、启停和连接测试。
- Pgvector 独立 PostgreSQL 实例配置新增、编辑、删除、启停和连接测试。
- 向量库初始化：
  - Elasticsearch 创建索引。
  - Milvus 创建并加载集合及向量索引。
  - Pgvector检查扩展、创建向量表及 HNSW 索引。
- 文档分块向量的新增或覆盖写入。
- 按知识库、数据集进行向量相似度检索。
- 按文档删除向量。
- 按知识库删除向量。
- 文档重解析时删除旧向量并写入新向量。
- 嵌入模型维度与向量库维度一致性校验。
- 管理端按向量库类型动态显示配置表单。

### 2.2 本期不包含

- Pgvector 复用 AGI 主业务数据库。
- 向量库之间自动迁移历史数据。
- Milvus 分区自动伸缩、资源组和多副本运维。
- Pgvector 分片、Citus 或跨数据库联合检索。
- 外部密钥管理系统改造。密码和令牌沿用现有秘密字段保存方式。

## 3. 技术选择

### 3.1 统一 Provider 适配层

新增统一的 `VectorStoreProvider` 契约。知识库业务服务不再直接依赖 `ElasticsearchVectorStoreClient`，而是通过 `VectorStoreProviderRegistry` 按 `storeType` 获取实现。

支持的类型：

| 类型 | Provider | 运行位置 |
| --- | --- | --- |
| `ELASTICSEARCH` | `ElasticsearchVectorStoreProvider` | 外部 Elasticsearch |
| `MILVUS` | `MilvusVectorStoreProvider` | 独立 Milvus |
| `PGVECTOR` | `PgvectorVectorStoreProvider` | 独立 PostgreSQL + pgvector |

`MEMORY` 继续使用项目现有本地向量记录和余弦计算，不作为外部 Provider。

### 3.2 Milvus 客户端

使用 Milvus 官方 Java SDK v2 API，目标兼容 Milvus 2.5/2.6。集合使用显式 Schema、`FLOAT_VECTOR`、COSINE 距离和 AUTOINDEX。

参考：

- [Milvus Java SDK](https://github.com/milvus-io/milvus-sdk-java)
- [Milvus 创建集合](https://milvus.io/docs/create-collection.md)

### 3.3 Pgvector 客户端

使用 PostgreSQL JDBC 驱动和 pgvector Java 类型支持。每个 Pgvector 配置创建独立 Hikari 数据源，不复用 AGI 主库连接池。数据源按配置 ID 缓存；配置更新或删除时关闭旧连接池。

向量检索使用 cosine distance：

```sql
1 - (embedding <=> CAST(? AS vector))
```

参考：

- [pgvector](https://github.com/pgvector/pgvector)
- [pgvector-java](https://github.com/pgvector/pgvector-java)

## 4. 配置模型

在 `agi_vector_store_config` 增加以下字段，并同时维护 PostgreSQL 与达梦迁移：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `host` | VARCHAR(500) | Milvus/Pgvector 主机；ES 可继续使用 endpoint |
| `port` | INTEGER | Milvus 默认 19530，Pgvector 默认 5432 |
| `database_name` | VARCHAR(200) | Milvus 数据库或 Pgvector 数据库 |
| `namespace_name` | VARCHAR(200) | ES 索引、Milvus 集合或 Pgvector 表名 |
| `vector_dimension` | INTEGER | 向量维度，必须大于 0 |
| `ssl_enabled` | BOOLEAN | 是否启用 TLS/SSL |
| `options_json` | TEXT | 经过白名单解析的扩展配置 |

现有 `endpoint` 和 `index_name` 保留兼容。服务层读取时将旧 `index_name` 映射为 `namespaceName`，避免已有 Elasticsearch 配置失效。

### 4.1 类型字段规则

#### Elasticsearch

- 必填：配置名称、Endpoint、索引名称、向量维度。
- 可选：用户名、密码、API Key、超时。

#### Milvus

- 必填：配置名称、主机、端口、数据库名、集合名、向量维度。
- 可选：用户名、密码或 Token、TLS、超时。
- 默认端口：`19530`。
- 默认数据库：`default`。

#### Pgvector

- 必填：配置名称、主机、端口、数据库名、向量表名、用户名、密码、向量维度。
- 可选：SSL、连接和读取超时。
- 默认端口：`5432`。
- Pgvector 仅连接独立部署的 PostgreSQL 实例。

### 4.2 名称安全

Milvus 集合名和 Pgvector 表名必须满足：

```text
^[A-Za-z_][A-Za-z0-9_]{0,62}$
```

动态标识符只能来自校验后的配置，不允许直接拼接未经校验的请求值。普通数据值全部使用参数化语句。

## 5. Provider 契约

```java
public interface VectorStoreProvider {
    String storeType();

    VectorStoreConnectionResult testConnection(VectorStoreConfig config);

    void ensureStore(VectorStoreConfig config, int dimensions);

    void upsertChunk(
            VectorStoreConfig config,
            KnowledgeChunk chunk,
            KnowledgeChunkVector vector
    );

    List<VectorStoreSearchHit> search(
            VectorStoreConfig config,
            VectorSearchRequest request
    );

    void deleteDocument(
            VectorStoreConfig config,
            String knowledgeBaseId,
            String documentId
    );

    void deleteKnowledgeBase(
            VectorStoreConfig config,
            String knowledgeBaseId
    );

    void close(VectorStoreConfig config);
}
```

`VectorSearchRequest` 统一承载：

- `knowledgeBaseId`
- `datasetId`
- `queryEmbedding`
- `topK`
- `similarityThreshold`

`VectorStoreSearchHit` 统一返回：

- `chunkId`
- `documentId`
- `datasetId`
- `score`
- `content`
- `metadata`

Provider 不负责生成 Embedding，也不负责业务权限判断。Embedding 和授权过滤仍由知识库服务控制。

## 6. Milvus 数据模型

每个配置使用一个集合。集合包含：

| 字段 | Milvus 类型 | 说明 |
| --- | --- | --- |
| `chunk_id` | VARCHAR，主键 | 分块 ID |
| `knowledge_base_id` | VARCHAR | 知识库 ID |
| `dataset_id` | VARCHAR | 数据集 ID，可为空字符串 |
| `document_id` | VARCHAR | 文档 ID |
| `content` | VARCHAR | 分块文本 |
| `metadata_json` | VARCHAR | 分块元数据 |
| `embedding` | FLOAT_VECTOR | 指定维度向量 |
| `updated_at` | INT64 | 更新时间戳 |

索引：

- `embedding`：AUTOINDEX + COSINE。
- 业务过滤字段依赖 Milvus 标量过滤。

写入使用 chunk ID 作为幂等主键并执行 upsert。搜索表达式必须包含 `knowledge_base_id`，存在数据集 ID 时追加 `dataset_id`。删除使用同样的标量过滤表达式。

集合存在时校验向量维度。维度不一致时拒绝启用配置或写入，不自动删除重建集合。

## 7. Pgvector 数据模型

每个配置使用一个独立向量表：

```sql
CREATE TABLE <validated_table_name> (
    chunk_id VARCHAR(100) PRIMARY KEY,
    knowledge_base_id VARCHAR(100) NOT NULL,
    dataset_id VARCHAR(100),
    document_id VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    embedding vector(<configured_dimension>) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

索引：

```sql
CREATE INDEX <table>_kb_idx
    ON <table> (knowledge_base_id);

CREATE INDEX <table>_doc_idx
    ON <table> (knowledge_base_id, document_id);

CREATE INDEX <table>_dataset_idx
    ON <table> (knowledge_base_id, dataset_id);

CREATE INDEX <table>_embedding_hnsw_idx
    ON <table> USING hnsw (embedding vector_cosine_ops);
```

初始化流程：

1. 建立独立 JDBC 连接。
2. 执行 `SELECT extversion FROM pg_extension WHERE extname = 'vector'`。
3. 若扩展不存在，返回明确错误；应用不自动执行 `CREATE EXTENSION`，避免要求超级用户权限。
4. 创建向量表和索引。
5. 已存在表时读取 `format_type` 校验向量维度。

写入使用：

```sql
INSERT ... ON CONFLICT (chunk_id) DO UPDATE ...
```

检索按知识库和可选数据集过滤，按 cosine distance 排序，并转换为 `0..1` 相似度分数。

## 8. 配置生命周期

### 8.1 新增或更新

1. 校验字段和命名规则。
2. 构造尚未持久化的配置对象。
3. 测试连接。
4. 启用状态下执行 `ensureStore`。
5. 成功后保存配置。
6. 更新配置时关闭该配置对应的旧客户端或连接池。

连接或初始化失败时不保存配置，并向管理端返回可操作错误，例如：

- Milvus 无法连接或认证失败。
- Milvus 集合维度与配置不一致。
- Pgvector 数据库不存在。
- Pgvector 扩展未安装。
- Pgvector 用户无建表或建索引权限。

### 8.2 删除

删除配置前检查是否仍被知识库引用。存在引用时拒绝删除并返回引用数量。删除配置不默认删除外部集合或表，防止误删生产数据。

### 8.3 连接测试

新增：

```http
POST /api/vector-store-configs/test-connection
POST /api/vector-store-configs/{id}/test-connection
```

未保存配置使用请求体测试；已保存配置可复用已保存秘密。响应包含：

- `success`
- `storeType`
- `serverVersion`
- `latencyMs`
- `message`

## 9. 知识库运行时路由

`KnowledgeBaseService` 的以下路径全部改为 Provider 路由：

- 文档首次处理后的向量写入。
- 异步处理完成后的向量写入。
- 文档重解析前删除旧向量。
- 删除文档。
- 删除知识库。
- VECTOR 和 HYBRID 模式相似度检索。

路由规则：

1. 未配置外部向量库或类型为 MEMORY：使用本地向量记录。
2. 配置未启用：拒绝向量写入和向量检索，并返回配置已停用错误。
3. 配置类型没有 Provider：拒绝执行，不回退到 Elasticsearch 或 Memory。
4. 外部 Provider 搜索失败：VECTOR 模式直接失败；HYBRID 模式记录错误并按系统现有降级策略决定是否保留关键词结果。

严禁配置为 Milvus/Pgvector 时仍调用 Elasticsearch。

## 10. 一致性与重试

本地文档、分块和向量记录是业务事实来源，外部向量库是可重建索引。

写入顺序：

1. 保存文档和分块。
2. 生成并保存本地向量记录。
3. 写入所选外部向量库。
4. 全部成功后将处理任务标记为成功。

外部写入失败：

- 当前处理任务标记为失败。
- 保存错误类型和消息。
- 允许从本地记录重新执行向量同步。
- 不返回“处理成功”。

删除采用幂等语义。外部不存在对应记录时视为成功。

## 11. 管理端交互

向量库类型选项：

- Memory
- Elasticsearch
- Milvus
- Pgvector

表单按类型动态展示，避免一个表单堆叠全部字段。

Milvus 字段：

- 配置名称
- 主机
- 端口
- 数据库名
- 集合名
- 用户名
- 密码或 Token
- 向量维度
- TLS
- 连接/读取超时
- 状态

Pgvector 字段：

- 配置名称
- 主机
- 端口
- 数据库名
- 向量表名
- 用户名
- 密码
- 向量维度
- SSL
- 连接/读取超时
- 状态

操作：

- 保存配置
- 测试连接
- 编辑
- 删除

秘密字段编辑时留空表示保留原值。列表不返回密码或 Token，只返回是否已配置。

## 12. 数据库兼容

AGI 自身配置表迁移必须同时覆盖：

- PostgreSQL Flyway。
- 达梦 Flyway。

Pgvector 是独立外部 PostgreSQL 服务，其向量表初始化 SQL 不通过 AGI 主库 Flyway 执行。

## 13. 测试策略

### 13.1 单元测试

- Provider Registry 按类型正确选择实现。
- 未注册类型明确失败。
- 名称白名单校验。
- 配置字段默认值和秘密保留。
- 知识库选择 Milvus 后只调用 Milvus Provider。
- 知识库选择 Pgvector 后只调用 Pgvector Provider。
- 写入、检索、文档删除和知识库删除路由。
- 维度不一致拒绝执行。
- Pgvector SQL 参数和过滤条件。
- Milvus 标量过滤表达式转义。

### 13.2 API 测试

- 新增和编辑 Milvus/Pgvector 配置。
- 连接测试成功及失败响应。
- 配置列表隐藏秘密。
- 被知识库引用的配置不可删除。

### 13.3 前端测试

- 类型选项包含 Milvus、Pgvector。
- 切换类型后字段正确显示。
- 默认端口和数据库名正确。
- 测试连接请求及反馈。
- 保存请求字段正确。

### 13.4 集成测试

提供可选 Testcontainers 测试：

- Milvus Standalone：建集合、upsert、search、delete。
- PostgreSQL + pgvector：建表、upsert、search、delete。

没有 Docker 的开发环境仍可运行全部单元测试和前端测试。

## 14. 验收标准

1. 管理端可创建、编辑和测试 Milvus 配置。
2. 管理端可创建、编辑和测试独立 Pgvector 配置。
3. 新建知识库选择 Milvus 后，上传文档会在指定集合产生向量记录。
4. 新建知识库选择 Pgvector 后，上传文档会在指定表产生向量记录。
5. VECTOR/HYBRID 检索能从所选后端返回正确分块。
6. 删除文档、删除知识库和重解析会同步处理所选后端向量。
7. 类型路由错误时不得静默回退到其他向量库。
8. 配置维度与嵌入模型维度不一致时有明确提示且禁止处理。
9. PostgreSQL 和达梦主库模式均能保存新增配置字段。
10. 后端测试、前端测试和构建全部通过。
