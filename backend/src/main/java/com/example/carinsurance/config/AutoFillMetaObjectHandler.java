package com.example.carinsurance.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 负责 createdAt / updatedAt / version / deleted 的统一赋值
 */
@Component
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        // createdAt：所有表都有
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        // updatedAt：大部分表有（少数表如 handoff_event / audit_log / order_status_log 没有）
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // version：乐观锁列，初始值取各表默认值（大部分 0，少数默认 1）
        // 这里统一填 0；若表实际默认 1（如 conversation_party / vehicle_profile / kb_term 的 version 默认 1），
        // 在 Service 层创建时按需 override
        this.strictInsertFill(metaObject, "version", Integer.class, 0);
        // deleted：逻辑删除，只有 sys_user / vehicle 有
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // updatedAt：所有有 updatedAt 的表
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
