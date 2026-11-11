package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一文件元数据表（不存二进制）。
 * 对应表 file_object。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file_object")
public class FileObject {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件业务编号（接口 fileId），唯一 */
    private String fileNo;

    /** 所属会话 conversation.id */
    private Long conversationId;

    /** 上传人 sys_user.id */
    private Long uploaderUserId;

    /** 类型：LICENSE_FRONT/LICENSE_BACK/ID_FRONT/ID_BACK/INSPECTION_PHOTO/INSPECTION_VIDEO/CHAT_IMAGE/OTHER */
    private String category;

    /** 证件归属角色：APPLICANT/INSURED/VEHICLE_OWNER */
    private String partyRole;

    /** 对象存储 key，不存二进制 */
    private String objectKey;

    /** 存储位置：LOCAL/OSS/COS */
    private String storage;

    /** 对象存储 bucket 名称 */
    private String bucket;

    /** 原始文件名 */
    private String fileName;

    /** MIME 类型 */
    private String mimeType;

    /** 文件大小（字节） */
    private Long sizeBytes;

    /** 图片宽度（像素） */
    private Integer width;

    /** 图片高度（像素） */
    private Integer height;

    /** 文件摘要，用于秒传/去重（SHA256，64位十六进制） */
    private String sha256;

    /** 同一材料重传版本 */
    private Integer revision;

    /** 状态：UPLOADED/PROCESSING/PARSED/CONFIRMED/REJECTED */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
