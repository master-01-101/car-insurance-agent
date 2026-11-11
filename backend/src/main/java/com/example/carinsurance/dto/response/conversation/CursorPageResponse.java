package com.example.carinsurance.dto.response.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 游标分页通用响应结构。
 *
 * @param <T> 列表项类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {

    /** 当前页数据 */
    private List<T> items;

    /** 下一页游标，空表示无更多数据 */
    private String nextCursor;

    /** 是否还有下一页 */
    private Boolean hasMore;
}
