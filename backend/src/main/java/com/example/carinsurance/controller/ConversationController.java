package com.example.carinsurance.controller;

import com.example.carinsurance.common.ApiResponse;
import com.example.carinsurance.dto.request.conversation.CreateConversationRequest;
import com.example.carinsurance.dto.request.conversation.CursorPageRequest;
import com.example.carinsurance.dto.request.conversation.UpdateConversationRequest;
import com.example.carinsurance.dto.response.conversation.ConversationResponse;
import com.example.carinsurance.dto.response.conversation.CursorPageResponse;
import com.example.carinsurance.security.SecurityUtils;
import com.example.carinsurance.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会话接口。
 */
@Tag(name = "会话 Conversation")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "当前用户会话列表（游标分页）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping
    public ApiResponse<CursorPageResponse<ConversationResponse>> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        CursorPageRequest req = new CursorPageRequest(cursor, limit);
        return ApiResponse.success(conversationService.list(req));
    }

    @Operation(summary = "新建或复用会话")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功（复用或新建）"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping
    public ApiResponse<ConversationResponse> create(@Valid @RequestBody CreateConversationRequest req) {
        return ApiResponse.success(conversationService.createOrReuse(req.getPlateNumber(), req.getVehicleId()));
    }

    @Operation(summary = "会话详情")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问该会话"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "会话不存在")
    })
    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> detail(
            @Parameter(description = "会话 id") @PathVariable Long conversationId) {
        return ApiResponse.success(conversationService.getDetail(conversationId));
    }

    @Operation(summary = "关闭会话")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "状态冲突（已关闭）")
    })
    @PatchMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> update(
            @Parameter(description = "会话 id") @PathVariable Long conversationId,
            @Valid @RequestBody UpdateConversationRequest req) {
        return ApiResponse.success(conversationService.update(conversationId, req));
    }
}
