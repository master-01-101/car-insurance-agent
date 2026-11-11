package com.example.carinsurance.controller;

import com.example.carinsurance.common.ApiResponse;
import com.example.carinsurance.dto.request.conversation.CursorPageRequest;
import com.example.carinsurance.dto.request.message.SendMessageRequest;
import com.example.carinsurance.dto.response.conversation.CursorPageResponse;
import com.example.carinsurance.dto.response.message.MessageResponse;
import com.example.carinsurance.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息接口。
 */
@Tag(name = "消息 Message")
@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "历史消息（游标分页）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问该会话"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "会话不存在")
    })
    @GetMapping
    public ApiResponse<CursorPageResponse<MessageResponse>> list(
            @Parameter(description = "会话 id") @PathVariable Long conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "30") Integer limit) {
        CursorPageRequest req = new CursorPageRequest(cursor, limit);
        return ApiResponse.success(messageService.list(conversationId, req));
    }

    @Operation(summary = "发送消息（含 AI 同步回复）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "会话已关闭"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败")
    })
    @PostMapping
    public ApiResponse<Map<String, Object>> send(
            @Parameter(description = "会话 id") @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest req) {

        MessageService.SendMessageResult result = messageService.send(conversationId, req);

        Map<String, Object> body = new HashMap<>();
        body.put("userMessage", result.userMessage());
        body.put("assistantMessage", result.assistantMessage());
        return ApiResponse.success(body);
    }
}
