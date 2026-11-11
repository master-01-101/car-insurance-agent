package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.UnderwritingRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 核保记录 Mapper（对应表 {@code underwriting_record}）。
 */
@Mapper
public interface UnderwritingRecordMapper extends BaseMapper<UnderwritingRecord> {
}
