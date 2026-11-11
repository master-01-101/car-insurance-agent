package com.example.carinsurance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.carinsurance.common.ResultCode;
import com.example.carinsurance.dto.request.conversation.CursorPageRequest;
import com.example.carinsurance.dto.request.conversation.UpdateConversationRequest;
import com.example.carinsurance.dto.response.conversation.ConversationResponse;
import com.example.carinsurance.dto.response.conversation.CursorPageResponse;
import com.example.carinsurance.entity.Conversation;
import com.example.carinsurance.entity.SysUser;
import com.example.carinsurance.exception.BusinessException;
import com.example.carinsurance.mapper.ConversationMapper;
import com.example.carinsurance.mapper.SysUserMapper;
import com.example.carinsurance.security.CurrentUser;
import com.example.carinsurance.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会话业务。
 *
 * 核心规则：
 *  - 同一用户 + 同号牌（归一化后）存在 ACTIVE 会话 → 复用，否则新建
 *  - CUSTOMER 只能看到自己作为 customer_id 的会话
 *  - SALESMAN 可看到所有会话（二期 workbench 阶段才细化）
 *  - 关闭（status=CLOSED）后不可再发消息（409）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final SysUserMapper userMapper;

    /**
     * 归一化车牌：全大写、去空格、去点（鲁 A·12345 → 鲁A12345）。
     */
    public static String normalizePlate(String plate) {
        if (plate == null) return null;
        return plate.replaceAll("\\s+", "").replace("·", "").replace(".", "").toUpperCase();
    }

    /**
     * 按当前用户查询会话列表。
     *
     * @param req 分页请求（cursor + limit）。cursor 为上一页最后一条会话的 id（String）。
     */
    public CursorPageResponse<ConversationResponse> list(CursorPageRequest req) {
        CurrentUser me = SecurityUtils.getCurrentUser();
        int limit = req.getLimit() != null ? req.getLimit() : 20;

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        // 归属过滤：CUSTOMER 只能看自己的
        if (me.isCustomer()) {
            wrapper.eq(Conversation::getCustomerId, me.getId());
        }
        // SALESMAN/ADMIN 暂不过滤（二期 workbench 阶段细化归属）

        // cursor 分页：游标作为 lastId，取 id < cursor 的前 N 条（倒序）
        long lastId;
        if (req.getCursor() != null && !req.getCursor().isBlank()) {
            try {
                lastId = Long.parseLong(req.getCursor());
            } catch (NumberFormatException e) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "cursor 格式无效");
            }
            wrapper.lt(Conversation::getId, lastId);
        } else {
            lastId = Long.MAX_VALUE;
        }

        wrapper.orderByDesc(Conversation::getId).last("LIMIT " + (limit + 1));

        List<Conversation> records = conversationMapper.selectList(wrapper);

        boolean hasMore = records.size() > limit;
        if (hasMore) records = records.subList(0, limit);

        String nextCursor = hasMore ? String.valueOf(records.get(records.size() - 1).getId()) : null;

        List<ConversationResponse> items = records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return CursorPageResponse.<ConversationResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    /**
     * 新建或复用会话。
     * 同一用户 + 同号牌 存在 ACTIVE 会话 → 返回已有会话，不新建。
     */
    @Transactional
    public ConversationResponse createOrReuse(String plateNumber, Long vehicleId) {
        CurrentUser me = SecurityUtils.getCurrentUser();
        String normalized = normalizePlate(plateNumber);

        // 查找可复用的 ACTIVE 会话：customer_id = me.id AND (plate_norm 匹配 或 vehicle_id 匹配)
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getCustomerId, me.getId())
                .eq(Conversation::getStatus, "ACTIVE");

        if (normalized != null) {
            wrapper.and(w -> w
                    .eq(Conversation::getPlateNumber, plateNumber)
                    .or()
                    .eq(Conversation::getVehicleId, vehicleId)
            );
        } else if (vehicleId != null) {
            wrapper.eq(Conversation::getVehicleId, vehicleId);
        }

        wrapper.last("LIMIT 1");
        Conversation existing = conversationMapper.selectOne(wrapper);
        if (existing != null) {
            log.info("复用已有会话 convNo={} for userId={}", existing.getConvNo(), me.getId());
            return toResponse(existing);
        }

        // 新建
        Conversation conv = Conversation.builder()
                .convNo("CNV_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .customerId(me.getId())
                .salesmanId(null)    // 自动分配逻辑二期做
                .vehicleId(vehicleId)
                .plateNumber(plateNumber)
                .title(StringUtils.hasText(plateNumber) ? plateNumber : "新会话")
                .stage("COLLECTING")
                .handlingMode("AI")
                .status("ACTIVE")
                .messageCount(0)
                .build();

        conversationMapper.insert(conv);
        log.info("新建会话 id={} convNo={} userId={} plate={}", conv.getId(), conv.getConvNo(), me.getId(), plateNumber);

        return toResponse(conv);
    }

    /**
     * 获取会话详情（含归属校验）。
     */
    public ConversationResponse getDetail(Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        checkOwnership(conv);
        return toResponse(conv);
    }

    /**
     * 更新会话（修改标题 / 关闭）。
     */
    @Transactional
    public ConversationResponse update(Long conversationId, UpdateConversationRequest req) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        checkOwnership(conv);

        if ("CLOSED".equals(req.getStatus())) {
            if ("CLOSED".equals(conv.getStatus())) {
                throw new BusinessException(ResultCode.STATE_CONFLICT, "会话已关闭");
            }
            conv.setStatus("CLOSED");
            conv.setClosedAt(LocalDateTime.now());
            conversationMapper.updateById(conv);
            log.info("关闭会话 id={}", conversationId);
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前仅支持关闭会话");
        }

        return toResponse(conv);
    }

    // ---------- 归属校验 ----------

    /**
     * 校验当前用户对会话是否有权访问。
     * CUSTOMER：必须是 customer_id
     * SALESMAN：必须是 customer_id 或 salesman_id（或 salesman 的 parentAgentId 链，二期细化）
     * ADMIN：放行
     */
    public void checkOwnership(Conversation conv) {
        CurrentUser me = SecurityUtils.getCurrentUser();

        if (me.isAdmin()) return;
        if (conv.getCustomerId().equals(me.getId())) return;
        if (me.isSalesman() && conv.getSalesmanId() != null && conv.getSalesmanId().equals(me.getId())) return;

        throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该会话");
    }

    /**
     * 仅内部用：获取原始 Entity（绕过归属校验）。
     * 业务接口调用请使用 getDetail。
     */
    public Conversation getConversationEntity(Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        return conv;
    }

    /**
     * 更新会话的最后消息时间和消息计数（内部使用，不触发归属校验）。
     */
    public void updateLastMessage(Conversation conv) {
        conversationMapper.updateById(conv);
    }

    /**
     * 校验会话是否仍处于可写状态（ACTIVE/HANDOFF）。
     */
    public void checkWritable(Conversation conv) {
        if ("CLOSED".equals(conv.getStatus()) || "ABORTED".equals(conv.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "会话已关闭，无法发送消息");
        }
    }

    // ---------- Entity → DTO ----------

    private ConversationResponse toResponse(Conversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .convNo(c.getConvNo())
                .customerId(c.getCustomerId())
                .salesmanId(c.getSalesmanId())
                .vehicleId(c.getVehicleId())
                .plateNumber(c.getPlateNumber())
                .title(c.getTitle())
                .stage(c.getStage())
                .handlingMode(c.getHandlingMode())
                .status(c.getStatus())
                .lastMessageAt(c.getLastMessageAt())
                .messageCount(c.getMessageCount())
                .closedAt(c.getClosedAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
