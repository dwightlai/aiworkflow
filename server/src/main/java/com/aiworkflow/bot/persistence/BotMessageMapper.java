package com.aiworkflow.bot.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BotMessageMapper extends BaseMapper<BotMessageEntity> {
}
