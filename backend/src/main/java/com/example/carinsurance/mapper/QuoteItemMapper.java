package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.QuoteItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报价险种明细 Mapper（对应表 {@code quote_item}）。
 */
@Mapper
public interface QuoteItemMapper extends BaseMapper<QuoteItem> {
}
