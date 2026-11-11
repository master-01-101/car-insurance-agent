package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 聊天消息 Mapper（对应表 chat_message）。
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 取会话内下一条 seq（带行锁，需在事务内调用）。
     *
     * <p>会话内最后一条消息的行被 FOR UPDATE 锁住 → 两个并发请求串行化 → 后一个请求等锁释放后
     * 拿到最新的 MAX(seq) 再 + 1 → 保证 seq 唯一。</p>
     *
     * <p>空会话（无消息）时 MAX(seq) = NULL，返回 1。空表时 FOR UPDATE 不锁行，两个并发请求
     * 同时拿到 seq=1 会被 DB 唯一索引 uk_chat_message_seq 拦住，由 Service 层重试一次。</p>
     */
    @Select("SELECT COALESCE(MAX(seq), 0) FROM chat_message WHERE conversation_id = #{convId} FOR UPDATE")
    Integer selectMaxSeqForUpdate(@Param("convId") Long conversationId);
}
