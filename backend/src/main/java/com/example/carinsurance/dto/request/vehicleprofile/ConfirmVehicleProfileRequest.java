package com.example.carinsurance.dto.request.vehicleprofile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 确认/修正车辆资料请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmVehicleProfileRequest {

    /** 来源会话 conversation.id */
    @NotNull(message = "conversationId 不能为空")
    private Long conversationId;

    /** 车辆 vehicle.id（可空，首次可能还没建档） */
    private Long vehicleId;

    /** 车牌号 */
    @Size(max = 16, message = "车牌号最多 16 字符")
    private String plateNumber;

    /** 品牌型号 */
    @Size(max = 100, message = "品牌型号最多 100 字符")
    private String brandModel;

    /** 车辆类型 */
    @Size(max = 32, message = "车辆类型最多 32 字符")
    private String vehicleType;

    /** 使用性质 */
    @Size(max = 20, message = "使用性质最多 20 字符")
    private String useNature;

    /** 注册登记日期 */
    private LocalDate registerDate;

    /** 车架号 VIN */
    @Size(max = 32, message = "VIN 最多 32 字符")
    private String vin;

    /** 发动机号脱敏值（前端已脱敏传入，后端不存原始值） */
    @Size(max = 32, message = "发动机号最多 32 字符")
    private String engineNoMasked;
}
