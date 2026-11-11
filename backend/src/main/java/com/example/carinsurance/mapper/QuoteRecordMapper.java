package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.QuoteRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报价记录 Mapper（对应表 {@code quote_record}）。
 */
@Mapper
public interface QuoteRecordMapper extends BaseMapper<QuoteRecord> {
}
