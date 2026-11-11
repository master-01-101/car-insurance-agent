package com.example.carinsurance.dto.request.conversation;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建会话请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    /** 车牌号（可空，空则由 Agent 首轮确定） */
    @Size(max = 16, message = "车牌号最多 16 字符")
    private String plateNumber;

    /** 车辆 vehicle.id（可选） */
    private Long vehicleId;
}
