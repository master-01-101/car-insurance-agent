package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.BatchJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批量任务 Mapper（对应表 {@code batch_job}）。
 */
@Mapper
public interface BatchJobMapper extends BaseMapper<BatchJob> {
}
