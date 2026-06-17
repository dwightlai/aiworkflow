package com.mw.ai.agi.generation.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_generation_output")
public class GenerationOutputEntity {
    @TableId
    private String id;
    private String tenantId;
    private String jobId;
    private String title;
    private String outputType;
    private String contentMarkdown;
    private String contentJson;
    private String contentDocxPath;
    private String outputTemplateId;
    private String citations;
    private String sourceSnapshot;
    private String status;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public String getContentDocxPath() { return contentDocxPath; }
    public void setContentDocxPath(String contentDocxPath) { this.contentDocxPath = contentDocxPath; }
    public String getOutputTemplateId() { return outputTemplateId; }
    public void setOutputTemplateId(String outputTemplateId) { this.outputTemplateId = outputTemplateId; }
    public String getCitations() { return citations; }
    public void setCitations(String citations) { this.citations = citations; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
