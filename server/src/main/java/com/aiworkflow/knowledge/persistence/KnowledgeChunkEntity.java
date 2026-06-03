package com.aiworkflow.knowledge.persistence;

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
}
