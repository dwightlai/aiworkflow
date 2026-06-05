package com.mw.ai.agi.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_department")
public class DepartmentEntity {
    @TableId
    private String id;
    private String tenantId;
    private String unitId;
    private String code;
    private String externalDepartmentId;
    private String parentId;
    private String name;
    private Integer sortOrder;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getExternalDepartmentId() { return externalDepartmentId; }
    public void setExternalDepartmentId(String externalDepartmentId) { this.externalDepartmentId = externalDepartmentId; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
