package com.mw.ai.agi.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_knowledge_base")
public class KnowledgeBaseEntity {
    @TableId
    private String id;
    private String name;
    private String description;
    private String ownerUnitId;
    private String embeddingModelId;
    private String vectorStoreConfigId;
    private Integer vectorDimension;
    private String splitterType;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private String retrievalMode;
    private Integer topK;
    private String status;
    private Integer documentCount;
    private Integer chunkCount;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerUnitId() { return ownerUnitId; }
    public void setOwnerUnitId(String ownerUnitId) { this.ownerUnitId = ownerUnitId; }
    public String getEmbeddingModelId() { return embeddingModelId; }
    public void setEmbeddingModelId(String embeddingModelId) { this.embeddingModelId = embeddingModelId; }
    public String getVectorStoreConfigId() { return vectorStoreConfigId; }
    public void setVectorStoreConfigId(String vectorStoreConfigId) { this.vectorStoreConfigId = vectorStoreConfigId; }
    public Integer getVectorDimension() { return vectorDimension; }
    public void setVectorDimension(Integer vectorDimension) { this.vectorDimension = vectorDimension; }
    public String getSplitterType() { return splitterType; }
    public void setSplitterType(String splitterType) { this.splitterType = splitterType; }
    public Integer getChunkSize() { return chunkSize; }
    public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
    public Integer getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(Integer chunkOverlap) { this.chunkOverlap = chunkOverlap; }
    public String getRetrievalMode() { return retrievalMode; }
    public void setRetrievalMode(String retrievalMode) { this.retrievalMode = retrievalMode; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getDocumentCount() { return documentCount; }
    public void setDocumentCount(Integer documentCount) { this.documentCount = documentCount; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
