package com.mw.ai.agi.auth.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuthAuditLogMapper extends BaseMapper<AuthAuditLogEntity> {
    @Select("""
            SELECT DISTINCT event_type
            FROM agi_auth_audit_log
            WHERE tenant_id = #{tenantId}
            ORDER BY event_type
            """)
    List<String> selectDistinctEventTypes(@Param("tenantId") String tenantId);
}
