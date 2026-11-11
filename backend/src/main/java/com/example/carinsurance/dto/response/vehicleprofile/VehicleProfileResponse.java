package com.example.carinsurance.dto.response.vehicleprofile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆资料响应。
 * 对应 entity Vehicle，**敏感字段仅返回脱敏值**，严禁暴露 engineNoHash / engineNoEnc 等。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleProfileResponse {

    /** 主键（vehicle.id） */
    private Long id;

    /** vehicle.id（与 id 相同，语义别名） */
    private Long vehicleId;

    /** 车牌号 */
    private String plateNumber;

    /** 归一化车牌（去空格/点，用于去重） */
    private String plateNorm;

    /** 车架号 VIN */
    private String vin;

    /** 发动机号脱敏值（**严禁返回 engineNoHash / engineNoEnc**） */
    private String engineNoMasked;

    /** 车主姓名脱敏值 */
    private String ownerNameMasked;

    /** 车辆类型 */
    private String vehicleType;

    /** 品牌型号 */
    private String brandModel;

    /** 使用性质 */
    private String useNature;

    /** 注册登记日期 */
    private LocalDate registerDate;

    /** 发证日期 */
    private LocalDate issueDate;

    /** 核定载客人数 */
    private Integer approvedPassengers;

    /** 状态：ACTIVE/INACTIVE */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
