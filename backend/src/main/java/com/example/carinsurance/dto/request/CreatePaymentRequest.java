package com.example.carinsurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建支付请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    /** 订单 id */
    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    /** 报价 id（可空） */
    private Long quoteId;

    /** 核保 id（可空） */
    private Long underwritingId;

    /** 支付渠道：MOCK / ALIPAY / WECHAT */
    @NotBlank(message = "channel 不能为空")
    @Pattern(regexp = "MOCK|ALIPAY|WECHAT", message = "channel 必须为 MOCK、ALIPAY 或 WECHAT")
    private String channel;

    /** 备注（可空） */
    @Size(max = 500, message = "notes 长度不能超过 500")
    private String notes;
}
