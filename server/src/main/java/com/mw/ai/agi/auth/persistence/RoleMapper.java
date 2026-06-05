package com.mw.ai.agi.auth.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
    @Select("""
            SELECT r.code
            FROM agi_user_role ur
            JOIN agi_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY r.code
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") String userId);
}
