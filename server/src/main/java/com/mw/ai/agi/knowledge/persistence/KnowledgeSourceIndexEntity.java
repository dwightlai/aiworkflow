package com.mw.ai.agi.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_knowledge_source_index")
public class KnowledgeSourceIndexEntity {
    @TableId
    private String id;
    private String tenantId;
    private String knowledgeBaseId;
    private String datasetId;
    private String topicId;
    private String sourceSystem;
    private String sourceType;
    private String sourceRefId;
    private String materialSourceType;
    private String materialType;
    private String sourceArchiveFileId;
    private String sourceTitleSnapshot;
    private String sourceVersion;
    private String sourceUrl;
    private String storagePath;
    private String metadataSnapshot;
    private String documentId;
    private String indexStatus;
    private Instant lastSyncTime;
    private Instant lastIndexTime;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(String sourceRefId) { this.sourceRefId = sourceRefId; }
    public String getMaterialSourceType() { return materialSourceType; }
    public void setMaterialSourceType(String materialSourceType) { this.materialSourceType = materialSourceType; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public String getSourceArchiveFileId() { return sourceArchiveFileId; }
    public void setSourceArchiveFileId(String sourceArchiveFileId) { this.sourceArchiveFileId = sourceArchiveFileId; }
    public String getSourceTitleSnapshot() { return sourceTitleSnapshot; }
    public void setSourceTitleSnapshot(String sourceTitleSnapshot) { this.sourceTitleSnapshot = sourceTitleSnapshot; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getMetadataSnapshot() { return metadataSnapshot; }
    public void setMetadataSnapshot(String metadataSnapshot) { this.metadataSnapshot = metadataSnapshot; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }
    public Instant getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(Instant lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    public Instant getLastIndexTime() { return lastIndexTime; }
    public void setLastIndexTime(Instant lastIndexTime) { this.lastIndexTime = lastIndexTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
