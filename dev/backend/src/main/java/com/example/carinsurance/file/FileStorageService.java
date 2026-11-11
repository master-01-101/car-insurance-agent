package com.example.carinsurance.file;

import com.example.carinsurance.entity.FileObject;

import java.io.InputStream;

/**
 * 文件存储统一接口 —— 实现 LOCAL / OSS / COS 等后端切换。
 * <p>本阶段只实现 LOCAL，OSS 后续引入时新增 @ConditionalOnProperty 类即可。</p>
 */
public interface FileStorageService {

    /**
     * 存储文件二进制。
     *
     * @param inputStream 文件流（由调用方负责关闭）
     * @param objectKey   相对存储路径（如 "2026/11/uuid.ext"），不包含 bucket
     * @return 实际存储路径（LOCAL 返回绝对路径，OSS 返回 objectKey）
     */
    String store(InputStream inputStream, String objectKey);

    /**
     * 读取文件流。
     *
     * @param objectKey 存储路径
     */
    InputStream read(String objectKey);

    /**
     * 检查文件是否存在。
     */
    boolean exists(String objectKey);

    /**
     * 删除文件（供异常回滚用，不对前端开放）。
     */
    void delete(String objectKey);

    /**
     * 生成本地预览/下载的内部访问路径（服务端转发用，不暴露给前端）。
     */
    String getInternalAccessPath(String objectKey);
}
