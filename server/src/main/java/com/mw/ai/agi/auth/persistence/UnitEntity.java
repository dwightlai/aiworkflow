package com.mw.ai.agi.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_unit")
public class UnitEntity {
    @TableId
    private String id;
    private String tenantId;
    private String code;
    private String externalUnitId;
    private String name;
    private String unitType;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getExternalUnitId() { return externalUnitId; }
    public void setExternalUnitId(String externalUnitId) { this.externalUnitId = externalUnitId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
