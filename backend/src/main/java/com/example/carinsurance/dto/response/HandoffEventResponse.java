package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人工接管事件响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoffEventResponse {

    /** 事件类型：REQUESTED / QUEUED / ACCEPTED / MESSAGE / RESOLVED / CANCELLED */
    private String eventType;

    /** 操作者类型：CUSTOMER / SALESMAN / ADMIN / SYSTEM / AI */
    private String actorType;

    /** 操作者 id */
    private Long actorId;

    /** 事件创建时间 */
    private LocalDateTime createdAt;
}
