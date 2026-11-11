package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper（对应表 conversation）。
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
