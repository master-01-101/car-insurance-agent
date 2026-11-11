package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.BatchJobItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批量任务明细 Mapper（对应表 {@code batch_job_item}）。
 */
@Mapper
public interface BatchJobItemMapper extends BaseMapper<BatchJobItem> {
}
