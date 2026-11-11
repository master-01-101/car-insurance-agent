package com.example.carinsurance.dto.request.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 根据车牌号创建或查询会话（独立端点）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ByPlateConversationRequest {

    /** 车牌号（必填） */
    @NotBlank(message = "车牌号不能为空")
    @Size(max = 16, message = "车牌号最多 16 字符")
    private String plateNumber;

    /** 车辆 vehicle.id（可选，用于更精确的复用匹配） */
    private Long vehicleId;
}
