package com.mw.ai.agi.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_auth_audit_log")
public class AuthAuditLogEntity {
    @TableId
    private String id;
    private String tenantId;
    private String eventType;
    private String userId;
    private String appId;
    private String unitId;
    private String departmentIds;
    private String roleIds;
    private String clientIp;
    private String userAgent;
    private String result;
    private String errorCode;
    private Instant occurredAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getDepartmentIds() { return departmentIds; }
    public void setDepartmentIds(String departmentIds) { this.departmentIds = departmentIds; }
    public String getRoleIds() { return roleIds; }
    public void setRoleIds(String roleIds) { this.roleIds = roleIds; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
