package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.HandoffEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人工接管事件时间线 Mapper（对应表 {@code handoff_event}）。
 */
@Mapper
public interface HandoffEventMapper extends BaseMapper<HandoffEvent> {
}
