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
 * 核保记录。
 *
 * <p>对应数据库表 {@code underwriting_record}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("underwriting_record")
public class UnderwritingRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 insurance_order.id */
    private Long orderId;

    /** 报价 quote_record.id */
    private Long quoteId;

    /** 核保编号，唯一 */
    private String caseNo;

    /** 补件重提关联的上一案件 */
    private Long previousCaseId;

    /** 状态：PROCESSING/PASSED/REJECTED/NEED_MATERIAL */
    private String status;

    /** 失败/补件原因码 */
    private String reasonCode;

    /** 可读原因 */
    private String reason;

    /** 补件要求（JSON） */
    private String requiredMaterials;

    /** 外部核保编号 */
    private String externalCaseNo;

    /** 核保渠道 */
    private String provider;

    /** 数据来源：DEMO/REAL */
    private String dataSource;

    /** 提交时间 */
    private LocalDateTime submittedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
