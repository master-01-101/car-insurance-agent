package com.example.carinsurance.service;

import com.example.carinsurance.common.ResultCode;
import com.example.carinsurance.config.FileProperties;
import com.example.carinsurance.dto.response.file.FileMetadataResponse;
import com.example.carinsurance.entity.Conversation;
import com.example.carinsurance.entity.FileObject;
import com.example.carinsurance.exception.BusinessException;
import com.example.carinsurance.file.FileStorageService;
import com.example.carinsurance.mapper.FileObjectMapper;
import com.example.carinsurance.security.CurrentUser;
import com.example.carinsurance.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

/**
 * 文件管理 Service。
 * <p>职责：上传（存储 + DB 元数据 + 权限校验 + 失败回滚）、查询元数据、下载/预览内容、内部 AI 读取。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileStorageService storage;
    private final FileProperties props;
    private final FileObjectMapper fileMapper;
    private final ConversationService conversationService;

    /** 业务允许的 category 值（与 DB 设计保持一致，大小写敏感） */
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "LICENSE_FRONT", "LICENSE_BACK",
            "ID_FRONT", "ID_BACK",
            "INSPECTION_PHOTO", "INSPECTION_VIDEO",
            "CHAT_IMAGE", "OTHER"
    );

    /**
     * 上传文件。
     *
     * @param file          上传文件本体（MultipartFile）
     * @param conversationId 关联会话（必填）
     * @param category      文件用途（必填，见 ALLOWED_CATEGORIES）
     * @param partyRole     可选，证件归属角色（APPLICANT/INSURED/VEHICLE_OWNER）
     */
    @Transactional
    public FileMetadataResponse upload(MultipartFile file, Long conversationId, String category, String partyRole) {
        CurrentUser me = SecurityUtils.getCurrentUser();

        // 1. 会话归属校验 + 可写校验
        Conversation conv = conversationService.getConversationEntity(conversationId);
        conversationService.checkOwnership(conv);
        conversationService.checkWritable(conv);

        // 2. category 校验
        if (category == null || !ALLOWED_CATEGORIES.contains(category)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "不支持的文件 category，允许值：" + ALLOWED_CATEGORIES);
        }

        // 3. 文件基本校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "空文件不允许上传");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "文件名为空");
        }

        // 4. 大小校验
        long size = file.getSize();
        if (size > props.getMaxSizeBytes()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    String.format("文件过大（%.2f MB > 上限 %.2f MB）",
                            size / 1024.0 / 1024, props.getMaxSizeBytes() / 1024.0 / 1024));
        }

        // 5. 扩展名 + MIME 校验（双重校验，防止伪装）
        String ext = extractExt(originalName);
        if (ext == null || !isAllowedExt(ext)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "不支持的文件扩展名：." + (ext == null ? "（无）" : ext));
        }
        String mime = file.getContentType();
        if (mime != null && !isAllowedMime(mime)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "不支持的 MIME 类型：" + mime);
        }

        // 6. 生成对象存储 key：yyyy/MM/{uuid}.{ext}
        String datePath = LocalDate.now().toString().replace("-", "/");
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectKey = datePath + "/" + uuid + "." + ext.toLowerCase();

        // 7. 存储文件 + 计算 SHA256（在同一个 try 里，任何异常都清理已存文件）
        String storedPath;
        String sha256;
        try (InputStream in = file.getInputStream()) {
            sha256 = computeSha256(in);
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }
        // 存储（SHA256 计算时已经读完流了，需要重新拿一个流）
        try (InputStream in2 = file.getInputStream()) {
            storedPath = storage.store(in2, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("文件存储失败", e);
        }

        // 8. 写 DB 元数据
        try {
            FileObject meta = FileObject.builder()
                    .fileNo("FILE_" + uuid.substring(0, 24))
                    .conversationId(conversationId)
                    .uploaderUserId(me.getId())
                    .category(category)
                    .partyRole(partyRole)
                    .objectKey(storedPath)
                    .storage("LOCAL")
                    .fileName(sanitizeFileName(originalName))
                    .mimeType(mime)
                    .sizeBytes(size)
                    .sha256(sha256)
                    .revision(1)
                    .status("UPLOADED")
                    .build();
            fileMapper.insert(meta);

            // 9. 清理孤儿文件：DB 成功后文件就不是孤儿了，无需额外处理
            return toResponse(meta);
        } catch (Exception e) {
            // DB 保存失败 → 清理已存文件，防止孤儿
            log.warn("DB 保存失败，清理已存文件 storedPath={}", storedPath);
            try { storage.delete(storedPath); } catch (Exception ignored) {}
            throw e;
        }
    }

    /**
     * 查询文件元数据（需权限校验）。
     */
    public FileMetadataResponse getMetadata(Long fileId) {
        FileObject f = fileMapper.selectById(fileId);
        if (f == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        checkFileOwnership(f);
        return toResponse(f);
    }

    /**
     * 读取文件内容（下载/预览）。返回存储对象和 MIME，由 Controller 输出。
     */
    public FileStream readFile(Long fileId) {
        FileObject f = fileMapper.selectById(fileId);
        if (f == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        checkFileOwnership(f);

        InputStream in = storage.read(f.getObjectKey());
        return new FileStream(in, f.getMimeType(), f.getFileName(), f.getSizeBytes());
    }

    /**
     * 内部 AI 服务读取：校验存储位置 → 直接返回文件流。
     * AI 服务通过 Java 后端代理读取，不暴露本地路径或临时 URL。
     */
    public FileStream readFileForInternal(Long fileId, String expectedToken) {
        // 简化：当前阶段只做服务端代理，不额外校验内部 token（后续 OSS 临时 URL 时补）
        FileObject f = fileMapper.selectById(fileId);
        if (f == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        // 内部调用不需要归属校验（AI 服务视为受信调用方，最终业务归属由上层保证）
        InputStream in = storage.read(f.getObjectKey());
        return new FileStream(in, f.getMimeType(), f.getFileName(), f.getSizeBytes());
    }

    // ---------- 权限校验 ----------

    /**
     * 校验当前用户是否有权访问指定文件。
     * 文件归属：通过 conversation_id 关联到会话 → 会话归属当前用户。
     */
    private void checkFileOwnership(FileObject f) {
        CurrentUser me = SecurityUtils.getCurrentUser();
        if ("ADMIN".equals(me.getUserType())) return;

        // 直接归属：uploaderUserId 是自己
        if (f.getUploaderUserId().equals(me.getId())) return;

        // 会话归属：文件关联的会话归属当前用户
        if (f.getConversationId() != null) {
            Conversation conv = conversationService.getConversationEntity(f.getConversationId());
            conversationService.checkOwnership(conv); // 会抛 FORBIDDEN
            return;
        }

        throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该文件");
    }

    // ---------- 辅助 ----------

    private FileMetadataResponse toResponse(FileObject f) {
        return FileMetadataResponse.builder()
                .fileId(f.getId())
                .fileNo(f.getFileNo())
                .conversationId(f.getConversationId())
                .category(f.getCategory())
                .partyRole(f.getPartyRole())
                .fileName(f.getFileName())
                .mimeType(f.getMimeType())
                .sizeBytes(f.getSizeBytes())
                .sha256(f.getSha256())
                .status(f.getStatus())
                .createdAt(f.getCreatedAt())
                .build();
    }

    private boolean isAllowedExt(String ext) {
        for (String ae : props.getAllowedExtensions()) {
            if (ae.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    private boolean isAllowedMime(String mime) {
        for (String prefix : props.getAllowedMimePrefixes()) {
            if (mime.toLowerCase().startsWith(prefix.toLowerCase())) return true;
        }
        return false;
    }

    private String extractExt(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i < 0 || i == fileName.length() - 1) return null;
        // 防止路径穿越：只取最后一个段的扩展名
        String lastSeg = fileName.substring(i + 1);
        if (lastSeg.contains("/") || lastSeg.contains("\\")) return null;
        return lastSeg.toLowerCase(Locale.ROOT);
    }

    /** 保留原始文件名的安全清理（去控制字符）。 */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
    }

    private String computeSha256(InputStream in) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            md.update(buf, 0, n);
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** 文件流包装（Controller 输出用，调用方负责关闭流）。 */
    public record FileStream(InputStream inputStream, String mimeType, String originalName, Long sizeBytes) {}
}
