package com.mw.ai.agi.generation.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_generation_job")
public class GenerationJobEntity {
    @TableId
    private String id;
    private String tenantId;
    private String botId;
    private String templateId;
    private String workflowId;
    private String unitId;
    private String userId;
    private String knowledgeBaseIds;
    private String externalCorpusRef;
    private String variables;
    private String status;
    private String outlineJson;
    private String sectionOutputsJson;
    private String workflowRunSnapshot;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getBotId() { return botId; }
    public void setBotId(String botId) { this.botId = botId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getKnowledgeBaseIds() { return knowledgeBaseIds; }
    public void setKnowledgeBaseIds(String knowledgeBaseIds) { this.knowledgeBaseIds = knowledgeBaseIds; }
    public String getExternalCorpusRef() { return externalCorpusRef; }
    public void setExternalCorpusRef(String externalCorpusRef) { this.externalCorpusRef = externalCorpusRef; }
    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOutlineJson() { return outlineJson; }
    public void setOutlineJson(String outlineJson) { this.outlineJson = outlineJson; }
    public String getSectionOutputsJson() { return sectionOutputsJson; }
    public void setSectionOutputsJson(String sectionOutputsJson) { this.sectionOutputsJson = sectionOutputsJson; }
    public String getWorkflowRunSnapshot() { return workflowRunSnapshot; }
    public void setWorkflowRunSnapshot(String workflowRunSnapshot) { this.workflowRunSnapshot = workflowRunSnapshot; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
