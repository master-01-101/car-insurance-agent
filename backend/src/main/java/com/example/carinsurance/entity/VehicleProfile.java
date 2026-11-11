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
 * 车辆资料确认快照与修改留痕。
 * 对应表 vehicle_profile。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("vehicle_profile")
public class VehicleProfile {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 车辆 vehicle.id */
    private Long vehicleId;

    /** 来源会话 conversation.id */
    private Long conversationId;

    /** 资料版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 本次确认的车辆字段快照（JSON） */
    private String fieldsJson;

    /** 用户修正过的字段及前后值（JSON） */
    private String correctedFields;

    /** 来源：OCR/MANUAL */
    private String source;

    /** 依据的 ocr_record.id */
    private Long ocrRecordId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
