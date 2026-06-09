package com.mw.ai.agi.model.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.Instant;

@TableName("agi_model_provider")
public class ModelProviderEntity {
    @TableId
    private String id;
    private String tenantId;
    private String ownerUnitId;
    private String name;
    private String modelType;
    private String modelUsage;
    private String description;
    private Boolean visionSupport;
    private BigDecimal pricePerMillionTokens;
    private String baseUrl;
    private String model;
    private String apiKeyRef;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getOwnerUnitId() { return ownerUnitId; }
    public void setOwnerUnitId(String ownerUnitId) { this.ownerUnitId = ownerUnitId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getModelUsage() { return modelUsage; }
    public void setModelUsage(String modelUsage) { this.modelUsage = modelUsage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getVisionSupport() { return visionSupport; }
    public void setVisionSupport(Boolean visionSupport) { this.visionSupport = visionSupport; }
    public BigDecimal getPricePerMillionTokens() { return pricePerMillionTokens; }
    public void setPricePerMillionTokens(BigDecimal pricePerMillionTokens) { this.pricePerMillionTokens = pricePerMillionTokens; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKeyRef() { return apiKeyRef; }
    public void setApiKeyRef(String apiKeyRef) { this.apiKeyRef = apiKeyRef; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
