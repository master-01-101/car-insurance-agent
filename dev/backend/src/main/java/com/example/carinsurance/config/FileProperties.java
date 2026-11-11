package com.example.carinsurance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置属性（前缀 app.file-storage）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.file-storage")
public class FileProperties {

    /** 存储类型：LOCAL / OSS（当前仅 LOCAL 实现） */
    private String storage = "LOCAL";

    /** 本地存储根目录（绝对路径），留空则用系统临时目录下的 car-insurance-files */
    private String localPath;

    /** 允许的 MIME 类型前缀（逗号分隔），支持图片 + PDF */
    private String[] allowedMimePrefixes = {"image/", "application/pdf"};

    /** 允许的文件扩展名（小写，不含点） */
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "webp", "pdf"};

    /** 单文件最大字节数（默认 10MB） */
    private long maxSizeBytes = 10L * 1024 * 1024;

    /** AI 服务临时读取 URL 有效期（分钟） */
    private int aiTempUrlExpireMinutes = 10;
}
