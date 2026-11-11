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
 * 续保查询记录（表 renewal_lookup，无 version 字段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("renewal_lookup")
public class RenewalLookup {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 查询编号，唯一 */
    private String lookupNo;

    /** 客户 sys_user.id */
    private Long customerId;

    /** 查询车牌 */
    private String plateNumber;

    /** 查询 VIN */
    private String vin;

    /** 保单年度 */
    private Integer policyYear;

    /** 状态：PROCESSING / COMPLETED / FAILED */
    private String status;

    /** 是否匹配到历史保单（tinyint → Integer） */
    private Integer matched;

    /** 候选历史保单快照（JSON） */
    private String resultJson;

    /** 失败原因 */
    private String errorMessage;

    /** 请求时间 */
    private LocalDateTime requestedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
