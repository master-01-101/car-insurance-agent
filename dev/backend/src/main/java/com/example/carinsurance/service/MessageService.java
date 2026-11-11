package com.example.carinsurance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.carinsurance.common.ResultCode;
import com.example.carinsurance.dto.request.conversation.CursorPageRequest;
import com.example.carinsurance.dto.request.message.SendMessageRequest;
import com.example.carinsurance.dto.response.conversation.CursorPageResponse;
import com.example.carinsurance.dto.response.message.MessageResponse;
import com.example.carinsurance.entity.ChatMessage;
import com.example.carinsurance.entity.Conversation;
import com.example.carinsurance.exception.BusinessException;
import com.example.carinsurance.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 消息业务：发送、历史查询、幂等防护。
 *
 * <p>本阶段只做 Java 持久化，不调用 C 的 AI Agent。AI 消息由 Java 模拟一条占位回复。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatMessageMapper messageMapper;
    private final ConversationService conversationService;
    private final ApplicationContext ctx; // 注入 Spring 容器，用来拿自身代理（REQUIRES_NEW 事务必须走代理）

    /** 拿 Spring 代理，保证 @Transactional(REQUIRES_NEW) 生效 */
    private MessageService self() {
        return ctx.getBean(MessageService.class);
    }

    /**
     * 发送用户消息并（模拟）返回 AI 回复。
     *
     * <p>幂等：同一 conversation_id + client_msg_id 由 DB 唯一索引保证；命中后直接返回已有消息。</p>
     * <p>并发：用户消息与 AI 回复各用独立小事务（REQUIRES_NEW），FOR UPDATE 锁范围互不交叠 → 无死锁。</p>
     * <p>AI 回复：PROCESSING → 模拟逻辑 → COMPLETED；异常时 FAILED + payload_json 记录错误。</p>
     *
     * @return 两条消息：[用户消息, AI 消息]
     */
    public SendMessageResult send(Long conversationId, SendMessageRequest req) {
        // 1. 加载会话 + 归属校验 + 可写校验
        Conversation conv = conversationService.getConversationEntity(conversationId);
        conversationService.checkOwnership(conv);
        conversationService.checkWritable(conv);

        // 2. 幂等检查：同一 conversation 下 client_msg_id 唯一
        if (StringUtils.hasText(req.getClientMsgId())) {
            ChatMessage existing = messageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getConversationId, conversationId)
                    .eq(ChatMessage::getClientMsgId, req.getClientMsgId())
                    .last("LIMIT 1"));
            if (existing != null) {
                log.info("幂等命中：conversationId={} clientMsgId={} → 返回已有消息", conversationId, req.getClientMsgId());
                ChatMessage assistant = findLatestAssistant(conversationId);
                return new SendMessageResult(toResponse(existing),
                        assistant != null ? toResponse(assistant) : null);
            }
        }

        // 3. 落库用户消息（独立小事务，锁范围仅覆盖用户消息）
        ChatMessage userMsg = self().insertUserMessageWithLock(conversationId, req.getContent(), req.getClientMsgId());

        // 4. （本阶段）模拟 AI 回复（独立小事务，锁范围仅覆盖 AI 回复）
        //    二期改为：Java 异步调用 C FastAPI Agent → 拿到回复内容 → 调 saveAssistantMessage() 落库
        ChatMessage assistantMsg = self().saveMockAssistantReplyLocked(conversationId, userMsg.getId(), req.getContent());

        // 5. 更新会话 lastMessageAt / messageCount
        self().updateConversationAfterSend(conv, 2);

        log.info("消息已落库 conversationId={} userSeq={} assistantSeq={}", conversationId, userMsg.getSeq(), assistantMsg.getSeq());

        return new SendMessageResult(toResponse(userMsg), toResponse(assistantMsg));
    }

    /**
     * 查询会话历史消息（游标分页，id 倒序）。
     */
    public CursorPageResponse<MessageResponse> list(Long conversationId, CursorPageRequest req) {
        // 归属校验
        Conversation conv = conversationService.getConversationEntity(conversationId);
        conversationService.checkOwnership(conv);

        int limit = req.getLimit() != null ? req.getLimit() : 30;

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId);

        long lastId;
        if (req.getCursor() != null && !req.getCursor().isBlank()) {
            try {
                lastId = Long.parseLong(req.getCursor());
            } catch (NumberFormatException e) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "cursor 格式无效");
            }
            wrapper.lt(ChatMessage::getId, lastId);
        } else {
            lastId = Long.MAX_VALUE;
        }

        wrapper.orderByDesc(ChatMessage::getId).last("LIMIT " + (limit + 1));

        List<ChatMessage> records = messageMapper.selectList(wrapper);

        boolean hasMore = records.size() > limit;
        if (hasMore) records = records.subList(0, limit);

        String nextCursor = hasMore ? String.valueOf(records.get(records.size() - 1).getId()) : null;

        List<MessageResponse> items = records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return CursorPageResponse.<MessageResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    // ---------- 内部辅助 ----------

    /**
     * 落库用户消息（独立小事务）。
     * FOR UPDATE 行锁 + INSERT 在同一 REQUIRES_NEW 事务内完成 → commit 后立即释放锁 → 无死锁。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ChatMessage insertUserMessageWithLock(Long conversationId, String content, String clientMsgId) {
        int nextSeq = nextSeqLocked(conversationId);
        try {
            ChatMessage msg = ChatMessage.builder()
                    .msgNo("MSG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                    .conversationId(conversationId)
                    .seq(nextSeq)
                    .senderType("USER")
                    .messageType("TEXT")
                    .content(content)
                    .requestId(org.slf4j.MDC.get("requestId"))
                    .clientMsgId(clientMsgId)
                    .status("COMPLETED")
                    .build();
            messageMapper.insert(msg);
            return msg;
        } catch (DuplicateKeyException e) {
            log.warn("用户消息 seq 冲突 conversationId={} seq={}", conversationId, nextSeq);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "消息序号冲突，请稍后重试");
        }
    }

    /**
     * 取会话内下一个 seq（带 FOR UPDATE 行锁）。
     * 需在事务内调用（REQUIRES_NEW 小事务即可）。
     */
    private int nextSeqLocked(Long conversationId) {
        Integer maxSeq = messageMapper.selectMaxSeqForUpdate(conversationId);
        return maxSeq == null ? 1 : maxSeq + 1;
    }

    /**
     * 模拟 AI 回复（独立小事务）。
     * FOR UPDATE 行锁 + INSERT + UPDATE 在同一 REQUIRES_NEW 事务内完成。
     * 异常时 status=FAILED，payload_json 存错误信息（不暴露堆栈）。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ChatMessage saveMockAssistantReplyLocked(Long conversationId, Long replyToUserId, String userContent) {
        int seq = nextSeqLocked(conversationId);
        ChatMessage processing = ChatMessage.builder()
                .msgNo("MSG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .conversationId(conversationId)
                .seq(seq)
                .senderType("ASSISTANT")
                .messageType("TEXT")
                .content(null)
                .requestId(org.slf4j.MDC.get("requestId"))
                .replyToMessageId(replyToUserId)
                .status("PROCESSING")
                .build();
        messageMapper.insert(processing);

        try {
            processing.setContent(buildMockReply(userContent));
            processing.setStatus("COMPLETED");
            messageMapper.updateById(processing);
            log.info("模拟 AI 回复完成 conversationId={} msgId={}", conversationId, processing.getId());
        } catch (Exception e) {
            log.error("模拟 AI 回复失败 conversationId={}", conversationId, e);
            processing.setStatus("FAILED");
            String errMsg = (e.getMessage() == null ? "unknown" : e.getMessage());
            String err = errMsg.replaceAll("[\"\\n\\r]", " ").substring(0, Math.min(200, errMsg.length()));
            processing.setPayloadJson("{\"error\":\"" + err + "\"}");
            messageMapper.updateById(processing);
        }
        return processing;
    }

    /**
     * 更新会话 lastMessageAt / messageCount。
     * 独立小事务避免与消息表行锁交互产生死锁。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateConversationAfterSend(Conversation conv, int delta) {
        conv.setLastMessageAt(LocalDateTime.now());
        conv.setMessageCount((conv.getMessageCount() == null ? 0 : conv.getMessageCount()) + delta);
        conversationService.updateLastMessage(conv);
    }

    /**
     * 供 AI Agent 集成调用的**公开内部方法**：保存 AI 回复。
     *
     * <p>二期架构：Java Controller 收到用户消息 → Java 异步调 C FastAPI Agent → Agent 回调 Java 内部 Service
     * 或 Java 轮询拿到 AI 回复 → 调此方法落库。</p>
     *
     * <p>独立 REQUIRES_NEW 事务：FOR UPDATE + INSERT 在一个小事务里完成 → commit 后立即释放锁。</p>
     *
     * @param conversationId    会话 id
     * @param replyToMessageId  关联的用户消息 id（可为 null）
     * @param content           AI 回复内容
     * @param errorInfo         AI 调用失败时的错误信息（null 表示成功）
     * @param relatedPlanId     关联方案 id（可为 null）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ChatMessage saveAssistantMessage(Long conversationId, Long replyToMessageId,
                                             String content, String errorInfo, Long relatedPlanId) {
        int seq = nextSeqLocked(conversationId);
        boolean hasError = StringUtils.hasText(errorInfo);

        ChatMessage msg = ChatMessage.builder()
                .msgNo("MSG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .conversationId(conversationId)
                .seq(seq)
                .senderType("ASSISTANT")
                .messageType("TEXT")
                .content(hasError ? null : content)
                .replyToMessageId(replyToMessageId)
                .relatedPlanId(relatedPlanId)
                .requestId(org.slf4j.MDC.get("requestId"))
                .status(hasError ? "FAILED" : "COMPLETED")
                .build();

        if (hasError) {
            String clean = errorInfo.replaceAll("[\"\\n\\r]", " ").substring(0, Math.min(200, errorInfo.length()));
            msg.setPayloadJson("{\"error\":\"" + clean + "\"}");
        }

        messageMapper.insert(msg);
        log.info("保存 AI 回复 conversationId={} seq={} status={}", conversationId, seq, msg.getStatus());
        return msg;
    }

    /** 查找会话内最后一条 ASSISTANT 消息（幂等命中时返回对应 AI 回复）。 */
    private ChatMessage findLatestAssistant(Long conversationId) {
        return messageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getSenderType, "ASSISTANT")
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 1"));
    }

    /** 模拟 AI 回复（二期替换为真实 C Agent 调用）。 */
    private String buildMockReply(String userContent) {
        if (userContent == null) userContent = "";
        if (userContent.contains("你好") || userContent.contains("hi") || userContent.toLowerCase().startsWith("hello")) {
            return "您好！我是车险出单智能体，可以帮您办理车险报价、方案推荐、核保和支付。请问您的车辆车牌号是多少？";
        }
        if (userContent.contains("价格") || userContent.contains("多少钱") || userContent.contains("报价")) {
            return "好的，我可以为您计算车险报价。但在生成报价前，需要先确认您的车辆信息和投保人资料。请先告知车牌号。";
        }
        return "已收到您的消息。我正在理解您的需求…（本阶段 AI 回复为 Java 模拟，二期将接入智能 Agent）";
    }

    private MessageResponse toResponse(ChatMessage m) {
        return MessageResponse.builder()
                .id(m.getId())
                .msgNo(m.getMsgNo())
                .conversationId(m.getConversationId())
                .seq(m.getSeq())
                .senderType(m.getSenderType())
                .messageType(m.getMessageType())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }

    // ---------- 发送结果 VO ----------

    public static record SendMessageResult(MessageResponse userMessage, MessageResponse assistantMessage) {}
}
