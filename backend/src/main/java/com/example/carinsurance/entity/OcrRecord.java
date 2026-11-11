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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OCR 识别记录表。
 * 对应表 ocr_record。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ocr_record")
public class OcrRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件 file_object.id */
    private Long fileId;

    /** 识别请求 ID，与 file_id 联合唯一 */
    private String recognitionRequestId;

    /** 类型：VEHICLE_LICENSE/ID_CARD */
    private String ocrType;

    /** 面：MAIN/SUB/FRONT/BACK */
    private String side;

    /** 证件角色：APPLICANT/INSURED/VEHICLE_OWNER */
    private String partyRole;

    /** OCR 服务商，可为 MOCK */
    private String provider;

    /** 状态：PENDING/SUCCEEDED/NEEDS_REVIEW/FAILED */
    private String status;

    /** 归一化字段（含置信度/异常标记，JSON） */
    private String fieldsJson;

    /** 服务商原始返回（JSON） */
    private String rawJson;

    /** 平均置信度 0~1 */
    private BigDecimal confidenceAvg;

    /** 缺失字段列表（JSON） */
    private String missingFields;

    /** 告警列表（JSON） */
    private String warnings;

    /** 识别完成时间 */
    private LocalDateTime recognizedAt;

    /** 失败原因 */
    private String errorMessage;

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
