package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeChunkVector;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcKnowledgeStore implements KnowledgeStore {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<Double>> DOUBLE_LIST = new TypeReference<>() {
    };

    public JdbcKnowledgeStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public KnowledgeBase saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        if (exists("knowledge_base", knowledgeBase.id())) {
            jdbcTemplate.update("""
                            UPDATE knowledge_base
                            SET name = ?, description = ?, embedding_model_id = ?, vector_store_config_id = ?,
                                vector_dimension = ?, splitter_type = ?, chunk_size = ?, chunk_overlap = ?, retrieval_mode = ?, top_k = ?,
                                status = ?, document_count = ?, chunk_count = ?, created_at = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    knowledgeBase.name(),
                    knowledgeBase.description(),
                    knowledgeBase.embeddingModelId(),
                    knowledgeBase.vectorStoreConfigId(),
                    knowledgeBase.vectorDimension(),
                    knowledgeBase.splitterType(),
                    knowledgeBase.chunkSize(),
                    knowledgeBase.chunkOverlap(),
                    knowledgeBase.retrievalMode(),
                    knowledgeBase.topK(),
                    knowledgeBase.status(),
                    knowledgeBase.documentCount(),
                    knowledgeBase.chunkCount(),
                    Timestamp.from(knowledgeBase.createdAt()),
                    Timestamp.from(knowledgeBase.updatedAt()),
                    knowledgeBase.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO knowledge_base
                                (id, name, description, embedding_model_id, vector_store_config_id, vector_dimension, splitter_type,
                                 chunk_size, chunk_overlap, retrieval_mode, top_k, status, document_count, chunk_count,
                                 created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    knowledgeBase.id(),
                    knowledgeBase.name(),
                    knowledgeBase.description(),
                    knowledgeBase.embeddingModelId(),
                    knowledgeBase.vectorStoreConfigId(),
                    knowledgeBase.vectorDimension(),
                    knowledgeBase.splitterType(),
                    knowledgeBase.chunkSize(),
                    knowledgeBase.chunkOverlap(),
                    knowledgeBase.retrievalMode(),
                    knowledgeBase.topK(),
                    knowledgeBase.status(),
                    knowledgeBase.documentCount(),
                    knowledgeBase.chunkCount(),
                    Timestamp.from(knowledgeBase.createdAt()),
                    Timestamp.from(knowledgeBase.updatedAt())
            );
        }
        return knowledgeBase;
    }

    @Override
    public Optional<KnowledgeBase> findKnowledgeBaseById(String id) {
        return jdbcTemplate.query("SELECT * FROM knowledge_base WHERE id = ?", knowledgeBaseMapper(), id)
                .stream()
                .findFirst();
    }

    @Override
    public List<KnowledgeBase> listKnowledgeBases() {
        return jdbcTemplate.query("SELECT * FROM knowledge_base ORDER BY created_at", knowledgeBaseMapper());
    }

    @Override
    public void deleteKnowledgeBase(String id) {
        deleteChunkVectors(id);
        deleteChunks(id);
        deleteDocuments(id);
        jdbcTemplate.update("DELETE FROM knowledge_base WHERE id = ?", id);
    }

    @Override
    public KnowledgeDocument saveDocument(KnowledgeDocument document) {
        if (exists("knowledge_document", document.id())) {
            jdbcTemplate.update("""
                            UPDATE knowledge_document
                            SET knowledge_base_id = ?, name = ?, chunk_count = ?, created_at = ?
                            WHERE id = ?
                            """,
                    document.knowledgeBaseId(),
                    document.name(),
                    document.chunkCount(),
                    Timestamp.from(document.createdAt()),
                    document.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO knowledge_document (id, knowledge_base_id, name, chunk_count, created_at)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    document.id(),
                    document.knowledgeBaseId(),
                    document.name(),
                    document.chunkCount(),
                    Timestamp.from(document.createdAt())
            );
        }
        return document;
    }

    @Override
    public List<KnowledgeDocument> listDocuments(String knowledgeBaseId) {
        return jdbcTemplate.query(
                "SELECT * FROM knowledge_document WHERE knowledge_base_id = ? ORDER BY created_at",
                documentMapper(),
                knowledgeBaseId
        );
    }

    @Override
    public void deleteDocuments(String knowledgeBaseId) {
        jdbcTemplate.update("DELETE FROM knowledge_document WHERE knowledge_base_id = ?", knowledgeBaseId);
    }

    @Override
    public void deleteDocument(String knowledgeBaseId, String documentId) {
        jdbcTemplate.update("DELETE FROM knowledge_document WHERE knowledge_base_id = ? AND id = ?", knowledgeBaseId, documentId);
    }

    @Override
    public KnowledgeChunk saveChunk(KnowledgeChunk chunk) {
        if (exists("knowledge_chunk", chunk.id())) {
            jdbcTemplate.update("""
                            UPDATE knowledge_chunk
                            SET knowledge_base_id = ?, document_id = ?, document_name = ?, content = ?,
                                chunk_index = ?, enabled = ?, token_estimate = ?
                            WHERE id = ?
                            """,
                    chunk.knowledgeBaseId(),
                    chunk.documentId(),
                    chunk.documentName(),
                    chunk.content(),
                    chunk.index(),
                    chunk.enabled(),
                    chunk.tokenEstimate(),
                    chunk.id()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO knowledge_chunk
                                (id, knowledge_base_id, document_id, document_name, content, chunk_index, enabled, token_estimate)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    chunk.id(),
                    chunk.knowledgeBaseId(),
                    chunk.documentId(),
                    chunk.documentName(),
                    chunk.content(),
                    chunk.index(),
                    chunk.enabled(),
                    chunk.tokenEstimate()
            );
        }
        return chunk;
    }

    @Override
    public List<KnowledgeChunk> listChunks(String knowledgeBaseId, String documentId) {
        return jdbcTemplate.query(
                "SELECT * FROM knowledge_chunk WHERE knowledge_base_id = ? AND document_id = ? ORDER BY chunk_index",
                chunkMapper(),
                knowledgeBaseId,
                documentId
        );
    }

    @Override
    public List<KnowledgeChunk> listChunks(String knowledgeBaseId) {
        return jdbcTemplate.query(
                "SELECT * FROM knowledge_chunk WHERE knowledge_base_id = ? ORDER BY document_name, chunk_index",
                chunkMapper(),
                knowledgeBaseId
        );
    }

    @Override
    public void deleteChunks(String knowledgeBaseId) {
        deleteChunkVectors(knowledgeBaseId);
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE knowledge_base_id = ?", knowledgeBaseId);
    }

    @Override
    public void deleteChunks(String knowledgeBaseId, String documentId) {
        deleteChunkVectors(knowledgeBaseId, documentId);
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE knowledge_base_id = ? AND document_id = ?", knowledgeBaseId, documentId);
    }

    @Override
    public KnowledgeChunkVector saveChunkVector(KnowledgeChunkVector vector) {
        if (exists("knowledge_chunk_vector", vector.chunkId(), "chunk_id")) {
            jdbcTemplate.update("""
                            UPDATE knowledge_chunk_vector
                            SET knowledge_base_id = ?, document_id = ?, embedding_model_id = ?, embedding = ?, created_at = ?
                            WHERE chunk_id = ?
                            """,
                    vector.knowledgeBaseId(),
                    vector.documentId(),
                    vector.embeddingModelId(),
                    serialize(vector.embedding()),
                    Timestamp.from(vector.createdAt()),
                    vector.chunkId()
            );
        } else {
            jdbcTemplate.update("""
                            INSERT INTO knowledge_chunk_vector
                                (chunk_id, knowledge_base_id, document_id, embedding_model_id, embedding, created_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    vector.chunkId(),
                    vector.knowledgeBaseId(),
                    vector.documentId(),
                    vector.embeddingModelId(),
                    serialize(vector.embedding()),
                    Timestamp.from(vector.createdAt())
            );
        }
        return vector;
    }

    @Override
    public List<KnowledgeChunkVector> listChunkVectors(String knowledgeBaseId) {
        return jdbcTemplate.query(
                "SELECT * FROM knowledge_chunk_vector WHERE knowledge_base_id = ?",
                chunkVectorMapper(),
                knowledgeBaseId
        );
    }

    @Override
    public void deleteChunkVectors(String knowledgeBaseId) {
        jdbcTemplate.update("DELETE FROM knowledge_chunk_vector WHERE knowledge_base_id = ?", knowledgeBaseId);
    }

    @Override
    public void deleteChunkVectors(String knowledgeBaseId, String documentId) {
        jdbcTemplate.update("DELETE FROM knowledge_chunk_vector WHERE knowledge_base_id = ? AND document_id = ?", knowledgeBaseId, documentId);
    }

    private boolean exists(String tableName, String id) {
        return exists(tableName, id, "id");
    }

    private boolean exists(String tableName, String id, String idColumn) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + idColumn + " = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<KnowledgeBase> knowledgeBaseMapper() {
        return (rs, rowNum) -> new KnowledgeBase(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("embedding_model_id"),
                rs.getString("vector_store_config_id"),
                rs.getInt("vector_dimension"),
                rs.getString("splitter_type"),
                rs.getInt("chunk_size"),
                rs.getInt("chunk_overlap"),
                rs.getString("retrieval_mode"),
                rs.getInt("top_k"),
                rs.getString("status"),
                rs.getInt("document_count"),
                rs.getInt("chunk_count"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<KnowledgeDocument> documentMapper() {
        return (rs, rowNum) -> new KnowledgeDocument(
                rs.getString("id"),
                rs.getString("knowledge_base_id"),
                rs.getString("name"),
                rs.getInt("chunk_count"),
                instant(rs, "created_at")
        );
    }

    private RowMapper<KnowledgeChunk> chunkMapper() {
        return (rs, rowNum) -> new KnowledgeChunk(
                rs.getString("id"),
                rs.getString("knowledge_base_id"),
                rs.getString("document_id"),
                rs.getString("document_name"),
                rs.getString("content"),
                rs.getInt("chunk_index"),
                rs.getBoolean("enabled"),
                rs.getInt("token_estimate")
        );
    }

    private RowMapper<KnowledgeChunkVector> chunkVectorMapper() {
        return (rs, rowNum) -> new KnowledgeChunkVector(
                rs.getString("chunk_id"),
                rs.getString("knowledge_base_id"),
                rs.getString("document_id"),
                rs.getString("embedding_model_id"),
                deserialize(rs.getString("embedding")),
                instant(rs, "created_at")
        );
    }

    private String serialize(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize embedding", exception);
        }
    }

    private List<Double> deserialize(String value) {
        try {
            return objectMapper.readValue(value, DOUBLE_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to deserialize embedding", exception);
        }
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
