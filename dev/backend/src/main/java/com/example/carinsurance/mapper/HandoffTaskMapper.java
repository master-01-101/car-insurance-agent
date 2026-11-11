package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.HandoffTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人工接管工单 Mapper（对应表 {@code handoff_task}）。
 */
@Mapper
public interface HandoffTaskMapper extends BaseMapper<HandoffTask> {
}
