package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.IdempotencyKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录 Mapper（对应表 {@code idempotency_key}）。
 */
@Mapper
public interface IdempotencyKeyMapper extends BaseMapper<IdempotencyKey> {
}
