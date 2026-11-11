package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆主数据（当前可信值）。
 * 对应表 vehicle。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("vehicle")
public class Vehicle {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 车主 sys_user.id */
    private Long ownerUserId;

    /** 车牌号（原始展示值） */
    private String plateNumber;

    /** 归一化车牌（去空格/点），唯一 */
    private String plateNorm;

    /** 车架号 VIN，唯一 */
    private String vin;

    /** 发动机号密文 AES-256-GCM（敏感加密字段） */
    private byte[] engineNoEnc;

    /** 发动机号脱敏值 */
    private String engineNoMasked;

    /** 发动机号哈希值（HMAC-SHA256，64位十六进制） */
    private String engineNoHash;

    /** 车主姓名脱敏值 */
    private String ownerNameMasked;

    /** 车辆类型 */
    private String vehicleType;

    /** 品牌型号 */
    private String brandModel;

    /** 使用性质：NON_OPERATING/OPERATING */
    private String useNature;

    /** 注册登记日期 */
    private LocalDate registerDate;

    /** 发证日期 */
    private LocalDate issueDate;

    /** 核定载客人数 */
    private Integer approvedPassengers;

    /** 来源：OCR/MANUAL/IMPORT */
    private String source;

    /** 状态：ACTIVE/INACTIVE */
    private String status;

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

    /** 逻辑删除：0-否 1-是 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
