package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话表（一个会话一个车牌）。
 * 对应表 conversation。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversation")
public class Conversation {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话编号，唯一 */
    private String convNo;

    /** 客户 sys_user.id */
    private Long customerId;

    /** 业务员 sys_user.id */
    private Long salesmanId;

    /** 车辆 vehicle.id */
    private Long vehicleId;

    /** 会话绑定车牌（标题取此值） */
    private String plateNumber;

    /** 会话标题，默认车牌号 */
    private String title;

    /** 业务阶段，见状态机 */
    private String stage;

    /** 处理方式：AI/HUMAN */
    private String handlingMode;

    /** 状态：ACTIVE/HANDOFF/CLOSED/ABORTED */
    private String status;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 最后一条消息时间 */
    private LocalDateTime lastMessageAt;

    /** 消息数量（冗余计数，便于列表展示） */
    private Integer messageCount;

    /** 关闭时间 */
    private LocalDateTime closedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
