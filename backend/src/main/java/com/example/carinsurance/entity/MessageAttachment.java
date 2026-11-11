package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息与文件关联表（一条消息可带多份材料）。
 * 对应表 message_attachment。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message_attachment")
public class MessageAttachment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息 chat_message.id */
    private Long messageId;

    /** 文件 file_object.id */
    private Long fileId;

    /** 用途：LICENSE/ID_CARD/INSPECTION/CHAT_IMAGE */
    private String category;

    /** 排序 */
    private Integer sortNo;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
