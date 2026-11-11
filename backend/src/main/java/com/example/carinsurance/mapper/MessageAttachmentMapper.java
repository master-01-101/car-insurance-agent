package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.MessageAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息与文件关联 Mapper（对应表 message_attachment）。
 */
@Mapper
public interface MessageAttachmentMapper extends BaseMapper<MessageAttachment> {
}
