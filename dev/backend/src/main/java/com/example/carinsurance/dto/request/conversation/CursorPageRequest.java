package com.example.carinsurance.dto.request.conversation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游标分页通用请求基类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageRequest {

    /** 上一页最后一条的 id 游标，空表示第一页 */
    private String cursor;

    /** 每页条数，默认 20 */
    @Min(value = 1, message = "limit 最小 1")
    @Max(value = 100, message = "limit 最大 100")
    @Builder.Default
    private Integer limit = 20;
}
