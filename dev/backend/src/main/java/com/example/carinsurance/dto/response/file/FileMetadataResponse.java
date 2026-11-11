package com.example.carinsurance.dto.response.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件元数据响应（不含二进制）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataResponse {

    private Long fileId;
    private String fileNo;
    private Long conversationId;

    /** 文件用途：LICENSE_FRONT/LICENSE_BACK/ID_FRONT/ID_BACK/... */
    private String category;

    /** 证件归属角色：APPLICANT/INSURED/VEHICLE_OWNER，可为 null */
    private String partyRole;

    /** 原始文件名（脱敏后的安全版本） */
    private String fileName;

    /** MIME 类型 */
    private String mimeType;

    /** 文件大小（字节） */
    private Long sizeBytes;

    /** SHA256 摘要（64 位十六进制），用于去重/秒传 */
    private String sha256;

    /** 状态：UPLOADED/PROCESSING/PARSED/CONFIRMED/REJECTED */
    private String status;

    /** 上传时间（yyyy-MM-dd HH:mm:ss） */
    private LocalDateTime createdAt;
}
