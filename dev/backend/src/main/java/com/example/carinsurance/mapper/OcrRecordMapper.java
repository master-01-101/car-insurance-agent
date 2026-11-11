package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.OcrRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * OCR 识别记录 Mapper（对应表 ocr_record）。
 */
@Mapper
public interface OcrRecordMapper extends BaseMapper<OcrRecord> {
}
