package com.mw.ai.agi.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_user_unit")
public class UserUnitEntity {
    @TableId
    private String id;
    private String tenantId;
    private String userId;
    private String unitId;
    private Boolean primaryUnit;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public Boolean getPrimaryUnit() { return primaryUnit; }
    public void setPrimaryUnit(Boolean primaryUnit) { this.primaryUnit = primaryUnit; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
