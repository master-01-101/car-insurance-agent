package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper（对应表 {@code audit_log}）。
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
