package com.mw.ai.agi.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_knowledge_document")
public class KnowledgeDocumentEntity {
    @TableId
    private String id;
    private String knowledgeBaseId;
    private String name;
    private Integer chunkCount;
    private Instant createdAt;
    private String datasetType;
    private String processingStatus;
    private String tags;
    private String category;
    private String source;
    private Integer rowCount;
    private String parserType;
    private String splitterType;
    private String splitterConfig;
    private String rawContent;
    private String errorMessage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getDatasetType() { return datasetType; }
    public void setDatasetType(String datasetType) { this.datasetType = datasetType; }
    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public String getParserType() { return parserType; }
    public void setParserType(String parserType) { this.parserType = parserType; }
    public String getSplitterType() { return splitterType; }
    public void setSplitterType(String splitterType) { this.splitterType = splitterType; }
    public String getSplitterConfig() { return splitterConfig; }
    public void setSplitterConfig(String splitterConfig) { this.splitterConfig = splitterConfig; }
    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
