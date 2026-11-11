package com.example.carinsurance.dto.request.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发送消息请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /** 前端生成的幂等 ID，防止重复提交 */
    @NotBlank(message = "客户端幂等 ID 不能为空")
    @Size(max = 64, message = "clientMsgId 最多 64 字符")
    private String clientMsgId;

    /** 消息文本内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 10000, message = "消息内容最多 10000 字符")
    private String content;

    /** 附件 fileId 列表（可选） */
    private List<Long> attachments;
}
