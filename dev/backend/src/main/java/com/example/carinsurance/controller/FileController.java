package com.example.carinsurance.controller;

import com.example.carinsurance.common.ApiResponse;
import com.example.carinsurance.dto.response.file.FileMetadataResponse;
import com.example.carinsurance.exception.BusinessException;
import com.example.carinsurance.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件管理 Controller —— 上传 / 元数据查询 / 内容读取。
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "上传、查询元数据、下载/预览文件内容")
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", description = "multipart/form-data 上传。category 必填，conversationId 必填，partyRole 可选。")
    public ApiResponse<FileMetadataResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long conversationId,
            @RequestParam String category,
            @RequestParam(required = false) String partyRole) {
        return ApiResponse.success(fileService.upload(file, conversationId, category, partyRole));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "查询文件元数据", description = "需身份认证 + 归属校验（会话归属或上传人本人）")
    public ApiResponse<FileMetadataResponse> getMetadata(@PathVariable Long fileId) {
        return ApiResponse.success(fileService.getMetadata(fileId));
    }

    @GetMapping("/{fileId}/content")
    @Operation(summary = "下载 / 预览文件内容", description = "需身份认证 + 归属校验。浏览器直接预览图片/PDF，其他类型触发下载。")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId) throws IOException {
        FileService.FileStream fs = fileService.readFile(fileId);
        InputStreamResource body = new InputStreamResource(fs.inputStream());

        String filename = fs.originalName() == null ? "file" : fs.originalName();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(fs.mimeType() != null ? fs.mimeType() : "application/octet-stream"));
        // inline 表示浏览器预览（图片/PDF），attachment 表示下载
        String disposition = isInlineContent(fs.mimeType())
                ? "inline; filename=\"" + filename + "\""
                : "attachment; filename=\"" + filename + "\"";
        headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition);
        if (fs.sizeBytes() != null && fs.sizeBytes() > 0) {
            headers.setContentLength(fs.sizeBytes());
        }
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private static boolean isInlineContent(String mime) {
        if (mime == null) return false;
        return mime.startsWith("image/") || mime.equalsIgnoreCase("application/pdf");
    }
}
