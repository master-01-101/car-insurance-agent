package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.ConversationParty;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话人员资料 Mapper（对应表 conversation_party）。
 */
@Mapper
public interface ConversationPartyMapper extends BaseMapper<ConversationParty> {
}
