package com.mw.ai.agi.knowledge.vector;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunkVector;
import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.pgvector.PGvector;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PgvectorVectorStoreProvider implements VectorStoreProvider {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");
    private final PgvectorDataSourceRegistry dataSources;

    public PgvectorVectorStoreProvider(PgvectorDataSourceRegistry dataSources) {
        this.dataSources = dataSources;
    }

    @Override
    public String storeType() {
        return "PGVECTOR";
    }

    @Override
    public VectorStoreConnectionResult testConnection(VectorStoreConfig config) {
        Instant started = Instant.now();
        try (Connection connection = connection(config);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT version(), EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')"
             )) {
            result.next();
            if (!result.getBoolean(2)) {
                throw new IllegalStateException("Pgvector extension 'vector' is not installed");
            }
            return new VectorStoreConnectionResult(
                    true, storeType(), result.getString(1),
                    Duration.between(started, Instant.now()).toMillis(), "Connection successful"
            );
        } catch (SQLException exception) {
            throw failure("Unable to connect to Pgvector", exception);
        }
    }

    @Override
    public void ensureStore(VectorStoreConfig config, int dimensions) {
        requireDimensions(config, dimensions);
        String table = table(config);
        try (Connection connection = connection(config); Statement statement = connection.createStatement()) {
            if (!extensionInstalled(connection)) {
                throw new IllegalStateException("Pgvector extension 'vector' is not installed");
            }
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS %s (
                        chunk_id VARCHAR(100) PRIMARY KEY,
                        knowledge_base_id VARCHAR(100) NOT NULL,
                        dataset_id VARCHAR(100),
                        document_id VARCHAR(100) NOT NULL,
                        document_name TEXT,
                        content TEXT NOT NULL,
                        metadata_json TEXT NOT NULL DEFAULT '{}',
                        embedding vector(%d) NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """.formatted(table, dimensions));
            verifyDimensions(connection, table, dimensions);
            statement.execute("CREATE INDEX IF NOT EXISTS " + table + "_kb_idx ON " + table + " (knowledge_base_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS " + table + "_doc_idx ON " + table
                    + " (knowledge_base_id, document_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS " + table + "_dataset_idx ON " + table
                    + " (knowledge_base_id, dataset_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS " + table + "_embedding_hnsw_idx ON " + table
                    + " USING hnsw (embedding vector_cosine_ops)");
        } catch (SQLException exception) {
            throw failure("Unable to initialize Pgvector table", exception);
        }
    }

    @Override
    public void upsertChunk(VectorStoreConfig config, KnowledgeChunk chunk, KnowledgeChunkVector vector) {
        ensureStore(config, vector.embedding().size());
        String sql = """
                INSERT INTO %s (
                    chunk_id, knowledge_base_id, dataset_id, document_id, document_name,
                    content, metadata_json, embedding, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (chunk_id) DO UPDATE SET
                    knowledge_base_id = EXCLUDED.knowledge_base_id,
                    dataset_id = EXCLUDED.dataset_id,
                    document_id = EXCLUDED.document_id,
                    document_name = EXCLUDED.document_name,
                    content = EXCLUDED.content,
                    metadata_json = EXCLUDED.metadata_json,
                    embedding = EXCLUDED.embedding,
                    updated_at = CURRENT_TIMESTAMP
                """.formatted(table(config));
        try (Connection connection = connection(config); PreparedStatement statement = connection.prepareStatement(sql)) {
            PGvector.registerTypes(connection);
            statement.setString(1, chunk.id());
            statement.setString(2, chunk.knowledgeBaseId());
            statement.setString(3, chunk.datasetId());
            statement.setString(4, chunk.documentId());
            statement.setString(5, chunk.documentName());
            statement.setString(6, chunk.content());
            statement.setString(7, chunk.metadataJson() == null ? "{}" : chunk.metadataJson());
            statement.setObject(8, vector(vector.embedding()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Unable to write Pgvector chunk", exception);
        }
    }

    @Override
    public List<VectorStoreSearchHit> search(VectorStoreConfig config, VectorStoreSearchRequest request) {
        requireDimensions(config, request.queryEmbedding().size());
        StringBuilder sql = new StringBuilder("""
                SELECT chunk_id, document_id, document_name, content, dataset_id, metadata_json,
                       1 - (embedding <=> ?) AS score
                FROM %s
                WHERE knowledge_base_id = ?
                """.formatted(table(config)));
        if (request.datasetId() != null && !request.datasetId().isBlank()) {
            sql.append(" AND dataset_id = ?");
        }
        sql.append(" AND 1 - (embedding <=> ?) >= ? ORDER BY embedding <=> ? LIMIT ?");
        try (Connection connection = connection(config);
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            PGvector.registerTypes(connection);
            PGvector query = vector(request.queryEmbedding());
            int index = 1;
            statement.setObject(index++, query);
            statement.setString(index++, request.knowledgeBaseId());
            if (request.datasetId() != null && !request.datasetId().isBlank()) {
                statement.setString(index++, request.datasetId());
            }
            statement.setObject(index++, query);
            statement.setDouble(index++, request.similarityThreshold());
            statement.setObject(index++, query);
            statement.setInt(index, request.topK());
            List<VectorStoreSearchHit> hits = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    hits.add(new VectorStoreSearchHit(
                            result.getString("chunk_id"), result.getString("document_id"),
                            result.getString("document_name"), result.getString("content"),
                            result.getString("dataset_id"), result.getDouble("score"),
                            result.getString("metadata_json")
                    ));
                }
            }
            return hits;
        } catch (SQLException exception) {
            throw failure("Unable to search Pgvector", exception);
        }
    }

    @Override
    public void deleteDocument(VectorStoreConfig config, String knowledgeBaseId, String documentId) {
        executeDelete(config, "knowledge_base_id = ? AND document_id = ?", knowledgeBaseId, documentId);
    }

    @Override
    public void deleteKnowledgeBase(VectorStoreConfig config, String knowledgeBaseId) {
        executeDelete(config, "knowledge_base_id = ?", knowledgeBaseId);
    }

    @Override
    public void close(VectorStoreConfig config) {
        dataSources.close(config.id());
    }

    static String jdbcUrl(VectorStoreConfig config) {
        String host = required(config.host(), "Pgvector host");
        int port = config.port() == null ? 5432 : config.port();
        String database = required(config.databaseName(), "Pgvector database name");
        return "jdbc:postgresql://" + host + ":" + port + "/" + database
                + (config.sslEnabled() ? "?sslmode=require" : "");
    }

    static String validatedIdentifier(String value) {
        String identifier = required(value, "Vector table name");
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Unsafe vector store identifier: " + identifier);
        }
        return identifier;
    }

    private Connection connection(VectorStoreConfig config) throws SQLException {
        return dataSources.get(config).getConnection();
    }

    private String table(VectorStoreConfig config) {
        return validatedIdentifier(config.namespaceName() == null ? config.indexName() : config.namespaceName());
    }

    private boolean extensionInstalled(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')"
             )) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private void verifyDimensions(Connection connection, String table, int expected) throws SQLException {
        String sql = """
                SELECT format_type(a.atttypid, a.atttypmod)
                FROM pg_attribute a
                WHERE a.attrelid = ?::regclass AND a.attname = 'embedding' AND NOT a.attisdropped
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !("vector(" + expected + ")").equalsIgnoreCase(result.getString(1))) {
                    throw new IllegalStateException("Pgvector table embedding dimensions mismatch");
                }
            }
        }
    }

    private void executeDelete(VectorStoreConfig config, String where, String... values) {
        try (Connection connection = connection(config);
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM " + table(config) + " WHERE " + where
             )) {
            for (int index = 0; index < values.length; index++) {
                statement.setString(index + 1, values[index]);
            }
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Unable to delete Pgvector records", exception);
        }
    }

    private void requireDimensions(VectorStoreConfig config, int actual) {
        if (config.vectorDimension() != actual) {
            throw new IllegalArgumentException("Vector dimensions mismatch: configured "
                    + config.vectorDimension() + ", actual " + actual);
        }
    }

    private PGvector vector(List<Double> values) {
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index).floatValue();
        }
        return new PGvector(result);
    }

    private IllegalStateException failure(String message, SQLException exception) {
        return new IllegalStateException(message + ": " + exception.getMessage(), exception);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
