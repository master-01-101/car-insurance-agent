package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.InsurancePlanItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 方案险种明细 Mapper（对应表 {@code insurance_plan_item}）。
 */
@Mapper
public interface InsurancePlanItemMapper extends BaseMapper<InsurancePlanItem> {
}
