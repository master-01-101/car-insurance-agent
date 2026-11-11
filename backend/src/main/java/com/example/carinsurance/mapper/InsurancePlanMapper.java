package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.InsurancePlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 保险方案（只追加，版本化） Mapper（对应表 {@code insurance_plan}）。
 */
@Mapper
public interface InsurancePlanMapper extends BaseMapper<InsurancePlan> {
}
