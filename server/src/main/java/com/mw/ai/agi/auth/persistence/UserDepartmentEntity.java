package com.mw.ai.agi.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_user_department")
public class UserDepartmentEntity {
    @TableId
    private String id;
    private String tenantId;
    private String userId;
    private String departmentId;
    private Boolean primaryDepartment;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public Boolean getPrimaryDepartment() { return primaryDepartment; }
    public void setPrimaryDepartment(Boolean primaryDepartment) { this.primaryDepartment = primaryDepartment; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
