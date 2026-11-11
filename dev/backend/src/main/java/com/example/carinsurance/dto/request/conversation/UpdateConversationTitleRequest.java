package com.example.carinsurance.dto.request.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改会话标题请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationTitleRequest {

    /** 新标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多 100 字符")
    private String title;
}
