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
 * 订单状态变更日志（表 order_status_log，无 version / 无 updated_at）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_status_log")
public class OrderStatusLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 insurance_order.id */
    private Long orderId;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作人 sys_user.id */
    private Long operatorId;

    /** 操作者类型：CUSTOMER / SALESMAN / ADMIN / SYSTEM / AI */
    private String operatorType;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
