package com.example.carinsurance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保险方案推荐请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendPlanRequest {

    /** 会话 id */
    @NotNull(message = "conversationId 不能为空")
    private Long conversationId;

    /** 模式：RECOMMEND（推荐新方案） / REUSE_LAST_YEAR（复用往年方案） */
    @Pattern(regexp = "RECOMMEND|REUSE_LAST_YEAR", message = "mode 必须为 RECOMMEND 或 REUSE_LAST_YEAR")
    private String mode;

    /** 用户改写方案的指令（可空，最多 500 字） */
    @Size(max = 500, message = "customizeRequirements 长度不能超过 500")
    private String customizeRequirements;
}
