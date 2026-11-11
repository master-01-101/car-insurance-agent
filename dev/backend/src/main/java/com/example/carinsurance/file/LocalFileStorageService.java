package com.example.carinsurance.file;

import com.example.carinsurance.config.FileProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

/**
 * 本地文件存储实现。
 * <p>存储目录：{localPath}/{yyyy}/{MM}/{uuid}.{ext}。</p>
 * <p>绝对物理路径存入 DB file_object.object_key（LOCAL 模式下 object_key 语义为绝对路径）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.file-storage", name = "storage", havingValue = "LOCAL", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final FileProperties props;
    private Path rootDir;

    @PostConstruct
    public void init() {
        String path = props.getLocalPath();
        if (path == null || path.isBlank()) {
            path = System.getProperty("java.io.tmpdir") + File.separator + "car-insurance-files";
        }
        this.rootDir = Paths.get(path).toAbsolutePath();
        try {
            Files.createDirectories(rootDir);
            log.info("本地文件存储根目录：{}", rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建文件存储目录：" + rootDir, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String objectKey) {
        Path target = rootDir.resolve(objectKey).normalize();
        // 防止路径穿越：normalize 后仍必须在 rootDir 下
        if (!target.startsWith(rootDir)) {
            throw new IllegalArgumentException("非法存储路径：" + objectKey);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("文件存储失败：" + target, e);
        }
        // LOCAL 模式：object_key 存相对根目录的路径（方便迁移），
        // 拼接 rootDir 后就是绝对物理路径
        return target.toAbsolutePath().toString();
    }

    @Override
    public InputStream read(String objectKey) {
        Path file = resolveFromRoot(objectKey);
        try {
            return Files.newInputStream(file);
        } catch (java.nio.file.NoSuchFileException e) {
            throw new IllegalArgumentException("文件不存在：" + file);
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败：" + file, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(resolveFromRoot(objectKey));
    }

    @Override
    public void delete(String objectKey) {
        Path file = resolveFromRoot(objectKey);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("文件删除失败（可忽略）：{}", file, e);
        }
    }

    @Override
    public String getInternalAccessPath(String objectKey) {
        return resolveFromRoot(objectKey).toString();
    }

    /**
     * DB 里 object_key 可能存绝对路径（store 返回值）或相对路径。
     * 绝对路径 → 直接用；相对路径 → 拼 rootDir。
     */
    private Path resolveFromRoot(String objectKey) {
        Path p = Paths.get(objectKey);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return rootDir.resolve(objectKey).normalize();
    }
}
