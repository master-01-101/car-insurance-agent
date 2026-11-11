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
import org.springframework.stereotype.Service;
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

    /**
     * 发送用户消息并（模拟）返回 AI 回复。
     *
     * @return 两条消息：[用户消息, AI 消息]
     */
    @Transactional
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

        // 3. 取 seq（会话内最后一条 seq + 1）
        ChatMessage lastSeqRecord = messageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .select(ChatMessage::getSeq)
                .orderByDesc(ChatMessage::getSeq)
                .last("LIMIT 1"));
        int nextSeq = (lastSeqRecord == null || lastSeqRecord.getSeq() == null) ? 1 : lastSeqRecord.getSeq() + 1;

        // 4. 落库用户消息
        ChatMessage userMsg = ChatMessage.builder()
                .msgNo("MSG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .conversationId(conversationId)
                .seq(nextSeq)
                .senderType("USER")
                .messageType("TEXT")
                .content(req.getContent())
                .requestId(org.slf4j.MDC.get("requestId"))
                .clientMsgId(req.getClientMsgId())
                .status("COMPLETED")
                .build();
        messageMapper.insert(userMsg);

        // 5. （本阶段）模拟 AI 回复：保存一条 ASSISTANT 消息
        //    二期改为：调用 C FastAPI Agent → 拿到回复内容 → 落库
        int assistantSeq = nextSeq + 1;
        ChatMessage assistantMsg = ChatMessage.builder()
                .msgNo("MSG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .conversationId(conversationId)
                .seq(assistantSeq)
                .senderType("ASSISTANT")
                .messageType("TEXT")
                .content(buildMockReply(req.getContent()))
                .requestId(org.slf4j.MDC.get("requestId"))
                .status("COMPLETED")
                .build();
        messageMapper.insert(assistantMsg);

        // 6. 更新会话 lastMessageAt / messageCount
        conv.setLastMessageAt(LocalDateTime.now());
        conv.setMessageCount((conv.getMessageCount() == null ? 0 : conv.getMessageCount()) + 2);
        conversationService.updateLastMessage(conv);

        log.info("消息已落库 conversationId={} userSeq={} assistantSeq={}", conversationId, nextSeq, assistantSeq);

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
