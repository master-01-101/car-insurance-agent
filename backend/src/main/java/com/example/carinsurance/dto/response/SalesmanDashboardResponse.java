package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 业务员工作台仪表盘响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesmanDashboardResponse {

    /** 总会话数 */
    private Integer totalConversations;

    /** 活跃会话数 */
    private Integer activeConversations;

    /** 待处理接管工单数 */
    private Integer pendingHandoffs;

    /** 今日报价数 */
    private Integer todayQuotations;

    /** 今日保单数 */
    private Integer todayPolicies;

    /** 客户列表 */
    private List<CustomerBriefResponse> customers;

    /** 最近接管工单 */
    private List<HandoffTicketBriefResponse> recentHandoffs;
}
