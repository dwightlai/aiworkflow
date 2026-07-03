package com.mw.ai.agi.knowledge.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class MilvusVectorStoreProvider implements VectorStoreProvider {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");
    private final Map<String, ClientEntry> clients = new ConcurrentHashMap<>();

    @Override
    public String storeType() {
        return "MILVUS";
    }

    @Override
    public VectorStoreConnectionResult testConnection(VectorStoreConfig config) {
        Instant started = Instant.now();
        MilvusClientV2 client = client(config);
        try {
            client.checkHealth();
            return new VectorStoreConnectionResult(
                    true, storeType(), client.getServerVersion(),
                    Duration.between(started, Instant.now()).toMillis(), "Connection successful"
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unable to connect to Milvus: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void ensureStore(VectorStoreConfig config, int dimensions) {
        requireDimensions(config, dimensions);
        MilvusClientV2 client = client(config);
        String collection = collection(config);
        String database = database(config);
        boolean exists = client.hasCollection(HasCollectionReq.builder()
                .databaseName(database).collectionName(collection).build());
        if (exists) {
            DescribeCollectionResp description = client.describeCollection(
                    DescribeCollectionReq.builder()
                            .databaseName(database).collectionName(collection).build()
            );
            Integer actual = description.getCollectionSchema().getField("embedding").getDimension();
            if (actual == null || actual != dimensions) {
                throw new IllegalStateException("Milvus collection embedding dimensions mismatch: current "
                        + actual + ", configured " + dimensions);
            }
            loadCollection(client, database, collection);
            return;
        }
        CreateCollectionReq.CollectionSchema schema = client.createSchema();
        schema.addField(field("chunk_id", DataType.VarChar, 100, true, null));
        schema.addField(field("knowledge_base_id", DataType.VarChar, 100, false, null));
        schema.addField(field("dataset_id", DataType.VarChar, 100, false, null));
        schema.addField(field("document_id", DataType.VarChar, 100, false, null));
        schema.addField(field("document_name", DataType.VarChar, 2048, false, null));
        schema.addField(field("content", DataType.VarChar, 65535, false, null));
        schema.addField(field("metadata_json", DataType.VarChar, 65535, false, null));
        schema.addField(field("updated_at", DataType.Int64, null, false, null));
        schema.addField(field("embedding", DataType.FloatVector, null, false, dimensions));
        IndexParam index = IndexParam.builder()
                .fieldName("embedding")
                .indexName("embedding_cosine_idx")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.COSINE)
                .build();
        client.createCollection(CreateCollectionReq.builder()
                .databaseName(database)
                .collectionName(collection)
                .collectionSchema(schema)
                .indexParams(List.of(index))
                .build());
        loadCollection(client, database, collection);
    }

    @Override
    public void upsertChunk(VectorStoreConfig config, KnowledgeChunk chunk, KnowledgeChunkVector vector) {
        ensureStore(config, vector.embedding().size());
        JsonObject row = new JsonObject();
        row.addProperty("chunk_id", chunk.id());
        row.addProperty("knowledge_base_id", chunk.knowledgeBaseId());
        row.addProperty("dataset_id", value(chunk.datasetId()));
        row.addProperty("document_id", chunk.documentId());
        row.addProperty("document_name", value(chunk.documentName()));
        row.addProperty("content", chunk.content());
        row.addProperty("metadata_json", value(chunk.metadataJson(), "{}"));
        row.addProperty("updated_at", System.currentTimeMillis());
        JsonArray embedding = new JsonArray();
        vector.embedding().forEach(embedding::add);
        row.add("embedding", embedding);
        client(config).upsert(UpsertReq.builder()
                .databaseName(database(config))
                .collectionName(collection(config))
                .data(List.of(row))
                .build());
    }

    @Override
    public List<VectorStoreSearchHit> search(VectorStoreConfig config, VectorStoreSearchRequest request) {
        requireDimensions(config, request.queryEmbedding().size());
        String filter = "knowledge_base_id == " + quoted(request.knowledgeBaseId());
        if (request.datasetId() != null && !request.datasetId().isBlank()) {
            filter += " && dataset_id == " + quoted(request.datasetId());
        }
        SearchResp response = client(config).search(SearchReq.builder()
                .databaseName(database(config))
                .collectionName(collection(config))
                .annsField("embedding")
                .metricType(IndexParam.MetricType.COSINE)
                .topK(request.topK())
                .filter(filter)
                .outputFields(List.of(
                        "chunk_id", "document_id", "document_name", "content",
                        "dataset_id", "metadata_json"
                ))
                .data(List.of(new FloatVec(floats(request.queryEmbedding()))))
                .build());
        List<VectorStoreSearchHit> hits = new ArrayList<>();
        if (response.getSearchResults().isEmpty()) {
            return hits;
        }
        for (SearchResp.SearchResult result : response.getSearchResults().get(0)) {
            double score = result.getScore();
            if (score < request.similarityThreshold()) {
                continue;
            }
            Map<String, Object> entity = result.getEntity();
            hits.add(new VectorStoreSearchHit(
                    string(entity, "chunk_id"), string(entity, "document_id"),
                    string(entity, "document_name"), string(entity, "content"),
                    string(entity, "dataset_id"), score, string(entity, "metadata_json")
            ));
        }
        return hits;
    }

    @Override
    public void deleteDocument(VectorStoreConfig config, String knowledgeBaseId, String documentId) {
        delete(config, "knowledge_base_id == " + quoted(knowledgeBaseId)
                + " && document_id == " + quoted(documentId));
    }

    @Override
    public void deleteKnowledgeBase(VectorStoreConfig config, String knowledgeBaseId) {
        delete(config, "knowledge_base_id == " + quoted(knowledgeBaseId));
    }

    @Override
    public void close(VectorStoreConfig config) {
        ClientEntry entry = clients.remove(config.id());
        if (entry != null) {
            entry.client().close();
        }
    }

    static String uri(VectorStoreConfig config) {
        String host = required(config.host(), "Milvus host");
        int port = config.port() == null ? 19530 : config.port();
        return (config.sslEnabled() ? "https://" : "http://") + host + ":" + port;
    }

    static String validatedCollection(String value) {
        String collection = required(value, "Milvus collection name");
        if (!IDENTIFIER.matcher(collection).matches()) {
            throw new IllegalArgumentException("Unsafe vector store identifier: " + collection);
        }
        return collection;
    }

    static String quoted(String value) {
        String escaped = value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private MilvusClientV2 client(VectorStoreConfig config) {
        String signature = uri(config) + "|" + database(config) + "|" + config.username()
                + "|" + config.password() + "|" + config.apiKey();
        ClientEntry current = clients.get(config.id());
        if (current != null && current.signature().equals(signature)) {
            return current.client();
        }
        close(config);
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri(uri(config))
                .dbName(database(config))
                .secure(config.sslEnabled())
                .connectTimeoutMs(config.connectTimeoutMs())
                .rpcDeadlineMs(config.readTimeoutMs());
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            builder.token(config.apiKey());
        } else {
            builder.username(value(config.username())).password(value(config.password()));
        }
        MilvusClientV2 client = new MilvusClientV2(builder.build());
        clients.put(config.id(), new ClientEntry(signature, client));
        return client;
    }

    private AddFieldReq field(
            String name, DataType type, Integer maxLength, boolean primary, Integer dimension
    ) {
        AddFieldReq.AddFieldReqBuilder<?> builder = AddFieldReq.builder()
                .fieldName(name).dataType(type).isPrimaryKey(primary).autoID(false);
        if (maxLength != null) {
            builder.maxLength(maxLength);
        }
        if (dimension != null) {
            builder.dimension(dimension);
        }
        return builder.build();
    }

    private void delete(VectorStoreConfig config, String filter) {
        client(config).delete(DeleteReq.builder()
                .databaseName(database(config))
                .collectionName(collection(config))
                .filter(filter)
                .build());
    }

    private void loadCollection(MilvusClientV2 client, String database, String collection) {
        client.loadCollection(LoadCollectionReq.builder()
                .databaseName(database)
                .collectionName(collection)
                .sync(true)
                .build());
    }

    private String collection(VectorStoreConfig config) {
        return validatedCollection(config.namespaceName() == null ? config.indexName() : config.namespaceName());
    }

    private String database(VectorStoreConfig config) {
        return config.databaseName() == null || config.databaseName().isBlank()
                ? "default" : config.databaseName().trim();
    }

    private void requireDimensions(VectorStoreConfig config, int actual) {
        if (config.vectorDimension() != actual) {
            throw new IllegalArgumentException("Vector dimensions mismatch: configured "
                    + config.vectorDimension() + ", actual " + actual);
        }
    }

    private List<Float> floats(List<Double> values) {
        return values.stream().map(Double::floatValue).toList();
    }

    private String string(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String value(String value) {
        return value(value, "");
    }

    private String value(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private record ClientEntry(String signature, MilvusClientV2 client) {
    }
}
