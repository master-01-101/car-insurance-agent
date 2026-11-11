package com.example.carinsurance.dto.request.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新会话请求（当前仅支持关闭会话）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationRequest {

    /** 目标状态，仅允许 CLOSED（关闭会话） */
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "CLOSED", message = "当前仅允许将会话状态更新为 CLOSED")
    private String status;
}
