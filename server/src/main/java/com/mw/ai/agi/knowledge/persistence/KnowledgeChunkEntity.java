package com.mw.ai.agi.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("agi_knowledge_chunk")
public class KnowledgeChunkEntity {
    @TableId
    private String id;
    private String knowledgeBaseId;
    private String documentId;
    private String documentName;
    private String content;
    @TableField("chunk_index")
    private Integer chunkIndex;
    private Boolean enabled;
    private Integer tokenEstimate;
    private String datasetId;
    private String sourceIndexId;
    private String topicId;
    private String chunkTitle;
    private String chunkType;
    private String sourceSystem;
    private String sourceType;
    private String sourceRefId;
    private String materialSourceType;
    private String materialType;
    private String sourceArchiveFileId;
    private String sourcePage;
    private String sourcePosition;
    private String citationText;
    private String metadataJson;
    private String securityLevel;
    private String logicalChunkId;
    private String parentChunkId;
    private String groupId;
    private String chunkLevel;
    private String sectionPath;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getTokenEstimate() { return tokenEstimate; }
    public void setTokenEstimate(Integer tokenEstimate) { this.tokenEstimate = tokenEstimate; }
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
    public String getSourceIndexId() { return sourceIndexId; }
    public void setSourceIndexId(String sourceIndexId) { this.sourceIndexId = sourceIndexId; }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public String getChunkTitle() { return chunkTitle; }
    public void setChunkTitle(String chunkTitle) { this.chunkTitle = chunkTitle; }
    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }
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
    public String getSourcePage() { return sourcePage; }
    public void setSourcePage(String sourcePage) { this.sourcePage = sourcePage; }
    public String getSourcePosition() { return sourcePosition; }
    public void setSourcePosition(String sourcePosition) { this.sourcePosition = sourcePosition; }
    public String getCitationText() { return citationText; }
    public void setCitationText(String citationText) { this.citationText = citationText; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public String getLogicalChunkId() { return logicalChunkId; }
    public void setLogicalChunkId(String logicalChunkId) { this.logicalChunkId = logicalChunkId; }
    public String getParentChunkId() { return parentChunkId; }
    public void setParentChunkId(String parentChunkId) { this.parentChunkId = parentChunkId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getChunkLevel() { return chunkLevel; }
    public void setChunkLevel(String chunkLevel) { this.chunkLevel = chunkLevel; }
    public String getSectionPath() { return sectionPath; }
    public void setSectionPath(String sectionPath) { this.sectionPath = sectionPath; }
}
