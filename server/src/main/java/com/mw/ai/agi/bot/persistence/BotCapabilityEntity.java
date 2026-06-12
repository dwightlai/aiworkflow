package com.mw.ai.agi.bot.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_bot_capability")
public class BotCapabilityEntity {
    @TableId
    private String id;
    private String tenantId;
    private String botId;
    private String capabilityType;
    private String capabilityId;
    private String capabilityCode;
    private String routingKeywords;
    @TableField("is_primary")
    private Boolean primaryCapability;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getBotId() { return botId; }
    public void setBotId(String botId) { this.botId = botId; }
    public String getCapabilityType() { return capabilityType; }
    public void setCapabilityType(String capabilityType) { this.capabilityType = capabilityType; }
    public String getCapabilityId() { return capabilityId; }
    public void setCapabilityId(String capabilityId) { this.capabilityId = capabilityId; }
    public String getCapabilityCode() { return capabilityCode; }
    public void setCapabilityCode(String capabilityCode) { this.capabilityCode = capabilityCode; }
    public String getRoutingKeywords() { return routingKeywords; }
    public void setRoutingKeywords(String routingKeywords) { this.routingKeywords = routingKeywords; }
    public Boolean getPrimaryCapability() { return primaryCapability; }
    public void setPrimaryCapability(Boolean primaryCapability) { this.primaryCapability = primaryCapability; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
