package com.mw.ai.agi.generation.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_generation_template")
public class GenerationTemplateEntity {
    @TableId
    private String id;
    private String tenantId;
    private String name;
    private String code;
    private String description;
    private String category;
    private String ownerUnitId;
    private String outputType;
    private String templateSchema;
    private String workflowId;
    private String workflowSnapshot;
    private String status;
    private Integer version;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getOwnerUnitId() { return ownerUnitId; }
    public void setOwnerUnitId(String ownerUnitId) { this.ownerUnitId = ownerUnitId; }
    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public String getTemplateSchema() { return templateSchema; }
    public void setTemplateSchema(String templateSchema) { this.templateSchema = templateSchema; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getWorkflowSnapshot() { return workflowSnapshot; }
    public void setWorkflowSnapshot(String workflowSnapshot) { this.workflowSnapshot = workflowSnapshot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
