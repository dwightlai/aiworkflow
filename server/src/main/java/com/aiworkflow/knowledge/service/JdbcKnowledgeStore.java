package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
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

    public JdbcKnowledgeStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public KnowledgeBase saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        if (exists("knowledge_base", knowledgeBase.id())) {
            jdbcTemplate.update("""
                            UPDATE knowledge_base
                            SET name = ?, description = ?, embedding_model_id = ?, vector_store_config_id = ?,
                                splitter_type = ?, chunk_size = ?, chunk_overlap = ?, retrieval_mode = ?, top_k = ?,
                                status = ?, document_count = ?, chunk_count = ?, created_at = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    knowledgeBase.name(),
                    knowledgeBase.description(),
                    knowledgeBase.embeddingModelId(),
                    knowledgeBase.vectorStoreConfigId(),
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
                                (id, name, description, embedding_model_id, vector_store_config_id, splitter_type,
                                 chunk_size, chunk_overlap, retrieval_mode, top_k, status, document_count, chunk_count,
                                 created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    knowledgeBase.id(),
                    knowledgeBase.name(),
                    knowledgeBase.description(),
                    knowledgeBase.embeddingModelId(),
                    knowledgeBase.vectorStoreConfigId(),
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
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE knowledge_base_id = ?", knowledgeBaseId);
    }

    @Override
    public void deleteChunks(String knowledgeBaseId, String documentId) {
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE knowledge_base_id = ? AND document_id = ?", knowledgeBaseId, documentId);
    }

    private boolean exists(String tableName, String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<KnowledgeBase> knowledgeBaseMapper() {
        return (rs, rowNum) -> new KnowledgeBase(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("embedding_model_id"),
                rs.getString("vector_store_config_id"),
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

    private Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
