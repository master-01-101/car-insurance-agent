package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper（对应表 chat_message）。
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
